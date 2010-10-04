
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <sys/time.h>
#include <sys/resource.h>

#include <glibtop.h>
#include <glibtop/procmem.h>
#include <glibtop/cpu.h>

//#include <glibtop/fsusage.h>
//#include <glibtop/mountlist.h>

int debug = 0;

void usage(char *prog_name)
{
    fprintf(stderr, "usage: %s cmd_to_measure [ cmd_arg1 ... ]\n", prog_name);
}


char *make_cmd(int n, char *words[])
{
    int i, ret;
    int first = 1;
    char *cmd = NULL;
    for (i = 0; i < n; i++) {
        if (first) {
            first = 0;
            ret = asprintf(&cmd, "'%s'", words[i]);
        } else {
            ret = asprintf(&cmd, "%s '%s'", cmd, words[i]);
        }
        if ((ret == -1) || (cmd == NULL)) {
            return NULL;
        }
    }
    return cmd;
}


int rusage_all_zeros(struct rusage *r)
{
    if (r->ru_maxrss != 0) return 0;
    if (r->ru_ixrss != 0) return 0;
    if (r->ru_idrss != 0) return 0;
    if (r->ru_isrss != 0) return 0;
    if (r->ru_minflt != 0) return 0;
    if (r->ru_majflt != 0) return 0;
    if (r->ru_nswap != 0) return 0;
    if (r->ru_inblock != 0) return 0;
    if (r->ru_oublock != 0) return 0;
    if (r->ru_msgsnd != 0) return 0;
    if (r->ru_msgrcv != 0) return 0;
    if (r->ru_nsignals != 0) return 0;
    if (r->ru_nvcsw != 0) return 0;
    if (r->ru_nivcsw != 0) return 0;
    return 1;
}


void print_rusage(FILE *f, struct rusage *r)
{
    fprintf(f, " maxrss=%ld", r->ru_maxrss);
    fprintf(f, " ixrss=%ld", r->ru_ixrss);
    fprintf(f, " idrss=%ld", r->ru_idrss);
    fprintf(f, " isrss=%ld", r->ru_isrss);
    fprintf(f, " minflt=%ld", r->ru_minflt);
    fprintf(f, " majflt=%ld", r->ru_majflt);
    fprintf(f, " nswap=%ld", r->ru_nswap);
    fprintf(f, " inblock=%ld", r->ru_inblock);
    fprintf(f, " oublock=%ld", r->ru_oublock);
    fprintf(f, " msgsnd=%ld", r->ru_msgsnd);
    fprintf(f, " msgrcv=%ld", r->ru_msgrcv);
    fprintf(f, " nsignals=%ld", r->ru_nsignals);
    fprintf(f, " nvcsw=%ld", r->ru_nvcsw);
    fprintf(f, " nivcsw=%ld", r->ru_nivcsw);
}


void print_rusage_verbose(FILE *f, struct rusage *r)
{
    fprintf(f, "%10ld  maximum resident set size (bytes)\n", r->ru_maxrss);
    fprintf(f, "%10ld  average shared memory size\n", r->ru_ixrss);
    fprintf(f, "%10ld  average unshared data size\n", r->ru_idrss);
    fprintf(f, "%10ld  average unshared stack size\n", r->ru_isrss);
    fprintf(f, "%10ld  page reclaims\n", r->ru_minflt);
    fprintf(f, "%10ld  page faults\n", r->ru_majflt);
    fprintf(f, "%10ld  swaps\n", r->ru_nswap);
    fprintf(f, "%10ld  block input operations\n", r->ru_inblock);
    fprintf(f, "%10ld  block output operations\n", r->ru_oublock);
    fprintf(f, "%10ld  messages sent\n", r->ru_msgsnd);
    fprintf(f, "%10ld  messages received\n", r->ru_msgrcv);
    fprintf(f, "%10ld  signals received\n", r->ru_nsignals);
    fprintf(f, "%10ld  voluntary context switches\n", r->ru_nvcsw);
    fprintf(f, "%10ld  involuntary context switches\n", r->ru_nivcsw);
}


guint64 global_maxmem;
int stop_polling;
int polling_stopped;


void do_polling(void *arg)
{
    glibtop_proc_mem buf;
    useconds_t poll_period_usec = 200 * 1000;  // 200 msec
    struct rusage r;

    pid_t pid = (pid_t) arg;

    while (!stop_polling) {
        glibtop_get_proc_mem(&buf, pid);
        if (debug) {
            fprintf(stderr, "buf.resident=%llu", buf.resident);
        }
        if (buf.resident > global_maxmem) {
            global_maxmem = buf.resident;
        }
        int ret = usleep(poll_period_usec);
        if (ret != 0) {
            fprintf(stderr, "usleep(%d) returned %d\n", poll_period_usec, ret);
            break;
        }
    }
    if (debug) {
        fprintf(stderr, "do_polling: stopped polling\n");
    }
    polling_stopped = 1;
    pthread_exit(0);
}


guint64 timeval_diff_msec(struct timeval *t1, struct timeval *t2)
{
    guint64 ret;
    ret = 1000 * (t2->tv_sec - t1->tv_sec);
    suseconds_t diff_usec;
    if (t2->tv_usec >= t1->tv_usec) {
        diff_usec = t2->tv_usec - t1->tv_usec;
    } else {
        ret -= 1000;
        diff_usec = (t2->tv_usec + 1000000) - t1->tv_usec;
    }
    if (debug) {
        fprintf(stderr, "timeval_diff_msec:\n");
        fprintf(stderr, "   t1->tv_sec=%10ld usec=%6d\n", t1->tv_sec, (unsigned int) t1->tv_usec);
        fprintf(stderr, "   t2->tv_sec=%10ld usec=%6d\n", t2->tv_sec, (unsigned int) t2->tv_usec);
        fprintf(stderr, "   ret=%lld diff_usec=%d\n", ret, (unsigned int) diff_usec);
    }
    ret += (diff_usec / 1000);
    return ret;
}


int main(int argc, char **argv, char **envp)
{
    if (argc == 1) {
        usage(argv[0]);
        exit(1);
    }

    //char *cmd = make_cmd(argc-1, &(argv[1]));
    //if (cmd == NULL) {
    //    fprintf(stderr, "Could not allocate memory for cmd.  Aborting.\n");
    //    exit(1);
    //}
    //fprintf(stderr, "cmd='%s'\n", cmd);

    int child_argc = argc-1;
    char **child_argv = (char **) malloc((unsigned) argc * sizeof(char *));
    int i;
    for (i = 1; i < argc; i++) {
        child_argv[i-1] = argv[i];
    }
    child_argv[argc-1] = NULL;
    // char **p;
    // for (p = child_argv; *p != NULL; p++) {
    //     fprintf(stderr, " p[%d]='%s'", p-child_argv, *p);
    // }
    // fprintf(stderr, "\n");

    glibtop_cpu cpu_start_buf;
    glibtop_cpu cpu_end_buf;
    struct timeval start_time;
    struct timeval end_time;

    glibtop_init();
    glibtop_get_cpu(&cpu_start_buf);
    int ret = gettimeofday(&start_time, NULL);
    // tbd: check ret

    pid_t pid = fork();
    if (pid == -1) {
        fprintf(stderr, "Error return status -1 while attempting"
                " to call fork().  errno=%d\n", errno);
        perror(argv[0]);
        exit(3);
    } else if (pid == 0) {

        // We are the child process
        int ret = execvp(child_argv[0], child_argv);
        // Normally the call above will not return.
        fprintf(stderr, "Error return status %d while attempting"
                " to call execvp().  errno=%d\n", ret, errno);
        perror(argv[0]);
        exit(2);

    } else {

        // We are the parent process.

        // Run a separate thread that periodically polls the child
        // process for its memory usage.
        pthread_t polling_thread;
        global_maxmem = 0;
        stop_polling = 0;
        polling_stopped = 0;
        int ret = pthread_create(&polling_thread, NULL, do_polling,
                                 (void *) pid);
        // tbd: check value of ret

        struct rusage r;

        //int wait_opts = WNOHANG;
        int wait_opts = 0;
        int wait_status;
        ret = wait4(pid, &wait_status, wait_opts, &r);
        if (ret == -1) {
            fprintf(stderr, "wait4() returned %d.  errno=%d\n", ret, errno);
            exit(5);
        }
        if (ret != pid) {
            fprintf(stderr, "wait4() returned pid=%d.  Expected pid"
                    " %d of child process.  Try again.\n", ret, pid);
            fprintf(stderr, "wait4->%d wait4 r.ru_maxrss=%ld",
                    ret, r.ru_maxrss);
            fprintf(stderr, "\n");
            exit(7);
        }

        if (debug) {
            fprintf(stderr, "wait4() returned pid=%d of child process."
                    "  Done!\n", pid);
        }
        ret = gettimeofday(&end_time, NULL);
        // tbd: check ret
	glibtop_get_cpu(&cpu_end_buf);

        // Tell the polling thread to stop, and wait for it to
        // acknowledge it has done so.
        if (debug) {
            fprintf(stderr, "parent requesting do_polling thread to stop\n");
        }
        stop_polling = 1;
        while (!polling_stopped) {
            usleep(1000);
        }
        if (debug) {
            fprintf(stderr, "parent received confirmation that do_polling thread has stopped\n");
        }

        // Show separate busy percentage for each CPU core
        if (debug) {
            fprintf(stderr, "ncpu=%d", glibtop_global_server->ncpu);
        }
        fprintf(stderr, "CPU utilization:");
        for (i = 0; i < glibtop_global_server->ncpu; i++) {
            guint64 total = (cpu_end_buf.xcpu_total[i] -
                             cpu_start_buf.xcpu_total[i]);
            int cpu_busy_percent = 0;
            if (total != 0) {
                guint64 idle = (cpu_end_buf.xcpu_idle[i] -
                                cpu_start_buf.xcpu_idle[i]);
                cpu_busy_percent =
                    (int) round(100.0 * (1.0 - ((float) idle)/total));
            }
            fprintf(stderr, " %d%%", cpu_busy_percent);
        }
        fprintf(stderr, "\n");

        // Elapsed time
        int elapsed_msec = timeval_diff_msec(&start_time, &end_time);
        fprintf(stderr, "real %9d.%03d\n", (elapsed_msec / 1000),
                (elapsed_msec % 1000));

        // User, sys times
        fprintf(stderr, "user %9ld.%03ld\n", r.ru_utime.tv_sec,
                r.ru_utime.tv_usec / 1000);
        fprintf(stderr, "sys  %9ld.%03ld\n", r.ru_stime.tv_sec,
                r.ru_stime.tv_usec / 1000);

        // Resource usage
        print_rusage_verbose(stderr, &r);

        // Difference between polled max resident memory used and that
        // returned by getrusage.
        if (global_maxmem < r.ru_maxrss) {
            // This seems to be common
            fprintf(stderr, "maximum resident set size seen by libgtop polling"
                    " was %lld less\n",
                    ((long long int) r.ru_maxrss) -
                    ((long long int) global_maxmem));
        } else {
            // I haven't seen this before
            fprintf(stderr, "maximum resident set size seen by libgtop polling"
                    " was %lld more\n",
                    ((long long int) global_maxmem) -
                    ((long long int) r.ru_maxrss));
        }
        //fprintf(stderr, "maxmem=%lld ru_maxrss-maxmem=%lld  %s\n",
        //        global_maxmem,
        //        ((long long int) r.ru_maxrss) - ((long long int) global_maxmem),
        //        (r.ru_maxrss == global_maxmem) ? "same" : "different");
        glibtop_close();
    }

    return 0;
}

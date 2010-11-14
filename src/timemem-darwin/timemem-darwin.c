#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <errno.h>
#include <mach/mach_host.h>


int debug = 0;


typedef unsigned long long uint64;

typedef struct cpu_usage_t {
    uint64 frequency;
    uint64 user;
    uint64 nice;
    uint64 sys;
    uint64 idle;
    uint64 total;
} cpu_usage;


unsigned int
get_num_cpus (void)
{
    unsigned int num_cpus;
    processor_cpu_load_info_data_t *proc_info;
    mach_msg_type_number_t proc_info_size;

    if (host_processor_info(mach_host_self(),
                            PROCESSOR_CPU_LOAD_INFO,
                            &num_cpus,
                            (processor_info_array_t *) &proc_info,
                            &proc_info_size))
    {
        return 0;
    }
    vm_deallocate(mach_task_self(), (vm_address_t) proc_info, proc_info_size);
    return num_cpus;
}


cpu_usage *
malloc_cpu_stats (unsigned int num_cpus)
{
    return (cpu_usage *) malloc((unsigned int) num_cpus * sizeof(cpu_usage));
}


void
get_cpu_usage (unsigned int num_cpus, cpu_usage info[], cpu_usage *total)
{
    unsigned int num_cpus_temp;
    processor_cpu_load_info_data_t *proc_info;
    mach_msg_type_number_t proc_info_size;
    int i;
    
    for (i = 0; i < num_cpus; i++) {
        bzero(&(info[i]), sizeof(cpu_usage));
    }
    if (total != NULL) {
        bzero(total, sizeof(cpu_usage));
    }
    
    if (host_processor_info(mach_host_self(),
                            PROCESSOR_CPU_LOAD_INFO,
                            &num_cpus_temp,
                            (processor_info_array_t *) &proc_info,
                            &proc_info_size))
    {
        return;
    }
    if (num_cpus != num_cpus_temp) {
        return;
    }
    for (i = 0; i < num_cpus; i++) {
        info[i].user  = proc_info[i].cpu_ticks[CPU_STATE_USER];
        info[i].sys   = proc_info[i].cpu_ticks[CPU_STATE_SYSTEM];
        info[i].idle  = proc_info[i].cpu_ticks[CPU_STATE_IDLE];
        info[i].nice  = proc_info[i].cpu_ticks[CPU_STATE_NICE];
        info[i].total = (info[i].user + info[i].sys +
                         info[i].idle + info[i].nice);
        info[i].frequency = 100;
        if (total != NULL) {
            total->user += info[i].user;
            total->sys += info[i].sys;
            total->idle += info[i].idle;
            total->nice += info[i].nice;
            total->total += info[i].total;
        }
    }
    vm_deallocate(mach_task_self(), (vm_address_t) proc_info, proc_info_size);
    if (total != NULL) {
        total->frequency = 100;
    }
}


uint64 timeval_diff_msec(struct timeval *t1, struct timeval *t2)
{
    uint64 ret;
    ret = 1000 * (t2->tv_sec - t1->tv_sec);
    suseconds_t diff_usec;
    if (t2->tv_usec >= t1->tv_usec) {
        diff_usec = t2->tv_usec - t1->tv_usec;
    } else {
        ret -= 1000;
        diff_usec = (t2->tv_usec + 1000000) - t1->tv_usec;
    }
    // if (debug) {
    //     fprintf(stderr, "timeval_diff_msec:\n");
    //     fprintf(stderr, "   t1->tv_sec=%10ld usec=%6d\n", t1->tv_sec, (unsigned int) t1->tv_usec);
    //     fprintf(stderr, "   t2->tv_sec=%10ld usec=%6d\n", t2->tv_sec, (unsigned int) t2->tv_usec);
    //     fprintf(stderr, "   ret=%lld diff_usec=%d\n", ret, (unsigned int) diff_usec);
    // }
    ret += (diff_usec / 1000);
    return ret;
}


void usage(char *prog_name)
{
    fprintf(stderr, "usage: %s cmd_to_measure [ cmd_arg1 ... ]\n", prog_name);
}


int
main (int argc, char **argv, char **envp)
{
    if (argc == 1) {
        usage(argv[0]);
        exit(1);
    }

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

    struct timeval start_time;
    struct timeval end_time;
    unsigned int num_cpus = get_num_cpus();
    cpu_usage *total_cpu_stats_start = malloc_cpu_stats(1);
    cpu_usage *per_cpu_stats_start = malloc_cpu_stats(num_cpus);
    cpu_usage *total_cpu_stats_end = malloc_cpu_stats(1);
    cpu_usage *per_cpu_stats_end = malloc_cpu_stats(num_cpus);

    get_cpu_usage(num_cpus, per_cpu_stats_start, total_cpu_stats_start);
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

        //int wait_opts = WNOHANG;
        int wait_opts = 0;
        int wait_status;
        struct rusage r;
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
        get_cpu_usage(num_cpus, per_cpu_stats_end, total_cpu_stats_end);

        // Elapsed time
        int elapsed_msec = timeval_diff_msec(&start_time, &end_time);
        fprintf(stderr, "real %9d.%03d\n", (elapsed_msec / 1000),
                (elapsed_msec % 1000));

        // User, sys times
        fprintf(stderr, "user %9ld.%03d\n", r.ru_utime.tv_sec,
                r.ru_utime.tv_usec / 1000);
        fprintf(stderr, "sys  %9ld.%03d\n", r.ru_stime.tv_sec,
                r.ru_stime.tv_usec / 1000);

        // Maximum resident set size

        // TBD: This needs to be tested on other version of Mac OS X,
        // and especially when compiled and running as a 64-bit
        // process.  I'm not sure how to do that yet.

        // At least on the Intel Core 2 Duo Mac OS X 10.5.8 machine on
        // which I first tested this code, it seemed to give a value
        // of up to 2^31-4096 bytes correctly, but if it went a little
        // bit over that, the fprintf statement showed it as 0, not
        // 2^31 bytes.  For now, I'll do a special check for 0 and
        // print out what I believe to be the correct value.

        // One way to test this on that machine is as follows.  The
        // first command below prints 2^31-4096 bytes as the maximum
        // resident set size.  Without the "if" condition below, the
        // second command below prints 0 as the maximum resident set
        // size.

        // ./timemem-darwin ../../memuse/test-memuse 2096863
        // ./timemem-darwin ../../memuse/test-memuse 2096864

        // Reference:
        // http://lists.apple.com/archives/darwin-kernel/2009/Mar/msg00005.html

        if (r.ru_maxrss == 0L) {
            // Print 2^31 bytes exactly
            fprintf(stderr, "2147483648  maximum resident set size\n");
        } else {
            fprintf(stderr, "%10lu  maximum resident set size\n",
                    (unsigned long) r.ru_maxrss);
        }

        // Show separate busy percentage for each CPU core
        fprintf(stderr, "Per core CPU utilization (%d cores):", num_cpus);
        for (i = 0; i < num_cpus; i++) {
            uint64 total = (per_cpu_stats_end[i].total -
                            per_cpu_stats_start[i].total);
            int cpu_busy_percent = 0;
            if (total != 0) {
                uint64 idle = (per_cpu_stats_end[i].idle -
                               per_cpu_stats_start[i].idle);
                cpu_busy_percent =
                    (int) round(100.0 * (1.0 - ((float) idle)/total));
            }
            fprintf(stderr, " %d%%", cpu_busy_percent);
        }
        fprintf(stderr, "\n");

        if (WIFEXITED(wait_status)) {
            // Exit with the same status that the child process did.
            exit(WEXITSTATUS(wait_status));
        } else if (WIFSIGNALED(wait_status)) {
            fprintf(stderr,
                    "Command stopped due to signal %d without calling exit().\n",
                    WTERMSIG(wait_status));
            exit(1);
        } else {
            fprintf(stderr,
                    "Command is stopped due to signal %d, and can be restarted.\n",
                    WSTOPSIG(wait_status));
            exit(2);
        }
    }

    return 0;
}

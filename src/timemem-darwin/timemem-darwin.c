#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <signal.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <errno.h>
#include <mach/mach.h>


typedef long long int64;
typedef unsigned long long uint64;

int64 timeval_diff_msec(struct timeval *t1, struct timeval *t2);

int debug = 0;
char *global_prog_name;

int global_sigalrm_handled;

struct poll_state {
    int use_polling;
    int first_poll;
    int verbose_poll;
    pid_t pid_to_poll;
    long num_polls;
    int64 consecutive_poll_separation_min_msec;
    int64 consecutive_poll_separation_max_msec;
    task_port_t tp;

    int wait4_returned_normally;
    int task_info_errored;
    struct timeval task_info_error_time;
    struct timeval prev_poll_time;
    struct timeval poll_time_when_maxrss_first_seen;
    natural_t max_rss_bytes;
};


typedef struct cpu_usage_t {
    uint64 frequency;
    uint64 user;
    uint64 nice;
    uint64 sys;
    uint64 idle;
    uint64 total;
} cpu_usage;


void
cleanup_temp_file (char *tmp_name)
{
    int ret = unlink(tmp_name);
    if (ret == -1) {
        perror(global_prog_name);
        exit(1);
    }
}


int
get_macosx_version (int *major_version_num, int *minor_version_num,
                    int *subminor_version_num)
{
    int exit_status = 1;
    char *tmp_name = tmpnam(NULL);
    if (tmp_name == NULL) {
        perror(global_prog_name);
        goto error_cleanup;
    }
    char *uname_cmd;
    int ret = asprintf(&uname_cmd, "uname -r > %s", tmp_name);
    if (ret == -1 || uname_cmd == NULL) {
        fprintf(stderr, "Could not allocate enough memory for command string: uname -r > %s\n", tmp_name);
        goto error_cleanup;
    }
    ret = system(uname_cmd);
    if (ret != 0) {
        fprintf(stderr, "error in %s: system(\"%s\") returned %d\n",
                global_prog_name, uname_cmd, ret);
        goto error_cleanup;
    }
    FILE *f = fopen(tmp_name, "r");
    if (f == NULL) {
        perror(global_prog_name);
        goto error_cleanup;
    }
    char buf[512];
    if (fgets(buf, sizeof(buf), f) == NULL) {
        perror(global_prog_name);
        goto error_cleanup;
    }
    ret = sscanf(buf, "%d.%d.%d",
                 major_version_num, minor_version_num, subminor_version_num);
    fclose(f);
    cleanup_temp_file(tmp_name);
    if (ret < 2) {
        fprintf(stderr, "%s: Output of 'uname -r' was '%s', from which no major and minor version number could be parsed.\n",
                global_prog_name, buf);
        exit(1);
    }
    return ret;

 error_cleanup:
    if (tmp_name != NULL) {
        cleanup_temp_file(tmp_name);
    }
    exit(exit_status);
}


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
run_as_superuser_msg (char *prog_name)
{
    fprintf(stderr,
"%s: Error attempting to access memory usage of specified command.\n\
The most likely cause for this is that %s needs to run as root.\n\
It is recommended that you do one of the following:\n\
\n\
  (1) As root, make this command setuid root, so that it runs\n\
      with super-user permissions every time automatically.\n\
\n\
      %% sudo chown root %s\n\
      %% sudo chmod 4555 %s\n\
      %% ls -l %s\n\
      -r-sr-xr-x  1 root  andy  17368 Jan 24 23:16 timemem-darwin\n\
\n\
  (2) Run the program as root every time, e.g. by using sudo:\n\
\n\
      %% sudo %s <cmd> <args> ...\n\
\n\
Both of these methods present security risks if there are certain\n\
kinds of bugs or malicious code in this program.  You should have the\n\
source code for it, and can examine it if this concerns you.  Programs\n\
like top(1) and ps(1) are also setuid root for the same reason.\n",
            prog_name, prog_name, prog_name, prog_name,
            prog_name, prog_name);
}


void sigalrm_handler (int sig)
{
    global_sigalrm_handled = 1;
}


int
init_polling_process_rss (pid_t pid, struct poll_state *ps)
{
    kern_return_t error;

    ps->first_poll = 1;
    ps->verbose_poll = debug;
    ps->pid_to_poll = pid;
    ps->num_polls = 0L;
    ps->consecutive_poll_separation_min_msec = 1000000L;
    ps->consecutive_poll_separation_max_msec = -1000000L;

    ps->wait4_returned_normally = 0;
    ps->task_info_errored = 0;
    
    error = task_for_pid(mach_task_self(), pid, &(ps->tp));
    if (error != KERN_SUCCESS) {
        fprintf(stderr,
                "%s: task_for_pid() returned %d != %d == KERN_SUCCESS\n",
                global_prog_name, error, KERN_SUCCESS);
        return 1;
    }
    return 0;
}


int
get_task_info (struct poll_state *ps, struct task_basic_info *tasks_info)
{
    kern_return_t error;
    unsigned int info_count = TASK_BASIC_INFO_COUNT;

    error = task_info(ps->tp, TASK_BASIC_INFO,
                      (task_info_t) tasks_info, &info_count);
    if (error != KERN_SUCCESS) {
        if (error == KERN_INVALID_ARGUMENT) {
            // This seems to happen intermittently very near the end
            // of the child process's life, perhaps after it has
            // exited.  Try to mask out such intermittent errors.  At
            // least don't print any error messages to stderr here.
            return 2;
        }
        fprintf(stderr,
                "%s: task_info() returned %d != %d == KERN_SUCCESS\n",
                global_prog_name, error, KERN_SUCCESS);
        fprintf(stderr, "num_polls=%ld wait4_returned_normally=%d\n",
                ps->num_polls, ps->wait4_returned_normally);
        return 1;
    }
    return 0;
}


void
poll_process_rss (struct poll_state *ps)
{
    struct task_basic_info ti;
    int ret;

    ++ps->num_polls;
    ret = get_task_info(ps, &ti);
    if (ret != 0) {
        if ((ret == 2) && (ps->num_polls >= 2)) {
            // Then guess that this is not because of lack of
            // permissions, because we have successfully made the call
            // before.  Guess that this is because the child process
            // has exited already.  Unfortunately I do not see a good
            // way to determine that this has happened other than by
            // guessing, but there is probably a Mach way to do it
            // with some API call.

            // Record the time this has happened, so we can later tell
            // if it is close to the time the child process exits.
            // Also indicate that we should disable the timer now.
            ps->task_info_errored = 1;
            ret = gettimeofday(&(ps->task_info_error_time), NULL);
            // TBD: check ret?
            return;
        }
        run_as_superuser_msg(global_prog_name);
        exit(1);
    }
    natural_t rss_bytes = ti.resident_size;
    if (ps->verbose_poll) {
        double rss_mbytes = (double) rss_bytes / (1024.0 * 1024.0);
        printf("rss (mb)=%.1f\n", rss_mbytes);
    }
    struct timeval this_poll_time;
    ret = gettimeofday(&this_poll_time, NULL);
    if (ps->first_poll || (rss_bytes > ps->max_rss_bytes)) {
        ps->max_rss_bytes = rss_bytes;
        memcpy(&(ps->poll_time_when_maxrss_first_seen),
               &this_poll_time, sizeof(struct timeval));
    }
    if (!ps->first_poll) {
        int64 delta = timeval_diff_msec(&(ps->prev_poll_time),
                                        &this_poll_time);
        if (delta < ps->consecutive_poll_separation_min_msec) {
            ps->consecutive_poll_separation_min_msec = delta;
        }
        if (delta > ps->consecutive_poll_separation_max_msec) {
            ps->consecutive_poll_separation_max_msec = delta;
        }
    }
    ps->first_poll = 0;
    memcpy(&(ps->prev_poll_time), &this_poll_time, sizeof(struct timeval));
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


int64 timeval_diff_msec(struct timeval *t1, struct timeval *t2)
{
    int64 ret;
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


void
enable_timer (int timer_period_msec)
{
    struct itimerval timeout_val;

    // When, from now, the timer should first fire:
    timeout_val.it_value.tv_sec = 0;
    timeout_val.it_value.tv_usec = timer_period_msec * 1000;
    // How often the timer should periodically fire after the
    // first time.
    timeout_val.it_interval.tv_sec = 0;
    timeout_val.it_interval.tv_usec = timer_period_msec * 1000;
    int ret = setitimer(ITIMER_REAL, &timeout_val, NULL);
    if (ret != 0) {
        perror(global_prog_name);
        exit(1);
    }
}


void
disable_timer (void)
{
    struct itimerval timeout_val;
    
    timeout_val.it_value.tv_sec = 0;
    timeout_val.it_value.tv_usec = 0;
    timeout_val.it_interval.tv_sec = 0;
    timeout_val.it_interval.tv_usec = 0;
    int ret = setitimer(ITIMER_REAL, &timeout_val, NULL);
    if (ret != 0) {
        perror(global_prog_name);
        exit(1);
    }
}


void
enable_handling_sigalrm (void)
{
    struct sigaction sigalrm_action;

    sigalrm_action.sa_handler = sigalrm_handler;
    sigalrm_action.sa_flags = 0;
    sigalrm_action.sa_mask = 0;
    int ret = sigaction(SIGALRM, &sigalrm_action, NULL);
    if (ret != 0) {
        perror(global_prog_name);
        exit(1);
    }
}


void
ignore_sigalrm (void)
{
    struct sigaction sigalrm_action;
    
    sigalrm_action.sa_handler = SIG_IGN;
    sigalrm_action.sa_flags = 0;
    sigalrm_action.sa_mask = 0;
    int ret = sigaction(SIGALRM, &sigalrm_action, NULL);
    if (ret != 0) {
        perror(global_prog_name);
        exit(1);
    }
}


void usage(char *prog_name)
{
    fprintf(stderr, "usage: %s cmd_to_measure [ cmd_arg1 ... ]\n", prog_name);
}


int
main (int argc, char **argv, char **envp)
{
    global_prog_name = argv[0];
    if (argc == 1) {
        usage(global_prog_name);
        exit(1);
    }

    // Determine which version of OS X we are running on.
    int major_version_num;
    int minor_version_num;
    int subminor_version_num;
    get_macosx_version(&major_version_num, &minor_version_num,
                       &subminor_version_num);
    struct poll_state ps;
    ps.use_polling = 0;
    if (major_version_num >= 10) {
        // Mac OS X 10.5.* and earlier return a number that appears
        // correct from the ru_maxrss field of getrusage(), also
        // filled in by wait4().

        // Mac OS X 10.6.* always returns 0 for ru_maxrss, so we must
        // use a different method to measure it.
        ps.use_polling = 1;
    }

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
        perror(global_prog_name);
        exit(3);
    } else if (pid == 0) {

        // We are the child process

        // Set the uid to the original uid of the process that invoked
        // the timemem-darwin process, so that the command being
        // measured is run with that user's priviliges, not with root
        // privileges.
        int original_uid = getuid();
        int ret = setuid(original_uid);
        if (ret != 0) {
            fprintf(stderr, "Error return status %d while attempting"
                    " to set uid to %d.  errno=%d\n", ret, original_uid,
                    errno);
            perror(global_prog_name);
            exit(4);
        }

        ret = execvp(child_argv[0], child_argv);
        // Normally the call above will not return.
        fprintf(stderr, "Error return status %d while attempting"
                " to call execvp().  errno=%d\n", ret, errno);
        perror(global_prog_name);
        exit(2);

    }

    // We are the parent process.

    // We want to wait until the child process finishes, but we also
    // want to periodically poll the child process's resident set size
    // (memory usage).  On OS X 10.5.8 and earlier, simply using
    // wait4() for the child to finish would fill in the rusage struct
    // with the maximum resident set size, but this value is always
    // filled in with 0 in OS X 10.6, hence the use of polling.

    // We implement the polling by calling setitimer() so that we are
    // sent a SIGALRM signal every 100 msec.  This should cause
    // wait4() to return early.  We handle the signal, and then call
    // wait4() again.

    // Read the current maximum resident set size once before starting
    // the timer, because the most likely reason for it to fail is
    // that we are not running with root privileges.
    if (ps.use_polling) {
        if (init_polling_process_rss(pid, &ps) != 0) {
            run_as_superuser_msg(global_prog_name);
            exit(1);
        }
        poll_process_rss(&ps);
        
        // Set up the SIGALRM signal handler.
        global_sigalrm_handled = 0;
        enable_handling_sigalrm();
        
        // Set timer to send us a SIGALRM signal every 100 msec.
        int timer_period_msec = 100;
        enable_timer(timer_period_msec);
    }

    //int wait_opts = WNOHANG;
    int wait_opts = 0;
    int wait_status;
    int wait4_ret;
    struct rusage r;
    while (1) {
        wait4_ret = wait4(pid, &wait_status, wait_opts, &r);
        if (wait4_ret != -1) {
            break;
        }
        if ((errno == EINTR) && ps.use_polling) {
            // Most likely the SIGALRM timer signal was handled.  If
            // so, poll the child process's memory use once.  The
            // timer should automatically signal again periodically
            // without having to reset it.
            if (global_sigalrm_handled) {
                poll_process_rss(&ps);
                global_sigalrm_handled = 0;
                if (ps.task_info_errored) {
                    disable_timer();
                    ignore_sigalrm();
                }
            }
            // Go around and call wait4() again.
        } else {
            fprintf(stderr, "wait4() returned %d.  errno=%d\n",
                    wait4_ret, errno);
            perror(global_prog_name);
            exit(5);
        }
    }

    // We may not use end_time if there are errors we haven't checked
    // for yet from wait4(), but it is more accurate to call this as
    // soon after wait4() returns as we can.  It is out of the loop
    // above to avoid the overhead of calling it on every poll time.
    if (debug) {
        fprintf(stderr, "About to call gettimeofday()\n");
    }
    ret = gettimeofday(&end_time, NULL);
    // tbd: check ret
    get_cpu_usage(num_cpus, per_cpu_stats_end, total_cpu_stats_end);
    
    if (wait4_ret != pid) {
        fprintf(stderr, "wait4() returned pid=%d.  Expected pid"
                " %d of child process.  Try again.\n", wait4_ret, pid);
        fprintf(stderr, "wait4 r.ru_maxrss=%ld\n", r.ru_maxrss);
        exit(7);
    }
    ps.wait4_returned_normally = 1;
    if (debug) {
        fprintf(stderr, "wait4() returned pid=%d of child process."
                "  Done!\n", pid);
    }
    
    if (ps.use_polling) {
        // Disable the timer.  Ignore SIGALRM, too, just in case one
        // more happens.
        if (debug) {
            fprintf(stderr, "About to disable the timer\n");
        }
        disable_timer();
        if (debug) {
            fprintf(stderr, "About to ignore SIGALRM\n");
        }
        ignore_sigalrm();
    }

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
    
    if (! ps.use_polling) {
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
            fprintf(stderr, "2147483648  maximum resident set size from getrusage\n");
        } else {
            fprintf(stderr, "%10lu  maximum resident set size from getrusage\n",
                    (unsigned long) r.ru_maxrss);
        }
    }
    if (ps.use_polling) {
        long delta = (long) ps.max_rss_bytes - (long) r.ru_maxrss;
        fprintf(stderr, "%10lu  maximum resident set size from polling (%.1f MB, delta %ld bytes = %.1f MB)\n",
                (unsigned long) ps.max_rss_bytes,
                (double) ps.max_rss_bytes / (1024.0 * 1024.0),
                delta,
                (double) delta / (1024.0 * 1024.0));
        double elapsed_time_sec = (double) elapsed_msec / 1000.0;
        fprintf(stderr,
                "number of times rss polled=%ld, avg of %.1f times per second\n", 
                ps.num_polls,
                (double) ps.num_polls / elapsed_time_sec);
        fprintf(stderr,
                "time between consecutive polls (msec): min=%.1f max=%.1f\n",
                (double) ps.consecutive_poll_separation_min_msec,
                (double) ps.consecutive_poll_separation_max_msec);
        int64 max_rss_first_seen_msec =
            timeval_diff_msec(&start_time,
                              &(ps.poll_time_when_maxrss_first_seen));
        fprintf(stderr, "Max RSS observed %.1f sec after start time\n",
                (double) max_rss_first_seen_msec / 1000.0);
        if (ps.task_info_errored) {
            int64 diff_msec = timeval_diff_msec(&end_time,
                                                &(ps.task_info_error_time));
            if (diff_msec <= 0 && diff_msec >= -100) {
                // Then the error most likely occurred because the
                // child process had already exited.  Ignore it.
            } else {
                fprintf(stderr, "A call to task_info() returned an error.  error_time - end_time = %.3f sec.  This may mean the maximum resident set size measurement above is too low.\n",
                        (double) diff_msec / 1000.0);
            }
        }
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

    return 0;
}

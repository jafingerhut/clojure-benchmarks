#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <pthread.h>


typedef unsigned int uint32;
typedef unsigned long long int uint64;

int debug = 0;

struct thread_params {
    int thread_id;
    int test_type;
    long num_jobs;
    long job_size;
    void *data;
};

struct thread_params *tp;

#define DEFAULT_JOB_SIZE 10000000

#define TEST_TYPE_WRITE_32B_2PASS  0
#define TEST_TYPE_WRITE_64B_2PASS  1
#define TEST_TYPE_READ_32B_2PASS   2
#define TEST_TYPE_READ_64B_2PASS   3

typedef struct test_type_info_ {
    int test_type_code;
    char *short_name;
} test_type_info_t;

test_type_info_t test_type_info[] = {
    { TEST_TYPE_WRITE_32B_2PASS, "write_32b_2pass" },
    { TEST_TYPE_WRITE_64B_2PASS, "write_64b_2pass" },
    { TEST_TYPE_READ_32B_2PASS, "read_32b_2pass" },
    { TEST_TYPE_READ_64B_2PASS, "read_64b_2pass" },
};

#define NUM_TEST_TYPES (sizeof(test_type_info) / sizeof(test_type_info_t))


int
check_type (char *arg, int *test_type)
{
    int i;
    for (i = 0; i < NUM_TEST_TYPES; i++) {
        if (strcmp(arg, test_type_info[i].short_name) == 0) {
            *test_type = test_type_info[i].test_type_code;
            return 1;
        }
    }
    return 0;
}


void
print_test_types (FILE *f, char *separator)
{
    int i;
    for (i = 0; i < NUM_TEST_TYPES; i++) {
        if (i != 0) {
            fprintf(f, "%s", separator);
        }
        fprintf(f, "%s", test_type_info[i].short_name);
    }
}


void
usage (char *prog_name)
{
    fprintf(stderr,
"usage: %s type num-jobs job-size num-threads-in-parallel\n\
    type must be one of: ",
            prog_name);
    print_test_types(stderr, ", ");
    fprintf(stderr, "\n");
    fprintf(stderr,
"    all other arguments must be integers >= 1\n\
    num-jobs is the number of jobs in the list to perform\n\
    job-size is the number of steps in each job\n\
        0 means to use the default number of steps: %llu\n\
    num-threads-in-parallel is the number of threads to run in parallel\n",
            DEFAULT_JOB_SIZE);
}


int
check_decimal_arg (char *arg, int size, void *val)
{
    char *p;
    for (p = arg; *p != '\0'; p++) {
        if ('0' <= *p && *p <= '9') {
            // ok so far
        } else {
            return 0;
        }
    }
    switch (size) {
    case 1:
        *((int *) val) = atoi(arg);
        break;
    case 2:
        *((long *) val) = atol(arg);
        break;
    case 3:
        *((long long *) val) = atoll(arg);
        break;
    default:
        return 0;
        break;
    }
    return 1;
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
    if (debug) {
        fprintf(stderr, "timeval_diff_msec:\n");
        fprintf(stderr, "   t1->tv_sec=%10ld usec=%6d\n", t1->tv_sec, t1->tv_usec);
        fprintf(stderr, "   t2->tv_sec=%10ld usec=%6d\n", t2->tv_sec, t2->tv_usec);
        fprintf(stderr, "   ret=%lld diff_usec=%d\n", ret, diff_usec);
    }
    ret += (diff_usec / 1000);
    return ret;
}


#define MBYTE (1024 * 1024)

void
do_test_init_part (int test_type, long num_jobs, long job_size, int num_threads,
                   struct thread_params *tp)
{
    switch (test_type) {
    case TEST_TYPE_WRITE_32B_2PASS:
        {
            int i, j;
            uint32 *d;
            // Allocate 512 MB per thread
            for (i = 0; i < num_threads; i++) {
                tp[i].data = (uint32 *) malloc((unsigned) 512 * MBYTE);
            }
            // Initialize it, to be sure that it has been paged in
            // before the body of the test begins.
            for (i = 0; i < num_threads; i++) {
                d = (uint32 *) tp[i].data;
                for (j = 0; j < ((512 * MBYTE) / sizeof(uint32)); j++) {
                    d[j] = 0;
                }
            }
        }
        break;
    case TEST_TYPE_WRITE_64B_2PASS:
        {
            int i, j;
            uint64 *d;
            // Allocate 512 MB per thread
            for (i = 0; i < num_threads; i++) {
                tp[i].data = (uint64 *) malloc((unsigned) 512 * MBYTE);
            }
            // Initialize it, to be sure that it has been paged in
            // before the body of the test begins.
            for (i = 0; i < num_threads; i++) {
                d = (uint64 *) tp[i].data;
                for (j = 0; j < ((512 * MBYTE) / sizeof(uint64)); j++) {
                    d[j] = 0;
                }
            }
        }
        break;
    case TEST_TYPE_READ_32B_2PASS:
        break;
    case TEST_TYPE_READ_64B_2PASS:
        break;
    default:
        fprintf(stderr, "do_test_init_part(): Unknown test_type value %d\n",
                test_type);
        exit(1);
        break;
    }
}


void *
do_test_measured_part (void *param)
{
    struct thread_params *tp = param;

    switch (tp->test_type) {
    case TEST_TYPE_WRITE_32B_2PASS:
        {
            uint32 *d = (uint32 *) tp->data;
            // Use pass to ensure that we write different data to the
            // array each time through it, to avoid the possibility
            // that caching somehow detects that writes are
            // redundantly writing the same data that is already in
            // main memory.
            uint32 pass = 0;
            uint32 write_data;
            long num_jobs = tp->num_jobs;
            long job_size = tp->job_size;
            int arr_size = (512 * MBYTE) / sizeof(uint32);
            long i, j;
            int k = arr_size - 1;
            write_data = k;
            struct timeval t1, t2;
            if (gettimeofday(&t1, NULL) != 0) {
                perror("gettimeofday");
                exit(1);
            }
            for (i = 0; i < num_jobs; i++) {
                for (j = 0; j < job_size; j++) {
                    d[k] = write_data;
                    if (k == 0) {
                        k = arr_size;
                        ++pass;
                        write_data = k + pass;
                    }
                    k--;
                    write_data--;
                }
            }
            if (gettimeofday(&t2, NULL) != 0) {
                perror("gettimeofday");
                exit(1);
            }
            uint64 elapsed_ms = timeval_diff_msec(&t1, &t2);
            printf("thread %d start=%d.%06d end=%d.%06d elapsed=%.3lf\n",
                   tp->thread_id, t1.tv_sec, t1.tv_usec, t2.tv_sec, t2.tv_usec,
                   (double) elapsed_ms / 1000.0);
        }
        break;
    case TEST_TYPE_WRITE_64B_2PASS:
        {
            uint64 *d = (uint64 *) tp->data;
            // Use pass to ensure that we write different data to the
            // array each time through it, to avoid the possibility
            // that caching somehow detects that writes are
            // redundantly writing the same data that is already in
            // main memory.
            uint64 pass = 0;
            uint64 write_data;
            long num_jobs = tp->num_jobs;
            long job_size = tp->job_size;
            int arr_size = (512 * MBYTE) / sizeof(uint64);
            long i, j;
            int k = arr_size - 1;
            write_data = k;
            struct timeval t1, t2;
            if (gettimeofday(&t1, NULL) != 0) {
                perror("gettimeofday");
                exit(1);
            }
            for (i = 0; i < num_jobs; i++) {
                for (j = 0; j < job_size; j++) {
                    d[k] = write_data;
                    if (k == 0) {
                        k = arr_size;
                        ++pass;
                        write_data = k + pass;
                    }
                    k--;
                    write_data--;
                }
            }
            if (gettimeofday(&t2, NULL) != 0) {
                perror("gettimeofday");
                exit(1);
            }
            uint64 elapsed_ms = timeval_diff_msec(&t1, &t2);
            printf("thread %d start=%d.%06d end=%d.%06d elapsed=%.3lf\n",
                   tp->thread_id, t1.tv_sec, t1.tv_usec, t2.tv_sec, t2.tv_usec,
                   (double) elapsed_ms / 1000.0);
        }
        break;
    case TEST_TYPE_READ_32B_2PASS:
        break;
    case TEST_TYPE_READ_64B_2PASS:
        break;
    default:
        fprintf(stderr, "do_test_init_part(): Unknown test_type value %d\n",
                tp->test_type);
        exit(1);
        break;
    }
}


void
do_test (int test_type, long num_jobs, long job_size, int num_threads)
{
    pthread_t threads[num_threads];

    tp = malloc((unsigned) num_threads * sizeof(struct thread_params));
    if (tp == NULL) {
        perror("malloc");
        exit(1);
    }
    int i;
    for (i = 0; i < num_threads; i++) {
        tp[i].thread_id = i;
        tp[i].test_type = test_type;
        tp[i].num_jobs = num_jobs;
        tp[i].job_size = job_size;
    }
    // Create num_threads parallel threads, each of which will run
    // num_jobs tests of type test_type with size job_size.
    struct timeval t1, t2, t3;
    if (gettimeofday(&t1, NULL) != 0) {
        perror("gettimeofday");
        exit(1);
    }
    do_test_init_part(test_type, num_jobs, job_size, num_threads, tp);
    if (gettimeofday(&t2, NULL) != 0) {
        perror("gettimeofday");
        exit(1);
    }
    for (i = 0; i < num_threads; i++) {
        if (pthread_create(&threads[i], 0, &do_test_measured_part, &(tp[i])) < 0) {
            perror("pthread_create");
            exit(1);
        }
    }
    for (i = 0; i < num_threads; i++) {
        pthread_join(threads[i], 0);
    }
    if (gettimeofday(&t3, NULL) != 0) {
        perror("gettimeofday");
        exit(1);
    }
    uint64 elapsed_init_ms = timeval_diff_msec(&t1, &t2);
    uint64 elapsed_run_ms = timeval_diff_msec(&t2, &t3);
    printf("Elapsed init time  = %.3lf sec\n",
           (double) elapsed_init_ms / 1000.0);
    printf("Elapsed run time   = %.3lf sec\n",
           (double) elapsed_run_ms / 1000.0);
    printf("Elapsed total time = %.3lf sec\n",
           (double) (elapsed_init_ms + elapsed_run_ms) / 1000.0);
}


int
main (int argc, char *argv[])
{
    // TBD: By default, are the caches on the machines I have set to
    // write through?  That is, if the CPU core does a write to a part
    // of memory for which no cache line is currently in its cache,
    // does it send the write data straight to main memory without
    // modifying the contents of the cache?  Or does it do something
    // else?

    // If it does write through, can it write to memory at the full
    // bandwidth the hardware is capable of while doing so?  Or is the
    // hardware capable of a faster way of writing to memory?

    // printf("sizeof(int)=%d\n", sizeof(int));
    // printf("sizeof(long)=%d\n", sizeof(long));
    // printf("sizeof(long long)=%d\n", sizeof(long long));
    // printf("sizeof(uint32)=%d\n", sizeof(uint32));
    // printf("sizeof(uint64)=%d\n", sizeof(uint64));
    // printf("sizeof(float)=%d\n", sizeof(float));
    // printf("sizeof(double)=%d\n", sizeof(double));

    char *prog_name = argv[0];
    if (argc != 5) {
        usage(prog_name);
        exit(1);
    }
    int test_type;
    if (!check_type(argv[1], &test_type)) {
        usage(prog_name);
        exit(1);
    }
    long num_jobs;
    if (!check_decimal_arg(argv[2], 2, &num_jobs)) {
        usage(prog_name);
        exit(1);
    }
    long job_size;
    if (!check_decimal_arg(argv[3], 2, &job_size)) {
        usage(prog_name);
        exit(1);
    }
    if (job_size == 0) {
        job_size = DEFAULT_JOB_SIZE;
    }
    int num_threads;
    if (!check_decimal_arg(argv[4], 1, &num_threads)) {
        usage(prog_name);
        exit(1);
    }

    do_test(test_type, num_jobs, job_size, num_threads);
}

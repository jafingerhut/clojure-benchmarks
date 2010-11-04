#include <stdio.h>
#include <stdlib.h>

typedef unsigned int uint32;

int main (int argc, char *argv[])
{
    int do_sleep = 0;
    unsigned long long N;
    if (argc != 2 && argc != 3) {
        fprintf(stderr, "usage: %s <kbytes_of_mem_to_alloc_on_heap>\n", argv[0]);
        exit(1);
    }
    N = atol(argv[1]);
    if (argc == 3) {
        do_sleep = 1;
    }
    fprintf(stderr, "Attempting to allocate %llu kbytes of memory...\n", N);
    uint32 *p = (uint32 *) malloc(N * 1024);
    if (p == NULL) {
        fprintf(stderr, "Failed.  Aborting.\n");
        fflush(stderr);
        exit(1);
    }
    fprintf(stderr, "Succeeded.  Attempting to initialize it all...\n");
    N /= 4;
    N *= 1024;
    uint32 i;
    for (i = 0; i < N; i++) {
        p[i] = i;
    }
    fprintf(stderr, "Initialization complete.");
    if (do_sleep) {
        fprintf(stderr, "  Sleeping forever.  You'll have to kill me now.\n");
        fflush(stderr);
        while (1) {
            sleep(1);
        }
    } else {
        fprintf(stderr, "  Exiting.\n");
        fflush(stderr);
    }
    exit(0);
}

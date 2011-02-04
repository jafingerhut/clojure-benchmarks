# andy_fingerhut@alum.wustl.edu
# Tue Jan  4 23:14:44 PST 2011
#
# gcc-mp-4.4 is installed in /opt/local/bin using a recent version of
# MacPorts as of Jan 4, 2011, using the command:
#
# sudo port install gcc44
#
# That version of gcc supports -fopenmp, but Apple's latest supported
# version of GCC does not.

CC = gcc-mp-4.4
CFLAGS = -O2

FF = g77
FFLAGS = -O2

all: stream_f.exe stream_c.exe

stream_f.exe: stream.f mysecond.o
	$(CC) $(CFLAGS) -c mysecond.c
	$(FF) $(FFLAGS) -c stream.f
	$(FF) $(FFLAGS) stream.o mysecond.o -o stream_f.exe

stream_c.exe: stream.c
	$(CC) $(CFLAGS) stream.c -o stream_c.exe

stream_seq: stream.c
	$(CC) $(CFLAGS) -o $@ stream.c

stream_openmp: stream.c
	$(CC) $(CFLAGS) -fopenmp -o $@ stream.c

shuffle_openmp: shuffle.c
	$(CC) $(CFLAGS) -fopenmp -o $@ shuffle.c

clean:
	rm -f stream_f.exe stream_c.exe *.o

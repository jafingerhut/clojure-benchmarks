CC=gcc
CFLAGS=-O2 -Wall -force_cpusubtype_ALL -mmacosx-version-min=10.4 -arch i386 -arch ppc

timemem-darwin: timemem-darwin.c
	$(CC) $(CFLAGS) $< -o $@ $(LDFLAGS)

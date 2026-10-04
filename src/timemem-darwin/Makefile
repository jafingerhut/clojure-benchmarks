CC=gcc
#CFLAGS=-O2 -Wall -force_cpusubtype_ALL -mmacosx-version-min=10.4 -arch ppc -arch i386 -arch x86_64

# This works on macOS 26.7, producing a universal binary that should
# run on both x86_64 or arm64 processors.
CFLAGS=-O2 -Wall -mmacosx-version-min=10.15 -arch x86_64 -arch arm64

timemem-darwin: timemem-darwin.c
	$(CC) $(CFLAGS) $< -o $@ $(LDFLAGS)

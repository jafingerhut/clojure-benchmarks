CC=gcc
#CFLAGS=-I/opt/local/include/glib-2.0 -I/opt/local/include/libgtop-2.0 -I/opt/local/lib/glib-2.0/include
#CFLAGS=`pkg-config --cflags libgtop-2.0`
CFLAGS=-I/usr/include/libgtop-2.0 -I/usr/include/glib-2.0 -I/usr/lib/glib-2.0/include

#LDFLAGS=-L/opt/local/lib
#LDFLAGS=`pkg-config --libs libgtop-2.0` -lpthread -lm
LDFLAGS=-lgtop-2.0 -lglib-2.0 -lintl -liconv -lpthread -lm

timemem.exe: timemem.o
	$(CC) $< -o $@ $(LDFLAGS)

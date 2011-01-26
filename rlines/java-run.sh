#! /bin/bash

if [ $# -lt 2 ]
then
    1>&2 echo "usage: `basename $0` <input-file> <output-file> [ cmd line args for Java program ]"
    exit 1
fi

source ../env.sh

INP="$1"
shift
OUTP="$1"
shift

# Summary of results:

# % wc long-input.txt 
#   2083335   2083338 127083364 long-input.txt
# All but first and last input line are 60 characters plus a newline
# long.

# -client
# smallest successful heap size: 544m
# largest failing heap size: 528m

# -server
# smallest successful heap size: 688m
# largest failing heap size: 672m

# -server takes about 144m more than -client (for this input).


# If Java strings are truly 2 bytes per character plus 38 bytes each,
# that is about:

# (2,083,335 lines) * ( (2 bytes/char) * (60 characters) + 38 bytes overhead)
# = 329,166,930 bytes
# Dividing that by 2^20 gives 313.92 Mbytes.


# According to the web page below, a LinkedList takes 24 bytes per
# element.  I don't know whether that depends upon -server or -client,
# but it definitely might, given that -server should be using 64-bit
# pointers instead of -client's 32-bit pointers.

# http://www.javaspecialists.co.za/archive/Issue029.html

# 24 bytes per LinkedList element:
# (2,083,335 lines) * ( 24 bytes / LinkedList element ) * (2 lists)
# / (2^20 bytes / Mbyte)
# = 95.4 Mbytes

# Those two total to 313.92 + 95.4 = 409.32 Mbytes



# (1) Works fine
#JVM_OPTS="-server -Xmx2048m"

# (2) Runs out of heap space:
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
#	at java.util.Arrays.copyOfRange(Arrays.java:3209)
#	at java.lang.String.<init>(String.java:216)
#	at java.io.BufferedReader.readLine(BufferedReader.java:331)
#	at java.io.BufferedReader.readLine(BufferedReader.java:362)
#	at revlines.main(revlines.java:20)
#JVM_OPTS="-server -Xmx512m"

# (3) works, taking a little longer than (1) above, probably due to GC
# time?
#JVM_OPTS="-server -Xmx768m"

# (4)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
#	at java.util.LinkedList.addBefore(LinkedList.java:778)
#	at java.util.LinkedList.addFirst(LinkedList.java:153)
#	at revlines.main(revlines.java:34)
#JVM_OPTS="-server -Xmx640m"

# (5) works in about 24 sec
#JVM_OPTS="-server -Xmx704m"

# (6)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
#	at java.util.LinkedList.addBefore(LinkedList.java:778)
#	at java.util.LinkedList.addFirst(LinkedList.java:153)
#	at revlines.main(revlines.java:34)
#JVM_OPTS="-server -Xmx672m"

# (7) works in about 27 sec
#JVM_OPTS="-server -Xmx688m"

########################################

# (8)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
#	at java.util.LinkedList.addBefore(LinkedList.java:778)
#	at java.util.LinkedList.addFirst(LinkedList.java:153)
#	at revlines.main(revlines.java:34)
#JVM_OPTS="-client -Xmx512m"

# (9) works in about 14 sec
#JVM_OPTS="-client -Xmx640m"

# (10) works in about 14 sec
#JVM_OPTS="-client -Xmx576m"

# (11) works in about 18 sec
#JVM_OPTS="-client -Xmx544m"

# -Xmx544m is enough on a 32-bit Hotspot JVM, but apparently not
# -enough on a 64-bit Hotspot JVM.
JVM_OPTS="-client -Xmx1024m"

# (12)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
#	at java.util.LinkedList.addBefore(LinkedList.java:778)
#	at java.util.LinkedList.addFirst(LinkedList.java:153)
#	at revlines.main(revlines.java:34)
#JVM_OPTS="-client -Xmx528m"

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --input "${INP}" --output "${OUTP}" "${JAVA}" -server -classpath "${JAVA_OBJ_DIR}" revcomp "$@"

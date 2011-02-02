#! /bin/bash

if [ $# -lt 3 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> <input-file> <output-file> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

# Read input from file named after CLJ_VERSION on command line
# Write output to file named after that on command line

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
# smallest successful heap size:
# 496m (90s), 512m (49s), 528m (31.5s), 768m (24s)
# largest failing heap size: 480m

# -server
# smallest successful heap size: 640m (106.5s), 768m (53.5s), 1024m (31s)
# largest failing heap size: 608m


#JVM_MEM_OPTS="-Xmx1536m -XX:NewRatio=2 -XX:+UseParallelGC"
#JVM_MEM_OPTS="-Xmx1536m -XX:NewRatio=5 -XX:+UseParallelGC"
#JVM_MEM_OPTS="-Xmx1024m"

# (1) works in 24 sec
#JVM_MEM_OPTS="-client -Xmx768m"

# (2) works in 49 sec
#JVM_MEM_OPTS="-client -Xmx512m"

# (3)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-client -Xmx384m"

# (4)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-client -Xmx448m"

# (5)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-client -Xmx480m"

# (6) works in 90 sec, clearly spending a lot more GC time than (1) or (2)
#JVM_MEM_OPTS="-client -Xmx496m"

# (7) works in 31.5 sec
#JVM_MEM_OPTS="-client -Xmx528m"

########################################

# (8) works in 53.5 sec
#JVM_MEM_OPTS="-server -Xmx768m"

# (9) works in 106.5 sec
#JVM_MEM_OPTS="-server -Xmx640m"

# (10)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-server -Xmx512m"

# (11)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-server -Xmx576m"

# (12)
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revlines.clj-1.clj:0)
#JVM_MEM_OPTS="-server -Xmx608m"

# (13) works in 
#JVM_MEM_OPTS="-server -Xmx1024m"



#JMX_MONITORING=-Dcom.sun.management.jmxremote
#JMX_MONITORING=

# Use a small limit to avoid using lots of memory.  It makes the
# garbage collector collect more often, but the extra CPU time is not
# much.
MAX_HEAP_MB=1024

../bin/measureproc ${MEASUREPROC_SHOWS_JVM_INFO} --jvm-gc-stats "${JVM_TYPE}" --input "${INP}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} -server -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" revlines "$@"

#! /bin/bash

if [ $# -lt 2 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> <output-file> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

# Write output to file named after Clojure version on the command line.
# After that, one argument expected on the command line, an integer.

OUTP="$1"
shift

# Use a small limit to avoid using lots of memory.  It makes the
# garbage collector collect more often, but the extra CPU time is not
# much.
if [ "${JVM_TYPE}" == "jrockit" ]
then
    # JRockit cannot go below max heap size of 16 MB it seems.
    MAX_HEAP_MB=16
else
    MAX_HEAP_MB=8
fi

../bin/measureproc ${MEASUREPROC_SHOWS_JVM_INFO} --jvm-gc-stats "${JVM_TYPE}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} -server -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" spectralnorm "$@"

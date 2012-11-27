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

# Use a small limit to avoid using lots of memory.  It makes the
# garbage collector collect more often, but the extra CPU time is not
# much.
MAX_HEAP_MB=384

../bin/measureproc ${MP_COMMON_ARGS} ${MP_ARGS_FOR_JVM_RUN} --input "${INP}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} ${JAVA_OPTS} -XX:+TieredCompilation -XX:+AggressiveOpts -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" revcomp "$@"

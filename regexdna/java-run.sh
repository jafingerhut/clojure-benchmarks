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

MAX_HEAP_MB=1024

../bin/measureproc ${MP_COMMON_ARGS} ${MP_ARGS_FOR_JVM_RUN} --input "${INP}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} ${JAVA_OPTS} -Xmx${MAX_HEAP_MB}m -classpath "${JAVA_OBJ_DIR}" regexdna "$@"

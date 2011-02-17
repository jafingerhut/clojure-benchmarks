#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <output-file> [ cmd line args for Java program ]"
    exit 1
fi

source ../env.sh

OUTP="$1"
shift

../bin/measureproc ${MP_COMMON_ARGS} ${MP_ARGS_FOR_JVM_RUN} --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} -server -classpath "${JAVA_OBJ_DIR}" nbody "$@"

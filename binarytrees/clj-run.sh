#! /bin/bash

if [ $# -lt 2 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> <output-file> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

OUTP="$1"
shift

../bin/measureproc ${MP_COMMON_ARGS} ${MP_ARGS_FOR_JVM_RUN} --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} ${JAVA_OPTS} -classpath "${PS_FULL_CLJ_CLASSPATH}" binarytrees "$@"

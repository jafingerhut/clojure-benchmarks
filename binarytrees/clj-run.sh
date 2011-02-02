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

../bin/measureproc ${MEASUREPROC_SHOWS_JVM_INFO} --jvm-gc-stats "${JVM_TYPE}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} -server -classpath "${PS_FULL_CLJ_CLASSPATH}" binarytrees "$@"

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

mkdir -p output

if [ "${JVM_TYPE}" == "hotspot" ]
then
    MAX_HEAP_MB=64
elif [ "${JVM_TYPE}" == "jrockit" ]
then
    MAX_HEAP_MB=512
else
    1>&2 echo "JVM_TYPE=${JVM_TYPE} has unsupported value.  Supported values are: hotspot, jrockit"
    exit 1
fi

../bin/measureproc ${MEASUREPROC_SHOWS_JVM_INFO} --jvm-gc-stats "${JVM_TYPE}" --output "${OUTP}" "${JAVA}" ${JAVA_PROFILING} -server -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" collatz "$@"

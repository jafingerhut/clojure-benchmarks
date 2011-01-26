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

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --output "${OUTP}" "${JAVA}" -server -classpath "${PS_FULL_CLJ_CLASSPATH}" binarytrees "$@"

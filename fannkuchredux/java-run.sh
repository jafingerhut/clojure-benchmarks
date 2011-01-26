#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <output-file> [ cmd line args for Java program ]"
    exit 1
fi

source ../env.sh

OUTP="$1"
shift

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --output "${OUTP}" "${JAVA}" -server -classpath "${JAVA_OBJ_DIR}" fannkuchredux "$@"

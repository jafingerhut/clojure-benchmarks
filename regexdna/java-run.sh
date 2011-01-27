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

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --input "${INP}" --output "${OUTP}" "${JAVA}" -server -classpath "${JAVA_OBJ_DIR}" regexdna "$@"

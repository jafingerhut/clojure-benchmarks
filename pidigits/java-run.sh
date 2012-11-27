#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <output-file> [ cmd line args for Java program ]"
    exit 1
fi

source ../env.sh

OUTP="$1"
shift

# Note: This program gives an error when attempting to run it without
# a "jpargmp" library installed.  I do not know where to get this
# library from.  Try running the GCC version of the program instead,
# if you wish to create an expected output file.

../bin/measureproc ${MP_COMMON_ARGS} ${MP_ARGS_FOR_JVM_RUN} --output "${OUTP}" "${JAVA}" -Djava.library.path=../include ${JAVA_PROFILING} ${JAVA_OPTS} -classpath "${JAVA_OBJ_DIR}" pidigits "$@"

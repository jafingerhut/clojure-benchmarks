#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <output-file> [ cmd line args for GCC program ]"
    exit 1
fi

source ../env.sh

OUTP="$1"
shift

../bin/measureproc --output "${OUTP}" "${GCC_OBJ_DIR}/meteor.gcc_run" "$@"

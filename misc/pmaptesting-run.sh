#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

"${JAVA}" -server ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" pmaptesting "$@"

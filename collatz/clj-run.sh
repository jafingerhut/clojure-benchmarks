#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

../bin/measureproc --jvm-info server --jvm-gc-stats hotspot --output output/${CLJ_VERSION}-output.txt "${JAVA}" -server -Xmx64m -classpath "${PS_FULL_CLJ_CLASSPATH}" collatz "$@"

#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh

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

# TBD: Does measureproc need this on Cygwin in order to work?  Is
# there some straightforward way to make measureproc detect when this
# change is needed on its own and do it automatically, but only when
# needed?
if [ "$OS" == "Cygwin" ]
then
    JAVA="`cygpath -w "${JAVA}"`"
fi

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --output output/${CLJ_VERSION}-output.txt "${JAVA}" -server -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" collatz "$@"

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

# If Hotspot JVM is in use:
JVM_TYPE=hotspot
MAX_HEAP_MB=64
# If JRockit JVM is in use:
#JVM_TYPE=jrockit
#MAX_HEAP_MB=512

# TBD: Does measureproc need this on Cygwin in order to work?  Is
# there some straightforward way to make measureproc detect when this
# change is needed on its own and do it automatically, but only when
# needed?
if [ "$OS" == "Cygwin" ]
then
    JAVA="`cygpath -w "${JAVA}"`"
fi

../bin/measureproc --jvm-info server --jvm-gc-stats "${JVM_TYPE}" --output output/${CLJ_VERSION}-output.txt "${JAVA}" -server -Xmx${MAX_HEAP_MB}m -classpath "${PS_FULL_CLJ_CLASSPATH}" collatz "$@"

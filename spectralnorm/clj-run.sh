#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

# One argument expected on the command line, an integer.

source ../env.sh

JVM_OPTS="-server"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

1>&2 echo "${JAVA} ${JVM_OPTS} ${JVM_MAX_MEM} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath ${PS_FULL_CLJ_CLASSPATH} spectralnorm" "$@"
           "${JAVA}" ${JVM_OPTS} ${JVM_MAX_MEM} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" spectralnorm "$@"

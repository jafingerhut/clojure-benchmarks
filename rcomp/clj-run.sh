#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

# Read input from stdin
# Write output to stdout

source ../env.sh

#JVM_OPTS="-client -Xmx1024m"
JVM_OPTS="-server -Xmx1536m"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

1>&2 echo "${JAVA} ${JVM_OPTS} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" revcomp" "$@"
           "${JAVA}" ${JVM_OPTS} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" revcomp "$@"

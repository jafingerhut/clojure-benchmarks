#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

# Assume that the clj-compile.sh script has alread been run
# successfully.

# Read input from stdin
# Write output to stdout

source ../env.sh
JVM_MEM_OPTS="-client -Xmx1024m"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

"${JAVA}" ${JVM_MEM_OPTS} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" regexdna "$@"


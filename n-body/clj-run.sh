#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

#JVM_MEM_OPTS="-client -Xmx1024m"
JVM_MEM_OPTS="-server"

#JMX_MONITORING=-Dcom.sun.management.jmxremote

# Current fastest
CLJ_PROG=nbody.clj-11.clj

$JAVA $JVM_MEM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main $CLJ_PROG "$@"

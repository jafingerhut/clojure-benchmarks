#! /bin/bash

# Assume that the clj-compile.sh script has alread been run
# successfully.

# Read input from stdin
# Write output to stdout

source ../env.sh
JVM_MEM_OPTS="-client -Xmx1024m"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

$JAVA $JVM_MEM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH}:./obj/clj regexdna "$@"


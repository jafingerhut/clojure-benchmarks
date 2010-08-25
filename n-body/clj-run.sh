#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

#JVM_OPTS="-client -Xmx1024m"
JVM_OPTS="-server"
#JVM_OPTS="-server -XX:+DoEscapeAnalysis -XX:+UseBiasedLocking"

#JMX_MONITORING=-Dcom.sun.management.jmxremote

# Current fastest
CLJ_PROG=nbody.clj-11.clj

1>&2 echo "$JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main $CLJ_PROG" "$@"
           $JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main $CLJ_PROG "$@"

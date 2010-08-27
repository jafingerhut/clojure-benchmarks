#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

JVM_MEM_OPTS="-client -Xmx1024m"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

CLJ_PROG=revcomp.clj-8.clj

1>&2 echo "$JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH}:./obj/clj revcomp" "$@"
           $JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH}:./obj/clj revcomp "$@"

#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

#JVM_OPTS="-client -Xmx1024m"
JVM_OPTS="-server -Xmx1536m"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

1>&2 echo "$JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH}:./obj/clj revcomp" "$@"
           $JAVA $JVM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH}:./obj/clj revcomp "$@"

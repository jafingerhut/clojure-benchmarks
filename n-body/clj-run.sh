#! /bin/bash

# One argument expected on the command line, an integer.

source ../env.sh

JVM_OPTS="-server"
#JMX_MONITORING=-Dcom.sun.management.jmxremote

1>&2 echo "${JAVA} ${JVM_OPTS} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath ${PS_FULL_CLJ_CLASSPATH} nbody" "$@"
           "${JAVA}" ${JVM_OPTS} ${JMX_MONITORING} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" nbody "$@"

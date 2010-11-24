#! /bin/bash

source ../env.sh

1>&2 echo "${JAVA}" ${JVM_OPTS} ${JVM_MAX_MEM} ${JMX_MONITORING} ${JAVA_PROFILING} "-Xbootclasspath/a:${PS_FULL_SCALA_CLASSPATH}" nbody "$@"
          "${JAVA}" ${JVM_OPTS} ${JVM_MAX_MEM} ${JMX_MONITORING} ${JAVA_PROFILING} "-Xbootclasspath/a:${PS_FULL_SCALA_CLASSPATH}" nbody "$@"

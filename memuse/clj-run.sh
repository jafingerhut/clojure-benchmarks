#! /bin/bash

source ../env.sh

OPTS="${JAVA_OPTS}"


#HPROF_OPTS="cpu=samples,depth=20,thread=y"
#JAVA_PROFILING="-agentlib:hprof=$HPROF_OPTS"
JAVA_PROFILING=""

#JDB="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
 JDB="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
#JDB="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"

1>&2 echo "${JAVA}  ${OPTS} -Xmx1024m ${JDB} ${JAVA_PROFILING} -classpath ${PS_FULL_CLJ_CLASSPATH} memuse" "$@"
          "${JAVA}" ${OPTS} -Xmx1024m ${JDB} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" memuse "$@"

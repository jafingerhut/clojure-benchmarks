#! /bin/bash

CLOJURE_JAR_DIR=$HOME/.clojure
#CLOJURE_CLASSPATH=$CLOJURE_JAR_DIR/clojure-1.1.0-alpha-SNAPSHOT.jar:$CLOJURE_JAR_DIR/clojure-contrib.jar
CLOJURE_CLASSPATH=$CLOJURE_JAR_DIR/clojure-1.1.0-alpha-SNAPSHOT.jar:$CLOJURE_JAR_DIR/clojure-contrib.jar:/Users/Shared/lang/clojure-benchmarks/memuse/

OPTS="-server"
#OPTS="-client"


#HPROF_OPTS="cpu=samples,depth=20,thread=y"
#JAVA_PROFILING="-agentlib:hprof=$HPROF_OPTS"
JAVA_PROFILING=""

#JDB="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
 JDB="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
#JDB="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"

CLJ_PROG=memuse.clj


echo "${JAVA}  ${OPTS} -Xmx1024m ${JDB} ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH} clojure.main ${CLJ_PROG}" "$@"
     "${JAVA}" ${OPTS} -Xmx1024m ${JDB} ${JAVA_PROFILING} -classpath ${CLOJURE_CLASSPATH} clojure.main ${CLJ_PROG} "$@"

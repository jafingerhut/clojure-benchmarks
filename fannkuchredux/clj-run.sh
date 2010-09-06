#! /bin/bash

source ../env.sh

$JAVA -server ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH}:./obj/clj fannkuchredux "$@"

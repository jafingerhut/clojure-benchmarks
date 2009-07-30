#! /bin/bash

source ../env.sh

$JAVA -server -Xmx1536m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main fannkuch.clj-1.clj "$@"

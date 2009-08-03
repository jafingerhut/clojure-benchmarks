#! /bin/bash

source ../env.sh

$JAVA -server -Xmx1024m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main fannkuch.clj-6.clj "$@"

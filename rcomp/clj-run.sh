#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

$JAVA -server -Xmx1280m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main revcomp.clj-2.clj "$@"

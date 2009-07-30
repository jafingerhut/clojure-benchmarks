#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

$JAVA -server -Xmx1536m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main revcomp.clj-4.clj "$@"

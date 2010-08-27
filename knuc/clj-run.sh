#! /bin/bash

# Read input from stdin
# Write output to stdout

# 512m - not enough
# 768m - not enough
# 1024m - not enough

source ../env.sh

$JAVA -server -Xmx1536m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH}:./obj/clj knucleotide "$@"

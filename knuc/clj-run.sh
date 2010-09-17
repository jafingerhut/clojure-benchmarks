#! /bin/bash

# Read input from stdin
# Write output to stdout

# 512m - not enough
# 768m - not enough
# 1024m - not enough

source ../env.sh

"${JAVA}" -server -Xmx1536m ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" knucleotide "$@"

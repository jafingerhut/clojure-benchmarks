#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

# Read input from stdin
# Write output to stdout

# 512m - not enough
# 768m - not enough
# 1024m - not enough

source ../env.sh

"${JAVA}" -server -Xmx1536m ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" knucleotide "$@"

#! /bin/bash

source ../env.sh

"${JAVA}" -server ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" fasta "$@"

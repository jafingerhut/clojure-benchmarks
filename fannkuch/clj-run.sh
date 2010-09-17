#! /bin/bash

source ../env.sh

"${JAVA}" -server -Xmx1024m ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" fannkuch "$@"

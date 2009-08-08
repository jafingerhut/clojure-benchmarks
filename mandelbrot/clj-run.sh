#! /bin/bash

# usage: ./clj-run.sh <size>

source ../env.sh

$JAVA -server -Xmx768m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main mandelbrot.clj-3.clj "$@"

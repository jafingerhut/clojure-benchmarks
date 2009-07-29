#! /bin/bash

# usage: ./clj-run.sh <size>

source ../env.sh

$JAVA -server -Xmx1280m ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main mandelbrot.clj "$@"

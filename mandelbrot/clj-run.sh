#! /bin/sh

# usage: ./clj-run.sh <size>

source ../env.sh

$JAVA -server -Xmx1280m ${JAVA_PROFILING} -cp ${CLOJURE_JAR} clojure.main mandelbrot.clj "$@"

#! /bin/bash

# usage: ./clj-run.sh <size>

source ../env.sh

JVM_OPTS="-server -Xmx768m"

# The latest Mac OS X Java release apparently disables escape
# analysis, and also prints a warning to the output about it.
# Aug 25, 2010   Mac OS X 10.6.4
# % java -version
# java version "1.6.0_20"
#JVM_OPTS="-server -XX:+DoEscapeAnalysis -XX:+UseBiasedLocking"
JVM_OPTS="-server -XX:+UseBiasedLocking"

1>&2 echo $JAVA ${JVM_OPTS} ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH}:./obj/clj mandelbrot "$@"
          $JAVA ${JVM_OPTS} ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH}:./obj/clj mandelbrot "$@"

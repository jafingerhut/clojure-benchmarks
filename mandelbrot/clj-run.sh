#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version> [ cmd line args for Clojure program ]"
    exit 1
fi
CLJ_VERSION="$1"
shift

# usage: ./clj-run.sh <size>

source ../env.sh

# The latest Mac OS X Java release apparently disables escape
# analysis, and also prints a warning to the output about it.
# Aug 25, 2010   Mac OS X 10.6.4
# % java -version
# java version "1.6.0_20"
#JVM_OPTS="-server -XX:+DoEscapeAnalysis -XX:+UseBiasedLocking"

# My Mac OS X 10.5.8 laptop running java 1.5.0_24 crashed when using
# the -XX:+UseBiasedLocking option with this program.  It did not
# crash if I left that option off.
#JVM_OPTS="-server -XX:+UseBiasedLocking"

JVM_OPTS="-server"

1>&2 echo ${TIME_CMD} $JAVA ${JVM_OPTS} ${JVM_MAX_MEM} ${JAVA_PROFILING} -classpath ${PS_FULL_CLJ_CLASSPATH} mandelbrot "$@"
          ${TIME_CMD} "${JAVA}" ${JVM_OPTS} ${JVM_MAX_MEM} ${JAVA_PROFILING} -classpath "${PS_FULL_CLJ_CLASSPATH}" mandelbrot "$@"

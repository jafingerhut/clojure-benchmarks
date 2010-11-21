#! /bin/bash

if [ $# -lt 1 ]; then
    1>&2 echo "usage: `basename $0` { quick | medium | long} [ benchmark-name args ]"
    exit 1
fi
INPUT_SIZE=$1

shift
if [ $# -ge 1 ]; then
    BENCHMARK="$@"
else
    BENCHMARK="knucleotide"
fi
#echo $BENCHMARK
#exit 0

OS=`uname -o 2>/dev/null`
if [ $? != 0 ]; then
    # Then likely we are running on a Mac OS X system with the default
    # uname installation, which accepts -s but not -o option.
    OS=`uname -s 2>/dev/null`
fi

set -ex

COMMON_ARGS="--verbose --jvm-gc-stats --alpha 1.05 --sorted-summary --results-file results.xml"
#COMMON_ARGS="--verbose --jvm-gc-stats --alpha 1.05 --sorted-summary --results-file results.xml --sweep-only --num-mb-values 2"

if [ "$OS" == "Cygwin" ]
then
    ../bin/tmcurve ${COMMON_ARGS} --input input\\${INPUT_SIZE}-input.txt --output output\\${INPUT_SIZE}-clj-1.2-output.txt --check-output-cmd "diff --strip-trailing-cr --brief output/${INPUT_SIZE}-expected-output.txt output/${INPUT_SIZE}-clj-1.2-output.txt" \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -server -Xmx%mbm -classpath "\\cygwin\\home\\Administrator\\lein\\swank-clj-1.2.0\\lib\\clojure-1.2.0.jar;.\\obj\\clj-1.2" ${BENCHMARK}
else
    ../bin/tmcurve ${COMMON_ARGS} --input input/${INPUT_SIZE}-input.txt --output output/${INPUT_SIZE}-clj-1.2-output.txt --check "diff --strip-trailing-cr --brief output/${INPUT_SIZE}-expected-output.txt %o" java -server -Xmx%mbm -classpath ~/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 ${BENCHMARK}
fi

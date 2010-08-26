#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="n-body"

# The compiled GHC executable crashes on my Mac.  Leaving it out of
# the default list of languages for now.

#ALL_LANGUAGES="sbcl perl ghc java clj"
ALL_LANGUAGES="sbcl perl java clj"
ALL_TESTS="quick medium long"

LANGUAGES=""
TESTS=""

while [ $# -ge 1 ]
do
    case $1 in
	sbcl|perl|ghc|java|clj) LANGUAGES="$LANGUAGES $1"
	    ;;
	quick|medium|medium2|long) TESTS="$TESTS $1"
	    ;;
	*)
	    1>&2 echo "Unrecognized command line parameter: $1"
	    exit 1
	    ;;
    esac
    shift
done

#echo "LANGUAGES=$LANGUAGES"
#echo "TESTS=$TESTS"

if [ "x$LANGUAGES" = "x" ]
then
    LANGUAGES=${ALL_LANGUAGES}
fi

if [ "x$TESTS" = "x" ]
then
    TESTS=${ALL_TESTS}
fi

echo "LANGUAGES=$LANGUAGES"
echo "TESTS=$TESTS"

for T in $TESTS
do
    case $T in
	quick)    N=1000
	    ;;
	medium)   N=500000
	    ;;
	medium2)  N=5000000
	    ;;
	long)     N=50000000
	    ;;
    esac
    for L in $LANGUAGES
    do
	case $L in
	    clj) CMD=./clj-run.sh
		( ./clj-compile.sh ) >& ${OUTPUT_DIR}/clj-compile-log.txt
		;;
	    java) CMD=./java-run.sh
		( ./java-compile.sh ) >& ${OUTPUT_DIR}/java-compile-log.txt
		;;
	    sbcl) CMD=./sbcl-run.sh
		( ./sbcl-compile.sh ) >& ${OUTPUT_DIR}/sbcl-compile-log.txt
		;;
	    perl) CMD="$PERL nbody.perl"
		;;
	    ghc) CMD=./ghc-run.sh
		( ./ghc-compile.sh ) >& ${OUTPUT_DIR}/ghc-compile-log.txt
	esac

	echo
	echo "benchmark: $BENCHMARK"
	echo "language: $L"
	echo "test: $T"
	OUT=${OUTPUT_DIR}/${T}-${L}-output.txt
	CONSOLE=${OUTPUT_DIR}/${T}-${L}-console.txt
	echo "( time ${CMD} ${N} > ${OUT} ) 2>&1 | tee ${CONSOLE}"
	( time ${CMD} ${N} > ${OUT} ) 2>&1 | tee ${CONSOLE}

	$CMP ${T}-expected-output.txt ${OUT} 2>&1 | tee --append ${CONSOLE}
    done
done

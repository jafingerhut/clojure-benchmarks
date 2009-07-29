#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="fasta"

#ALL_LANGUAGES="sbcl perl ghc java clj"
ALL_LANGUAGES="sbcl perl ghc java"
ALL_TESTS="quick medium long"

LANGUAGES=""
TESTS=""

while [ $# -ge 1 ]
do
    case $1 in
	sbcl|perl|ghc|java|clj) LANGUAGES="$LANGUAGES $1"
	    ;;
	quick|knuc|medium|long) TESTS="$TESTS $1"
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
	quick)  N=1000
	    ;;
	knuc)   N=10000
	    ;;
	medium) N=1000000
	    ;;
	long)   N=25000000
	    ;;
    esac
    for L in $LANGUAGES
    do
	case $L in
	    clj) CMD=./clj-run.sh
		;;
	    java) CMD=./java-run.sh
		( ./java-compile.sh ) >& ${OUTPUT_DIR}/java-compile-log.txt
		;;
	    sbcl) CMD=./sbcl-run.sh
		( ./sbcl-compile.sh ) >& ${OUTPUT_DIR}/sbcl-compile-log.txt
		;;
	    perl) CMD="$PERL fasta.perl-4.perl"
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

	$CMP ${T}-expected-output.txt ${OUT}
    done
done

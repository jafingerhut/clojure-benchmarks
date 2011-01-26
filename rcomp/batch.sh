#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir -p $OUTPUT_DIR

BENCHMARK="reverse-complement"

# SBCL 1.0.23 works on Mac OS X, but SBCL 1.0.29 doesn't like opening
# /dev/stdin with :element-type '(unsigned-byte 8) for some reason.
# SBCL 1.0.39 seems to work fine.

ALL_LANGUAGES="sbcl perl ghc java clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3 clj-1.3-alpha4"
ALL_TESTS="quick medium long"

LANGUAGES=""
TESTS=""

while [ $# -ge 1 ]
do
    case $1 in
	sbcl|perl|ghc|java|clj*) LANGUAGES="$LANGUAGES $1"
	    ;;
	quick|medium|long) TESTS="$TESTS $1"
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
    for L in $LANGUAGES
    do
	case $L in
	    clj*) CMD="./clj-run.sh $L"
		( ./clj-compile.sh $L ) >& ${OUTPUT_DIR}/clj-compile-log.txt
		;;
	    java) CMD=./java-run.sh
		( ./java-compile.sh ) >& ${OUTPUT_DIR}/java-compile-log.txt
		;;
	    sbcl) CMD=./sbcl-run.sh
		( ./sbcl-compile.sh ) >& ${OUTPUT_DIR}/sbcl-compile-log.txt
		;;
	    perl) CMD="$PERL revcomp.perl-2.perl"
		;;
	    ghc) CMD=./ghc-run.sh
		( ./ghc-compile.sh ) >& ${OUTPUT_DIR}/ghc-compile-log.txt
	esac
	
	echo
	echo "benchmark: $BENCHMARK"
	echo "language: $L"
	echo "test: $T"
	IN=./input/${T}-input.txt
	OUT=${OUTPUT_DIR}/${T}-${L}-output.txt
	CONSOLE=${OUTPUT_DIR}/${T}-${L}-console.txt
	case $L in
	    clj*|java)
		echo "( ${CMD} ${IN} ${OUT} ) 2>&1 | tee ${CONSOLE}"
		( ${CMD} ${IN} ${OUT} ) 2>&1 | tee ${CONSOLE}
		;;
	    *)
		echo "( time ${CMD} < ${IN} > ${OUT} ) 2>&1 | tee ${CONSOLE}"
		( time ${CMD} < ${IN} > ${OUT} ) 2>&1 | tee ${CONSOLE}
		;;
	esac
	$CMP ${OUTPUT_DIR}/${T}-expected-output.txt ${OUT} 2>&1 | tee -a ${CONSOLE}
    done
done

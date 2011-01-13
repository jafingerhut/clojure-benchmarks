#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir -p $OUTPUT_DIR

BENCHMARK="pidigits"

#ALL_LANGUAGES="sbcl perl ghc gcc java clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3 clj-1.3-alpha4"
ALL_LANGUAGES="gcc jruby"
ALL_TESTS="quick medium long"

LANGUAGES=""
TESTS=""

while [ $# -ge 1 ]
do
    case $1 in
	sbcl|perl|ghc|gcc|java|clj*|jruby) LANGUAGES="$LANGUAGES $1"
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
    case $T in
	quick) N=1000
	    ;;
	medium) N=5000
	    ;;
	long) N=10000
	    ;;
    esac
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
	    perl) CMD="$PERL pidigits.perl"
		;;
	    gcc) CMD=./gcc-run.sh
		( ./gcc-compile.sh ) >& ${OUTPUT_DIR}/gcc-compile-log.txt
		;;
	    ghc) CMD=./ghc-run.sh
		( ./ghc-compile.sh ) >& ${OUTPUT_DIR}/ghc-compile-log.txt
		;;
	    jruby) CMD="$JRUBY --server pidigits.jruby-3.jruby"
		;;
	esac
	case $L in
	    sbcl) EXTRA_LANG_ARGS="1"
		;;
	esac

	echo
	echo "benchmark: $BENCHMARK"
	echo "language: $L"
	echo "test: $T"
	OUT=${OUTPUT_DIR}/${T}-${L}-output.txt
	CONSOLE=${OUTPUT_DIR}/${T}-${L}-console.txt
	echo "( time ${CMD} ${N} ${EXTRA_LANG_ARGS} > ${OUT} ) 2>&1 | tee ${CONSOLE}"
	( time ${CMD} ${N} ${EXTRA_LANG_ARGS} > ${OUT} ) 2>&1 | tee ${CONSOLE}

	$CMP ${OUTPUT_DIR}/${T}-expected-output.txt ${OUT} 2>&1 | tee -a ${CONSOLE}
    done
done

#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir -p $OUTPUT_DIR

BENCHMARK="knucleotide"

#ALL_LANGUAGES="sbcl perl ghc java clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3 clj-1.3-alpha4"
# Note that knucleotide.clojure-7.clojure does not compile with
# clj-1.2 or clj-1.2.1.
#
# While it compiles with clj-1.3-alpha5 and clj-1.3-alpha6, it uses
# enough more memory than later versions with this program that the
# -Xmx command line argument currently used is not enough, and it runs
# out of memory.  Leave those Clojure versions out for now.
ALL_LANGUAGES="java clj-1.3-alpha7 clj-1.3-alpha8 clj-1.3-beta1 clj-1.3-beta2 clj-1.3-beta3 clj-1.3 clj-1.4-alpha1 clj-1.4-alpha2 clj-1.4-alpha3 clj-1.4-alpha4 clj-1.4-alpha5 clj-1.4-beta1 clj-1.4-beta2 clj-1.4-beta3 clj-1.4-beta4 clj-1.4-beta5 clj-1.4-beta6 clj-1.4-beta7 clj-1.4 clj-1.5-alpha1 clj-1.5-alpha2 clj-1.5-alpha3 clj-1.5-alpha4 clj-1.5-alpha5 clj-1.5-alpha6 clj-1.5-alpha7"
ALL_TESTS="quick medium long"

LANGUAGES=""
TESTS=""

while [ $# -ge 1 ]
do
    case $1 in
	sbcl|perl|ghc|java) LANGUAGES="$LANGUAGES $1"
	    ;;
	quick|medium|long) TESTS="$TESTS $1"
	    ;;
	*)
	    check_clojure_version_spec $1
            if [ $? != 0 ]
            then
	        1>&2 echo "Unrecognized command line parameter: $1"
	        exit 1
            fi
            LANGUAGES="$LANGUAGES clj-${CLJ_VERSION_STR}"
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
	    perl) CMD="$PERL knucleotide.perl-2.perl"
		;;
	    ghc) CMD=./ghc-run.sh
		( ./ghc-compile.sh ) >& ${OUTPUT_DIR}/ghc-compile-log.txt
	esac
	case $L in
	    # Put a number between the double quotes to make
	    # knucleotide.clj-8.clj use the specified number of
	    # threads in parallel.  With an empty string, the default
	    # is 2 more threads than the number of available
	    # processors.
#	    clj*)  EXTRA_LANG_ARGS="1"
	    clj*)  EXTRA_LANG_ARGS=""
		;;
	esac

	echo
	echo "benchmark: $BENCHMARK  language: $L  test: $T"
	IN=./input/${T}-input.txt
	OUT=${OUTPUT_DIR}/${T}-${L}-output.txt
	CONSOLE=${OUTPUT_DIR}/${T}-${L}-console.txt
	case $L in
	    clj*|java)
		echo "( ${CMD} ${IN} ${OUT} ${EXTRA_LANG_ARGS} ) 2>&1 | tee ${CONSOLE}"
		( ${CMD} ${IN} ${OUT} ${EXTRA_LANG_ARGS} ) 2>&1 | tee ${CONSOLE}
		;;
	    *)
		echo "( time ${CMD} ${EXTRA_LANG_ARGS} < ${IN} > ${OUT} ) 2>&1 | tee ${CONSOLE}"
		( time ${CMD} ${EXTRA_LANG_ARGS} < ${IN} > ${OUT} ) 2>&1 | tee ${CONSOLE}
		;;
	esac
	cmp_and_rm_2nd_if_correct ${OUTPUT_DIR}/${T}-expected-output.txt ${OUT} 2>&1 | tee -a ${CONSOLE}
    done
done

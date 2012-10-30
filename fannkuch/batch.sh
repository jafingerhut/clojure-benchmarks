#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir -p $OUTPUT_DIR

BENCHMARK="fannkuch"

# No Clojure version created yet.

#ALL_LANGUAGES="sbcl perl ghc java clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3 clj-1.3-alpha4"
ALL_LANGUAGES="java ${ALL_BENCHMARK_CLOJURE_VERSIONS}"
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
            LANGUAGES="$LANGUAGES clj-${SHORT_CLJ_VERSION_STR}"
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
	quick)  N=7
	    ;;
	medium) N=10
	    ;;
	long)   N=12
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
	    perl) CMD="$PERL fannkuch.perl"
		;;
	    ghc) CMD=./ghc-run.sh
		( ./ghc-compile.sh ) >& ${OUTPUT_DIR}/ghc-compile-log.txt
	esac
	case $L in
	    sbcl) EXTRA_LANG_ARGS="1"
		;;
	    # Put a number between the double quotes to make
	    # fannkuch.clj-9.clj use the specified number of threads
	    # in parallel.  With an empty string, the default is 2
	    # more threads than the number of available processors.
	    clj*)  EXTRA_LANG_ARGS=""
		;;
	esac
	
	echo
	echo "benchmark: $BENCHMARK  language: $L  test: $T"
	OUT=${OUTPUT_DIR}/${T}-${L}-output.txt
	CONSOLE=${OUTPUT_DIR}/${T}-${L}-console.txt
	case $L in
	    clj*|java)
		echo "( ${CMD} ${OUT} ${N} ${EXTRA_LANG_ARGS} ) 2>&1 | tee ${CONSOLE}"
		( ${CMD} ${OUT} ${N} ${EXTRA_LANG_ARGS} ) 2>&1 | tee ${CONSOLE}
		;;
	    *)
		echo "( time ${CMD} ${N} > ${OUT} ) 2>&1 | tee ${CONSOLE}"
		( time ${CMD} ${N} ${EXTRA_LANG_ARGS} > ${OUT} ) 2>&1 | tee ${CONSOLE}
		;;
	esac
	cmp_and_rm_2nd_if_correct ${OUTPUT_DIR}/${T}-expected-output.txt ${OUT} 2>&1 | tee -a ${CONSOLE}
    done
done

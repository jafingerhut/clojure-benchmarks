#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir -p $OUTPUT_DIR

BENCHMARK="revcomp"

# SBCL 1.0.23 works on Mac OS X, but SBCL 1.0.29 doesn't like opening
# /dev/stdin with :element-type '(unsigned-byte 8) for some reason.
# SBCL 1.0.39 seems to work fine.

#ALL_LANGUAGES="sbcl perl ghc java clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3 clj-1.3-alpha4"
# + Note that revcomp.clj-15.clj does not compile with Clojure
#   versions 1.2 or 1.2.1 because of its use of *unchecked-math*.
# + It compiles but throws an exception with version 1.3-alpha5 and
#   1.3-alpha6 because it attempts to set! *unchecked-math*.
# + It compiles and runs correctly with 1.3-alpha6 through 1.4-alpha2,
#   but is *very* slow due to reflection on line 108 when attempting
#   to do = on (aget read-buf i) and gt, both of which should be of
#   type byte.
#
# Thus I will only include Clojure versions 1.4-alpha3 and later for
# this program.
ALL_LANGUAGES="java `all_benchmark_clojure_versions_except 1.2 1.2.1 1.3-alpha5 1.3-alpha6 1.3-alpha7 1.3-alpha8 1.3-beta1 1.3-beta2 1.3-beta3 1.3 1.4-alpha1 1.4-alpha2`"
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
	echo "benchmark: $BENCHMARK  language: $L  test: $T"
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
	cmp_and_rm_2nd_if_correct ${OUTPUT_DIR}/${T}-expected-output.txt ${OUT} 2>&1 | tee -a ${CONSOLE}
    done
done

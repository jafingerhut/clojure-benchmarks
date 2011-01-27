#! /bin/bash

# Uncomment this line to make the script only do echoing, no "real work"
#ECHO_ONLY="echo"
# Uncomment this line to make the script do real work
ECHO_ONLY=""

ROOT=`pwd`

#    1>&2 echo "usage: `basename $0` [ benchmark-name ... ]"

if [ $# -ge 1 ]; then
    BENCHMARK="$@"
else
    BENCHMARK="binarytrees fannkuch fannkuchredux fasta knucleotide mandelbrot nbody regexdna revcomp revlines spectralnorm"
fi
#echo "ROOT: $ROOT  BENCHMARK: $BENCHMARK"
#exit 0

OS=`uname -o 2>/dev/null`
if [ $? != 0 ]; then
    # Then likely we are running on a Mac OS X system with the default
    # uname installation, which accepts -s but not -o option.
    OS=`uname -s 2>/dev/null`
fi

JROCKIT=0

#set -ex

COMMON_ARGS="--verbose --jvm-gc-stats --alpha 1.05 --sorted-summary --results-file ${ROOT}/results.xml"
#COMMON_ARGS="--verbose --jvm-gc-stats --alpha 1.05 --sorted-summary --results-file ${ROOT}/results.xml --sweep-only --num-mb-values 2"

# Useful when testing hello.clj memory use
EXTRA_ARGS="--min 1 --precision 1"


for b in $BENCHMARK
do
    cd "${ROOT}/$b"
    #${ECHO_ONLY} ./java-compile.sh
    ${ECHO_ONLY} ./clj-compile.sh clj-1.2
    ${ECHO_ONLY} ./clj-compile.sh clj-1.3-alpha1
    ${ECHO_ONLY} ./clj-compile.sh clj-1.3-alpha3

    case $b in
    binarytrees)
        BENCHMARK_ARGS="binarytrees 20"
	INFILE=""
	;;
    fannkuch)
        BENCHMARK_ARGS="fannkuch 12"
	INFILE=""
	;;
    fannkuchredux)
        BENCHMARK_ARGS="fannkuchredux 12"
	INFILE=""
	;;
    fasta)
        BENCHMARK_ARGS="fasta 25000000"
	INFILE=""
	;;
    knucleotide)
        # TBD: Would be nice to have infrastructure that makes it easy
        # to run the same source file with same "size" but different
        # command line args, like having "1" below present, or absent.
        BENCHMARK_ARGS="knucleotide 1"
	INFILE="input/long-input.txt"
	;;
    mandelbrot)
        BENCHMARK_ARGS="mandelbrot 1"
	INFILE=""
	;;
    nbody)
        BENCHMARK_ARGS="nbody"
	INFILE=""
	;;
    regexdna)
        BENCHMARK_ARGS="regexdna"
	INFILE="input/long-input.txt"
	;;
    revcomp)
        BENCHMARK_ARGS="revcomp"
	INFILE="input/long-input.txt"
	;;
    revlines)
        BENCHMARK_ARGS="revlines"
	INFILE="input/long-input.txt"
	;;
    spectralnorm)
        BENCHMARK_ARGS="spectralnorm 5500"
	INFILE=""
	;;
    *)
        1>&2 echo "Unrecognized value of variable b: $b"
        exit 1
        ;;
    esac

    # TBD: Add Java support
    for lang in clj-1.2 clj-1.3-alpha1 clj-1.3-alpha3
    do
	echo "----------------------------------------"
	echo $b $lang
	echo "----------------------------------------"
	echo ""

	OUTFILE="output/long-${lang}-output.txt"
	case $lang in
	clj-1.2)
	    CP1="${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar"
	    CP2="./obj/${lang}"
	    ;;
        clj-1.3-alpha1)
	    CP1="${HOME}/lein/clj-1.3.0-alpha1/lib/clojure-1.3.0-alpha1.jar"
	    CP2="./obj/${lang}"
	    ;;
        clj-1.3-alpha3)
	    CP1="${HOME}/lein/clj-1.3.0-alpha3/lib/clojure-1.3.0-alpha3.jar"
	    CP2="./obj/${lang}"
	    ;;
	*)
	    ;;
	esac

	EXP_OUTFILE="output/long-expected-output.txt"

        if [ "$OS" == "Cygwin" ]
	then
	    PS_INFILE=`cygpath -w ${INFILE}`
	    PS_OUTFILE=`cygpath -w ${OUTFILE}`
	    PS_EXP_OUTFILE=`cygpath -w ${EXP_OUTFILE}`
	    PS_CLASSPATH="`cygpath -w ${CP1};`cygpath -w ${CP2}"
	else
	    PS_INFILE=${INFILE}
	    PS_OUTFILE=${OUTFILE}
	    PS_EXP_OUTFILE=${EXP_OUTFILE}
	    PS_CLASSPATH="${CP1}:${CP2}"
	fi
	if [ "${INFILE}" == "" ]
	then
	    INFILE_ARGS=""
	else
	    INFILE_ARGS="--input ${PS_INFILE}"
	fi
	OUTFILE_ARGS="--output ${PS_OUTFILE}"

        if [ "$OS" == "Cygwin" ]
	then
	    if [ $JROCKIT -eq 1 ]
	    then
		${ECHO_ONLY} "${ROOT}/bin/tmcurve" ${COMMON_ARGS} ${EXTRA_ARGS} ${INFILE_ARGS} ${OUTFILE_ARGS} --check-output-cmd "diff --strip-trailing-cr --brief ${EXP_OUTFILE} ${OUTFILE}" \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -server -Xmx%mbm -classpath "${PS_CLASSPATH}" ${BENCHMARK_ARGS}
	    else
		${ECHO_ONLY} "${ROOT}/bin/tmcurve" ${COMMON_ARGS} ${EXTRA_ARGS} ${INFILE_ARGS} ${OUTFILE_ARGS} --check-output-cmd "diff --strip-trailing-cr --brief ${EXP_OUTFILE} ${OUTFILE}" \\Program\ Files\\Java\\jdk1.6.0_21\\bin\\java -server -Xmx%mbm -classpath "${PS_CLASSPATH}" ${BENCHMARK_ARGS}
	    fi
	else
	    ${ECHO_ONLY} "${ROOT}/bin/tmcurve" ${COMMON_ARGS} ${EXTRA_ARGS} ${INFILE_ARGS} ${OUTFILE_ARGS} --check "diff --strip-trailing-cr --brief ${EXP_OUTFILE} ${OUTFILE}" java -server -Xmx%mbm -classpath "${PS_CLASSPATH}" ${BENCHMARK_ARGS}
	fi
    done

    cd "${ROOT}"
done

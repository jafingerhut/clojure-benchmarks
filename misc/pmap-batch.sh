#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="pmap-testing"

TIME="/usr/bin/time -lp"
HPROF_OPTS="cpu=samples,depth=20,thread=y"

for TYPE in double int
do
    case $TYPE in
	double) NUM_JOBS=16
	    ;;
	int)    NUM_JOBS=64
	    ;;
    esac
    for THREADS in 1 2
    do
	echo
	echo "benchmark: $BENCHMARK"
	echo "type: $TYPE"
	echo "threads: $THREADS"
	OUT=output/pmap-${TYPE}-${THREADS}-out.txt
	HPROF_OUT=output/pmap-${TYPE}-${THREADS}-hprof.txt
	( $TIME java -agentlib:hprof=$HPROF_OPTS -cp $CLOJURE_CLASSPATH clojure.main pmap-testing.clj $TYPE $NUM_JOBS 1000000000 $THREADS ) 2>&1 | tee $OUT
	/bin/mv -f java.hprof.txt $HPROF_OUT
    done
done

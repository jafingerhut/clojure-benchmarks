#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="ParallelTest.java"

TIME="/usr/bin/time -lp"
HPROF_OPTS="cpu=samples,depth=20,thread=y"

for TYPE in newdouble double
do
    for THREADS in 1 2
    do
	case $THREADS in
	    1) PARSEQ=sequential
		;;
	    2) PARSEQ=parallel
		;;
	esac
	NUM_JOBS=2
	JOB_SIZE=10000000000
	echo
	echo "benchmark: $BENCHMARK"
	echo "type: $TYPE"
	echo "threads: $THREADS"
	OUT=output/ParallelTest-${TYPE}-${THREADS}-out.txt
	HPROF_OUT=output/ParallelTest-${TYPE}-${THREADS}-hprof.txt
	uname -a > $OUT
	java -version >> $OUT
	echo "( $TIME java -agentlib:hprof=$HPROF_OPTS ParallelTest $TYPE $NUM_JOBS $JOB_SIZE $PARSEQ ) 2>&1 | tee -a $OUT"
	( $TIME java -agentlib:hprof=$HPROF_OPTS ParallelTest $TYPE $NUM_JOBS $JOB_SIZE $PARSEQ ) 2>&1 | tee -a $OUT
	/bin/mv -f java.hprof.txt $HPROF_OUT
    done
done

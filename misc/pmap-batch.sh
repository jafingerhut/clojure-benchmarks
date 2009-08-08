#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="pmap-testing"

TIME="/usr/bin/time -lp"
HPROF_OPTS="cpu=samples,depth=20,thread=y"

#for TYPE in int long float-primitive double double2 double-primitive
for TYPE in double1 double2
do
    JOB_SIZE=1000000000
    case $TYPE in
	int|long) NUM_JOBS=64
	    ;;
	float*)   NUM_JOBS=16
	    ;;
	double1|double2)  NUM_JOBS=2
	    JOB_SIZE=10000000000
	    ;;
	double*)  NUM_JOBS=16
#	double*)  NUM_JOBS=4
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
	uname -a > $OUT
	java -version >> $OUT
	( $TIME java -agentlib:hprof=$HPROF_OPTS -cp $CLOJURE_CLASSPATH clojure.main pmap-testing.clj $TYPE $NUM_JOBS $JOB_SIZE $THREADS ) 2>&1 | tee -a $OUT
	/bin/mv -f java.hprof.txt $HPROF_OUT
    done
done

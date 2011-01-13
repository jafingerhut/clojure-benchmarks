#! /bin/bash

source ../env.sh

OUTPUT_DIR=./output
mkdir $OUTPUT_DIR

BENCHMARK="ParallelTest.java"

TIME="/usr/bin/time -p"
HPROF_OPTS="cpu=samples,depth=20,thread=y"
#JAVA_PROFILING_OPTS="-agentlib:hprof=$HPROF_OPTS"
JAVA_PROFILING_OPTS=""
JVM_OPTS="-server -Xmx1024m"

#for TYPE in newdoubleB newdoubleA double cachebuster1
#for TYPE in newdoubleAInt2 newdoubleAInt4 newdoubleAInt8 newdoubleAInt16

for TYPE in newdoubleC
do
    for THREADS in 1 2
    do
	JOB_SIZE=1000000000
	echo
	echo "benchmark: $BENCHMARK"
	echo "type: $TYPE"
	echo "threads: $THREADS"
	OUT=output/ParallelTest-${TYPE}-${THREADS}-out.txt
	HPROF_OUT=output/ParallelTest-${TYPE}-${THREADS}-hprof.txt
	uname -a > $OUT
	"${JAVA}" -version >> $OUT
	echo "( $TIME ${JAVA} $JVM_OPTS $JAVA_PROFILING_OPTS -classpath ${JAVA_OBJ_DIR} ParallelTest $TYPE $THREADS $JOB_SIZE ) 2>&1 | tee -a $OUT"
	      ( $TIME "${JAVA}" $JVM_OPTS $JAVA_PROFILING_OPTS -classpath "${JAVA_OBJ_DIR}" ParallelTest $TYPE $THREADS $JOB_SIZE) 2>&1 | tee -a $OUT
	/bin/mv -f java.hprof.txt $HPROF_OUT
    done
done

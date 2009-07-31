#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

#JVM_MEM_OPTS="-Xmx1536m -XX:NewRatio=2 -XX:+UseParallelGC"
#JVM_MEM_OPTS="-Xmx1536m -XX:NewRatio=5 -XX:+UseParallelGC"
#JVM_MEM_OPTS="-Xmx1024m"

# (1)
# reverse-lines.clj-1.clj worked with the following line, -client
# instead of -server, and this input file:
# 4 lrwxrwxr-x 1 andy andy 39 2009-07-30 18:38 long-input.txt -> long-input-only-one-biggest-dna-seq.txt
#JVM_MEM_OPTS="-Xmx768m -XX:NewRatio=2 -XX:+UseParallelGC"

# (2)
# reverse-lines.clj-1.clj failed with the following line, -client,
# with the same input file as (1), with this error:
# Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded (reverse-lines.clj-1.clj:0)
# JVM_MEM_OPTS="-Xmx512m -XX:NewRatio=2 -XX:+UseParallelGC"

# (3)
# same as (2), but with -server instead of -client
#JVM_MEM_OPTS="-Xmx512m -XX:NewRatio=2 -XX:+UseParallelGC"


########################################

# (4)
# I killed it after 39 mins.  jconsole said that it had used 29 of
# those 39 minutes in GC.  It had only written 77 Mbytes out of 124
# Mbytes of output that it should eventually write.
#JVM_MEM_OPTS="-client -Xmx1024m"

# (5) Failed after about 8 mins, without having written any output.
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revcomp.clj-5.clj:0)
# ...
#Caused by: java.lang.OutOfMemoryError: Java heap space
#	at java.util.Arrays.copyOfRange(Arrays.java:3209)
#	at java.lang.String.<init>(String.java:216)
#	at java.io.BufferedReader.readLine(BufferedReader.java:331)
#	at java.io.BufferedReader.readLine(BufferedReader.java:362)
# ...
#JVM_MEM_OPTS="-client -Xmx1024m -XX:NewRatio=2 -XX:+UseParallelGC"

# (6) Took about 2 mins before it crashed, without producing any output.
#Exception in thread "main" java.lang.OutOfMemoryError: Java heap space (revcomp.clj-5.clj:0)
# ...
#Caused by: java.lang.OutOfMemoryError: Java heap space
#	at java.util.Arrays.copyOfRange(Arrays.java:3209)
#	at java.lang.String.<init>(String.java:216)
#	at java.io.BufferedReader.readLine(BufferedReader.java:331)
#	at java.io.BufferedReader.readLine(BufferedReader.java:362)
# ...
#JVM_MEM_OPTS="-client -Xmx1024m -XX:+UseParallelGC"


# With source file revcomp.clj-6.clj, and the 'shortened long input',
# the one that is only the longest FASTA DNA sequence in the last half
# of the original long-input.txt, this option ran out of memory while
# reading the input.
#JVM_MEM_OPTS="-client -Xmx1024m -XX:+UseParallelGC"

JVM_MEM_OPTS="-client -Xmx1024m"


#JMX_MONITORING=-Dcom.sun.management.jmxremote

CLJ_PROG=revcomp.clj-6.clj

$JAVA $JVM_MEM_OPTS $JMX_MONITORING ${JAVA_PROFILING} -cp ${CLOJURE_CLASSPATH} clojure.main $CLJ_PROG "$@"

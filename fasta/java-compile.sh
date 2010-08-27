#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP fasta.java-2.java fasta.java
mkdir -p obj/java
$JAVAC -d obj/java fasta.java
$RM fasta.java

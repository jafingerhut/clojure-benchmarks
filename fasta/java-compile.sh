#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP fasta.java-2.java fasta.java
$JAVAC fasta.java
$RM fasta.java

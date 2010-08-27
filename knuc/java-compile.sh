#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
#$CP knucleotide.java-3.java knucleotide.java
$CP knucleotide.andys.java knucleotide.java
mkdir -p obj/java
$JAVAC -d obj/java knucleotide.java
$RM knucleotide.java

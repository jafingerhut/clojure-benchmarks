#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP knucleotide.java-2.java knucleotide.java
mkdir -p obj/java
$JAVAC -d obj/java knucleotide.java
$RM knucleotide.java

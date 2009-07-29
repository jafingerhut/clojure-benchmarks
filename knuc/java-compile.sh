#! /bin/sh

source ../env.sh

$JAVA -version
$JAVAC -version
#$CP knucleotide.java-3.java knucleotide.java
$CP knucleotide.andys.java knucleotide.java
$JAVAC knucleotide.java
$RM knucleotide.java

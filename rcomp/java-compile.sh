#! /bin/sh

source ../env.sh

$JAVA -version
$JAVAC -version
$CP revcomp.java-4.java revcomp.java
#$CP revcomp.andys.java revcomp.java
$JAVAC revcomp.java
$RM revcomp.java

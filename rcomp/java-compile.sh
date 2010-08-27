#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP revcomp.java-4.java revcomp.java
#$CP revcomp.andys.java revcomp.java
mkdir -p obj/java
$JAVAC -d obj/java revcomp.java
$RM revcomp.java

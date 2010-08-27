#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP revlines.java-1.java revlines.java
mkdir -p obj/java
$JAVAC -d obj/java revlines.java
$RM revlines.java

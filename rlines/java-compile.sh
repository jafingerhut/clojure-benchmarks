#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP revlines.java-1.java revlines.java
$JAVAC revlines.java
$RM revlines.java

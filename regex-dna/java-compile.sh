#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP regexdna.java-2.java regexdna.java
$JAVAC regexdna.java
$RM regexdna.java

#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP regexdna.java-2.java regexdna.java
mkdir -p obj/java
$JAVAC -d obj/java regexdna.java
$RM regexdna.java

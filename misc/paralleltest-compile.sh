#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
mkdir -p obj/java
$JAVAC -d obj/java CacheBuster1.java
$JAVAC -d obj/java ParallelTest.java

#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP nbody.java-2.java nbody.java
mkdir -p obj/java
$JAVAC -d obj/java nbody.java
$RM nbody.java

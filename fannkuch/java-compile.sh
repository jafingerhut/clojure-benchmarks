#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
#$CP fannkuch.java-4.java fannkuch.java
mkdir -p obj/java
$JAVAC -d obj/java fannkuch.java
#$RM fannkuch.java

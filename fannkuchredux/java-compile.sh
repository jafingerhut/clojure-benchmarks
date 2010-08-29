#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
# No need to rename this file
#$CP fannkuchredux.java fannkuchredux.java
mkdir -p obj/java
$JAVAC -d obj/java fannkuchredux.java

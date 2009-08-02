#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP nbody.java-2.java nbody.java
$JAVAC nbody.java
$RM nbody.java

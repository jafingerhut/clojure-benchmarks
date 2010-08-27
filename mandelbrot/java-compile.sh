#! /bin/bash

source ../env.sh

$JAVA -version
$JAVAC -version
$CP mandelbrot.java-3.java mandelbrot.java
mkdir -p obj/java
$JAVAC -d obj/java mandelbrot.java
$RM mandelbrot.java

#! /bin/sh

source ../env.sh

$JAVA -version
$JAVAC -version
$CP mandelbrot.java-3.java mandelbrot.java
$JAVAC mandelbrot.java
$RM mandelbrot.java

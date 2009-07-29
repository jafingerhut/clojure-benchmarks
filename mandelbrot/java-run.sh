#! /bin/sh

source ../env.sh

$JAVA -server -Xmx2048m mandelbrot "$@"

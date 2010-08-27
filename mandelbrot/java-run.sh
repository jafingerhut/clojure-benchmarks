#! /bin/bash

source ../env.sh

$JAVA -server -Xmx2048m -cp obj/java mandelbrot "$@"

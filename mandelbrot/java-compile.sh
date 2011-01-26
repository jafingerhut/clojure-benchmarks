#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
"${CP}" mandelbrot.java-6.java mandelbrot.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" mandelbrot.java
"${RM}" mandelbrot.java

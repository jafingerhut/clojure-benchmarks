#! /bin/bash

source ../env.sh

"${JAVA}" -server -Xmx2048m -classpath "${JAVA_OBJ_DIR}" mandelbrot "$@"

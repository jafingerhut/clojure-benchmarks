#! /bin/bash

source ../env.sh

"${JAVA}" -server -Xmx1024m -classpath "${JAVA_OBJ_DIR}" mandelbrot "$@"

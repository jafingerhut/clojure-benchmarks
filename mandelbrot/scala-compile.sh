#! /bin/bash

source ../env.sh

"${SCALAC}" -version
mkdir -p "${SCALA_OBJ_DIR}"
"${CP}" mandelbrot.scala-3.scala "${SCALA_OBJ_DIR}/mandelbrot.scala"
cd "${SCALA_OBJ_DIR}"
"${SCALAC}" mandelbrot.scala

#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

"${CP}" mandelbrot.clj-6.clj "${CLJ_OBJ_DIR}/mandelbrot.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile mandelbrot

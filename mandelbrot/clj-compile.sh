#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: `basename $0` <clj-version>"
    exit 1
fi
CLJ_VERSION="$1"
shift

source ../env.sh
"${RM}" -fr "${CLJ_OBJ_DIR}"
mkdir -p "${CLJ_OBJ_DIR}"

"${CP}" mandelbrot.clj-9-par-1.3.clj "${CLJ_OBJ_DIR}/mandelbrot.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile mandelbrot

#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

cp revlines.clj-1.clj "${CLJ_OBJ_DIR}/revlines.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile revlines

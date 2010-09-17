#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

cp revcomp.clj-10.clj "${CLJ_OBJ_DIR}/revcomp.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile revcomp

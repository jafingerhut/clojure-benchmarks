#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

cp knucleotide.clj-8.clj "${CLJ_OBJ_DIR}/knucleotide.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile knucleotide

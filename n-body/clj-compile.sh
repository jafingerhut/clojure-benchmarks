#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

cp nbody.clj-12.clj "${CLJ_OBJ_DIR}/nbody.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile nbody

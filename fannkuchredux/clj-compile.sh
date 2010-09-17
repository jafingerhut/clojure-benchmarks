#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

"${CP}" fannkuchredux.clj-12.clj "${CLJ_OBJ_DIR}/fannkuchredux.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile fannkuchredux

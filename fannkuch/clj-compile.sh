#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

"${CP}" fannkuch.clj-11.clj "${CLJ_OBJ_DIR}/fannkuch.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile fannkuch

#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" pidigits.java

#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" CacheBuster1.java
"${JAVAC}" -d "${JAVA_OBJ_DIR}" ParallelTest.java

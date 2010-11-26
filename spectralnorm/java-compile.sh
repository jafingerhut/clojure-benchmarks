#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
"${CP}" spectralnorm.java-2.java spectralnorm.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" spectralnorm.java
"${RM}" spectralnorm.java

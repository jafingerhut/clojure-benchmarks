#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
"${CP}" regexdna.java-5.java regexdna.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" regexdna.java
"${RM}" regexdna.java

#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
"${CP}" binarytrees.java-1.java binarytrees.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" binarytrees.java
"${RM}" binarytrees.java

#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
"${CP}" fasta.java-3.java fasta.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" fasta.java
"${RM}" fasta.java

#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
# No need to rename this file
#"${CP}" binarytrees.java binarytrees.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" binarytrees.java
#"${RM}" binarytrees.java

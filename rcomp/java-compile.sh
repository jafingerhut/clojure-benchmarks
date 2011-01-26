#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
#"${CP}" revcomp.java revcomp.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" revcomp.java
#"${RM}" revcomp.java

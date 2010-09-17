#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
# No need to rename file
#"${CP}" fannkuch.java fannkuch.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" fannkuch.java
#"${RM}" fannkuch.java

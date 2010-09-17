#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
# No need to rename this file
#"${CP}" fannkuchredux.java fannkuchredux.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" fannkuchredux.java
#"${RM}" fannkuch.java

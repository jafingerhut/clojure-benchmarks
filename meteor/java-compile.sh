#! /bin/bash

source ../env.sh

"${JAVA}" -version
"${JAVAC}" -version
#"${CP}" meteor.java-2.java meteor.java
"${CP}" meteor.java-1.java meteor.java
mkdir -p "${JAVA_OBJ_DIR}"
"${JAVAC}" -d "${JAVA_OBJ_DIR}" meteor.java
"${RM}" meteor.java

#! /bin/bash

source ../env.sh

"${JAVA}" -Xmx2048m -server -classpath "${JAVA_OBJ_DIR}" knucleotide "$@"

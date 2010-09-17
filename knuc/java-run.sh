#! /bin/bash

source ../env.sh

"${JAVA}" -Xmx1536m -server -classpath "${JAVA_OBJ_DIR}" knucleotide "$@"

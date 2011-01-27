#! /bin/bash

source ../env.sh

"${SCALAC}" -version
mkdir -p "${SCALA_OBJ_DIR}"
"${CP}" nbody.scala "${SCALA_OBJ_DIR}"
cd "${SCALA_OBJ_DIR}"
"${SCALAC}" nbody.scala

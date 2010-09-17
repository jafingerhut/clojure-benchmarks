#! /bin/bash

source ../env.sh

"${JAVA}" -server -classpath "${JAVA_OBJ_DIR}" fannkuchredux "$@"

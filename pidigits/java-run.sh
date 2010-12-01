#! /bin/bash

source ../env.sh

# Note: This program gives an error when attempting to run it without
# a "jpargmp" library installed.  I do not know where to get this
# library from.  Try running the GCC version of the program instead,
# if you wish to create an expected output file.

"${JAVA}" -Djava.library.path=Include/java -server -classpath "${JAVA_OBJ_DIR}" pidigits "$@"

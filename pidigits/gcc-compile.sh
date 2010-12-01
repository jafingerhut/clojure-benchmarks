#! /bin/bash

source ../env.sh

gcc --version
mkdir -p "${GCC_OBJ_DIR}"
gcc -pipe -Wall -O3 -fomit-frame-pointer -march=native -lgmp pidigits.gcc-4.c -o "${GCC_OBJ_DIR}/pidigits.gcc-4.gcc_run"


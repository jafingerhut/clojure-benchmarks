#! /bin/bash

source ../env.sh

gcc --version
mkdir -p "${GCC_OBJ_DIR}"

if [ "$OS" == "Darwin" ]
then
    # -march=native causes problems for Apple's gcc
    CFLAGS="-pipe -Wall -O3 -fomit-frame-pointer"
elif [ "$OS" == "Cygwin" ]
then
    CFLAGS="-Wall -O3"
else
    CFLAGS="-pipe -Wall -O3 -fomit-frame-pointer -march=native"
fi

"${CP}" meteor.gcc meteor.c
gcc $CFLAGS meteor.c $LDFLAGS -o "${GCC_OBJ_DIR}/meteor.gcc_run"
"${RM}" meteor.c

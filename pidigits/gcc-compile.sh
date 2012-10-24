#! /bin/bash

source ../env.sh

gcc --version
mkdir -p "${GCC_OBJ_DIR}"

if [ "$OS" == "Darwin" ]
then
    # -march=native causes problems for Apple's gcc
    # If you use MacPorts to install the gmp package, then its header
    # and compiled library files will be in the indicated directories.
    if [ -r /opt/local/include/gmp.h ]
    then
	CFLAGS="-I/opt/local/include -L/opt/local/lib -pipe -Wall -O3 -fomit-frame-pointer -lgmp"
    else
	1>&2 echo "On Mac OS X, one way to get the required gmp library is by installing MacPorts and then 'sudo port install gmp'.  Or if you've installed it some other way, modify script $0 to point at it."
	exit 1
    fi
elif [ "$OS" == "Cygwin" ]
then
    CFLAGS="-Wall -O3"
    LDFLAGS="-lgmp"
else
    CFLAGS="-pipe -Wall -O3 -fomit-frame-pointer -march=native"
    LDFLAGS="-lgmp"
fi

gcc $CFLAGS pidigits.gcc-4.c $LDFLAGS -o "${GCC_OBJ_DIR}/pidigits.gcc-4.gcc_run"

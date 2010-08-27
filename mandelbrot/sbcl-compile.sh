#! /bin/bash

source ../env.sh

$SBCL --version
mkdir -p obj/sbcl
cp mandelbrot.sbcl obj/sbcl
cd obj/sbcl
$SBCL --no-userinit --no-sysinit --load ../../mandelbrot.sbcl_compile

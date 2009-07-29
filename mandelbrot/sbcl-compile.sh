#! /bin/sh

source ../env.sh

$SBCL --version
$SBCL --no-userinit --no-sysinit --load mandelbrot.sbcl_compile

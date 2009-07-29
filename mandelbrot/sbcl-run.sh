#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

$SBCL --noinform --core sbcl.core --no-userinit --no-sysinit --load mandelbrot.sbcl_run "$@"

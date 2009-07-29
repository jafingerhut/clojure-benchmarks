#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

$SBCL --noinform --core sbcl.core --no-userinit --no-sysinit --load revcomp.sbcl_run 0

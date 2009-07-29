#! /bin/bash

source ../env.sh

# Read input from stdin
# Write output to stdout

$SBCL --noinform --core sbcl.core --no-userinit --no-sysinit --load knucleotide.sbcl-3.sbcl_run 0

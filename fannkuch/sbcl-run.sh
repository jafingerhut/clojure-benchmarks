#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

$SBCL --noinform --core sbcl.core --no-userinit --no-sysinit --load fannkuch.sbcl-2.sbcl_run "$@"

#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

"${SBCL}" --dynamic-space-size 1000 --noinform --core "${SBCL_OBJ_DIR}/sbcl.core" --no-userinit --no-sysinit --load regexdna.sbcl-3.sbcl_run 0

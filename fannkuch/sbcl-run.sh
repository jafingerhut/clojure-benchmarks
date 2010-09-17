#! /bin/bash

# Read input from stdin
# Write output to stdout

source ../env.sh

"${SBCL}" --noinform --core "${SBCL_OBJ_DIR}/sbcl.core" --no-userinit --no-sysinit --load fannkuch.sbcl-2.sbcl_run "$@"

# Experiment to verify that I can put all of fannkuch.sbcl-2.sbcl_run
# contents on command line, if I wish.

#"${SBCL}" --noinform --core sbcl.core --no-userinit --no-sysinit --eval "(proclaim '(optimize (speed 3) (safety 0) (debug 0) (compilation-speed 0) (space 0)))" --eval "(main)" --eval "(quit)" "$@"

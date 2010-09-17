#! /bin/bash

source ../env.sh

"${SBCL}" --version
mkdir -p "${SBCL_OBJ_DIR}"
"${CP}" fannkuch.sbcl-2.sbcl fannkuch.sbcl-2.sbcl_compile "${SBCL_OBJ_DIR}"
cd "${SBCL_OBJ_DIR}"
"${SBCL}" --no-userinit --no-sysinit --load fannkuch.sbcl-2.sbcl_compile

# Experiment to verify that I can put all of fannkuch.sbcl-2.sbcl_compile
# contents on the command line, if I wish.

#SBCL_SOURCE_FILE="fannkuch.sbcl-2.sbcl"
#
#"${SBCL}" --no-userinit --no-sysinit --eval "(proclaim '(optimize (speed 3) (safety 0) (debug 0) (compilation-speed 0) (space 0)))" --eval "(handler-bind ((sb-ext:defconstant-uneql (lambda (c) (abort c)))) (load (compile-file \"${SBCL_SOURCE_FILE}\")))" --eval "(save-lisp-and-die \"sbcl.core\" :purify t)"

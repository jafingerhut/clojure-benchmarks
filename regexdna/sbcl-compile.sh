#! /bin/bash

source ../env.sh

"${SBCL}" --version
mkdir -p "${SBCL_OBJ_DIR}"
"${CP}" regexdna.sbcl-3.sbcl regexdna.sbcl-3.sbcl_compile "${SBCL_OBJ_DIR}"
cd "${SBCL_OBJ_DIR}"
"${SBCL}" --no-userinit --no-sysinit --load regexdna.sbcl-3.sbcl_compile

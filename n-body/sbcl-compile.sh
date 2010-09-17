#! /bin/bash

source ../env.sh

"${SBCL}" --version
mkdir -p "${SBCL_OBJ_DIR}"
"${CP}" nbody.sbcl nbody.sbcl_compile "${SBCL_OBJ_DIR}"
cd "${SBCL_OBJ_DIR}"
"${SBCL}" --no-userinit --no-sysinit --load nbody.sbcl_compile

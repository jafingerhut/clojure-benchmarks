#! /bin/bash

source ../env.sh

"${SBCL}" --version
mkdir -p "${SBCL_OBJ_DIR}"
"${CP}" knucleotide.sbcl-3.sbcl knucleotide.sbcl-3.sbcl_compile "${SBCL_OBJ_DIR}"
cd "${SBCL_OBJ_DIR}"
"${SBCL}" --no-userinit --no-sysinit --load knucleotide.sbcl-3.sbcl_compile

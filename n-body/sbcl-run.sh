#! /bin/bash

source ../env.sh

"${SBCL}" --noinform --core "${SBCL_OBJ_DIR}/sbcl.core" --no-userinit --no-sysinit --load nbody.sbcl_run "$@"

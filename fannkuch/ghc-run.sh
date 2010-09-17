#! /bin/bash

source ../env.sh

"${GHC_OBJ_DIR}/fannkuch.ghc-5.ghc_run" +RTS -N5 -RTS "$@"

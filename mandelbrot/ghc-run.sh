#! /bin/bash

source ../env.sh

"${GHC_OBJ_DIR}/mandelbrot.ghc-2.ghc_run" +RTS -N4 -RTS "$@"

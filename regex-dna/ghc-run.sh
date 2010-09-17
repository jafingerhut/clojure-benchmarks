#! /bin/bash

source ../env.sh

"${GHC_OBJ_DIR}/regexdna.ghc-4.ghc_run" +RTS -N4 -qw -RTS 0

#! /bin/bash

source ../env.sh

"${GHC_OBJ_DIR}/knucleotide.ghc-3.ghc_run" +RTS -N4 -RTS 0

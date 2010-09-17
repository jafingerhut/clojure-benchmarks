#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" revcomp.ghc-3.ghc "${GHC_OBJ_DIR}/revcomp.ghc-3.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -funfolding-use-threshold=32 -optc-O3 revcomp.ghc-3.hs -o revcomp.ghc-3.ghc_run

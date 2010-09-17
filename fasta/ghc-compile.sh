#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" fasta.ghc-2.ghc "${GHC_OBJ_DIR}/fasta.ghc-2.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 fasta.ghc-2.hs -o fasta.ghc-2.ghc_run

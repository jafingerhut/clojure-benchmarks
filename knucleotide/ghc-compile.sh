#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" knucleotide.ghc-3.ghc "${GHC_OBJ_DIR}/knucleotide.ghc-3.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -fvia-C -optc-O3 -threaded knucleotide.ghc-3.hs -o knucleotide.ghc-3.ghc_run

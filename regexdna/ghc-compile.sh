#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" regexdna.ghc-4.ghc "${GHC_OBJ_DIR}/regexdna.ghc-4.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -package regex-posix -optc-O3 -threaded regexdna.ghc-4.hs -o regexdna.ghc-4.ghc_run

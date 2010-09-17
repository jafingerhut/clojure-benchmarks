#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" fannkuch.ghc-5.ghc "${GHC_OBJ_DIR}/fannkuch.ghc-5.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -threaded fannkuch.ghc-5.hs -o fannkuch.ghc-5.ghc_run

#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" fannkuchredux.ghc-3.ghc "${GHC_OBJ_DIR}/fannkuchredux.ghc-3.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -O2 -fexcess-precision -fasm fannkuchredux.ghc-3.hs -o fannkuchredux.ghc-3.ghc_run

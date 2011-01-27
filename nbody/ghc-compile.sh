#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" nbody.ghc "${GHC_OBJ_DIR}/nbody.hs"
cd "${GHC_OBJ_DIR}"
#"${GHC}" --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O3 nbody.hs -o nbody.ghc_run
"${GHC}" --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O nbody.hs -o nbody.ghc_run

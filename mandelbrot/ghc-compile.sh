#! /bin/bash

source ../env.sh

mkdir -p "${GHC_OBJ_DIR}"
"${CP}" mandelbrot.ghc-2.ghc "${GHC_OBJ_DIR}/mandelbrot.ghc-2.hs"
cd "${GHC_OBJ_DIR}"
"${GHC}" --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 -optc-O3 -threaded mandelbrot.ghc-2.hs -o mandelbrot.ghc-2.ghc_run

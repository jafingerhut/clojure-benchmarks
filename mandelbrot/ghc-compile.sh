#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP mandelbrot.ghc-2.ghc obj/ghc/mandelbrot.ghc-2.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 -optc-O3 -threaded mandelbrot.ghc-2.hs -o mandelbrot.ghc-2.ghc_run

#! /bin/sh

source ../env.sh

$CP mandelbrot.ghc-2.ghc mandelbrot.ghc-2.hs
$GHC --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 -optc-O3 -threaded mandelbrot.ghc-2.hs -o mandelbrot.ghc-2.ghc_run
$RM mandelbrot.ghc-2.hs

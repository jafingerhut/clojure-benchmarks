#! /bin/sh

source ../env.sh

./mandelbrot.ghc-2.ghc_run +RTS -N4 -RTS "$@"

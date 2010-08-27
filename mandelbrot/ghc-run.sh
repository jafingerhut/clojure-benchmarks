#! /bin/bash

source ../env.sh

./obj/ghc/mandelbrot.ghc-2.ghc_run +RTS -N4 -RTS "$@"

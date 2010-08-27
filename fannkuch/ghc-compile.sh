#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP fannkuch.ghc-5.ghc obj/ghc/fannkuch.ghc-5.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -threaded fannkuch.ghc-5.hs -o fannkuch.ghc-5.ghc_run

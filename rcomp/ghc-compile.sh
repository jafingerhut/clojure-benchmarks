#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP revcomp.ghc-3.ghc obj/ghc/revcomp.ghc-3.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -funfolding-use-threshold=32 -optc-O3 revcomp.ghc-3.hs -o revcomp.ghc-3.ghc_run

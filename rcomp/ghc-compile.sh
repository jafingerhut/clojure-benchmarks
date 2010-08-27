#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP revcomp.ghc-2.ghc obj/ghc/revcomp.ghc-2.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -fvia-C -optc-O3 revcomp.ghc-2.hs -o revcomp.ghc-2.ghc_run

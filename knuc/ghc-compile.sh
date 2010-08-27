#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP knucleotide.ghc-3.ghc obj/ghc/knucleotide.ghc-3.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -fvia-C -optc-O3 -threaded knucleotide.ghc-3.hs -o knucleotide.ghc-3.ghc_run

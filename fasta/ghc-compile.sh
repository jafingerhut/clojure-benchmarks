#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP fasta.ghc-2.ghc obj/ghc/fasta.ghc-2.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 fasta.ghc-2.hs -o fasta.ghc-2.ghc_run

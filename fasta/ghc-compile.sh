#! /bin/sh

source ../env.sh

$CP fasta.ghc-2.ghc fasta.ghc-2.hs
$GHC --make -O2 -fglasgow-exts -optc-mfpmath=sse -optc-msse2 fasta.ghc-2.hs -o fasta.ghc-2.ghc_run
$RM fasta.ghc-2.hs

#! /bin/sh

source ../env.sh

$CP knucleotide.ghc-3.ghc knucleotide.ghc-3.hs
$GHC --make -O2 -fglasgow-exts -fvia-C -optc-O3 -threaded knucleotide.ghc-3.hs -o knucleotide.ghc-3.ghc_run
$RM knucleotide.ghc-3.hs

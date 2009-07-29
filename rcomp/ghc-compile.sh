#! /bin/bash

source ../env.sh

$CP revcomp.ghc-2.ghc revcomp.ghc-2.hs
$GHC --make -O2 -fglasgow-exts -fvia-C -optc-O3 revcomp.ghc-2.hs -o revcomp.ghc-2.ghc_run
$RM revcomp.ghc-2.hs

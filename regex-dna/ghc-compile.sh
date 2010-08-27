#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP regexdna.ghc-4.ghc obj/ghc/regexdna.ghc-4.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -package regex-posix -optc-O3 -threaded regexdna.ghc-4.hs -o regexdna.ghc-4.ghc_run

#! /bin/bash

source ../env.sh

$CP regexdna.ghc-4.ghc regexdna.ghc-4.hs
$GHC --make -O2 -fglasgow-exts -package regex-posix -optc-O3 -threaded regexdna.ghc-4.hs -o regexdna.ghc-4.ghc_run
$RM regexdna.ghc-4.hs

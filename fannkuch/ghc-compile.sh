#! /bin/bash

source ../env.sh

$CP fannkuch.ghc-5.ghc fannkuch.ghc-5.hs
$GHC --make -O2 -fglasgow-exts -threaded fannkuch.ghc-5.hs -o fannkuch.ghc-5.ghc_run
$RM fannkuch.ghc-5.hs

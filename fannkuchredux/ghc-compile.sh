#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP fannkuchredux.ghc-3.ghc obj/ghc/fannkuchredux.ghc-3.hs
cd obj/ghc
$GHC --make -O2 -fglasgow-exts -O2 -fexcess-precision -fasm fannkuchredux.ghc-3.hs -o fannkuchredux.ghc-3.ghc_run

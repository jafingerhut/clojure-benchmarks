#! /bin/bash

source ../env.sh

mkdir -p obj/ghc
$CP nbody.ghc obj/ghc/nbody.hs
cd obj/ghc
#$GHC --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O3 nbody.hs -o nbody.ghc_run
$GHC --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O nbody.hs -o nbody.ghc_run

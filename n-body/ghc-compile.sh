#! /bin/bash

source ../env.sh

$CP nbody.ghc nbody.hs
#$GHC --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O3 nbody.hs -o nbody.ghc_run
$GHC --make -O2 -fglasgow-exts -funbox-strict-fields -fbang-patterns -optc-O nbody.hs -o nbody.ghc_run
$RM nbody.hs

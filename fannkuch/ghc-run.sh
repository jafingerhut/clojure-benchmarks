#! /bin/bash

source ../env.sh

./obj/ghc/fannkuch.ghc-5.ghc_run +RTS -N5 -RTS "$@"

#! /bin/bash

source ../env.sh

./fannkuch.ghc-5.ghc_run +RTS -N5 -RTS "$@"

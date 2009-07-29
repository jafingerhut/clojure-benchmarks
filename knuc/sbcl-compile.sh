#! /bin/bash

source ../env.sh

$SBCL --version
$SBCL --no-userinit --no-sysinit --load knucleotide.sbcl-3.sbcl_compile

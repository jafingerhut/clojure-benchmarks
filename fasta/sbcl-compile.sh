#! /bin/bash

source ../env.sh

$SBCL --version
$SBCL --no-userinit --no-sysinit --load fasta.sbcl_compile

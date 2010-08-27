#! /bin/bash

source ../env.sh

$SBCL --version
mkdir -p obj/sbcl
cp fasta.sbcl obj/sbcl
cd obj/sbcl
$SBCL --no-userinit --no-sysinit --load ../../fasta.sbcl_compile

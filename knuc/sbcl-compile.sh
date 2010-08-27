#! /bin/bash

source ../env.sh

mkdir -p obj/sbcl
cp knucleotide.sbcl-3.sbcl obj/sbcl
cd obj/sbcl
$SBCL --version
$SBCL --no-userinit --no-sysinit --load ../../knucleotide.sbcl-3.sbcl_compile

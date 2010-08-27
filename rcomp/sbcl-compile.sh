#! /bin/bash

source ../env.sh

$SBCL --version
mkdir -p obj/sbcl
cp revcomp.sbcl obj/sbcl
cd obj/sbcl
$SBCL --no-userinit --no-sysinit --load ../../revcomp.sbcl_compile

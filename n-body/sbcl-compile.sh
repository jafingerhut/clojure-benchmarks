#! /bin/bash

source ../env.sh

$SBCL --version
mkdir -p obj/sbcl
cp nbody.sbcl obj/sbcl
cd obj/sbcl
$SBCL --no-userinit --no-sysinit --load ../../nbody.sbcl_compile

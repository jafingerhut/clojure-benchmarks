#! /bin/bash

source ../env.sh

$SBCL --version
mkdir -p obj/sbcl
cp regexdna.sbcl-3.sbcl obj/sbcl
cd obj/sbcl
$SBCL --no-userinit --no-sysinit --load ../../regexdna.sbcl-3.sbcl_compile

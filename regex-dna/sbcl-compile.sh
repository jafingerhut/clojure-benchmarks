#! /bin/bash

source ../env.sh

$SBCL --version
$SBCL --no-userinit --no-sysinit --load regexdna.sbcl-3.sbcl_compile

#! /bin/bash

source ../env.sh

$SBCL --version
$SBCL --no-userinit --no-sysinit --load fannkuch.sbcl-2.sbcl_compile

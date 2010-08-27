#! /bin/bash

source ../env.sh

$SBCL --noinform --core obj/sbcl/sbcl.core --no-userinit --no-sysinit --load fasta.sbcl_run "$@"

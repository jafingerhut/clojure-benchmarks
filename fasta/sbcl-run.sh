#! /bin/bash

source ../env.sh

$SBCL --noinform --core sbcl.core --no-userinit --no-sysinit --load fasta.sbcl_run "$@"

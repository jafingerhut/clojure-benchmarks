#! /bin/bash

source ../env.sh

$JAVA -Xmx2048m -server knucleotide "$@"

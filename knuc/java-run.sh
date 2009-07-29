#! /bin/sh

source ../env.sh

$JAVA -Xmx2048m -server knucleotide "$@"

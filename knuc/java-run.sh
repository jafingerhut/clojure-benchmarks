#! /bin/bash

source ../env.sh

$JAVA -Xmx2048m -server -cp obj/java knucleotide "$@"

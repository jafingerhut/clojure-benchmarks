#! /bin/bash

source ../env.sh

$JAVA -server -cp obj/java fasta "$@"

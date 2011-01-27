#! /bin/bash

for j in binarytrees fannkuch fannkuchredux fasta knucleotide mandelbrot nbody regexdna revcomp spectralnorm
do
    cd $j
    ./batch.sh "$@"
    cd ..
done

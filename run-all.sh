#! /bin/bash

for B in rcomp knuc mandelbrot fasta
do
    cd $B
    ./batch.sh
    cd ..
done

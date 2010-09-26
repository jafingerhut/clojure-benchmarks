#! /bin/bash

#for j in fannkuch fannkuchredux fasta knuc mandelbrot n-body rcomp regex-dna

for j in fannkuch fannkuchredux fasta knuc mandelbrot rcomp regex-dna
do
    cd $j
    ./batch.sh "$@"
    cd ..
done

#! /bin/bash

for j in fannkuch fannkuchredux fasta knuc mandelbrot n-body rcomp regex-dna rlines
do
    for CLJ_VERSION in clj-1.2.0 clj-1.3.0-alpha1
    do
        cd $j
        \rm -fr obj/${CLJ_VERSION}
        ./clj-compile.sh ${CLJ_VERSION}
        cd ..
    done
done

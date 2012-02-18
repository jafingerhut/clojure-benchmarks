#! /bin/bash

if [ $# -lt 1 ]
then
    CLJ_VERSIONS="clj-1.2.1 clj-1.3.0 clj-1.4-beta1"
else
    CLJ_VERSIONS="$@"
fi

for j in binarytrees fannkuch fannkuchredux fasta knucleotide mandelbrot meteor nbody pidigits regexdna revcomp revlines spectralnorm
do
    for CLJ_VERSION in ${CLJ_VERSIONS}
    do
        cd $j
        \rm -fr obj/${CLJ_VERSION}
        ./clj-compile.sh ${CLJ_VERSION}
        cd ..
    done
done

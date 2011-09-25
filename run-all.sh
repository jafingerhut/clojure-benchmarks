#! /bin/bash

# fannkuch works for Java and Clojure, but it is one of the slowest
# Clojure programs, partly because it has received less optimization
# attention because it is now considered obsolete on the Computer
# Language Benchmarks Game web site.  Leave it out unless someone
# really wants it by editing this file.

# pidigits works for Clojure version that does not use the GMP
# library, but the Java and C versions requires the GMP library.
# Leave it out by default.

# chameneosredux and threadring have not yet been implemented in
# Clojure.

#for j in binarytrees fannkuch fannkuchredux fasta knucleotide mandelbrot meteor nbody pidigits regexdna revcomp spectralnorm

for j in binarytrees fannkuchredux fasta knucleotide mandelbrot nbody pidigits regexdna revcomp spectralnorm
do
    cd $j
    ./batch.sh "$@"
    cd ..
done

#! /bin/bash

# Some of the input and expected output files are quite large.  Rather
# than waste space on github, it seems best to use one of the
# benchmark programs to generate them, where possible.  I'll pick
# Java, since the -server version is within 3 times the computation
# time of the C and C++ implementations on all benchmarks, and if you
# are getting this set of files, you are interested in Clojure and so
# must have a Java installation handy.

# We'll just trust the Java benchmark program to do a good job.  If
# you start getting mismatches of expected output to actual output,
# then you may want to investigate more carefully to see whether it is
# the Java implementation that is in error, or the one it is being
# compared against.

do_java_runs () {
    local B=$1
    shift
    local T

    cd $B
    ./batch.sh java $*
    for T in $*
    do
	/bin/mv -f output/${T}-java-output.txt ${T}-expected-output.txt
    done
    cd ..
}

# These don't have input files, just command line parameters that vary
# for the different "size" tests.
do_java_runs mandelbrot quick medium long
do_java_runs fasta quick knuc medium regexdna long

# k-nucleotide (knuc) and reverse-complement (rcomp) have input files
# that are produced as output from the fasta benchmark programs.
cd knuc
/bin/rm -f quick-input.txt medium-input.txt long-input.txt
ln -s ../fasta/knuc-expected-output.txt quick-input.txt
ln -s ../fasta/medium-expected-output.txt medium-input.txt
ln -s ../fasta/long-expected-output.txt long-input.txt
cd ..
cd rcomp
/bin/rm -f quick-input.txt medium-input.txt long-input.txt
ln -s ../fasta/quick-expected-output.txt quick-input.txt
ln -s ../fasta/medium-expected-output.txt medium-input.txt
ln -s ../fasta/long-expected-output.txt long-input.txt
cd ..

# rlines isn't one of the benchmarks from the shootout web site.  I
# created it as a simplified version of reverse-complement, to try to
# figure out why Clojure was using so much memory for one of my
# earlier solution attempts.  I'm keeping it around for future
# reference.
cd rlines
sed -n -e '/^>THREE/,$p' ../fasta/long-expected-output.txt >| long-input.txt
cd ..

cd regex-dna
ln -s ../fasta/knuc-expected-output.txt quick-input.txt
ln -s ../fasta/regexdna-expected-output.txt long-input.txt
cd ..

do_java_runs knuc quick medium long
do_java_runs rcomp quick medium long
do_java_runs rlines long
do_java_runs regex-dna quick long

exit 0

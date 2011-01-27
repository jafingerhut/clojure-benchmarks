#! /bin/bash

MAKE_EXPECTED_OUTPUT_FILES=0
if [ $# -eq 1 ]
then
    MAKE_EXPECTED_OUTPUT_FILES=1
fi

OS=`uname -o 2>/dev/null`
if [ $? -ne 0 ]
then
    # Option -o does not exist in Mac OS X default version of uname
    # command.
    OS=`uname -s 2>/dev/null`
fi

# It is nice to avoid creating multiple copies of large files, but on
# Cygwin we measure the cpu and memory usage of commands using
# timemem.exe in a DOS/Windows batch file, and redirecting input files
# from Cygwin symbolic links does not work.  So just make a copy.
if [ "$OS" == "Cygwin" ]
then
    LINK_OR_COPY="cp"
else
    LINK_OR_COPY="ln -s"
fi

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

make_expected_output_files () {
    local B=$1
    shift
    local T
    local L

    cd $B
    if [ "$B" == "pidigits" ]
    then
	L="gcc"
    else
	L="java"
    fi
    ./batch.sh ${L} $*
    for T in $*
    do
	/bin/mv -f output/${T}-${L}-output.txt output/${T}-expected-output.txt
    done
    cd ..
}

# Make all input files

# There are no fasta input files, but its output files are the input
# files for several other benchmarks.
make_expected_output_files fasta quick knucleotide medium regexdna long

# knucleotide and revcomp have input files that are produced as output
# from the fasta benchmark programs.
cd knucleotide
mkdir ./input
cd ./input
/bin/rm -f quick-input.txt medium-input.txt long-input.txt
${LINK_OR_COPY} ../../fasta/output/knucleotide-expected-output.txt quick-input.txt
${LINK_OR_COPY} ../../fasta/output/medium-expected-output.txt medium-input.txt
${LINK_OR_COPY} ../../fasta/output/long-expected-output.txt long-input.txt
cd ../..
cd revcomp
mkdir ./input
cd ./input
/bin/rm -f quick-input.txt medium-input.txt long-input.txt
${LINK_OR_COPY} ../../fasta/output/quick-expected-output.txt quick-input.txt
${LINK_OR_COPY} ../../fasta/output/medium-expected-output.txt medium-input.txt
${LINK_OR_COPY} ../../fasta/output/long-expected-output.txt long-input.txt
cd ../..

# revlines isn't one of the benchmarks from the shootout web site.  I
# created it as a simplified version of reverse-complement, to try to
# figure out why Clojure was using so much memory for one of my
# earlier solution attempts.  I'm keeping it around for future
# reference.
cd revlines
mkdir ./input
cd ./input
sed -n -e '/^>THREE/,$p' ../../fasta/output/long-expected-output.txt >| long-input.txt
cd ../..

cd regexdna
mkdir ./input
cd ./input
${LINK_OR_COPY} ../../fasta/output/knucleotide-expected-output.txt quick-input.txt
${LINK_OR_COPY} ../../fasta/output/regexdna-expected-output.txt long-input.txt
cd ../..

if [ $MAKE_EXPECTED_OUTPUT_FILES == 0 ]
then
    exit 0
fi

# Make all expected output files

# These don't have input files, just command line parameters that vary
# for the different "size" tests.
make_expected_output_files binarytrees quick medium long
make_expected_output_files fannkuch quick medium long
make_expected_output_files fannkuchredux quick medium long
make_expected_output_files mandelbrot quick medium long
make_expected_output_files nbody quick medium long
make_expected_output_files pidigits quick medium long
make_expected_output_files spectralnorm quick medium long

# These do have input files, which are all output files of the fasta
# benchmark program.
make_expected_output_files knucleotide quick medium long
make_expected_output_files regexdna quick long
make_expected_output_files revcomp quick medium long
make_expected_output_files revlines long

exit 0

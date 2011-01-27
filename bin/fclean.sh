#! /bin/bash

# Find files that are automatically generated as output of runs, or by
# init.sh, to get a directory tree "distribution clean".

find . -name 'output' -o -name '*-expected-output.txt' -o -path './knucleotide/*-input.txt' -o -path './revcomp/*-input.txt' -o -path './revlines/*-input.txt' -o -path './regexdna/*-input.txt' | egrep -v 'fannkuch/(long|quick)-expected-output.txt'

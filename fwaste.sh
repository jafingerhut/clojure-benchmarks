#! /bin/sh

# Find files that are outputs of compilation runs, to make it easier to
# clean up afterwards.

find . -name '*~' -o -name '*.class' -o -name '*.core' -o -name '*.fasl' -o -name '*.hi' -o -name '*.o' -o -name '*.ghc_run'

#! /bin/bash

bindir="../../bin"

R2G="${bindir}/results2graphs"
CLEAN="${bindir}/clean-cljexpr-benchmark-output-files.sh"

set -x
${CLEAN}
find . -name 'cljexprs*.txt' | xargs ${R2G} -t -b

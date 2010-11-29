#! /bin/bash

if [ $# -lt 1 ]
then
	1>&2 echo "usage: `basename $0` <clj-version>"
	exit 1
fi
CLJ_VERSION="$1"
shift
source ../env.sh

mkdir -p ./output
set -x
./clj-compile.sh ${CLJ_VERSION}
../bin/measureproc --jvm-gc-stats hotspot --output output/long-${CLJ_VERSION}-output.txt "${JAVA}" -server -cp "${PS_FULL_CLJ_CLASSPATH}" fannkuchredux 12

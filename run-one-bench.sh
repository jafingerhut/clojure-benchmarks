#! /bin/bash

if [ $# -lt 1 ]
then
    echo "usage: $0 <benchmark_name>"
    exit 1
fi

BENCHMARK="$1"

# ${BENCHMARK}/batch.sh long

# will run all Clojure versions that compile correctly, and don't take
# extremely long due to reflection, and thus are worth benchmarking.
# No reason to duplicate that version info here.

# 0-4 for Linux.  0 must be first unless I make a setup script for OpenJDK 1.6
#for jdk in 0 1 2 3 4
# 5 for Mac OS X
for jdk in 5
do
    for run in 1 2 3
    do
	case $jdk in
	    0)
		# No need to source any setup file on my Ubuntu install to use OpenJDK 6
		;;
	    1)
		source ${HOME}/jdks/setup-oracle-1.6-32bit.sh
		;;
	    2)
		source ${HOME}/jdks/setup-oracle-1.6-64bit.sh
		;;
	    3)
		source ${HOME}/jdks/setup-oracle-1.7-32bit.sh
		;;
	    4)
		source ${HOME}/jdks/setup-oracle-1.7-64bit.sh
		;;
	    5)
		# No need to source any setup file on Mac OS X
		;;
	esac
	echo ""
	echo "    ----------------------------------------"
	echo "    Set ${run} using JDK number ${jdk} with 'java -version':"
	java -version
	echo "    ----------------------------------------"
	set -x
	cd ${BENCHMARK}
	./batch.sh long
	cd ..
	set +x
    done
done

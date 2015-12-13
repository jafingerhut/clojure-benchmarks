#! /bin/bash

source_if_exists() {
    f="$1"
    if [ -r "$f" ]
    then
	source "$f"
    else
	echo "No such file to be source'd: $f"
	exit 1
    fi
}


set -x
# 0-6 for Linux.  0 must be first unless I make a setup script for OpenJDK 1.7
# These are 64-bit JVMs
for jdk in 0 2 4 6 8
# Do 32-bit JVMs later
#for jdk in 1 3 5 7
# 100 for Mac OS X
#for jdk in 100
do
    for run in 1 2 3
    do
	case $jdk in
	    0)
		# No need to source any setup file on my Ubuntu install to use OpenJDK 6
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-openjdk1.7-64bit"
		;;
	    1)
		source_if_exists ${HOME}/jdks/setup-oracle-1.6-32bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.6-32bit"
		;;
	    2)
		source_if_exists ${HOME}/jdks/setup-oracle-1.6-64bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.6-64bit"
		;;
	    3)
		source_if_exists ${HOME}/jdks/setup-oracle-1.7-32bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.7-32bit"
		;;
	    4)
		source_if_exists ${HOME}/jdks/setup-oracle-1.7-64bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.7-64bit"
		;;
	    5)
		source_if_exists ${HOME}/jdks/setup-oracle-1.8-32bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.8-32bit"
		;;
	    6)
		source_if_exists ${HOME}/jdks/setup-oracle-1.8-64bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.8-64bit"
		;;
	    7)
		source_if_exists ${HOME}/jdks/setup-oracle-1.9-32bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.9-32bit"
		;;
	    8)
		source_if_exists ${HOME}/jdks/setup-oracle-1.9-64bit.sh
		SAVEDIR="./results/our-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-14.04.3-oracle-jdk1.9-64bit"
		;;
	    100)
		# No need to source any setup file on Mac OS X
		SAVEDIR="./results/our-macpro-osx-10.6.8-oracle-jdk1.6-64bit"
		;;
	esac
	./scripts/run.sh cljs
	mkdir -p ${SAVEDIR}/set${run}/
	mv results/cljexprs*.txt ${SAVEDIR}/set${run}/
    done
done

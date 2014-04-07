#! /bin/bash

set -x
# 0-6 for Linux.  0 must be first unless I make a setup script for OpenJDK 1.6
#for jdk in 0 1 2 3 4 5 6
# 100 for Mac OS X
for jdk in 100
do
    for run in 1 2 3
    do
	case $jdk in
	    0)
		# No need to source any setup file on my Ubuntu install to use OpenJDK 6
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-openjdk1.6-64bit"
		;;
	    1)
		source ${HOME}/jdks/setup-oracle-1.6-32bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.6-32bit"
		;;
	    2)
		source ${HOME}/jdks/setup-oracle-1.6-64bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.6-64bit"
		;;
	    3)
		source ${HOME}/jdks/setup-oracle-1.7-32bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.7-32bit"
		;;
	    4)
		source ${HOME}/jdks/setup-oracle-1.7-64bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.7-64bit"
		;;
	    5)
		source ${HOME}/jdks/setup-oracle-1.8-32bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.8-32bit"
		;;
	    6)
		source ${HOME}/jdks/setup-oracle-1.8-64bit.sh
		SAVEDIR="./results/andys-macpro-osx-10.6.8-vmwarefusion-3.1.4-ubuntu-12.04.3-oracle-jdk1.8-64bit"
		;;
	    100)
		# No need to source any setup file on Mac OS X
		SAVEDIR="./results/andys-macpro-osx-10.6.8-oracle-jdk1.6-64bit"
		;;
	esac
	./scripts/run.sh cljs
	mkdir -p ${SAVEDIR}/set${run}/
	mv results/cljexprs*.txt ${SAVEDIR}/set${run}/
    done
done

#! /bin/bash

set -x
for run in 1 2 3
do
    # 1-4 for Linux
    #for jdk in 1 2 3 4
    # 5 for Mac OS X
    for jdk in 5
    do
	case $jdk in
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
		# No need to source any setup file on Mac OS X
		SAVEDIR="./results/andys-macpro-osx-10.6.8-oracle-jdk1.6-64bit"
		;;
	esac
	./scripts/run.sh cljs
	mkdir -p ${SAVEDIR}/set${run}/
	mv results/cljexprs*.txt ${SAVEDIR}/set${run}/
    done
done

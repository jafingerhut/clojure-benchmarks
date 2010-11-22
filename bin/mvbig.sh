#! /bin/bash

if [ $# -ne 1 ]
then
    1>&2 echo "usage: `basename $0` [ away | back ]"
    exit 1
fi

if [ $1 == "away" ]
then
    TARGET="$HOME/clojure-benchmarks-big-files"
    for j in */input */output */obj
    do
	d=`dirname $j`
	f=`basename $j`
        if [ "$d" == "hello" -a "$f" == "output" ]; then
	    # Do nothing
	    true
	else
	    mkdir -p "${TARGET}/$d"
	    mv $j "${TARGET}/$d"
	fi
    done
elif [ $1 == "back" ]
then
    TARGET=`pwd`
    SOURCE="$HOME/clojure-benchmarks-big-files"
    cd "${SOURCE}"
    for j in */input */output */obj
    do
	d=`dirname $j`
	f=`basename $j`
        if [ "$d" == "hello" -a "$f" == "output" ]; then
	    # Do nothing
	    true
	else
	    mv $j "${TARGET}/$d"
	fi
    done
else
    1>&2 echo "usage: `basename $0` [ away | back ]"
    exit 1
fi

exit 0

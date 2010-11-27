#! /bin/bash

OS=`uname -o 2>/dev/null`
if [ $? -ne 0 ]
then
    # Option -o does not exist in Mac OS X default version of uname
    # command.
    OS=`uname -s 2>/dev/null`
fi

set -x

JAVA="${1:-none-given}"

if [ "$OS" == "Cygwin" ]
then
    if [ "$JAVA" == "none-given" ]
    then
	JAVA_BIN="/cygdrive/c/Program Files/Java/jrmc-4.0.1-1.6.0/bin"
	JAVA="java"
	echo "Got here 1"
    fi
    CP1=`cygpath -w ${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar`
    CP2="."
    PSEP=";"
    CLASSPATH="${CP1}${PSEP}${CP2}"
else
    if [ "$JAVA" == "none-given" ]
    then
	JAVA="java"
    fi
    CP1="${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar"
    CP2="."
    PSEP=":"
    CLASSPATH="${CP1}${PSEP}${CP2}"
fi

which java
"${JAVA}" -version
for TYPE in -server -client
do
    set +x
    echo "----------------------------------------"
    echo "java ${TYPE} information"
    echo "----------------------------------------"

    set -x
    "${JAVA}" "${TYPE}" -version
    "${JAVA}" "${TYPE}" -cp "${CLASSPATH}" clojure.main jvm-ptr-size.clj sysinfo
    "${JAVA}" "${TYPE}" -cp "${CLASSPATH}" clojure.main jvm-ptr-size.clj

    set +x
    for j in 2 4 8 16 32 64 128 256 384 512 640 768 896 1024 1152 1280 1408 1536 1664 1792 1920
    do
	echo "${JAVA} "${TYPE}" -Xmx${j}m"
	"${JAVA}" "${TYPE}" -Xmx${j}m -cp "${CLASSPATH}" clojure.main jvm-ptr-size.clj 
    done
done

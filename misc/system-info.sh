#! /bin/bash


OS=`uname -o 2>/dev/null`
if [ $? -ne 0 ]
then
    # Option -o does not exist in Mac OS X default version of uname
    # command.
    OS=`uname -s 2>/dev/null`
fi

set -ex

uname -a
which java
java -version
java -server -version
java -client -version

# TBD: What kind of commands are there for Windows like the ones below?

if [ "$OS" == "GNU/Linux" ]
then
    cat /proc/cpuinfo
elif [ "$OS" == "Darwin" ]
then
    /usr/sbin/system_profiler -detailLevel full SPHardwareDataType
    sw_vers
fi

java -server -cp ${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:. clojure.main jvm-ptr-size.clj sysinfo
java -server -cp ${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:. clojure.main jvm-ptr-size.clj

set +ex
for j in 2 4 8 16 32 64 128 256 384 512 640 768 896 1024 1152 1280 1408 1536 1664 1792 1920
do
    echo "java -server -Xmx${j}m"
    java -server -Xmx${j}m -cp ${HOME}/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:. clojure.main jvm-ptr-size.clj 
done

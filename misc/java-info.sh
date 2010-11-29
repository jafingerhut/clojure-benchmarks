#! /bin/bash

source ../env.sh

set -x
"${JAVA}" -version

for TYPE in -server -client
do
    set +x
    echo "----------------------------------------"
    echo "java ${TYPE} information"
    echo "----------------------------------------"

    set -x
    "${JAVA}" "${TYPE}" -version
    "${JAVA}" "${TYPE}" -cp "${PS_FULL_CLJ_CLASSPATH}" clojure.main jvm-ptr-size.clj sysinfo
    "${JAVA}" "${TYPE}" -cp "${PS_FULL_CLJ_CLASSPATH}" clojure.main jvm-ptr-size.clj

    set +x
    for j in 2 4 8 16 32 64 128 256 384 512 640 768 896 1024 1152 1280 1408 1536 1664 1792 1920
    do
	echo "${JAVA} "${TYPE}" -Xmx${j}m"
	"${JAVA}" "${TYPE}" -Xmx${j}m -cp "${PS_FULL_CLJ_CLASSPATH}" clojure.main jvm-ptr-size.clj 
    done
done

#! /bin/bash

if [ $# -lt 1 ]
then
    1>&2 echo "usage: $0 <benchmark_name>"
    exit 1
fi
BENCH_NAMES=$*

# Aids in testing script changes
#ECHO="echo"
ECHO=""

LEIN="lein2"

source ../env.sh

# criterium doesn't compile with Clojure 1.2 or 1.2.1, and even with
# minor tweaks that allow it to compile, it fails its unit tests.

# criterium 

ALL_CLOJURE_VERSIONS_HERE=`all_benchmark_clojure_versions_except 1.2 1.2.1`
echo ${ALL_CLOJURE_VERSIONS_HERE}

${ECHO} ${LEIN} clean
${ECHO} mkdir -p results
${ECHO} cp -pf project.clj project.orig.clj

for v in ${ALL_CLOJURE_VERSIONS_HERE}
do
    check_clojure_version_spec $v
    exit_status=$?
    if [ $exit_status != 0 ]
    then
	1>&2 echo "Unrecognized Clojure version spec: $v"
	exit $exit_status
    fi
    echo ""
    echo "     ============= ${CLJ_VERSION_STR} ============="
    echo ""
    echo ${LEIN} run replace-leiningen-project-clojure-version project.orig.clj ${CLJ_VERSION_STR} project.clj
    ${ECHO} ${LEIN} run replace-leiningen-project-clojure-version project.orig.clj ${CLJ_VERSION_STR} project.clj
    exit_status=$?
    if [ $exit_status != 0 ]
    then
	1>&2 echo "Unrecognized Clojure version spec: $v"
	exit $exit_status
    fi
    echo ${LEIN} clean
    ${ECHO} ${LEIN} clean
    echo ${LEIN} run benchmark results/cljexprs-${CLJ_VERSION_STR}.txt ${BENCH_NAMES}
    ${ECHO} ${LEIN} run benchmark results/cljexprs-${CLJ_VERSION_STR}.txt ${BENCH_NAMES}
done

${ECHO} cp -pf project.orig.clj project.clj

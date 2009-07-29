#! /bin/sh

# usage: ./clj-run.sh <input-file-name> <output-file-name>

# Can use /dev/stdin or /dev/stdout on some OS's (Linux, Mac OS X) in
# place of a name.

# 512m - not enough
# 768m - not enough
# 1024m - not enough

source ../env.sh

$JAVA -server -Xmx1280m ${JAVA_PROFILING} -cp ${CLOJURE_JAR} clojure.main knucleotide.clj $0 "$@"

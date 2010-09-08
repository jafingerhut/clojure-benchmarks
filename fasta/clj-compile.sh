#! /bin/sh

source ../env.sh
mkdir -p ./obj/clj

cp fasta.clj-1.clj ./obj/clj/fasta.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile fasta

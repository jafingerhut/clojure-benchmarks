#! /bin/bash

source ../env.sh
rm -fr ./obj/clj
mkdir -p ./obj/clj

cp knucleotide.clj-8.clj ./obj/clj/knucleotide.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile knucleotide

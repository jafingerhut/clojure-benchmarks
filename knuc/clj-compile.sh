#! /bin/sh

source ../env.sh
mkdir ./obj/clj

cp knucleotide.clj-9.clj ./obj/clj/knucleotide.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile knucleotide

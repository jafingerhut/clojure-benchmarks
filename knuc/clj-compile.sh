#! /bin/sh

source ../env.sh
mkdir ./clj-classes

cp knucleotide.clj-9.clj ./clj-classes/knucleotide.clj

java -Dclojure.compile.path=./clj-classes -classpath ${CLOJURE_CLASSPATH}:./clj-classes clojure.lang.Compile knucleotide

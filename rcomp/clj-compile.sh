#! /bin/sh

source ../env.sh
mkdir -p ./obj/clj

cp revcomp.clj-10.clj ./obj/clj/revcomp.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile revcomp

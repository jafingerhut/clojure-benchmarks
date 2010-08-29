#! /bin/sh

source ../env.sh
mkdir -p ./obj/clj

cp revlines.clj-1.clj ./obj/clj/revlines.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile revlines

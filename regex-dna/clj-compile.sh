#! /bin/sh

source ../env.sh
mkdir ./obj/clj

cp regexdna.clj-1.clj ./obj/clj/regexdna.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile regexdna

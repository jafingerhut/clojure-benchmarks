#! /bin/sh

source ../env.sh
mkdir ./obj/clj

cp fannkuch.clj-11.clj ./obj/clj/fannkuch.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile fannkuch

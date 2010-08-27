#! /bin/sh

source ../env.sh
mkdir ./obj/clj

cp nbody.clj-12.clj ./obj/clj/nbody.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile nbody

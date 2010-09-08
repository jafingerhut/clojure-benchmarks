#! /bin/bash

source ../env.sh
mkdir -p ./obj/clj
rm -f ./obj/clj/*

cp pmap-alternates.clj ./obj/clj/pmapalternates.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile pmapalternates

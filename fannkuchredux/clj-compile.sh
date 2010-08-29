#! /bin/sh

source ../env.sh
mkdir -p ./obj/clj

cp fannkuchredux.clj-11.clj ./obj/clj/fannkuchredux.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile fannkuchredux

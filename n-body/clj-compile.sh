#! /bin/sh

source ../env.sh
mkdir ./clj-classes

cp nbody.clj-12.clj ./clj-classes/nbody.clj

java -Dclojure.compile.path=./clj-classes -classpath ${CLOJURE_CLASSPATH}:./clj-classes clojure.lang.Compile nbody

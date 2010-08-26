#! /bin/sh

source ../env.sh
mkdir ./clj-classes

cp regexdna.clj-1.clj ./clj-classes/regexdna.clj

java -Dclojure.compile.path=./clj-classes -classpath ${CLOJURE_CLASSPATH}:./clj-classes clojure.lang.Compile regexdna

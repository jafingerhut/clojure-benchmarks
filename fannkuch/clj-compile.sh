#! /bin/sh

source ../env.sh
mkdir ./clj-classes

cp fannkuch.clj-11.clj ./clj-classes/fannkuch.clj

java -Dclojure.compile.path=./clj-classes -classpath ${CLOJURE_CLASSPATH}:./clj-classes clojure.lang.Compile fannkuch

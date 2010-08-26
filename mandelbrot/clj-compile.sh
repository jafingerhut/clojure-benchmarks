#! /bin/sh

source ../env.sh
mkdir ./clj-classes

cp mandelbrot.clj-6.clj ./clj-classes/mandelbrot.clj

java -Dclojure.compile.path=./clj-classes -classpath ${CLOJURE_CLASSPATH}:./clj-classes clojure.lang.Compile mandelbrot

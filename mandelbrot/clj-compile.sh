#! /bin/bash

source ../env.sh
mkdir -p ./obj/clj

cp mandelbrot.clj-6.clj ./obj/clj/mandelbrot.clj

java -Dclojure.compile.path=./obj/clj -classpath ${CLOJURE_CLASSPATH}:./obj/clj clojure.lang.Compile mandelbrot

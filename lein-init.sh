#! /bin/bash

# A script to assist Andy in creating a consistent set of Clojure .jar
# files in the same directory, relative to the home directory, on
# multiple operating systems.

LEIN_FILES="`dirname $0`/lein-files"

#echo $LEIN_FILES
#exit 0

mkdir -p "${HOME}/lein/lein-files"
cp -pr "${LEIN_FILES}"/* "${HOME}/lein/lein-files/"

cd "${HOME}/lein"

lein new clj-1.2.0
cd clj-1.2.0
cp -p "../lein-files/project.clj-for-clj-1.2.0-only" project.clj
lein deps
cd ..

lein new swank-clj-1.2.0
cd swank-clj-1.2.0
cp -p "../lein-files/project.clj-for-clj-1.2.0-plus-contrib-plus-swank" project.clj
lein deps
cd ..

lein new clj-1.3.0-alpha1
cd clj-1.3.0-alpha1
cp -p "../lein-files/project.clj-for-clj-1.3.0-alpha1-only" project.clj
lein deps
cd ..

# This one doesn't quite work yet, but none of the Clojure programs
# need anything from clojure-contrib yet.

#lein new contrib-1.3.0-alpha
#cd contrib-1.3.0-alpha
#cp -p "${LEIN_FILES}/project.clj-for-clj-1.3.0-plus-contrib" project.clj
#lein deps
#cd ..

lein new clj-1.3.0-alpha3
cd clj-1.3.0-alpha3
cp -p "../lein-files/project.clj-for-clj-1.3.0-alpha3-only" project.clj
lein deps
cd ..

lein new clj-1.3.0-alpha4
cd clj-1.3.0-alpha4
cp -p "../lein-files/project.clj-for-clj-1.3.0-alpha4-only" project.clj
lein deps
cd ..

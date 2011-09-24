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

for clj_1_2_point_release in 0 1
do
    lein new clj-1.2.${clj_1_2_point_release}
    cd clj-1.2.${clj_1_2_point_release}
    cp -p "../lein-files/project.clj-for-clj-1.2.${clj_1_2_point_release}-only" project.clj
    lein deps
    cd ..
done

lein new swank-clj-1.2.0
cd swank-clj-1.2.0
cp -p "../lein-files/project.clj-for-clj-1.2.0-plus-contrib-plus-swank" project.clj
lein deps
cd ..

for alpha in 1 3 4 5 6
do
    lein new clj-1.3.0-alpha${alpha}
    cd clj-1.3.0-alpha${alpha}
    cp -p "../lein-files/project.clj-for-clj-1.3.0-alpha${alpha}-only" project.clj
    lein deps
    cd ..
done

# This one doesn't quite work yet, but none of the Clojure programs
# need anything from clojure-contrib yet.

#lein new contrib-1.3.0-alpha
#cd contrib-1.3.0-alpha
#cp -p "${LEIN_FILES}/project.clj-for-clj-1.3.0-plus-contrib" project.clj
#lein deps
#cd ..

for beta in 1 2 3
do
    lein new clj-1.3.0-beta${beta}
    cd clj-1.3.0-beta${beta}
    cp -p "../lein-files/project.clj-for-clj-1.3.0-beta${beta}-only" project.clj
    lein deps
    cd ..
done

for clj_1_3_point_release in 0
do
    lein new clj-1.3.${clj_1_3_point_release}
    cd clj-1.3.${clj_1_3_point_release}
    cp -p "../lein-files/project.clj-for-clj-1.3.${clj_1_3_point_release}-only" project.clj
    lein deps
    cd ..
done

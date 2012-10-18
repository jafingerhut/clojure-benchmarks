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
    lein new clojure-1.2.${clj_1_2_point_release}
    cd clojure-1.2.${clj_1_2_point_release}
    cp -p "../lein-files/project.clj-for-clojure-1.2.${clj_1_2_point_release}" project.clj
    lein deps
    cd ..
done

lein new swank-clj-1.2.0
cd swank-clj-1.2.0
cp -p "../lein-files/project.clj-for-clojure-1.2.0-plus-contrib-plus-swank" project.clj
lein deps
cd ..

# It appears that 1.3.0 alpha version 1, 3, and 4 are no longer
# available from Maven repo.
#for alpha in 1 3 4 5 6 7 8
for alpha in 5 6 7 8
do
    lein new clojure-1.3.0-alpha${alpha}
    cd clojure-1.3.0-alpha${alpha}
    cp -p "../lein-files/project.clj-for-clojure-1.3.0-alpha${alpha}" project.clj
    lein deps
    cd ..
done

# This one doesn't quite work yet, but none of the Clojure programs
# need anything from clojure-contrib yet.

#lein new contrib-1.3.0-alpha
#cd contrib-1.3.0-alpha
#cp -p "${LEIN_FILES}/project.clj-for-clojure-1.3.0-plus-contrib" project.clj
#lein deps
#cd ..

for beta in 1 2 3
do
    lein new clojure-1.3.0-beta${beta}
    cd clojure-1.3.0-beta${beta}
    cp -p "../lein-files/project.clj-for-clojure-1.3.0-beta${beta}" project.clj
    lein deps
    cd ..
done

for clj_1_3_point_release in 0
do
    lein new clojure-1.3.${clj_1_3_point_release}
    cd clojure-1.3.${clj_1_3_point_release}
    cp -p "../lein-files/project.clj-for-clojure-1.3.${clj_1_3_point_release}" project.clj
    lein deps
    cd ..
done

for alpha in 1 2 3 4 5
do
    lein new clojure-1.4.0-alpha${alpha}
    cd clojure-1.4.0-alpha${alpha}
    cp -p "../lein-files/project.clj-for-clojure-1.4.0-alpha${alpha}" project.clj
    lein deps
    cd ..
done

for beta in 1 2 3 4 5 6 7
do
    lein new clojure-1.4.0-beta${beta}
    cd clojure-1.4.0-beta${beta}
    cp -p "../lein-files/project.clj-for-clojure-1.4.0-beta${beta}" project.clj
    lein deps
    cd ..
done

for clj_1_4_point_release in 0
do
    lein new clojure-1.4.${clj_1_4_point_release}
    cd clojure-1.4.${clj_1_4_point_release}
    cp -p "../lein-files/project.clj-for-clojure-1.4.${clj_1_4_point_release}" project.clj
    lein deps
    cd ..
done

for alpha in 1 2 3 4 5 6 7
do
    lein new clojure-1.5.0-alpha${alpha}
    cd clojure-1.5.0-alpha${alpha}
    cp -p "../lein-files/project.clj-for-clojure-1.5.0-alpha${alpha}" project.clj
    lein deps
    cd ..
done

#! /bin/bash

# A script to assist Andy in creating a consistent set of Clojure .jar
# files in the same directory, relative to the home directory, on
# multiple operating systems.

make_project_clj_file()
{
    local VERSION="$1"
    local FNAME="$2"

    echo "(defproject clojure-${VERSION} \"1.0.0-SNAPSHOT\"" > ${FNAME}
    echo "  :description \"FIXME: write\"" >> ${FNAME}
    echo "  :dependencies [[org.clojure/clojure \"${VERSION}\"]])" >> ${FNAME}
}

LEIN_FILES="`dirname $0`/lein-files"

#echo $LEIN_FILES
#exit 0

mkdir -p "${HOME}/lein"
cd "${HOME}/lein"

for clj_1_2_point_release in 0 1
do
    v="1.2.${clj_1_2_point_release}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

# It appears that 1.3.0 alpha version 1, 3, and 4 are no longer
# available from Maven repo.
#for alpha in 1 3 4 5 6 7 8
for alpha in 5 6 7 8
do
    v="1.3.0-alpha${alpha}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for beta in 1 2 3
do
    v="1.3.0-beta${beta}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for clj_1_3_point_release in 0
do
    v="1.3.${clj_1_3_point_release}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for alpha in 1 2 3 4
do
    v="1.4.0-alpha${alpha}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for beta in 1 2 3 4 5 6 7
do
    v="1.4.0-beta${beta}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for clj_1_4_point_release in 0
do
    v="1.4.${clj_1_4_point_release}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for alpha in 1 2 3 4 5 6 7
do
    v="1.5.0-alpha${alpha}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for beta in 1 2 7 8 9 10 11 12 13
do
    v="1.5.0-beta${beta}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

for RC in 1 2 3 4 5 6 14 15 16 17
do
    v="1.5.0-RC${RC}"
    lein1 new clojure-${v}
    cd clojure-${v}
    make_project_clj_file ${v} project.clj
    lein1 deps
    cd ..
done

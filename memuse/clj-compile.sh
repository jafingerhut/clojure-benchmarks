#! /bin/bash

source ../env.sh
mkdir -p "${CLJ_OBJ_DIR}"

FT_DIR="/Users/andy/sw/git/finger-tree"
sed 's/clojure.lang.fingertree/fingertree/' "${FT_DIR}/finger_tree.clj" > "${CLJ_OBJ_DIR}/fingertree.clj"
#sed 's/clojure.lang.fingertree/fingertree/' "${FT_DIR}/finger_tree.wider-nodes.clj" > "${CLJ_OBJ_DIR}/fingertree.clj"
#sed 's/clojure.lang.fingertree/fingertree/' "${FT_DIR}/finger_tree.without-split.clj" > "${CLJ_OBJ_DIR}/fingertree.clj"
"${CP}" memuse.clj "${CLJ_OBJ_DIR}/memuse.clj"

"${JAVA}" "-Dclojure.compile.path=${PS_CLJ_OBJ_DIR}" -classpath "${PS_FULL_CLJ_CLASSPATH}" clojure.lang.Compile memuse

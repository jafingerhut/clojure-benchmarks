#! /bin/bash

"${JAVA}" -server -Xmx1024m -classpath "${CLASSPATH_CLOJURE}" clojure.main try.clj $0 "$@"

#! /bin/sh

java -server -Xmx1024m -cp /Users/andy/sw/clojure/clojure/clojure.jar:/Users/andy/sw/clojure/clojure-contrib/src/ clojure.main try.clj $0 "$@"

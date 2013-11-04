(defproject cljexprs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/jafingerhut/clojure-benchmarks"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [criterium "0.4.2"]]
  :jvm-opts ^:replace ["-Xmx1024m"]   ; several benchmarks take significant memory
  :main cljexprs.core)

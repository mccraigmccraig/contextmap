(defproject confmap "0.0.1-SNAPSHOT"
  :description "maps of delays for config"
  :url "https://github.com/mccraigmccraig/confmap"
  :license {:name ""
            :url ""}
  :dependencies [[prismatic/plumbing "0.4.4"]
                 [prismatic/schema "0.4.3"]
                 [potemkin "0.3.13"]
                 [cats "0.4.0"]]

  :deploy-repositories {"releases" :clojars
                        "snapshots" :clojars}

  :source-paths ["src"]
  :test-paths ["test"]

  :cljsbuild {:test-commands {"test" ["node" "output/tests.js"]}
              :builds [{:id "test"
                        :source-paths ["src" "test"]
                        :notify-command ["node" "output/tests.js"]
                        :compiler {:output-to "output/tests.js"
                                   :output-dir "output"
                                   :source-map true
                                   :static-fns true
                                   :cache-analysis false
                                   :main confmap
                                   :optimizations :none
                                   :target :nodejs
                                   :pretty-print true}}]}

  :jar-exclusions [#"\.swp|\.swo"]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "0.0-3297"]]
                   :codeina {:sources ["src"]
                             :output-dir "doc/codeina"}
                   :plugins [[funcool/codeina "0.1.0-SNAPSHOT"
                              :exclusions [org.clojure/clojure]]
                             [lein-cljsbuild "1.0.6"]]}})

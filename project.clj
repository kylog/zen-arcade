(defproject zen-arcadia "1.0.0-SNAPSHOT"
  :description "Zen arcadia"
  :url "http://zen-arcadia.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure     "1.6.0"]
                 [compojure               "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [clj-http                "2.2.0"]
                 [org.clojure/data.json   "0.2.6"]
                 [org.clojure/data.codec  "0.1.0"]
                 [org.clojure/core.async  "0.1.346.0-17112a-alpha"]
                 [circleci/clj-yaml       "0.5.5"]
                 [ring/ring-mock          "0.3.0"]
                 [environ                 "1.0.0"]]
  :resource-paths ["resources" "test-resources"]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "zen-arcadia-standalone.jar"
  :profiles {:production {:env {:production true}}})

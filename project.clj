(defproject threatgrid/clj-jwt "0.4.0"
  :description  "Clojure library for JSON Web Token(JWT)"
  :url          "https://github.com/threatgrid/clj-jwt"
  :pedantic?    :abort
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.bouncycastle/bcpkix-jdk15on "1.52"]
                 [crypto-equality "1.0.0"]
                 [clj-time "0.15.2"]
                 [metosin/jsonista "0.2.6"]]
  :profiles {:dev {:dependencies [[midje "1.9.9"]]}}
  :global-vars {*warn-on-reflection* true}
  :plugins  [[lein-midje "3.2.1"]])

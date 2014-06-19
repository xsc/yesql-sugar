(defproject yesql-sugar "0.1.1-SNAPSHOT"
  :description "Syntactic Sugar for yesql."
  :url "https://github.com/xsc/yesql-sugar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [yesql "0.4.0"]]
  :profiles {:dev {:plugins [[codox "0.8.9"]]
                   :codox {:include [yesql.sugar]
                           :src-dir-uri "https://github.com/xsc/yesql-sugar/blob/master/"
                           :src-linenum-anchor-prefix "L"
                           :defaults {:doc/format :markdown}}}}
  :pedantic? :abort)

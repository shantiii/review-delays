(defproject review-delays "0.1.0"
  :description "Chart the difference between time-to-first-comment on pull requests"
  :url "http://github.com/shantiii/review-delays"
  :license {:name "MIT"
            :url "https://www.tldrlegal.com/l/mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                [clj-http "1.1.2"]
                [clj-time "0.10.0"]
                [http-kit "2.1.18"]
                [incanter/incanter-core "1.9.0"]
                [incanter/incanter-charts "1.9.0"]])
  :main ^:skip-aot review-delays.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

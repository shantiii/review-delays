(ns review-delays.core
  (:gen-class)
  (:require [review-delays.github :as github :refer [init get-review-delay-distribution]]
            [clojure.tools.cli :refer [parse-opts]]
            [incanter.core :refer [dataset $where view]]
            [incanter.charts :refer [box-plot add-box-plot set-axis log-axis]]))

;; You will need to tweak these functions to customize the data for your own organization.
(declare munge-data)
(declare graph-dataset)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [parse (parse-opts args [["-u" "--user GITHUB_USERNAME" :required "Your github account username" :id :username]
                                ["-t" "--token GITHUB_TOKEN" :required "A github authentication token for your account" :id :token]])
        opts (:options parse)
        organization (first (:arguments parse))]
    (github/init (opts :username) (opts :token))
    (-> organization
        github/get-review-delay-distribution
        munge-data
        graph-dataset)))

(defn munge-data
  "Return an incanter/dataset from the provided data. overload this method to implement your own categorizations for usernames"
  [user-distribution]
  (let [women    #{"shantiii"}
        men      #{"not-all"}
        gend     #(cond (contains? women %) :female
                        (contains? men %) :male
                        :else :other)]
    (dataset [:username :gender :ttfc]
             (vec (mapcat
                   (fn [[k v]] (map #(vector k (gend k) (/ % 60.)) v))
                   user-distribution)))))

(defn graph-dataset
  [my-data]
  (doto
    (box-plot :ttfc
              :data ($where {:gender :female} my-data)
              :series-label "wimminz"
              :legend true)
    (add-box-plot :ttfc
                  :data ($where {:gender :male} my-data)
                  :series-label "manz")
    (set-axis :y (log-axis :base 2, :label "Minutes to First Comment"))
    view))

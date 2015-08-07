(ns review-delays.github
  (:require [clj-http.client :as client])
  (:require [clj-time.format :refer [parse formatters show-formatters]])
  (:require [clj-time.core :refer [from-now years earliest interval in-seconds]]))

;; entry point to this file
(defn init
  "Initialize the github connection stuff with a github username and a github API token."
  [gh-username gh-token]
  (def github-username gh-username)
  (def github-token gh-token))

(defn raw-get
  "Sends a raw HTTP request to url"
  [url]
  (client/get url
   {:basic-auth [github-username github-token],
    :as :json-strict}))

(defn gh-get
  "Submits a request to github for a specified path"
  [path query]
  (client/get (str "https://api.github.com" path)
              {:basic-auth [github-username github-token],
               :query-params (merge {:per_page 100} query),
               :as :json-strict}))

(defn all-pages
  "Returns all of the response pages, with selector applied to them, combined."
  [first-response selector]
  (let [next-url (fn [response]
                   (-> response :links :next :href))
        next-page (fn [response]
                    (if (nil? (next-url response))
                      nil
                      (raw-get (next-url response))))]
    (mapcat selector (take-while some? (iterate next-page first-response)))))

(defn org-repos
  "Returns a lazy sequence of all of the repositories hosted in org"
  [org]
  (map :full_name (filter #(contains? #{"Java" "Ruby" "JavaScript"} (:language %)) (all-pages (gh-get (str "/orgs/" org "/repos") {:type "private", :per_page 100}) :body))))

(defn repo-prs
  "Returns all of the closed PRs associated with repo. repo is in username/reponame syntax"
  [repo]
  (all-pages (gh-get (str "/repos/" repo "/pulls") {:state "closed"}) :body))

(defn get-pull-request
  "Fetches a pull request for a certain repository"
  [repo pull-request-number]
  (:body (gh-get (str "/repos/" repo "/pulls/" pull-request-number) {})))

(defn repo-issue-comments
  "All of the issue comments (not to be confused with diff comments) in repo"
  [repo]
  (let [key-fn #(second (re-find (re-matcher #"^https://(?:.+/)+(\d+)#?" (:html_url %))))]
    (loop [pages (all-pages (gh-get (str "/repos/" repo "/issues/comments") {:state "closed"}) :body)
           url-map (transient {})]
      (if (empty? pages)
        (persistent! url-map)
        (recur
         (rest pages)
         (assoc! url-map
                 (key-fn (first pages))
                 (conj
                  (get url-map (key-fn (first pages)) [])
                  (:created_at (first pages)))))))))

(defn repo-review-comments
  "All of the diff comments (not to be confused with review comments) in repo"
  [repo]
  (let [key-fn #(second (re-find (re-matcher #"^https://(?:.+/)+(\d+)#?" (:pull_request_url %))))]
    (loop [pages (all-pages (gh-get (str "/repos/" repo "/pulls/comments") {:state "closed"}) :body)
           url-map (transient {})]
      (if (empty? pages)
        (persistent! url-map)
        (recur
         (rest pages)
         (assoc! url-map
                 (key-fn (first pages))
                 (conj
                  (get url-map (key-fn (first pages)) [])
                  (:created_at (first pages)))))))))

(defn repo-comments
  [repo]
  (merge-with into (repo-review-comments repo) (repo-issue-comments repo))) ;; code

(defn first-comment-time
  "The time of the first comment on pull-request-id, as located by timestamp-map"
  [pull-request-id timestamp-map]
  (let [pr-id (str pull-request-id)
        to-date #(parse (formatters :date-time-no-ms) %)] ;; converts a string to a date
    (earliest (map to-date (get timestamp-map pr-id)))))

(defn time-to-first-comment
  "Returns the interval between the creation of and first commit on a pr"
  [pull-request]
  (if (nil? pull-request)
    nil
    (interval
     (parse (formatters :date-time-no-ms) (pull-request :created_at))
     (first-comment-time (:number pull-request)))))

(defn repo-distribution
  "Return the map of usernames-to-ttfc for repo"
  [repo]
  (println "Processing " repo)
  (let [comments (repo-comments repo)
        pull-requests (filter #(contains? comments (str (:number %))) (repo-prs repo))]
    (loop [pull-request (first pull-requests)
           remaining-pull-requests (rest pull-requests)
           distribution (transient {})]
      (if (nil? pull-request)
        (persistent! distribution)
        (recur
         (first remaining-pull-requests)
         (rest remaining-pull-requests)
         (assoc! distribution
                 (:login (:user pull-request))
                 (conj
                  (get distribution (:login (:user pull-request)) [])
                  (in-seconds
                   (interval
                    (parse (formatters :date-time-no-ms) (:created_at pull-request))
                    (earliest
                     (map parse (get comments (str (:number pull-request)) []))))))))))))

;; another entry point to this file
(defn get-review-delay-distribution
  "Returns a map of usernames to sequences containing the time-to-first-comment for each PR that user created. Only looks at users and pull requests in the private branches of provided organization."
  [org]
  (println "hi")
  (reduce #(merge-with into %1 %2) {} (map repo-distribution (org-repos org))))

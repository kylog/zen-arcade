(ns zen-arcadia.web
  (:require [compojure.core            :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler         :refer [site]]
            [compojure.route           :as route]
            [ring.adapter.jetty        :as jetty]
            [clojure.java.io           :as io]
            [clojure.string            :as str]
            [clojure.data.json         :as json]
            [clj-yaml.core             :as yaml]
            [clj-http.client           :as client]
            [clojure.data.codec.base64 :as b64]
            [environ.core              :refer [env]]))

;; TODO private repos, hmm
;; TODO logging, i.e. for debug, hmm

(defn slurp-github
  "Apply the GET method on a github api endpoint, using the provided
   context to pull an auth token and anything else that's handy.
   For now, this is just a placeholder function, but would need to
   be fleshed out for accessing private repositories."
  [url]
  (try
    (slurp url)
    (catch Exception e)))

(defn get-github-token []
  "Get the github token"
  ;; TODO something helpful if there is no token
  ;; TODO something helpful if token is not valid?
  (System/getenv "ZEN_ARCADIA_GITHUB_OAUTH_TOKEN"))

(defn get-commits-url [pull-request]
  (let [body-map (json/read-str pull-request :key-fn keyword)
        {{{{commits-url :href} :commits} :_links} :pull_request} body-map]
      commits-url))

(defn get-commits [body]
  (json/read-str (slurp-github (get-commits-url body)) :key-fn keyword))

(defn get-config-url [pull-request]
  (let [body-map (json/read-str pull-request :key-fn keyword)
        ;; extract out the two things needed: the contents_url and the ref
        {{{{contents-url :contents_url} :repo ref :ref} :head} :pull_request} body-map
        ;; and use them to cook up the url to the config file
        config-url (str/join [(nth (str/split contents-url #"\{") 0) ".zen-arcadia.yml?ref=" ref])]
        config-url))

(defn get-contents [contents-body]
  (let [body-map (json/read-str contents-body :key-fn keyword)
        {content :content} body-map
        ;; the content value is \n delimited base64, seems weird but whatever
        b64-content (clojure.string/join (clojure.string/split-lines content))]
        (String. (b64/decode (.getBytes b64-content "UTF-8")))))

(defn parse-config [config-as-yaml]
  (try
    ((yaml/parse-string config-as-yaml) :checks)
    ;; TODO something useful with ill-formatted yaml
    (catch Exception e (str "caught exception: " (.getMessage e)))))

(defn get-config [body]
  (parse-config (get-contents (slurp-github (get-config-url body)))))

(defn get-statuses-url [pull-request]
  (let [body-map (json/read-str pull-request :key-fn keyword)
        {{{{statuses-url :href} :statuses} :_links} :pull_request} body-map]
    statuses-url))

(defn get-comments-url [pull-request]
  (let [body-map (json/read-str pull-request :key-fn keyword)
        {{{{comments-url :href} :comments} :_links} :pull_request} body-map]
    comments-url))

(defn escape-regex [regex-string]
  "Perform any escaping of the regex string needed. I've honestly
   confused myself at this point, and this is now just a placeholder."
  regex-string)

(defn check-one-commit [params commit]
  "Check the specified commit in the pull request, commenting on
   anything that doesn't match and returning success."
  (let [{{message :message} :commit} commit
        regex (:regex params)]
    (if (re-matches (re-pattern (escape-regex regex)) message)
      { :commit commit :success true  }
      { :commit commit :success false } )))

(defn build-status [num-fails]
  "Build up a github status suitable for POSTing to:
   /repos/:owner/:repo/statuses/:sha"
  (format
    "{
       \"state\":       \"%s\",
       \"target_url\":  \"https://http.cat/403\",
       \"description\": \"%s\",
       \"context\":     \"zen-arcadia/commit\"
     }"
    (if (= num-fails 0) "success" "failure")
    (if (= num-fails 0)
      "All commits look good - thanks!"
      (format "%s didn't look right - I commented on the pull request"
              (if (= num-fails 1)
                "One commit message"
                (format "%d commit messages" num-fails)))))
  )

(defn post-status [pull-request num-fails]
  (client/post (get-statuses-url pull-request)
               {:body (build-status num-fails)
                :headers {"Authorization" (format "token %s" (get-github-token))}}))

(defn build-comment [message]
  "Build up a github comment suitable for POSTing to:
   /repos/:owner/:repo/pulls/:number/comments"
  (format
    "{
       \"body\": \"The commit summary '%s' did not conform to this repo's requirements\"
     }"
    message))

(defn post-comment [pull-request message]
  "Post a comment to the pull-request about the provided comment"
  (client/post (get-comments-url pull-request)
               {:body (build-comment message)
                :headers {"Authorization" (format "token %s" (get-github-token))}}))

(defn check-commit [pull-request params]
  "Check all commits in the pull request, commenting on anything
   that doesn't match and updating status when done."
  (let [pr-commits (get-commits pull-request)
        commits (map (partial check-one-commit params) pr-commits)
        ]
    ;; add comment for each failing commit
    (doseq [commit commits]
      (if (not (commit :success))
        (let [{{{message :message} :commit} :commit} commit]
          (post-comment pull-request message))))
    ;; post summary status
    (post-status pull-request (count (filter (fn [x] (not (x :success))) commits)))
  ))

(defn check-pr
  "Check a pull request using the provided checker"
  [pull-request checker]
  (let [{check :check params :params} checker]
    (case check
      "commit" (check-commit pull-request (first params))
      "ignore unknown checks"))
  )

(defn process-pull-request
  "Process the body of a github pull request event webhook"
  [pr-event]
  (let [pull-request (slurp pr-event)]
    (let [config (get-config pull-request)]
      (doseq [checker config] (check-pr pull-request checker))
      )))

(defroutes app
  (POST "/" {body :body}
    (process-pull-request body)
    {:status 200 :headers {"Content-Type" "text/plain"}})
  (ANY "*" []
    (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))

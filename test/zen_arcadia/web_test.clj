(ns zen-arcadia.web-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.java.io :as io]
            [clojure.data.json  :as json]
            [clojure.string :as str]
            [zen-arcadia.web :refer :all]))

(deftest bad-input-handler
  (is (= 404 (:status (app (mock/request :get "/any/random/crap"))))))

(deftest get-right-config-url
  (is (= (get-config-url (slurp (io/resource "pull-request-event.json")))
         (str/trim-newline (slurp (io/resource "config-url.txt"))))))

(deftest get-right-commits-url
  (is (= (get-commits-url (slurp (io/resource "pull-request-event.json")))
         (str/trim-newline (slurp (io/resource "commits-url.txt"))))))

(deftest get-right-statuses-url
  (is (= (get-statuses-url (slurp (io/resource "pull-request-event.json")))
         (str/trim-newline (slurp (io/resource "statuses-url.txt"))))))

(deftest get-right-comments-url
  (is (= (get-comments-url (slurp (io/resource "pull-request-event.json")))
         (str/trim-newline (slurp (io/resource "comments-url.txt"))))))

(deftest get-right-contents
  (is (= (get-contents (slurp (io/resource "contents.json")))
         (slurp (io/resource "contents.txt")))))

(deftest get-right-checks
  (let [config (parse-config (slurp (io/resource "zen-arcadia.yml")))]
    (is (= ((first config) :check)
           "commit"))
    (is (= ((first (next config)) :check)
           "example_unknown"))
    ))
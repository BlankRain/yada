;; Copyright © 2015, JUXT LTD.

(ns yada.methods-test
  (:require
   [byte-streams :as bs]
   [clojure.test :refer :all]
   [juxt.iota :refer (given)]
   [ring.mock.request :as mock]
   [ring.util.codec :as codec]
   [schema.core :as s]
   [yada.methods :refer (Get Post)]
   [yada.resources.misc :refer (just-methods)]
   [yada.resource :refer [ResourceAllowedMethods allowed-methods ResourceEntityTag make-resource]]
   [yada.yada :as yada]))

(deftest post-test
  (let [handler (yada/resource
                 (just-methods
                  :post {:response (fn [ctx]
                                     (assoc (:response ctx)
                                            :status 201
                                            :body "foo"))}))]
    (given @(handler (mock/request :post "/"))
      :status := 201
      [:body bs/to-string] := "foo")))

(deftest dynamic-post-test
  (let [handler (yada/resource
                 (just-methods
                  :post {:response (fn [ctx]
                                     (assoc (:response ctx)
                                            :status 201 :body "foo"))}))]
    (given @(handler (mock/request :post "/"))
      :status := 201
      [:body bs/to-string] := "foo")))

(deftest multiple-headers-test
  (let [handler (yada/resource
                 (just-methods
                  :post {:response (fn [ctx] (assoc (:response ctx)
                                                   :status 201 :headers {"set-cookie" ["a" "b"]}))}))]
    (given @(handler (mock/request :post "/"))
      :status := 201
      [:headers "set-cookie"] := ["a" "b"])))

;; Allowed methods ---------------------------------------------------

;; To ensure coercion to StringResource which satisfies GET (tested
;; below)
(require 'yada.resources.string-resource)

(deftest allowed-methods-test
  (testing "methods-deduced"
    (are [r e] (= (allowed-methods (make-resource r)) e)
      nil #{:get}
      "Hello" #{:get}
      (reify Get (GET [_ _] "foo")) #{:get}
      (reify
        Get (GET [_ _] "foo")
        Post (POST [_ _] "bar")) #{:get :post})))

;; ETags -------------------------------------------------------------

(defn etag? [etag]
  (and (string? etag)
       (re-matches #"-?\d+" etag)))

(defrecord ETagTestResource [v]
  ResourceEntityTag
  (etag [_ ctx] @v)
  Get
  (GET [_ ctx] "foo")
  Post
  (POST [_ ctx] (do (swap! v inc) "OK")))

(deftest etag-test
  (testing "etags-identical-for-consecutive-gets"
    (let [v (atom 1)
          handler (yada/resource (->ETagTestResource v))
          r1 @(handler (mock/request :get "/"))
          r2 @(handler (mock/request :get "/"))]
      (given [r1 r2]
        [first :status] := 200
        [second :status] := 200
        [first :headers "etag"] :? etag?
        [second :headers "etag"] :? etag?)
      ;; ETags are the same in both responses
      (is (= (get-in r1 [:headers "etag"])
             (get-in r2 [:headers "etag"])))))

  (testing "etags-different-after-post"
    (let [v (atom 1)
          handler (yada/resource (->ETagTestResource v))
          r1 @(handler (mock/request :get "/"))
          r2 @(handler (mock/request :post "/"))
          r3 @(handler (mock/request :get "/"))]
      (given [r1 r3]
        [first :status] := 200
        [second :status] := 200
        [first :headers "etag"] :? etag?
        [second :headers "etag"] :? etag?)
      (given r2
        :status := 200)
      ;; ETags are the same in both responses
      (is (not (= (get-in r1 [:headers "etag"])
                  (get-in r3 [:headers "etag"]))))))

  (testing "use-of-old-entity-tag-causes-412"
    (let [v (atom 1)
          handler (yada/resource (->ETagTestResource v))
          r1 @(handler (mock/request :get "/"))
          r2 @(handler (mock/request :post "/"))
          ]

      (let [etag (get-in r1 [:headers "etag"])
            r3 @(handler (-> (mock/request :get "/")
                             (update-in [:headers] merge {"if-match" etag})))]
        (given r3
        :status := 412)))))

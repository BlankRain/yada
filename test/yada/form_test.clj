(ns yada.form-test
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.edn :as edn]
   [clojure.test :refer :all]
   [byte-streams :as bs]
   [juxt.iota :refer (given)]
   [ring.mock.request :as mock]
   [ring.util.codec :as codec]
   [schema.core :as s]
   [yada.yada :as yada :refer [yada]]
   [yada.resource :refer [new-custom-resource]]))

(defn body-to-string [body]
  (bs/convert body String))

(deftest post-test
  (let [handler (yada
                 (new-custom-resource
                  {:methods
                   {:post {:parameters {:form {(s/required-key :foo) s/Str}}
                           :handler (fn [ctx]
                                      (pr-str (:parameters ctx)))}}}))]

    ;; Nil post body
    (let [response (handler (mock/request :post "/"))]
      (given @response
        :status := 200
        [:body body-to-string edn/read-string] := {}))

    ;; Form post body
    (let [response (handler (mock/request :post "/"
                                          {"foo" "bar"}))]
      @response
      (given @response
        :status := 200
        [:body body-to-string edn/read-string] := {:form {"foo" "bar"}}))))

(deftest post-test-with-query-params
  (let [handler (yada
                 (new-custom-resource
                  {:methods {:post {:parameters {:query {(s/required-key "foo") s/Str}
                                                 :form {(s/required-key "bar") s/Str}}
                                    :handler (fn [ctx] (pr-str (:parameters ctx)))}}}))]

    ;; Nil post body
    (let [response (handler (mock/request :post "/?foo=123"))]
      (given @response
             :status := 200
             [:body body-to-string edn/read-string] := {"foo" "123"
                                                      :query {"foo" "123"}}))

    ;; Form post body
    (let [response (handler (mock/request :post "/?foo=123"
                                          {"bar" "456"}))]
      (given @response
             :status := 200
             [:body body-to-string edn/read-string] := {"foo" "123" "bar" "456"
                                                      :query {"foo" "123"}
                                                      :form {"bar" "456"}}))))

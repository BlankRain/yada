;; Copyright © 2015, JUXT LTD.

(ns phonebook.www
  (:require
   [bidi.bidi :refer [path-for]]
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [phonebook.db :as db]
   [phonebook.html :as html]
   [yada.methods :as m]
   [yada.protocols :as p]
   [yada.resource :refer [new-custom-resource]]
   [yada.yada :as yada])
  (:import [manifold.stream.core IEventSource]))

(defn new-index-resource [db *routes]
  (new-custom-resource
   {:description "Phonebook index"
    :produces [{:media-type
                #{"text/html" "application/edn;q=0.9" "application/json;q=0.8"}
                :charset "UTF-8"}]
    :methods
    {:get {:parameters {:query {(s/optional-key :q) String}}
           :handler (fn [ctx]
                      (let [q (get-in ctx [:parameters :query :q])
                            _ (infof "q is %s" q)
                            entries (if q
                                      (db/search-entries db q)
                                      (db/get-entries db))]
                        (case (yada/content-type ctx)
                          "text/html" (html/index-html entries @*routes q)
                          entries)))}

     :post {:parameters {:form {:surname String :firstname String :phone String}}
            :consumes [{:media-type
                        #{"application/x-www-form-urlencoded"}
                        :charset "UTF-8"}]

            :handler (fn [ctx]
                       (let [id (db/add-entry db (get-in ctx [:parameters :form]))]
                         (yada/redirect-after-post
                          ctx (path-for @*routes :phonebook.api/entry :entry id))))}}}))

(defn new-entry-resource [db *routes]
  (new-custom-resource
   {:description "Phonebook entry"
    :parameters {:path {:entry Long}}
    :produces [{:media-type #{"text/html"
                              "application/edn;q=0.9"
                              "application/json;q=0.8"}
                :charset "UTF-8"}]
    :methods
    {:get
     {:handler
      (fn [ctx]
        
        (let [id (get-in ctx [:parameters :path :entry])
              _ (infof "id is %s, type is %s" id (type id))
              {:keys [firstname surname phone] :as entry} (db/get-entry db id)]
          (when entry
            (infof "entry found")
            (case (yada/content-type ctx)
              "text/html"
              (html/entry-html
               entry
               {:entry (path-for @*routes :phonebook.api/entry :entry id)
                :index (path-for @*routes :phonebook.api/index)})
              entry))))}

     :put
     {:parameters
      {:form {:surname String
              :firstname String
              :phone String}}
      :consumes
      [{:media-type #{"multipart/form-data"}}]
      :handler
      (fn [ctx]
        (let [entry (get-in ctx [:parameters :path :entry])
              form (get-in ctx [:parameters :form])]
          (db/update-entry db entry form)))}

     :delete
     {:handler
      (fn [ctx]
        (let [id (get-in ctx [:parameters :path :entry])]
          (db/delete-entry db id)
          ;; nil body
          nil))}}}))

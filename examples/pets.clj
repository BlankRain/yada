;; Copyright © 2015, JUXT LTD.

(ns pets
  (:require
   [yada.swagger :refer (->ResourceListing
                         ->Resource
                         map->Operation)]
   [manifold.deferred :as d]
   [bidi.bidi :as bidi]))

(defn find-pets [db]
  @(:atom db))

(defn find-pet-by-id [db id]
  (when-let [row (get @(:atom db) id)]
    (assoc row :id id)))

(defn pets-api [database]
  ["/api/"
   (->ResourceListing
    {:api-version "1.0.0"}
    [["pet" (->Resource
             {:description "Operations about pets"}
             [["pets"
               {:get
                (map->Operation
                 {:description "Returns all pets from the system that the user has access to"
                  :operationId :findPets
                  :scopes [""]
                  :produces ["application/json" "application/xml" "text/xml" "text/html"]
                  :parameters [{:name "tags" :in :query :description "tags to filter by"
                                :required false
                                :type :array
                                :items {:type :string}
                                :collectionFormat :csv}
                               {:name "limit"
                                :in :query
                                :description "maximum number of results to return"
                                :required false
                                :type :integer
                                :format :int32}]
                  :responses {200 {:description "pet.response"
                                   :schema []}}

                  :yada/handler
                  {:entity (fn [resource] (d/future (find-pets database)))
                   :body {}}

                  })
                :post
                (map->Operation
                 {:description "Creates a new pet in the store.  Duplicates are allowed"
                  :operationId :addPet
                  :body "TODO: addPet body"})}]

              [["pets/" :id]
               {:get
                (map->Operation
                 {:description "Returns a user based on a single ID, if the user does not have access to the pet"
                  :operationId :findPetById
                  :produces ["application/json" "application/xml" "text/xml" "text/html"]
                  :find-resource (fn [opts] {:id (-> opts :params :id)})
                  :entity (fn [{id :id}] (d/future (find-pet-by-id database id)))
                  })}]])]
     ["user" (->Resource {:description "Operations about user"} [])]
     ["store" (->Resource {:description "Operations about store"} [])]
     ])])

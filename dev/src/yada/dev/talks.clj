;; Copyright © 2015, JUXT LTD.

(ns yada.dev.talks
  (:require
   [bidi.bidi :refer (RouteProvider)]
   [bidi.ring :refer (files resources-maybe)]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [using]]
   [hiccup.core :refer (html)]
   [modular.bidi :refer [path-for]]
   [modular.component.co-dependency :refer (co-using)]
   [modular.component.co-dependency.schema :refer [co-dep]]
   [schema.core :as s]
   [yada.dev.config :as config]
   [yada.dev.template :refer [new-template-resource]]
   [yada.yada :as yada :refer [yada]])
  (:import [modular.bidi Router]))

(s/defrecord Talks [*router :- (co-dep Router)
                    config :- config/ConfigSchema]
  RouteProvider
  (routes [component]
          ["/talks/" (yada (io/file "talks") {:id ::index})]))

(defn new-talks [config]
  (-> (map->Talks {:config config})
      (co-using [:router])))

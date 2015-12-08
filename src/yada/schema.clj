;; Copyright © 2015, JUXT LTD.

(ns ^{:doc
      "This namespace provides the coercions to transform a wide
variety of shorthand descriptions of a resource into a canonical
resource map. This allows the rest of the yada code-base to remain
agnostic to the syntax of shorthand forms, which significantly
simplifies coding while giving the author of yada resources the
convenience of terse, expressive short-hand descriptions."}
    yada.schema
  (:require
   [clojure.walk :refer [postwalk]]
   [yada.media-type :as mt]
   [schema.core :as s]
   [schema.coerce :as sc]
   [yada.charset :refer [to-charset-map]])
  (:import
   [yada.charset CharsetMap]
   [yada.media_type MediaTypeMap]))

(defprotocol SetCoercion
  (as-set [_] ""))

(extend-protocol SetCoercion
  clojure.lang.APersistentSet
  (as-set [s] s)
  Object
  (as-set [s] #{s}))

(defprotocol VectorCoercion
  (as-vector [_] ""))

(extend-protocol VectorCoercion
  clojure.lang.PersistentVector
  (as-vector [v] v)
  Object
  (as-vector [o] [o]))

(s/defschema Parameters
  {(s/optional-key :parameters)
   {(s/enum :query :path :header :cookie :form :body)
    {s/Keyword s/Any}}})

(def ParametersMappings {})

(s/defschema Representation
  (s/constrained
   {:media-type MediaTypeMap
    (s/optional-key :charset) CharsetMap
    (s/optional-key :language) String
    (s/optional-key :encoding) String}
   not-empty))

(s/defschema RepresentationSet
  (s/constrained
   {:media-type #{MediaTypeMap}
    (s/optional-key :charset) #{CharsetMap}
    (s/optional-key :language) #{String}
    (s/optional-key :encoding) #{String}}
   not-empty))

(defn representation-seq
  "Return a sequence of all possible individual representations from the
  result of coerce-representations."
  [reps]
  (for [rep reps
        media-type (or (:media-type rep) [nil])
        charset (or (:charset rep) [nil])
        language (or (:language rep) [nil])
        encoding (or (:encoding rep) [nil])]
    (merge
     (when media-type {:media-type media-type})
     (when charset {:charset charset})
     (when language {:language language})
     (when encoding {:encoding encoding}))))

(defprotocol MediaTypeCoercion
  (as-media-type [_] ""))

(defprotocol RepresentationSetCoercion
  (as-representation-set [_] ""))

(extend-protocol MediaTypeCoercion
  MediaTypeMap
  (as-media-type [mt] mt)
  String
  (as-media-type [s] (mt/string->media-type s)))

(extend-protocol RepresentationSetCoercion
  clojure.lang.APersistentSet
  (as-representation-set [s] {:media-type s})
  clojure.lang.APersistentMap
  (as-representation-set [m] m)
  String
  (as-representation-set [s] {:media-type s}))

(def RepresentationSetMappings
  {[RepresentationSet] as-vector
   RepresentationSet as-representation-set
   #{MediaTypeMap} as-set
   MediaTypeMap as-media-type
   #{CharsetMap} as-set
   CharsetMap to-charset-map})

(def representation-set-coercer
  (sc/coercer [RepresentationSet] RepresentationSetMappings))

(def RepresentationSeqMappings
  ;; If representation-set-coercer is an error, don't proceed with the
  ;; representation-set-coercer
  {[Representation] (comp representation-seq representation-set-coercer)})

(def representation-seq-coercer
  (sc/coercer [Representation] RepresentationSeqMappings))

(s/defschema Produces
  {(s/optional-key :produces) [Representation]})

(s/defschema Consumes
  {(s/optional-key :consumes) [Representation]})

(defprotocol FunctionCoercion
  (as-fn [_] "Coerce to function"))

(extend-protocol FunctionCoercion
  clojure.lang.Fn
  (as-fn [f] f)
  Object
  (as-fn [o] (constantly o)))

(s/defschema Context {})

(s/defschema HandlerFunction
  (s/=> s/Any Context))

(s/defschema PropertiesResult
  {(s/optional-key :last-modified) s/Inst
   (s/optional-key :version) s/Any})

(s/defschema PropertiesHandlerFunction
  (s/=> PropertiesResult Context))

(s/defschema Properties
  {(s/optional-key :properties) PropertiesHandlerFunction})

(s/defschema Method
  (merge {:handler HandlerFunction}
         Parameters
         Produces
         Consumes))

(s/defschema Methods
  {:methods {s/Keyword Method}})

(defprotocol MethodCoercion
  (as-method-map [_] "Coerce to Method"))

(extend-protocol MethodCoercion
  clojure.lang.APersistentMap
  (as-method-map [m] m)
  String
  (as-method-map [o] {:handler o
                      :produces "text/plain"})
  Object
  (as-method-map [o] {:handler o
                      :produces "application/octet-stream"}))

(def MethodsMappings
  (merge {Method as-method-map
          HandlerFunction as-fn}
         RepresentationSeqMappings))

(def Resource
  (merge Parameters
         Properties
         Produces
         Consumes
         Methods))

(def ResourceMappings
  (merge {PropertiesHandlerFunction as-fn}
         RepresentationSeqMappings
         MethodsMappings))

(def resource-coercer (sc/coercer Resource ResourceMappings))

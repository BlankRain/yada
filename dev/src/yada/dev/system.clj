;; Copyright © 2015, JUXT LTD.

(ns yada.dev.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [modular.component.co-dependency :as co-dependency]

   [modular.bidi :refer (new-router new-web-resources new-archived-web-resources new-redirect)]
   [yada.dev.docsite :refer (new-docsite)]
   [yada.dev.console :refer (new-console)]
   [yada.dev.cors-demo :refer (new-cors-demo)]
   [yada.dev.talks :refer (new-talks)]
   [yada.dev.user-manual :refer (new-user-manual)]
   [yada.dev.database :refer (new-database)]
   [yada.dev.user-api :refer (new-verbose-user-api)]
   [modular.aleph :refer (new-webserver)]
   [modular.component.co-dependency :refer (co-using system-co-using)]

   [yada.dev.async :refer (new-handler)]
   [yada.dev.config :as config]
   [yada.dev.hello :refer (new-hello-world-example)]
   [yada.dev.error-example :refer (new-error-example)]

   [phonebook.system :refer (new-phonebook)]
   [selfie.system :refer (new-selfie-app)]))

(defn database-components [system]
  (assoc system :database (new-database)))

(defn api-components [system]
  (assoc system :user-api (new-verbose-user-api)))

(defn docsite-components [system config]
  (assoc
   system
   :user-manual (new-user-manual :prefix (config/docsite-origin config))

   :docsite (new-docsite :config config)
   :jquery (new-web-resources
            :key :jquery
            :uri-context "/jquery"
            :resource-prefix "META-INF/resources/webjars/jquery/2.1.3")
   :bootstrap (new-web-resources
               :key :bootstrap
               :uri-context "/bootstrap"
               :resource-prefix "META-INF/resources/webjars/bootstrap/3.3.2")
   :web-resources (new-web-resources
                   :uri-context "/static"
                   :resource-prefix "static")
   :highlight-js-resources
   (new-archived-web-resources :archive (io/resource "highlight.zip") :uri-context "/hljs/")
   :console (new-console config)
   ))

(defn swagger-ui-components [system]
  (assoc system
         :swagger-ui
         (new-web-resources
          :key :swagger-ui
          :uri-context "/swagger-ui"
          :resource-prefix "META-INF/resources/webjars/swagger-ui/2.1.1")))

(defn http-server-components [system config]
  (assoc system
         :docsite-server
         (new-webserver
          :port (config/docsite-port config)
          ;; raw-stream? = true gives us a manifold stream of io.netty.buffer.ByteBuf instances
          :raw-stream? true)

         :docsite-router (new-router)

         :console-server (new-webserver :port (config/console-port config))
         :console-router (new-router)

         :cors-demo-server (new-webserver :port (config/cors-demo-port config))
         :cors-demo-router (new-router)

         :talks-server (new-webserver :port (config/talks-port config))
         :talks-router (new-router)
         ))

(defn hello-world-components [system config]
  (assoc system :hello-world (new-hello-world-example config)))

(defn error-components [system]
  (assoc system :error-example (new-error-example)))

(defn cors-demo-components [system config]
  (assoc system :cors-demo (new-cors-demo config)))

(defn talks-components [system config]
  (assoc system :talks (new-talks config)))

(defn phonebook-components [system config]
  (assoc system :phonebook (new-phonebook (:phonebook config))))

(defn selfie-components [system config]
  (assoc system :selfie (new-selfie-app (:selfie config))))

(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}
          #_(database-components)
        #_(api-components)
        #_(docsite-components config)
        #_(swagger-ui-components)
        #_(http-server-components config)
        #_(hello-world-components config)
        #_(error-components)
        #_(cors-demo-components config)
        #_(talks-components config)
        (phonebook-components config)
        #_(selfie-components config)

        (assoc :docsite-redirect (new-redirect :from "/" :to :yada.dev.docsite/index))
        (assoc :console-redirect (new-redirect :from "/" :to :yada.dev.console/index))
        (assoc :cors-demo-redirect (new-redirect :from "/" :to :yada.dev.cors-demo/index))
        (assoc :talks-redirect (new-redirect :from "/" :to :yada.dev.talks/index))))))

(defn new-dependency-map
  []
  {#_:docsite-server #_{:request-handler :docsite-router}
   #_:console-server #_{:request-handler :console-router}
   #_:cors-demo-server #_{:request-handler :cors-demo-router}
   #_:talks-server #_{:request-handler :talks-router}

   #_:docsite-router #_[:swagger-ui
                        :hello-world
                        :error-example
                        :user-api
                        :user-manual
                        :docsite
                        :jquery :bootstrap
                        :web-resources
                        :highlight-js-resources
                        :docsite-redirect]

   #_:console-router #_[:console
                        :console-redirect]

   #_:cors-demo-router #_[:cors-demo
                          :jquery :bootstrap
                          :web-resources
                          :highlight-js-resources
                          :cors-demo-redirect]

   #_:talks-router #_[:talks
                      :talks-redirect]})

(defn new-co-dependency-map
  []
  {#_:docsite #_{:router :docsite-router
                 :cors-demo-router :cors-demo-router
                 :talks-router :talks-router
                 :console-router :console-router
                 :phonebook :phonebook
                 :selfie :selfie}
   #_:user-manual #_{:router :docsite-router}
   #_:console #_{:router :console-router}
   #_:cors-demo #_{:router :cors-demo-router}
   #_:talks #_{:router :talks-router}})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map (config/config :prod))
      (system-using (new-dependency-map))
      (system-co-using (new-co-dependency-map))))

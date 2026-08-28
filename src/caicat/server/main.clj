(ns caicat.server.main
  (:require [caicat.server.service :refer [save-person]]
            [caicat.shared.schema :as schema]
            [muuntaja.core :as m]
            [taoensso.timbre :as log]
            [nrepl.server]
            [reitit.coercion.malli]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]))

(def routes
  [["/"
    (ring/create-resource-handler {:root "public"})]
   ["/api"
    ["/ping"
     {:get (fn [_] {:status 200 :body "pong"})}]
    ["/person"
     {:post {:parameters {:multipart schema/Person}
             :handler save-person}}]]])

(def app
  (-> (ring/ring-handler
        (ring/router
          routes
          {:data
           {:coercion   reitit.coercion.malli/coercion
            :muuntaja   m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         (exception/create-exception-middleware
                          (merge
                            exception/default-handlers
                            {::exception/wrap (fn [handler e request]
                                                (log/error e "Request errored" {:uri (:uri request)})
                                                (handler e request))}))
                         muuntaja/format-request-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         multipart/multipart-middleware]}})
        (ring/create-default-handler))
      (wrap-resource "public")
      wrap-content-type
      wrap-not-modified))

(defn -main [& _]
  (log/merge-config!
    {:appenders
     {:spit (log/spit-appender {:fname "logs/app.log"})
      :println (log/println-appender)}})
  ; (log/set-level! :debug)
  (let [http-port 8080
        nrepl-port 7888]
    (future
      (jetty/run-jetty #'app {:port http-port :join? false}))
    (println "server running in port" http-port)

    (nrepl.server/start-server :port nrepl-port)
    (println "nREPL Server started on port" nrepl-port)))

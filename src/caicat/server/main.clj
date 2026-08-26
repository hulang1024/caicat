(ns caicat.server.main
  (:require [caicat.webui.page :refer [app-page]]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [muuntaja.core :as m]
            [nrepl.server]
            [reitit.coercion.malli]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.malli]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :as jetty]))

(defn- save [{{{:keys [name phone weixin qq idcard-a idcard-b face-photo]} :multipart} :parameters}]
  (let [dir (io/file "data" "member" phone)]
    (.mkdirs dir)
    ; 写图片
    (doseq [[target-name {:keys [filename tempfile]}] {"idcard-a" idcard-a
                                                       "idcard-b" idcard-b
                                                       "face-photo" face-photo}]
      (let [ext (subs filename (-> filename (.lastIndexOf ".")))]
        (io/copy tempfile (io/file dir (str target-name ext)))))
    ; 写json
    (let [info {:name name :phone phone :weixin weixin :qq qq}
          jsonf (io/file dir "info.json")]
      (with-open [writer (io/writer jsonf)]
        (json/generate-stream info writer))))
  {:status 200 :body {:message "OK"}})

(def routes
  [["/"
    {:get (fn [_]
            {:status 200
             :headers {"Content-Type" "text/html"}
             :body (str (app-page))})}]
   ["/api"
    ["/ping"
     {:get (fn [_] {:status 200 :body "pong"})}]
    ["/form"
     {:post {:parameters
             {:multipart {:idcard-a   reitit.ring.malli/temp-file-part
                          :idcard-b   reitit.ring.malli/temp-file-part
                          :face-photo reitit.ring.malli/temp-file-part
                          :name       [:string {:min 2}]
                          :phone      [:re #"^1[3-9]\d{9}$"]
                          :weixin     [:string {:min 6 :max 11}]
                          :qq         [:string {:min 6}]}}
             :handler save}}]]])

(def app
  (ring/ring-handler
    (ring/router
      routes
      {:data
       {:coercion   reitit.coercion.malli/coercion
        :muuntaja   m/instance
        :middleware [parameters/parameters-middleware
                     muuntaja/format-middleware
                     exception/exception-middleware
                     muuntaja/format-request-middleware
                     coercion/coerce-response-middleware
                     coercion/coerce-request-middleware
                     multipart/multipart-middleware]}})
    (ring/create-default-handler)))

(defn -main [& _]
  (let [http-port 8080
        nrepl-port 7888]
    (future
      (jetty/run-jetty #'app {:port http-port :join? false}))
    (println "server running in port" http-port)

    (nrepl.server/start-server :port nrepl-port)
    (println "nREPL Server started on port" nrepl-port)))

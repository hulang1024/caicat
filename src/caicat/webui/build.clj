(ns caicat.webui.build 
  (:require [hiccup.page :as page]
            [clojure.tools.build.api :as b]))

(defn app-page []
  (page/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:link {:rel "stylesheet"
             :href "/css/mantine-styles.css"}]]
    [:body
     [:div#app]
     [:script {:src "js/main.js"}]]))

(defn -main [& _]
  (spit "resources/public/index.html" (str (app-page)))
  (b/process {:command-args ["mkdir" "-p" "resources/public/css"]})
  (b/process {:command-args ["cp" "node_modules/@mantine/core/styles.css" "resources/public/css/mantine-styles.css"]}))

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

(defn -main []
  (b/process {:command-args ["npx" "shadow-cljs" "compile" "app"]})
  (spit "public/index.html" (str (app-page))))

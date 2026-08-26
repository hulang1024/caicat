(ns caicat.webui.page 
  (:require [hiccup.page :as page]))

(defn app-page []
  (page/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]]
    [:body
     [:div#app]
     [:script {:src "js/main.js"}]]))


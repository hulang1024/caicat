(ns caicat.webui.util
  (:require [ajax.core :refer [ajax-request]]))

(defn parse-search [search]
  (let [params (js/URLSearchParams. (or search ""))]
    (reduce (fn [acc [k v]]
              (update acc (keyword k)
                      (fn [old]          
                        (cond
                          (nil? old) v 
                          (string? old) [old v]
                          :else (conj old v)))))
            {}
            (.entries params))))

(defn request [req]
  (js/Promise.
    (fn [resolve reject]
      (ajax-request
        (assoc req
               :handler (comp resolve second)
               :error-handler reject)))))



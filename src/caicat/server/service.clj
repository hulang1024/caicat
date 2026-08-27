(ns caicat.server.service 
  (:require
    [cheshire.core :as json]
    [clojure.java.io :as io]))

(defn save [{{{:keys [name phone weixin qq idcard-a idcard-b face-photo]} :multipart} :parameters}]
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


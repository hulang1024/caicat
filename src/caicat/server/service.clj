(ns caicat.server.service 
  (:require
    [cheshire.core :as json]
    [clojure.java.io :as io]))

(defn save-person [{{{:keys [role phone] :as person} :multipart} :parameters :as req}]
  (tap> (get-in req [:parameters :multipart]))
  (let [dir (io/file "data" role phone)]
    (.mkdirs dir)
    ; 写图片
    (when (= role "member")
      (doseq [[target-name
               {:keys [filename tempfile]}] {"idcard-a" (:idcard-a person)
                                             "face-photo" (:face-photo person)}]
        (let [ext (subs filename (-> filename (.lastIndexOf ".")))]
          (io/copy tempfile (io/file dir (str target-name ext))))))
    ; 写json
    (let [info (dissoc person :idcard-a :face-photo)
          jsonf (io/file dir "info.json")]
      (with-open [writer (io/writer jsonf)]
        (json/generate-stream info writer))))
  {:status 200 :body {:ok true}})


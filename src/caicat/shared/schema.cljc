(ns caicat.shared.schema
  (:require [malli.util]
            [reitit.ring.malli :as rrm]))

(def ^:private file-part-schema
  #?(:clj  rrm/temp-file-part
     :cljs [:fn some?]))

(def PersonBase
  [:map
   [:admin-nickname [:string {:min 2}]]
   [:name           [:string {:min 2}]]
   [:phone          [:re #"^1[3-9]\d{9}$"]]
   [:weixin         [:string {:min 6 :max 11}]]
   [:qq             [:string {:min 6}]]])

(def Member
  (malli.util/merge 
    PersonBase
    [:map
     [:role [:= "member"]]
     [:idcard     [:string {:min 15}]]
     [:idcard-a   file-part-schema]
     [:face-photo file-part-schema]]))

(def Merchant
  (malli.util/merge
    PersonBase
    [:map 
     [:role [:= "merchant"]]
     [:shop-url [:re #"^https?://[^\s]+$"]]]))

(def Person
  [:multi {:dispatch :role}
   ["member" Member]
   ["merchant" Merchant]])

(ns caicat.webui.main
  (:require
    ; ["@mantine/core/styles.css"]
    ["@mantine/core" :refer [AspectRatio Button Center createTheme FileInput
                             Image MantineProvider Notification Select Space
                             Stack TextInput Title]]
    [ajax.core]
    [caicat.shared.schema :as schema]
    [caicat.webui.util :refer [parse-search request]]
    [clojure.string :as str]
    [malli.core :as m]
    [reagent.core :as r]
    [reagent.dom.client :as rd]
    [cljs.core]))

(def role-search (:role (parse-search js/location.search)))
(def role-init-val (when (contains? #{"member" "merchant"} role-search) role-search))
(defn initial-form []
  {:role (or role-init-val "")
   :admin-nickname ""
   :name ""
   :phone ""
   :weixin ""
   :qq ""
   :shop-url ""
   :idcard ""
   :idcard-a nil
   :face-photo nil
   :submitting? false
   :errors #{}
   :success? nil})

(defn ^:async submit-form [data]
  (let [form-data (js/FormData.)]
    (doseq [key (keys data)]
      (.append form-data (name key) (get data key)))
    (await
      (request
        {:method :post
         :uri "/api/person"
         ; :headers {"Authorization" (str "Bearer " token)}
         :body form-data
         :response-format (ajax.core/json-response-format {:keywords? true})}))))

(defn ^:async on-submit-click [form]
  (when-not (str/blank? (:role @form))
    (let [form-data (dissoc @form :submitting? :errors :success?)]
      (if (m/validate schema/Person form-data)
        (do 
          (swap! form assoc :submitting? true :success? nil)
          (try
            (when-let [res (await (submit-form form-data))]
              (when (:ok res)
                (reset! form (initial-form)))
              (swap! form assoc :success? (:ok res)))
            (finally
              (swap! form assoc :submitting? false))))
        (let [errs (m/explain schema/Person form-data)
              err-keys (set (map #(first (:in %)) (:errors errs)))]
          (tap> [:validation errs])
          (tap> [:form-data form-data])
          (swap! form assoc :errors err-keys))))))

(def theme
  (createTheme
    (clj->js {:components {:Input {:defaultProps {:size "md"}}
                           :Button {:defaultProps {:size "lg"}}}})))

(defn upload-photo [{:keys [label state key submitting?]}]
  (let [file-url
        (r/reaction
          (when-let [file (key @state)]
            (js/URL.createObjectURL file)))]
    [:div 
     [:> FileInput
      {:label label
       :accept "image/png,image/jpeg"
       :placeholder "请点击上传文件"
       :error (when (contains? (:errors @state) key) "请上传文件")
       :disabled submitting?
       :onChange (fn [file]
                   (swap! state assoc
                          key file
                          :errors (remove #(= key %) (:errors @state))))
       :required true}]
     [:> Space {:h "xs"}]
     (when @file-url
       [:> AspectRatio {:ratio (/ 16 9)}
        [:> Image {:class "upload-preview"
                   :fit "cover"
                   :src @file-url
                   :radius "md"}]])]))

(defn info-form []
  (let [form (r/atom (initial-form))]
    (defn inject-test-form []
      (swap! form assoc
             :role "member"
             :admin-nickname "123"
             :name "张三"
             :phone "18112345678"
             :weixin "18100001111"
             :qq "1013644379"
             :shop-url "https://www.baidu.com"
             :idcard "421124111122222012"
             :idcard-a nil
             :face-photo nil))
    (fn []
      (let [{:keys [role admin-nickname name phone weixin qq shop-url idcard submitting? errors success?]} @form]
        [:> MantineProvider {:theme theme}
         [:div {:style {:min-height "100vh" :padding "20px" :background "#fafafa"}}
          [:> Center
           [:> Title {:order 2}
            (str "填写" (case role "member" "会员" "merchant" "商家" "") "信息")]]
          [:> Stack {:gap "sm" :style {:width "100%"}}
           (if (nil? success?)
             [:<>
              (when (empty? role-init-val)
                [:> Select
                 {:label "类型"
                  :value role
                  :placeholder "请选择"
                  :data [{:value "member" :label "会员"}
                         {:value "merchant" :label "商家"}]
                  :required true
                  :onChange #(swap! form assoc :role %1)}])
              [:> TextInput
               {:label       "管理员昵称"
                :placeholder "请输入"
                :value       admin-nickname
                :error       (when (contains? errors :admin-nickname) "请输入有效的管理员昵称")
                :disabled    submitting?
                :onChange    #(swap! form assoc :admin-nickname (.. % -target -value) :error nil)
                :required    true}]
              [:> TextInput
               {:label       "姓名"
                :placeholder "请输入"
                :value       name
                :error       (when (contains? errors :name) "请输入有效的姓名")
                :disabled    submitting?
                :onChange    #(swap! form assoc :name (.. % -target -value) :error nil)
                :required    true}]
              [:> TextInput
               {:label       "手机号"
                :placeholder "请输入"
                :value       phone
                :error       (when (contains? errors :phone) "请输入有效的手机号")
                :disabled    submitting?
                :onChange    #(swap! form assoc :phone (.. % -target -value) :error nil)
                :required    true}]
              [:> TextInput
               {:label       "微信号"
                :placeholder "请输入"
                :value       weixin
                :error       (when (contains? errors :weixin) "请输入有效的微信号")
                :disabled    submitting?
                :onChange    #(swap! form assoc :weixin (.. % -target -value) :error nil)
                :required    true}]
              [:> TextInput
               {:label       "QQ号"
                :placeholder "请输入"
                :value       qq
                :error       (when (contains? errors :qq) "请输入有效的QQ号")
                :disabled    submitting?
                :onChange    #(swap! form assoc :qq (.. % -target -value) :error nil)
                :required    true}]
              (when (= role "merchant")
                [:> TextInput
                 {:label       "店铺链接"
                  :placeholder "请输入"
                  :value       shop-url
                  :error       (when (contains? errors :shop-url) "请输入有效的店铺链接")
                  :disabled    submitting?
                  :onChange    #(swap! form assoc :shop-url (.. % -target -value) :error nil)
                  :required    true}])
              (when (= role "member")
                [:<>
                 [:> TextInput
                  {:label       "身份证号"
                   :placeholder "请输入"
                   :value       idcard
                   :error       (when (contains? errors :idcard) "请输入有效的身份证")
                   :disabled    submitting?
                   :onChange    #(swap! form assoc :idcard (.. % -target -value) :error nil)
                   :required    true}]
                 [upload-photo
                  {:label    "身份证人像面"
                   :key      :idcard-a
                   :state    form
                   :disabled submitting?}]
                 [upload-photo
                  {:label    "本人露脸持身份证照片"
                   :key      :face-photo
                   :state    form
                   :disabled submitting?}]])
              [:> Space {:h "xs"}]
              [:> Button
               {:fullWidth true
                :loading   submitting?
                :onClick   #(on-submit-click form)}
               "提交"]]
             [:<>
              [:> Space {:h "xl"}]
              [:> Notification
               {:color "teal"
                :title "提示"
                :withBorder true
                :onClose #(swap! form assoc :success? nil)}
               "您的信息已提交成功"]])]]]))))

(defonce root (delay (rd/create-root (.getElementById js/document "app"))))

(defn ^:dev/after-load init []
  (println "init")
  (rd/render @root [info-form]))

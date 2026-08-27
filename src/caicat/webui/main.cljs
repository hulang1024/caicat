(ns caicat.webui.main
  (:require
    ; ["@mantine/core/styles.css"]
    ["@mantine/core" :refer [AspectRatio Button Center createTheme FileInput
                             Image MantineProvider Notification Select Space
                             Stack TextInput Title]]
    [cljs.pprint]
    [reagent.core :as r]
    [reagent.dom.client :as rd]))

(def theme
  (createTheme
    (clj->js {:components {:Input {:defaultProps {:size "md"}}
                           :Button {:defaultProps {:size "lg"}}}})))

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

(defn upload-photo [{:keys [label state key loading?]}]
  (let [file-url
        (r/reaction
          (when-let [file (key @state)]
            (js/URL.createObjectURL file)))]
    [:div 
     [:> FileInput
      {:label label
       :accept "image/png,image/jpeg"
       :placeholder "请点击上传文件"
       :disabled loading?
       :onChange #(swap! state assoc key %)
       :required true}]
     [:> Space {:h "xs"}]
     (when @file-url
       [:> AspectRatio {:ratio (/ 16 9)}
        [:> Image {:class "upload-preview mx-4"
                   :fit "cover"
                   :src @file-url
                   :radius "md"}]])]))

(defn info-form []
  (let [role-search (:role (parse-search js/location.search))
        role-init-val (when (contains? #{"member" "merchant"} role-search) role-search)
        form
        (r/atom
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
           :loading? false
           :error nil
           :success? false})]

    (fn []
      (let [{:keys [role admin-nickname name phone weixin qq shop-url idcard loading? error success?]} @form]
        [:> MantineProvider {:theme theme}
         [:div {:style {:min-height "100vh" :padding "20px" :background "#fafafa"}}
          [:> Center
           [:> Title {:order 2}
            (str "填写" (case role "member" "会员" "merchant" "商家" "") "信息")]]
          [:> Stack {:gap "sm" :style {:width "100%"}}
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
             :error       (when (= error :admin-nickname) "请输入有效的管理员昵称")
             :disabled    loading?
             :onChange    #(swap! form assoc :admin-nickname (.. % -target -value) :error nil)
             :required    true}]
           [:> TextInput
            {:label       "姓名"
             :placeholder "请输入"
             :value       name
             :error       (when (= error :name) "请输入有效的姓名")
             :disabled    loading?
             :onChange    #(swap! form assoc :name (.. % -target -value) :error nil)
             :required    true}]
           [:> TextInput
            {:label       "手机号"
             :placeholder "请输入"
             :value       phone
             :error       (when (= error :phone) "请输入有效的手机号")
             :disabled    loading?
             :onChange    #(swap! form assoc :phone (.. % -target -value) :error nil)
             :required    true}]
           [:> TextInput
            {:label       "微信号"
             :placeholder "请输入"
             :value       weixin
             :error       (when (= error :weixin) "请输入有效的微信号")
             :disabled    loading?
             :onChange    #(swap! form assoc :weixin (.. % -target -value) :error nil)
             :required    true}]
           [:> TextInput
            {:label       "QQ号"
             :placeholder "请输入"
             :value       qq
             :error       (when (= error :qq) "请输入有效的QQ号")
             :disabled    loading?
             :onChange    #(swap! form assoc :qq (.. % -target -value) :error nil)
             :required    true}]
           (when (= role "merchant")
            [:> TextInput
             {:label       "店铺链接"
              :placeholder "请输入"
              :value       shop-url
              :error       (when (= error :shop-url) "请输入有效的店铺链接")
              :disabled    loading?
              :onChange    #(swap! form assoc :shop-url (.. % -target -value) :error nil)
              :required    true}])
           (when (= role "member")
             (list [:> TextInput
                    {:label       "身份证号"
                     :placeholder "请输入"
                     :value       idcard
                     :disabled    loading?
                     :onChange    #(swap! form assoc :idcard (.. % -target -value) :error nil)
                     :required    true}]
                   [upload-photo
                    {:label    "身份证人像面"
                     :key      :idcard-a
                     :state    form
                     :disabled loading?}]
                   [upload-photo
                    {:label    "本人露脸持身份证照片"
                     :key      :face-photo
                     :state    form
                     :disabled loading?}]))
           [:> Space {:h "xs"}]
           [:> Button
            {:fullWidth true
             :loading   loading?
             :onClick   (fn [] 
                          (swap! form assoc :loading? true)
                          (js/setTimeout
                            (fn []
                              (cond
                                (not (re-matches #".+@.+\..+" name))
                                (swap! form assoc :loading? false :error :name)
                                (< (count qq) 6)
                                (swap! form assoc :loading? false :error :phone)
                                :else
                                (do
                                  (swap! form assoc
                                         :loading? false
                                         :success? true
                                         :error nil)
                                  (js/setTimeout #(swap! form assoc :success? false) 3000))))
                            1000))}
            "提交"]

           (when success?
             [:> Notification
              {:icon "✅" :color "teal" :title "登录成功" :withBorder true}
              "欢迎回来！"])]]]))))

(defonce root (delay (rd/create-root (.getElementById js/document "app"))))

(defn ^:dev/after-load init []
  (println "init")
  (rd/render @root [info-form]))

(ns toutiao.logic.qier
  (:require [toutiao.logic.driver :as mydriver]
            [etaoin.api :refer :all]))

(def driver (mydriver/create-default-browser))

(defn login [driver email password]
  (fill driver {:tag :input :name :email} email)
  (fill driver {:tag :input :name :password} password)
  (click driver [{:class :login-submit}
                 {:tag :button}]))

(defn- enter-article-page
  "进入发布文章页"
  [driver]
  (go driver "https://om.qq.com/article/articlePublish")
  #_(click driver [{:class :articlePublish}
                 {:tag :a}]))

(defn- upload-article-image
  "上传文章图片"
  [driver image-path]
  (click-visible driver [{:id :edui5_body}])
  (upload-file driver
               [{:id :filePickerReady}
                {:tag :input :class :webuploader-element-invisible}]
               image-path)
  (wait-has-text driver
                 {:tag :em :class :percent}
                 "上传成功")
  (click driver
         [{:class :layui-layer-btn}
          {:tag :a}]))

(defn- fill-article-content
  "编辑文章，article必须是一个列表，包含图片和文字"
  [driver article]
  (doseq [{:keys [type content]} article]
    (if (= type :image)
      (upload-article-image driver content)
      (fill driver [{:id :edui2_iframeholder}
                    {:tag :iframe}]
            content))))

(defn- fill-article-type [driver k]
  (scroll-query driver {:class :ui-select})
  (click driver [{:class :ui-select}])
  (fill-active driver k)
  (click driver [{:class :active-result
                  :tag :li}]))

(defn post-article
  "发布文章"
  [driver type title article]
  (fill driver [{:id :om-art-normal-title}
                {:tag :input}]
        title)
  (fill-article-content driver article)
  (fill-article-type driver type)
  (click driver [{:id :mod-actions}
                 {:tag :button}]))

#_(login driver "dasheng523@163.com" "a5235013")
#_(enter-article-page driver)
#_(post-article
 driver
 "qinggan"
 "test test"
 [{:type :image
   :content "/Users/huangyesheng/Downloads/2719544-5f38ee75812cfb2c.jpg"}
  {:type :text
   :content "清晨的阳光洒满了整个屋子，窗台的几盆花开得正艳。
“呃，还有，你追我的方式也傻得可爱。”她对着镜子笑个不停，握着口红的手停在了半空中。"}
  {:type :image
   :content "/Users/huangyesheng/Downloads/2719544-5f38ee75812cfb2c.jpg"}
  {:type :text
   :content "清晨的阳光洒满了整个屋子，窗台的几盆花开得正艳。
“呃，还有，你追我的方式也傻得可爱。”她对着镜子笑个不停，握着口红的手停在了半空中。"}])



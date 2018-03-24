(defproject toutiao2 "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[buddy "2.0.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cider/cider-nrepl "0.15.1-SNAPSHOT"]
                 [clj-oauth "1.5.4"]
                 [clj-time "0.14.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljs-ajax "0.7.3"]
                 [com.h2database/h2 "1.4.196"]
                 [compojure "1.6.0"]
                 [conman "0.7.1"]
                 [cprop "0.1.11"]
                 [funcool/struct "1.1.0"]
                 [luminus-jetty "0.1.6"]
                 [luminus-migrations "0.5.0"]
                 [luminus-nrepl "0.1.4"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [markdown-clj "1.0.1"]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/muuntaja "0.3.2"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.11"]
                 [org.clojure/clojure "1.9.0-RC2"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.reader "1.1.0"]
                 [org.webjars.bower/tether "1.4.0"]
                 [org.webjars/bootstrap "4.0.0-beta.2"]
                 [org.webjars/font-awesome "4.7.0"]
                 [re-frame "0.10.2"]
                 [re-com "2.1.0"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-servlet "1.4.0"]
                 [funcool/cuerdas "2.0.5"]
                 [secretary "1.2.3"]                        ;前端导航
                 [selmer "1.11.3"]
                 [slingshot "0.12.2"]                       ; 异常处理
                 [day8.re-frame/http-fx "0.1.4"]            ; 前端http
                 [clj-webdriver/clj-webdriver "0.7.2"]
                 [etaoin "0.2.1"] ; 跟webdriver一样，是操作浏览器的
                 [clj-http "3.7.0"]
                 [enlive "1.1.6"]
                 [com.taoensso/timbre "4.1.0"]
                 [cheshire "5.8.0"] ; json
                 [dk.ative/docjure "1.11.0"]                ; Excel
                 [org.clojure/data.csv "0.1.4"]             ; CSV
                 [prismatic/schema "1.1.7"]                 ; 验证库
                 [com.hankcs/hanlp "portable-1.5.3"]        ; 自然语言库
                 [mysql/mysql-connector-java "5.1.34"]      ; mysql
                 [honeysql "0.9.1"]                         ; sql
                 [org.clojure/java.jdbc "0.7.5"]            ; jdbc
                 [digest "1.4.6"]                           ; 加密用的
                 [javax.xml.bind/jaxb-api "2.2.8"]          ; xml
                 [flake "0.4.5"]                            ; 生成唯一字符串
                 [hikari-cp "2.2.0"]                        ; 数据库连接池
                 [tongue "0.2.4"]                           ; i18n
                 #_[opencv/opencv "3.3.1"]
                 #_[opencv/opencv-native "3.3.1"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot toutiao2.core
  :migratus {:store :database :db ~(get (System/getenv) "DATABASE_URL")}

  :plugins [[lein-cprop "1.0.3"]
            [migratus-lein "0.5.2"]
            [org.clojars.punkisdead/lein-cucumber "1.0.5"]
            [lein-cljsbuild "1.1.5"]
            [lein-sassc "0.10.4"]
            [lein-auto "0.1.2"]
            [lein-kibit "0.1.2"]
            [lein-uberwar "0.2.0"]]
  :cucumber-feature-paths ["test/clj/features"]

   :sassc
   [{:src "resources/scss/screen.scss"
     :output-to "resources/public/css/screen.css"
     :style "nested"
     :import-path "resources/scss"}]

   :auto
   {"sassc"
    {:file-pattern #"\.(scss|sass)$"
     :paths ["resources/scss"]}}

;;  :hooks [leiningen.sassc]
  :uberwar
  {:handler toutiao2.handler/app
   :init toutiao2.handler/init
   :destroy toutiao2.handler/destroy
   :name "toutiao2.war"}

  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware
   [cemerick.piggieback/wrap-cljs-repl cider.nrepl/cider-middleware]}

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-to "target/cljsbuild/public/js/app.js"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}}}
             :aot :all
             :uberjar-name "toutiao2.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.4"]
                                 [ring/ring-mock "0.3.1"]
                                 [ring/ring-devel "1.6.3"]
                                 [pjstadig/humane-test-output "0.8.3"]
                                 [binaryage/devtools "0.9.7"]
                                 [com.cemerick/piggieback "0.2.2"]
                                 [directory-naming/naming-java "0.8"]
                                 [doo "0.1.8"]
                                 [figwheel-sidecar "0.5.14"]
                                 [org.apache.httpcomponents/httpcore "4.4"]
                                 [org.clojure/core.cache "0.7.1"]
                                 [org.seleniumhq.selenium/selenium-server "2.48.2" :exclusions [org.bouncycastle/bcprov-jdk15on org.bouncycastle/bcpkix-jdk15on]]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.19.0"]
                                 [lein-doo "0.1.8"]
                                 [lein-figwheel "0.5.14"]
                                 [org.clojure/clojurescript "1.9.946"]]
                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :figwheel {:on-jsload "toutiao2.core/mount-components"}
                     :compiler
                     {:main "toutiao2.app"
                      :asset-path "/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}}}
                  :doo {:build "test"}
                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "toutiao2.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}
                  }
   :profiles/dev {}
   :profiles/test {}})

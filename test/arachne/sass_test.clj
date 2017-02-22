(ns arachne.sass-test
  (:require [clojure.test :refer :all]
            [arachne.core :as arachne]
            [arachne.error :as error]
            [arachne.core.runtime :as rt]
            [arachne.core.dsl :as ac]
            [arachne.sass.build :as build]
            [arachne.sass.dsl :as sass]
            [arachne.core.config :as cfg]
            [arachne.assets.dsl :as a]
            [com.stuartsierra.component :as component]
            [arachne.fileset :as fs]
            [clojure.java.io :as io]))

(defn build-cfg
  "DSL function to build a simple SASS config"
  [entrypoint output-dir watch]

  (a/input-dir :test/input "test/arachne/sass" :watch? watch)
  (sass/build :test/build
              :output-to "application.css"
              :output-dir "css"
              :entrypoint entrypoint)
  (a/output-dir :test/output output-dir)
  (a/pipeline [:test/input :test/build] [:test/build :test/output])
  (ac/runtime :test/rt [:test/output]))

(deftest basic-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:org.arachne-framework/arachne-sass]
                                         `(arachne.sass-test/build-cfg "basic.scss" ~(.getCanonicalPath output-dir) false))
        rt         (component/start (rt/init cfg [:arachne/id :test/rt]))
        result     (slurp (io/file output-dir "css/application.css"))]
    (is (re-find #"background-color: #0000FF;" result))))

;; This can never be true since pipeline errors are logged but not thrown from the top level
#_(deftest error-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:org.arachne-framework/arachne-sass]
                                         `(arachne.sass-test/build-cfg "error.scss" ~(.getCanonicalPath output-dir) false))]
    (is (thrown? arachne.ArachneException
                 (component/start (rt/init cfg [:arachne/id :test/rt]))))))

(deftest basic-sass-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:org.arachne-framework/arachne-sass]
                                         `(arachne.sass-test/build-cfg "basic.sass" ~(.getCanonicalPath output-dir) false))
        rt         (component/start (rt/init cfg [:arachne/id :test/rt]))
        result     (slurp (io/file output-dir "css/application.css"))]
    (is (re-find #"background-color: #0000FF;" result))))

;;
;; Test a more complicated build
;;
(defn complicated-build-cfg
  "DSL function to build a simple SASS config"
  [entrypoint output-dir watch]

  ;; for all the SASSC compiler options, all paths are relative to the output fileset
  (def opts {:output-to  "application.css"
             :output-dir "css"
             :load-path ["some-lib" "some-other-lib"] ;; some-lib is a directory under the vendor directory
             :source-map true
             :precision 2
             :omit-map-comment true
             :entrypoint entrypoint})

  (a/input-dir :test/input "test/arachne/sass" :watch? watch)
  (a/input-dir :test/vendored-input "test/arachne/vendor" :watch? watch)
  (sass/build :test/build opts)
  (a/output-dir :test/output output-dir)
  (a/pipeline [:test/input :test/build]
              [:test/vendored-input :test/build]
              [:test/build :test/output])
  (ac/runtime :test/rt [:test/output]))

(deftest complicated-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:org.arachne-framework/arachne-sass]
                                         `(arachne.sass-test/complicated-build-cfg "complicated.scss" ~(.getCanonicalPath output-dir) false))
        rt         (component/start (rt/init cfg [:arachne/id :test/rt]))
        result     (slurp (io/file output-dir "css/application.css"))
        source-map (slurp (io/file output-dir "css/application.css.map"))]
    (is (re-find #"\.grid" result)) ;; Test loading from the load path
    (is (re-find #"\.btn" result))
    (is (re-find #"33.33%" result)) ;; Test precision
    (is (nil? (re-find #"sourceMappingURL" result))) ;; Test omit-map-comment
    (is (re-find #"mappings" source-map))  ;; Test the source map file
))

(comment

  (def cfg (arachne/build-config [:org.arachne-framework/arachne-sass]
                                 '(arachne.sass-test/build-cfg "/tmp/out" true)))

  (def rt (rt/init cfg [:arachne/id :test/rt]))

  (def rt (component/start rt))
  (def rt (component/stop rt)))

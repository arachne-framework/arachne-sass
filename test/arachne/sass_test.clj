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

  (ac/id :test/input (a/input-dir "test/arachne/sass" :watch? watch))
  (ac/id :test/build (sass/build
                      :output-to "css/application.css"
                      :entrypoint entrypoint))
  (ac/id :test/output (a/output-dir output-dir))
  (a/pipeline [:test/input :test/build] [:test/build :test/output])
  (ac/id :test/rt (ac/runtime [:test/output])))

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
  (def opts {:output-to  "css/application.css"
             :load-path ["some-lib" "some-other-lib"] ;; some-lib is a directory under the vendor directory
             :source-map true
             :source-comments true
             :precision 2
             :omit-map-comment true
             :style :expanded
             :entrypoint entrypoint})

  (ac/id :test/input (a/input-dir "test/arachne/sass" :watch? watch))
  (ac/id :test/vendored-input (a/input-dir "test/arachne/vendor" :watch? watch))
  (ac/id :test/build (sass/build opts))
  (ac/id :test/output (a/output-dir output-dir))
  (a/pipeline [:test/input :test/build]
              [:test/vendored-input :test/build]
              [:test/build :test/output])

  (ac/id :test/rt (ac/runtime [:test/output])))

(deftest complicated-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:org.arachne-framework/arachne-sass]
                                         `(arachne.sass-test/complicated-build-cfg "subfolder/complicated.scss" ~(.getCanonicalPath output-dir) false))
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

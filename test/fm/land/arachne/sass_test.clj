(ns fm.land.arachne.sass-test
  (:require [clojure.test :refer :all]
            [arachne.core :as arachne]
            [arachne.error :as error]
            [arachne.core.runtime :as rt]
            [arachne.core.dsl :as ac]
            [fm.land.arachne.sass.build :as build]
            [fm.land.arachne.sass.dsl :as sass]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [arachne.core.config :as cfg]
            [arachne.assets.dsl :as a]
            [com.stuartsierra.component :as component]
            [clojure.walk :as w]
            [arachne.fileset :as fs]
            [clojure.java.io :as io]))

;; Used to smuggle a value into the config script
(def ^:dynamic *compiler-opts*)

(defn roundtrip-cfg
  "DSL function to build test config that doesn't do much with the config data."
  []
  (a/input-dir :test/input "test")
  (sass/build :test/build *compiler-opts*)
  (a/pipeline [:test/input :test/build])
  (ac/runtime :test/rt [:test/build]))

(defn- normalize
  "Convert all nested sequences to sets so items can be compared in an order-agnostic way"
  [o]
  (w/prewalk (fn [f]
               (if (and (not (map-entry? f)) (sequential? f))
                 (into #{} f)
                 f)) o))

(defn- roundtrip
  [compile-opts]
  (binding [*compiler-opts* compile-opts]
    (let [cfg (arachne/build-config [:fm.land/arachne-sass]
                                    '(fm.land.arachne.sass-test/roundtrip-cfg))
          opts (cfg/q cfg '[:find ?co .
                            :where
                            [?b :arachne/id :test/build]
                            [?b :fm.land.arachne.sass.build/compiler-options ?co]])]
      (@#'build/extract (cfg/pull cfg '[*] opts)))))

(defspec sass-configs-roundtrip-through-arachne 70
  (prop/for-all [compile-opts (s/gen :fm.land.arachne.sass.dsl/compiler-options)]
                (let [output (roundtrip compile-opts)]
                  (= (normalize output)
                     (normalize compile-opts)))))

(defn build-cfg
  "DSL function to build a simple SASS config"
  [entrypoint output-dir watch]

  ;; for all the SASSC compiler options, all paths are relative to the output fileset
  (def opts {:output-to     "application.css"
             :output-dir    "css"
             :entrypoint    entrypoint})

  (a/input-dir :test/input "test/fm/land/arachne/sass" :watch? watch)
  (sass/build :test/build opts)
  (a/output-dir :test/output output-dir)
  (a/pipeline [:test/input :test/build] [:test/build :test/output])
  (ac/runtime :test/rt [:test/output]))

(deftest basic-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:fm.land/arachne-sass]
                                         `(fm.land.arachne.sass-test/build-cfg "basic.scss" ~(.getCanonicalPath output-dir) false))
        rt         (component/start (rt/init cfg [:arachne/id :test/rt]))
        result     (slurp (io/file output-dir "css/application.css"))]
    (is (re-find #"background-color: #0000FF;" result))))

;; (deftest error-build
;;   (let [output-dir (fs/tmpdir!)
;;         cfg        (arachne/build-config [:fm.land/arachne-sass]
;;                                          `(fm.land.arachne.sass-test/build-cfg "error.scss" ~(.getCanonicalPath output-dir) false))]
;;     (is (thrown? arachne.ArachneException
;;                  (component/start (rt/init cfg [:arachne/id :test/rt]))))))

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

  (a/input-dir :test/input "test/fm/land/arachne/sass" :watch? watch)
  (a/input-dir :test/vendored-input "test/fm/land/arachne/vendor" :watch? watch)
  (sass/build :test/build opts)
  (a/output-dir :test/output output-dir)
  (a/pipeline [:test/input :test/build]
              [:test/vendored-input :test/build]
              [:test/build :test/output])
  (ac/runtime :test/rt [:test/output]))

(deftest complicated-build
  (let [output-dir (fs/tmpdir!)
        cfg        (arachne/build-config [:fm.land/arachne-sass]
                                         `(fm.land.arachne.sass-test/complicated-build-cfg "complicated.scss" ~(.getCanonicalPath output-dir) false))
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

  (def cfg (arachne/build-config [:fm.land/arachne-sass]
                                 '(fm.land.arachne.sass-test/build-cfg "/tmp/out" true)))

  (def rt (rt/init cfg [:arachne/id :test/rt]))

  (def rt (component/start rt))
  (def rt (component/stop rt)))

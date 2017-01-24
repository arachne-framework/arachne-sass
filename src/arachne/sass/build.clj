(ns arachne.sass.build
  (:require
    [sass.build.api :as sass]
    [clojure.spec :as s]
    [arachne.sass.schema :as schema]
    [arachne.error :as e :refer [deferror error]]
    [arachne.core.config :as cfg]
    [arachne.core.util :as u]
    [arachne.assets.pipeline :as p]
    [arachne.fileset :as fs]
    [clojure.java.io :as io]
    [arachne.log :as log]))

(defn- foreign-lib
  [entity]
  (u/map-transform entity {}
    :arachne.sass.foreign-library/file :file identity
    :arachne.sass.foreign-library/file-min :file-min identity
    :arachne.sass.foreign-library/provides :provides vec
    :arachne.sass.foreign-library/requires :requires vec
    :arachne.sass.foreign-library/module-type :module-type identity
    :arachne.sass.foreign-library/preproccess :preprocess identity))

(defn- modules
  [modules]
  (reduce (fn [output module]
            (assoc output (:arachne.sass.closure-module/id module)
                          (u/map-transform module {}
                            :arachne.sass.closure-module/output-to :output-to identity
                            :arachne.sass.closure-module/entries :entries set
                            :arachne.sass.closure-module/depends-on :depends-on set)))
    {} modules))

(defn- warnings
  [warnings]
  (into {} (map (fn [warning]
                  [(:arachne.sass.warning/type warning)
                   (:arachne.sass.warning/enabled warning)]) warnings)))

(defn- closure-warnings
  [warnings]
  (into {} (map (fn [warning]
                  [(:arachne.sass.closure-warning/type warning)
                   (:arachne.sass.closure-warning/value warning)]) warnings)))

(defn- closure-defines
  [defines]
  (into {} (map (fn [define]
                  [(:arachne.sass.closure-define/variable define)
                   (:arachne.sass.closure-define/annotate define)]) defines)))

(defn- extract
  "Giventhe entity map of a arachne.sass/CompilerOptions entity, return a standard SASS options
   map, as it was stored in the config."
  [entity]
  (u/map-transform entity {}
    :arachne.sass.compiler-options/main :main #(symbol (namespace %) (name %))
    :arachne.sass.compiler-options/asset-path :asset-path identity
    :arachne.sass.compiler-options/output-to :output-to identity
    :arachne.sass.compiler-options/output-dir :output-dir identity
    :arachne.sass.compiler-options/foreign-libs :foreign-libs #(map foreign-lib %)
    :arachne.sass.compiler-options/modules :modules modules
    :arachne.sass.compiler-options/common-warnings? :warnings identity
    :arachne.sass.compiler-options/warnings :warnings warnings
    :arachne.sass.compiler-options/closure-warnings :closure-warnings closure-warnings
    :arachne.sass.compiler-options/closure-defines :closure-defines closure-defines
    :arachne.sass.compiler-options/optimizations :optimizations identity
    :arachne.sass.compiler-options/source-map :source-map #(cond
                                                             (= % "true") true
                                                             (= % "false") false
                                                             :else %)
    :arachne.sass.compiler-options/verbose :verbose identity
    :arachne.sass.compiler-options/pretty-print :pretty-print identity
    :arachne.sass.compiler-options/target :target identity
    :arachne.sass.compiler-options/externs :externs vec
    :arachne.sass.compiler-options/preloads :preloads #(vec (map (fn [kw] (symbol (namespace kw) (name kw))) %))
    :arachne.sass.compiler-options/source-map-path :source-map-path identity
    :arachne.sass.compiler-options/source-map-asset-path :source-map-asset-path identity
    :arachne.sass.compiler-options/source-map-timestamp :source-map-timestamp identity
    :arachne.sass.compiler-options/cache-analysis :cache-analysis identity
    :arachne.sass.compiler-options/recompile-dependents :recompile-dependents identity
    :arachne.sass.compiler-options/static-fns :static-fns identity
    :arachne.sass.compiler-options/load-tests :load-tests identity
    :arachne.sass.compiler-options/elide-asserts :elide-asserts identity
    :arachne.sass.compiler-options/pseudo-names :pseudo-names identity
    :arachne.sass.compiler-options/print-input-delimiter :print-input-delimiter identity
    :arachne.sass.compiler-options/output-wrapper :output-wrapper identity
    :arachne.sass.compiler-options/libs :libs vec
    :arachne.sass.compiler-options/preamble :preamble vec
    :arachne.sass.compiler-options/hashbang :hashbang identity
    :arachne.sass.compiler-options/compiler-stats :compiler-stats identity
    :arachne.sass.compiler-options/language-in :language-in identity
    :arachne.sass.compiler-options/language-out :language-out identity
    :arachne.sass.compiler-options/closure-extra-annotations :closure-extra-annotations set
    :arachne.sass.compiler-options/anon-fn-naming-policy :anon-fn-naming-policy identity
    :arachne.sass.compiler-options/optimize-constants :optimize-constants identity))

;(fs/checksum fs false)

(defn- append-path
  "Apped the given path suffix to a base path (a File)."
  [base suffix]
  (let [suffix (or suffix "")
        suffix (if (= "/" suffix) "" suffix)]
    (.getCanonicalPath (io/file base suffix))))

(defn update-if-present
  "Like clojure.core/update, but does not alter the map if the key is not present"
  [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn compiler-options
  "Return a map of ClojureScript compiler options, ready to use.

  Takes the config, the eid of the CompilerOptions entity, and a File of the absolute output directory"
  [options-entity out-dir]
  (-> (extract options-entity)
    (update-if-present :output-to #(append-path out-dir %))
    (update-if-present :output-dir #(append-path out-dir %))
    (update-if-present :source-map #(if (string? %)
                           (append-path out-dir %)
                           %))
    (update-if-present :externs #(map (fn [extern] (append-path out-dir extern)) %))
    (update-if-present :libs #(map (fn [lib] (append-path out-dir lib)) %))
    (update-if-present :preamble #(map (fn [pre] (append-path out-dir pre)) %))
    (update-if-present :modules (fn [module-map]
                                  (let [module-map (into {} (map (fn [[name module-opts]]
                                                                   [name (update module-opts :output-to
                                                                           #(append-path out-dir %))])
                                                              module-map))]
                                    (if (empty? module-map)
                                      nil
                                      module-map))))))

(defn build-transducer
  "Return a transducer over filesets that builds ClojureScript files"
  [component]
  (let [options-entity (:arachne.sass.build/compiler-options component)
        out-dir (fs/tmpdir!)
        build-id (:arachne/id component)]
    (map (fn [input-fs]
           (let [src-dir (fs/tmpdir!)]
             (fs/commit! input-fs src-dir)
             (log/info :msg "Building ClojureScript" :build-id build-id)
             (let [started (System/currentTimeMillis)
                   sass-opts (compiler-options options-entity out-dir)]
               (sass/build (.getCanonicalPath src-dir) sass-opts)
               (let [elapsed (- (System/currentTimeMillis) started)
                     elapsed-seconds (float (/ elapsed 1000))]
                 (log/info :msg (format "ClojureScript build complete in %.2f seconds" elapsed-seconds)
                           :build-id build-id
                           :elapsed-ms elapsed)))
             (fs/add (fs/empty input-fs) out-dir))))))

(ns arachne.sass.dsl
  "DSL code to handle ClojureScript compiler options"
  (:require [clojure.spec :as s]
            [arachne.error :as e :refer [deferror error]]
            [arachne.core.dsl :as core]
            [arachne.core.config :as cfg]
            [arachne.core.config.script :as script :refer [defdsl]]
            [arachne.core.util :as u]))

(s/def ::string (s/and string? #(not-empty %)))

(s/def ::file ::string)
(s/def ::provides (s/coll-of ::string :min-count 1))
(s/def ::file-min ::string)
(s/def ::requires (s/coll-of ::string :min-count 1))
(s/def ::module-type #{:commonjs :amd :es6})
(s/def ::preprocess keyword?)

(s/def ::foreign-libs
  (s/coll-of
    (s/keys
      :req-un [::file
               ::provides]
      :opt-un [::file-min
               ::requires
               ::module-type
               ::preprocess])
    :min-count 1))

(s/def ::entries (s/coll-of ::string :min-count 1 :into #{}))
(s/def ::depends-on (s/coll-of keyword? :min-count 1 :into #{}))

(s/def ::module
  (s/keys
    :req-un [::output-to
             ::entries]
    :opt-un [::depends-on]))

(s/def ::modules (s/map-of keyword? ::module :min-count 1))

(s/def ::warnings (s/or :boolean boolean?
                        :enumerated (s/map-of keyword? boolean? :min-count 1)))

(s/def ::closure-warnings (s/map-of keyword? #{:off :error :warning} :min-count 1))

(s/def ::closure-defines (s/map-of ::string boolean? :min-count 1))

(s/def ::optimizations #{:none :whitespace :simple :advanced})

(s/def ::source-map (s/or :boolean boolean?
                          :string ::string))

(s/def ::verbose boolean?)
(s/def ::pretty-print boolean?)
(s/def ::target #{:nodejs})
(s/def ::externs (s/coll-of ::string :min-count 1))
(s/def ::preloads (s/coll-of symbol? :min-count 1))

(s/def ::source-map-path ::string)
(s/def ::source-map-asset-path ::string)
(s/def ::source-map-timestamp boolean?)
(s/def ::cache-analysis boolean?)
(s/def ::recompile-dependents boolean?)
(s/def ::static-fns boolean?)
(s/def ::load-tests boolean?)
(s/def ::elide-asserts boolean?)
(s/def ::pseudo-names boolean?)
(s/def ::print-input-delimiter boolean?)
(s/def ::output-wrapper boolean?)
(s/def ::libs (s/coll-of ::string :min-count 1))
(s/def ::preamble (s/coll-of ::string :min-count 1))
(s/def ::hashbang boolean?)
(s/def ::compiler-stats boolean?)
(s/def ::language-in #{:ecmascript3, :ecmascript5, :ecmascript5-strict, :ecmascript6-typed, :ecmascript6-strict, :ecmascript6, :no-transpile})
(s/def ::language-out #{:ecmascript3, :ecmascript5, :ecmascript5-strict, :ecmascript6-typed, :ecmascript6-strict, :ecmascript6, :no-transpile})
(s/def ::closure-extra-annotations (s/coll-of ::string :min-count 1 :into #{}))
(s/def ::anon-fn-naming-policy #{:off :mapped :unmapped})
(s/def ::optimize-constants boolean?)
(s/def ::main symbol?)
(s/def ::output-to ::string)
(s/def ::output-dir ::string)
(s/def ::asset-path ::string)


;; Note: these are more strict than SASS itself, but they avoid several confusing edge cases, and are almost
;; certainly what you want anyway.
(s/def ::compiler-options
  (s/keys
    :req-un [(or ::output-to ::modules)
             ::main]
    :opt-un [::optimizations
             ::output-dir
             ::asset-path
             ::foreign-libs
             ::warnings
             ::closure-warnings
             ::closure-defines
             ::source-map
             ::verbose
             ::pretty-print
             ::target
             ::externs
             ::preloads
             ::source-map-timestamp
             ::source-map-path
             ::source-map-asset-path
             ::cache-analysis
             ::recompile-dependents
             ::static-fns
             ::load-tests
             ::elide-asserts
             ::pseudo-names
             ::print-input-delimiter
             ::output-wrapper
             ::libs
             ::preamble
             ::hashbang
             ::compiler-stats
             ::language-in
             ::language-out
             ::closure-extra-annotations
             ::anon-fn-naming-policy
             ::optimize-constants]))

(defn- foreign-lib
  [m]
  (u/map-transform m {}
    :file :arachne.sass.foreign-library/file identity
    :file-min :arachne.sass.foreign-library/file-min identity
    :provides :arachne.sass.foreign-library/provides vec
    :requires :arachne.sass.foreign-library/requires vec
    :module-type :arachne.sass.foreign-library/module-type identity
    :preprocess :arachne.sass.foreign-library/preproccess identity))

(defn- modules
  [module-map]
  (vec (map (fn [[id mm]]
              (u/map-transform mm {:arachne.sass.closure-module/id id}
                :output-to :arachne.sass.closure-module/output-to identity
                :entries :arachne.sass.closure-module/entries vec
                :depends-on :arachne.sass.closure-module/depends-on vec))
         module-map)))

(defn- warnings
  [[tag warnings-map]]
  (when (= :enumerated tag)
    (vec (map (fn [[type enabled]]
                {:arachne.sass.warning/type type
                 :arachne.sass.warning/enabled enabled})
           warnings-map))))

(defn- closure-warnings
  [warnings-map]
  (vec (map (fn [[type value]]
              {:arachne.sass.closure-warning/type type
               :arachne.sass.closure-warning/value value})
         warnings-map)))

(defn- closure-defines
  [defines-map]
  (vec (map (fn [[variable annotate]]
              {:arachne.sass.closure-define/variable (str variable)
               :arachne.sass.closure-define/annotate annotate})
         defines-map)))

(defn compiler-options
  "Given a conformed map of compiler options, return an entity map for a arachne.sass/CompilerOptions entity."
  [opts]
  (u/map-transform opts {}
    :main :arachne.sass.compiler-options/main keyword
    :asset-path :arachne.sass.compiler-options/asset-path identity
    :output-to :arachne.sass.compiler-options/output-to identity
    :output-dir :arachne.sass.compiler-options/output-dir identity
    :foreign-libs :arachne.sass.compiler-options/foreign-libs #(map foreign-lib %)
    :modules :arachne.sass.compiler-options/modules modules
    :warnings :arachne.sass.compiler-options/common-warnings? (fn [[tag val]] (when (= :boolean tag) val))
    :warnings :arachne.sass.compiler-options/warnings warnings
    :closure-warnings :arachne.sass.compiler-options/closure-warnings closure-warnings
    :closure-defines :arachne.sass.compiler-options/closure-defines closure-defines
    :optimizations :arachne.sass.compiler-options/optimizations identity
    :source-map :arachne.sass.compiler-options/source-map (fn [[tag val]] (str val))
    :verbose :arachne.sass.compiler-options/verbose identity
    :pretty-print :arachne.sass.compiler-options/pretty-print identity
    :target :arachne.sass.compiler-options/target identity
    :externs :arachne.sass.compiler-options/externs vec
    :preloads :arachne.sass.compiler-options/preloads #(vec (map keyword %))
    :source-map-path :arachne.sass.compiler-options/source-map-path identity
    :source-map-asset-path :arachne.sass.compiler-options/source-map-asset-path identity
    :source-map-timestamp :arachne.sass.compiler-options/source-map-timestamp identity
    :cache-analysis :arachne.sass.compiler-options/cache-analysis identity
    :recompile-dependents :arachne.sass.compiler-options/recompile-dependents identity
    :static-fns :arachne.sass.compiler-options/static-fns identity
    :load-tests :arachne.sass.compiler-options/load-tests identity
    :elide-asserts :arachne.sass.compiler-options/elide-asserts identity
    :pseudo-names :arachne.sass.compiler-options/pseudo-names identity
    :print-input-delimiter :arachne.sass.compiler-options/print-input-delimiter identity
    :output-wrapper :arachne.sass.compiler-options/output-wrapper identity
    :libs :arachne.sass.compiler-options/libs vec
    :preamble :arachne.sass.compiler-options/preamble vec
    :hashbang :arachne.sass.compiler-options/hashbang identity
    :compiler-stats :arachne.sass.compiler-options/compiler-stats identity
    :language-in :arachne.sass.compiler-options/language-in identity
    :language-out :arachne.sass.compiler-options/language-out identity
    :closure-extra-annotations :arachne.sass.compiler-options/closure-extra-annotations vec
    :anon-fn-naming-policy :arachne.sass.compiler-options/anon-fn-naming-policy identity
    :optimize-constants :arachne.sass.compiler-options/optimize-constants identity))

(defdsl build
  "Define an Asset transducer component which builds ClojureScript.

  Arguments are:

  - arachne-id (optional): the Arachne ID of the component
  - compiler-options: A ClojureScript compiler options map. See the ClojureScript documentation
    for possible values. The only difference is that options which specify paths (:output-to, :output-dir,
    :preamble, :externs, etc.) will relative to the asset fileset rather than the process as a whole.

  Returns the entity ID of the newly-created component."

  (s/cat :arachne-id (s/? ::core/arachne-id) :compiler-opts ::compiler-options)
  [<arachne-id> compiler-opts]
  (let [tid (cfg/tempid)
        entity (u/mkeep {:db/id tid
                         :arachne/id (:arachne-id &args)
                         :arachne.component/constructor :arachne.assets.pipeline/transducer
                         :arachne.assets.transducer/constructor :arachne.sass.build/build-transducer
                         :arachne.sass.build/compiler-options (compiler-options (:compiler-opts &args))})]
    (script/transact [entity] tid)))

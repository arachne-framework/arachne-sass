(ns arachne.sass.dsl
  "DSL code to handle SASSC compiler options"
  (:require [clojure.spec.alpha :as s]
            [arachne.error :as e :refer [deferror error]]
            [arachne.core.dsl :as core]
            [arachne.core.config :as cfg]
            [arachne.core.config.script :as script :refer [defdsl]]
            [arachne.core.util :as u]))

(s/def ::string (s/and string? #(not-empty %)))
(s/def ::path ::string)

(s/def ::style #{:nested :expanded :compact :compressed})
(s/def ::line-numbers boolean?)
(s/def ::entrypoint ::path)
(s/def ::output-to ::path)
(s/def ::load-path (s/coll-of ::path :min-count 1))
(s/def ::plugin-path (s/coll-of ::path :min-count 1))
(s/def ::source-map boolean?)
(s/def ::omit-map-comment boolean?)
(s/def ::precision integer?)
(s/def ::sass boolean?)

(s/def ::compiler-options (u/keys** :req-un [::entrypoint
                                             ::output-to]
                                    :opt-un [::style
                                             ::line-numbers
                                             ::load-path
                                             ::plugin-path
                                             ::source-map
                                             ::omit-map-comment
                                             ::precision
                                             ::sass]))

(defn- compiler-options [opts]
  (u/map-transform opts {}
                   :entrypoint :arachne.sass.compiler-options/entrypoint identity
                   :output-to :arachne.sass.compiler-options/output-to identity
                   :style :arachne.sass.compiler-options/style identity
                   :line-numbers :arachne.sass.compiler-options/line-numbers identity
                   :load-path :arachne.sass.compiler-options/load-path vec
                   :plugin-path :arachne.sass.compiler-options/plugin-path vec
                   :source-map :arachne.sass.compiler-options/source-map identity
                   :omit-map-comment :arachne.sass.compiler-options/omit-map-comment identity
                   :precision :arachne.sass.compiler-options/precision identity
                   :sass :arachne.sass.compiler-options/sass identity))

(defdsl build
  "Define an Asset transducer component which builds SASSC.

  Arguments are:

  - compiler-options: A SASSC compiler options map. The only difference is that options which specify
                      paths (:output-to, :entrypoint, etc.) will relative to the asset fileset rather
                      than the process as a whole.

  Returns the entity ID of the newly-created component."

  (s/cat :compiler-opts ::compiler-options)
  [compiler-opts]
  (let [tid (cfg/tempid)
        entity (u/mkeep {:db/id tid
                         :arachne.component/constructor :arachne.assets.pipeline/transducer
                         :arachne.assets.transducer/constructor :arachne.sass.build/build-transducer
                         :arachne.sass.build/compiler-options (compiler-options (-> &args :compiler-opts second))})]
    (script/transact [entity] tid)))

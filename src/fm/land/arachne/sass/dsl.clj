(ns fm.land.arachne.sass.dsl
  "DSL code to handle SASSC compiler options"
  (:require [clojure.spec :as s]
            [arachne.error :as e :refer [deferror error]]
            [arachne.core.dsl :as core]
            [arachne.core.config :as cfg]
            [arachne.core.config.script :as script :refer [defdsl]]
            [arachne.core.util :as u]))

(s/def ::string (s/and string? #(not-empty %)))
(s/def ::path ::string)

(s/def ::style #{:nested :expanded :compact :compressed})
(s/def ::line-numbers boolean?)
(s/def ::load-path ::path)
(s/def ::plugin-path ::path)
(s/def ::source-map boolean?)
(s/def ::omit-map-comment boolean?)
(s/def ::precision number?)
(s/def ::sass boolean?)

(s/def ::compiler-options (s/keys :opt-un [::style
                                           ::line-numbers
                                           ::load-path
                                           ::plugin-path
                                           ::source-map
                                           ::omit-map-comment
                                           ::precision
                                           ::sass]))

(defdsl build
  "Define an Asset transducer component which builds SASSC.

  Arguments are:

  - arachne-id (optional): the Arachne ID of the component
  - compiler-options: A SASSC compiler options map. See the SASSC documentation
    for possible values. The only difference is that options which specify paths (:output-to, :output-dir,
    :preamble, :externs, etc.) will relative to the asset fileset rather than the process as a whole.

  Returns the entity ID of the newly-created component."

  (s/cat :arachne-id (s/? ::core/arachne-id) :compiler-opts ::compiler-options)
  [<arachne-id> compiler-opts]
  (let [tid (cfg/tempid)
        entity (u/mkeep {:db/id tid
                         :arachne/id (:arachne-id &args)
                         :arachne.component/constructor :arachne.assets.pipeline/transducer
                         :arachne.assets.transducer/constructor :fm.land.arachne.sass.build/build-transducer
                         :fm.land.arachne.sass.build/compiler-options (:compiler-opts &args)})]
    (script/transact [entity] tid)))

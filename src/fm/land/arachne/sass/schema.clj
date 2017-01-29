(ns fm.land.arachne.sass.schema
  (:require [arachne.core.config.model :as m]))

(def schema
  "Schema for the module"
  (concat

    (m/type :fm.land.arachne.sass/Build [:arachne.assets/Transducer]
      "A SASSC asset transducer"
      (m/attr :fm.land.arachne.sass.build/compiler-options :one :component :fm.land.arachne.sass/CompilerOptions
        "SASSC compiler options for this build"))

    (m/type :fm.land.arachne.sass/CompilerOptions []
      "Options for the SASSC compiler"

      (m/attr :fm.land.arachne.sass.compiler-options/style :one-or-none :keyword
              "Output style. Can be: :nested, :expanded, :compact, :compressed.")
      (m/attr :fm.land.arachne.sass.compiler-options/line-numbers :one-or-none :boolean
              "Emit comments showing original line numbers.")
      (m/attr :fm.land.arachne.sass.compiler-options/load-path :one-or-none :string
              "Set Sass import path.")
      (m/attr :fm.land.arachne.sass.compiler-options/plugin-path :one-or-none :string
              "Set path to autoload plugins.")
      (m/attr :fm.land.arachne.sass.compiler-options/source-map :one-or-none :boolean
              "Emit source map.")
      (m/attr :fm.land.arachne.sass.compiler-options/omit-map-comment :one-or-none :boolean
              "Omits the source map url comment.")
      (m/attr :fm.land.arachne.sass.compiler-options/precision :one-or-none :long
              "Set the precision for numbers.")
      (m/attr :fm.land.arachne.sass.compiler-options/sass :one-or-none :boolean
              "Treat input as indented syntax.")
      )))

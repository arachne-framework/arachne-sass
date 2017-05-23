(ns arachne.sass.schema
  (:require [arachne.core.config.model :as m]))

(def schema
  "Schema for the module"
  (concat

    (m/type :arachne.sass/Build [:arachne.assets/Transducer]
      "A SASSC asset transducer"
      (m/attr :arachne.sass.build/compiler-options :one :component :arachne.sass/CompilerOptions
        "SASSC compiler options for this build"))

    (m/type :arachne.sass/CompilerOptions []
      "Options for the SASSC compiler"

      (m/attr :arachne.sass.compiler-options/entrypoint :one :string
              "The entrypoint file for the stylesheet.")
      (m/attr :arachne.sass.compiler-options/output-to :one :string
              "The output CSS file")
      (m/attr :arachne.sass.compiler-options/style :one-or-none :keyword
              "Output style. Can be: :nested, :expanded, :compact, :compressed.")
      (m/attr :arachne.sass.compiler-options/source-comments :one-or-none :boolean
              "Emit comments showing original source line.")
      (m/attr :arachne.sass.compiler-options/load-path :many :string
              "Set Sass import path.")
      (m/attr :arachne.sass.compiler-options/plugin-path :many :string
              "Set path to autoload plugins.")
      (m/attr :arachne.sass.compiler-options/source-map :one-or-none :boolean
              "Emit source map.")
      (m/attr :arachne.sass.compiler-options/omit-map-comment :one-or-none :boolean
              "Omits the source map url comment.")
      (m/attr :arachne.sass.compiler-options/precision :one-or-none :long
              "Set the precision for numbers.")
      (m/attr :arachne.sass.compiler-options/sass :one-or-none :boolean
              "Treat input as indented syntax.")
      )))

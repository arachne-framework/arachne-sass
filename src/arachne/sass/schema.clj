(ns arachne.sass.schema
  (:require [arachne.core.config.model :as m]))

(def schema
  "Schema for the module"
  (concat

    (m/type :arachne.sass/Build [:arachne.assets/Transducer]
      "A ClojureScript asset transducer"
      (m/attr :arachne.sass.build/compiler-options :one :component :arachne.sass/CompilerOptions
        "ClojureScript compiler options for this build"))

    (m/type :arachne.sass/CompilerOptions []
      "Options for the ClojureScript compiler"

      (m/attr :arachne.sass.compiler-options/main :one-or-none :keyword
        "Specifies an entry point namespace. When combined with optimization level :none, :main will cause the compiler to emit a single JavaScript file that will import goog/base.js, the JavaScript file for the namespace, and emit the required goog.require statement. This permits leaving HTML markup identical between dev and production.")
      (m/attr :arachne.sass.compiler-options/asset-path :one-or-none :string
        "When using :main it is often necessary to control where the entry point script attempts to load scripts from due to the configuration of the web server. :asset-path is a relative URL path not a file system path. For example, if your output directory is :output-dir \"resources/public/js/compiled/out\" but your webserver is serving files from \"resources/public\" then you want the entry point script to load scripts from \"js/compiled/out\".")
      (m/attr :arachne.sass.compiler-options/output-to :one-or-none :string
        "The path to the JavaScript file that will be output.")
      (m/attr :arachne.sass.compiler-options/output-dir :one-or-none :string
        "Sets the output directory target for files emitted during compilation. Defaults to \"out\".")
      (m/attr :arachne.sass.compiler-options/source-map-timestamp :one-or-none :boolean
        "Add cache busting timestamps to source map urls. This is helpful for keeping source maps up to date when live reloading code.")
      (m/attr :arachne.sass.compiler-options/optimizations :one-or-none :keyword
        "The optimization level. May be :none, :whitespace, :simple, or :advanced. Only :none and :simple are supported for bootstrapped ClojureScript.\n\n:none is the recommended setting for development, while :advanced is the recommended setting for production, unless something prevents it (incompatible external library, bug, etc.).")
      (m/attr :arachne.sass.compiler-options/source-map :one-or-none :string
        "See https://github.com/clojure/clojurescript/wiki/Source-maps. Under optimizations :none the valid values are \"true\" and \"false\", with the default being \"true\". Under all other optimization settings must specify a path to where the source map will be written.")
      (m/attr :arachne.sass.compiler-options/verbose :one-or-none :boolean
        "Emit details and measurements from compiler activity.")
      (m/attr :arachne.sass.compiler-options/pretty-print :one-or-none :boolean
        "Determines whether the JavaScript output will be tabulated in a human-readable manner. Defaults to true.")
      (m/attr :arachne.sass.compiler-options/target :one-or-none :keyword
        "If targeting nodejs set to :nodejs. Takes no other options at the moment. The default (no :target specified) implies browsers are being targeted. Have a look here for more information on how to run your code in nodejs.\n\n:target :nodejs")
      (m/attr :arachne.sass.compiler-options/externs :many :string
        "Configure externs files for external libraries.")
      (m/attr :arachne.sass.compiler-options/preloads :many :keyword
        "Developing ClojureScript commonly requires development time only side effects such as enabling printing, logging, spec instrumentation, and connecting REPLs. :preloads permits loading such side effect boilerplate right after sass.core.")
      (m/attr :arachne.sass.compiler-options/source-map-path :one-or-none :string
        "Set the path to source files references in source maps to avoid further web server configuration. This option affects the sources entry of the emitted source map V3 JSON file.")
      (m/attr :arachne.sass.compiler-options/source-map-asset-path :one-or-none :string
        "Provides fine grained control over the sourceMappingURL comment that is appended to generated JavaScript files when source mapping is enabled.")
      (m/attr :arachne.sass.compiler-options/cache-analysis :one-or-none :boolean
        "Experimental. Cache compiler analysis to disk. This enables faster cold build and REPL start up times.\n\nFor REPLs, defaults to true. Otherwise, defaults to true if and only if :optimizations is :none.")
      (m/attr :arachne.sass.compiler-options/recompile-dependents :one-or-none :boolean
        "For correctness the ClojureScript compiler now always recompiles dependent namespaces when a parent namespace changes. This prevents corrupted builds and swallowed warnings. However this can impact compile times depending on the structure of the application. This option defaults to true.")
      (m/attr :arachne.sass.compiler-options/static-fns :one-or-none :boolean
        "Employs static dispatch to specific function arities in emitted JavaScript, as opposed to making use of the call construct. Defaults to false except under advanced optimizations. Useful to have set to false at REPL development to facilitate function redefinition, and useful to set to true for release for performance.\n\nThis setting does not apply to the standard library, which is always compiled with :static-fns implicitly set to true.")
      (m/attr :arachne.sass.compiler-options/load-tests :one-or-none :boolean
        "This flag will cause deftest from sass.test to be ignored if false.\n\nUseful for production if deftest has been used in the production classpath.\n\nDefault is true. Has the same effect as binding sass.analyzer/*load-tests*.")
      (m/attr :arachne.sass.compiler-options/elide-asserts :one-or-none :boolean
        "This flag will cause all (assert x ) calls to be removed during compilation, including implicit asserts associated with :pre and :post conditions. Useful for production. Default is always false even in advanced compilation. Does NOT specify goog.asserts.ENABLE_ASSERTS, which is different and used by the Closure library.")
      (m/attr :arachne.sass.compiler-options/pseudo-names :one-or-none :boolean
        "With :advanced mode optimizations, determines whether readable names are emitted. This can be useful when debugging issues in the optimized JavaScript and can aid in finding missing externs. Defaults to false.")
      (m/attr :arachne.sass.compiler-options/print-input-delimiter :one-or-none :boolean
        "Determines whether comments will be output in the JavaScript that can be used to determine the original source of the compiled code.\n\nDefaults to false.")
      (m/attr :arachne.sass.compiler-options/output-wrapper :one-or-none :boolean
        "Wrap the JavaScript output in (function(){...};)() to avoid clobbering globals. Defaults to false.")
      (m/attr :arachne.sass.compiler-options/libs :many :string
        "Adds dependencies on external js libraries, i.e. Google Closure-compatible javascript files with correct goog.provides() and goog.requires() calls. Note that files in these directories will be watched and a rebuild will occur if they are modified.\n\nPaths or filenames can be given. Relative paths are relative to the current working directory (usually project root).\n\nDefaults to the empty vector []")
      (m/attr :arachne.sass.compiler-options/preamble :many :string
        "Prepends the contents of the given files to each output file. Only valid with optimizations other than :none.\n\n")
      (m/attr :arachne.sass.compiler-options/hashbang :one-or-none :boolean
        "When using :target :nodejs the compiler will emit a shebang as the first line of the compiled source, making it executable. When your intention is to build a node.js module, instead of executable, use this option to remove the shebang.")
      (m/attr :arachne.sass.compiler-options/compiler-stats :one-or-none :boolean
        "Report basic timing measurements on compiler activity.\n\nDefaults to false.")
      (m/attr :arachne.sass.compiler-options/language-in :one-or-none :keyword
        "Configure the input languages for the closure library. May be :ecmascript3, :ecmascript5, :ecmascript5-strict, :ecmascript6-typed, :ecmascript6-strict, :ecmascript6 or :no-transpile.\n\nDefaults to :ecmascript3")
      (m/attr :arachne.sass.compiler-options/language-out :one-or-none :keyword
        "Configure the output languages for the closure library. May be :ecmascript3, :ecmascript5, :ecmascript5-strict, :ecmascript6-typed, :ecmascript6-strict, :ecmascript6 or :no-transpile.\n\nDefaults to :ecmascript3")
      (m/attr :arachne.sass.compiler-options/closure-extra-annotations :many :string
        "Define extra JSDoc annotations that a closure library might use so that they don't trigger compiler warnings.")
      (m/attr :arachne.sass.compiler-options/anon-fn-naming-policy :one-or-none :keyword
        "Strategies for how the Google Closure compiler does naming of anonymous functions that occur as r-values in assignments and variable declarations. Defaults to :off. The following values are supported:\n\n:off Don't give anonymous functions names.\n:unmapped Generates names that are based on the left-hand side of the assignment. Runs after variable and property renaming, so that the generated names will be short and obfuscated.\n:mapped Generates short unique names and provides a mapping from them back to a more meaningful name that's based on the left-hand side of the assignment.")
      (m/attr :arachne.sass.compiler-options/optimize-constants :one-or-none :boolean
        "When set to true, constants, such as keywords and symbols, will only be created once and will be written to a separate file called constants_table.js. The compiler will emit a reference to the constant as defined in the constants table instead of creating a new object for it. This option is mainly intended to be used for a release build since it can increase performance due to decreased allocation. Defaults to true under :advanced optimizations otherwise to false.")
      (m/attr :arachne.sass.compiler-options/parallel-build :one-or-none :boolean
        "When set to true, compile source in parallel, utilizing multiple cores.")
      (m/attr :arachne.sass.compiler-options/foreign-libs :many :component :arachne.sass/ForeignLibrary
        "Adds dependencies on foreign libraries.")
      (m/attr :arachne.sass.compiler-options/modules :many :component :arachne.sass/ClosureModule
        "A new option for emitting Google Closure Modules. See the ClojureScript documentation for details.")
      (m/attr :arachne.sass.compiler-options/common-warnings? :one-or-none :boolean
        "When set to true, enable common warnings. Has no effect if :arachne.sass.compiler-options/warnings are present.")
      (m/attr :arachne.sass.compiler-options/warnings :many :component :arachne.sass/Warning
        "Warnings which are enabled/disabled for this build. See the ClojureScript documentation for possible warning types.")
      (m/attr :arachne.sass.compiler-options/closure-warnings :many :component :arachne.sass/ClosureWarning
        "Closure Warning definitions for this build. See the Closure documentation for possible warning types.")
      (m/attr :arachne.sass.compiler-options/closure-defines :many :component :arachne.sass/ClosureDefine
        "Set the values of Closure libraries' variables annotated with @define or with the sass.core/goog-define helper macro."))

    (m/type :arachne.sass/ClosureDefine []
      "A warning option for the Google Closure compiler"

      (m/attr :arachne.sass.closure-define/variable :one :string
        "The name of the variable to be annotated")
      (m/attr :arachne.sass.closure-define/annotate :one :boolean
        "Should the variable be annotated with @define?"))

    (m/type :arachne.sass/ClosureWarning []
      "A warning option for the Google Closure compiler"

      (m/attr :arachne.sass.closure-warning/type :one :keyword
        "The type of the warning")
      (m/attr :arachne.sass.closure-warning/value :one :keyword
        "How to handle warnings of this type. Only :error, :warning and :off are supported."))

    (m/type :arachne.sass/Warning []
      "A warning flag for the ClojureScript compiler"

      (m/attr :arachne.sass.warning/type :one :keyword
        "The type of the warning")
      (m/attr :arachne.sass.warning/enabled :one :boolean
        "Is the warning enabed or disabled?"))

    (m/type :arachne.sass/ClosureModule []
      "Options to define a Google Closure module"

      (m/attr :arachne.sass.closure-module/id :one :keyword
        "Identifier for the module")
      (m/attr :arachne.sass.closure-module/output-to :one :string
        "JS file to which the module will be written.")
      (m/attr :arachne.sass.closure-module/entries :many :string
        "Namespaces that are a part of this module")
      (m/attr :arachne.sass.closure-module/depends-on :many :keyword
        "Identifiers for Closure modules that this module depends upon"))

    (m/type :arachne.sass/ForeignLibrary []
      "Foreign Library definition for SASS compiler options"

      (m/attr :arachne.sass.foreign-library/file :one :string
        "Indicates the URL to the library. Must return a 200 status code")
      (m/attr :arachne.sass.foreign-library/file-min :one-or-none :string
        "(Optional) Indicates the URL to the minified variant of the library.")
      (m/attr :arachne.sass.foreign-library/provides :one-or-more :string
        ":provides A synthetic namespace that is associated with the library. If the library exposes its functionality via globals, say by assigning a new field to window, you still access it the same way (e.g. js/Moment), but you must also require it ((require [my.example])) somewhere in your code to keep it from being pruned away during optimization. This is typically a vector with a single string, but it has the capability of specifying multiple namespaces (typically used only by Google Closure libraries).")
      (m/attr :arachne.sass.foreign-library/requires :many :string
        "(Optional) A vector explicitly identifying dependencies (:provides values from other foreign libs); used to form a topological sort honoring dependencies.")
      (m/attr :arachne.sass.foreign-library/module-type :one-or-none :keyword
        ":module-type (Optional) indicates that the foreign lib uses a given module system. Can be one of :commonjs, :amd, :es6. Note that if supplied, :requires is not used (as it is implicitly determined). ")
      (m/attr :arachne.sass.foreign-library/preproccess :one-or-none :keyword
        "(Optional) Used to preprocess / transform code in other dialects (JSX, etc.). A defmethod for sass.clojure/js-transforms must be provided that matches the supplied value in order to effect the desired code transformation."))

    ;; Not supported
    ;:watch-fn
      ))

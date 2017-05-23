(ns arachne.sass.build
  (:require
   [clojure.spec.alpha :as s]
   [arachne.sass.schema :as schema]
   [arachne.error :as e :refer [deferror error]]
   [arachne.core.config :as cfg]
   [arachne.core.util :as u]
   [arachne.assets.pipeline :as p]
   [arachne.fileset :as fs]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as string]
   [arachne.log :as log]
   [clojure.data.json :as json])
  (:import
   [io.bit3.jsass Options Output OutputStyle CompilationException]))

(defn- append-path
  "Append the given path suffix to a base path (a File)."
  [base suffix]
  (let [suffix (or suffix "")
        suffix (if (= "/" suffix) "" suffix)]
    (.getCanonicalPath (io/file base suffix))))

(deferror ::sassc-error
  :message "Error compiling SASS/SCSS (:file::line): :msg"
  :explanation "There was an error compiling your SASS/SCSS.\n:original-output"
  :ex-data-docs {:msg  "The error message from sassc"
                 :original-output  "The original error output from sassc"
                 :file "The file where the error occurred"
                 :line "The line number where the error occurred"})

(deferror ::jsass-error
  :message "Error compiling SASS/SCSS (:file::line): :message"
  :explanation "There was an error compiling your SASS/SCSS.\n::formatted"
  :ex-data-docs {:message         "The error message from jsass"
                 :status          "The status of the error"
                 :formatted       "The full error message formatted"
                 :file            "The file where the error occurred"
                 :line            "The line number where the error occurred"
                 :column          "The column number where the error occurred"})

(defn- parse-sassc-error-output
  "Parses the output of the sassc compiler to find the line number/file where the error is"
  [output]
  (let [error-message (or (-> (re-find #"(?m)^Error: (.*)$" output)
                              second)
                          "")
        error-loc     (re-find #"(?m)\s*on line (\d+) of (.*)$" output)
        error-line    (or (second error-loc) "")
        error-file    (or (nth error-loc 2) "")]
    {:msg  error-message
     :original-output output
     :line error-line
     :file error-file}))

(defn- exec!
  "Run a shell command (as per sh/sh), throwing with an error message if it did
  not return zero"
  [& args]
  (let [result (apply sh/sh args)]
    (when-not (zero? (:exit result))
      (let [parsed-output (parse-sassc-error-output (:err result))]
        (error ::sassc-error parsed-output)))
    result))

(defmulti flag
  "Returns a collection of arguments (as strings) for the given compiler option to pass into sassc"
  (fn [key value src-dir dest-dir options-map] key))
(defmethod flag :default
  [key value src-dir dest-dir options-map]
  [(str "--" (name key)) (str value)])
(defmethod flag :arachne.sass.compiler-options/load-path
  [key value src-dir dest-dir options-map]
  ["--load-path" (string/join ":"
                              (map #(append-path src-dir %) value))])
(defmethod flag :arachne.sass.compiler-options/plugin-path
  [key value src-dir dest-dir options-map]
  ["--plugin-path" (string/join ":"
                              (map #(append-path src-dir %) value))])
(defmethod flag :arachne.sass.compiler-options/source-map
  [key value src-dir dest-dir options-map]
  (if value
    ["--sourcemap"]
    []))
(defmethod flag :arachne.sass.compiler-options/omit-map-comment
  [key value src-dir dest-dir options-map]
  (if value
    ["--omit-map-comment"]
    []))
(defmethod flag :arachne.sass.compiler-options/sass
  [key value src-dir dest-dir options-map]
  (if value
    ["--sass"]
    []))

(defn- sassc-flags
  "Convert options from the config db to an array of strings to pass as arguments to the sassc program"
  [src-dir dest-dir options-map]
  (mapcat (fn [[key value]] (flag key value src-dir dest-dir options-map))
          (select-keys options-map
                       [:arachne.sass.compiler-options/style
                        :arachne.sass.compiler-options/source-comments
                        :arachne.sass.compiler-options/load-path
                        :arachne.sass.compiler-options/plugin-path
                        :arachne.sass.compiler-options/source-map
                        :arachne.sass.compiler-options/omit-map-comment
                        :arachne.sass.compiler-options/precision])))

(defn- sassc-options [src-dir dest-dir options-map]
  (let [in (:arachne.sass.compiler-options/entrypoint options-map)
        out (io/file (:arachne.sass.compiler-options/output-to options-map))]
    [(str (append-path src-dir in))
     (str (append-path dest-dir out))]))

(defn- sassc
  "Runs SASSC on the input directory with the given options"
  [src-dir dest-dir options]
  (let [result (apply exec! "sassc"
                      (concat (sassc-flags src-dir dest-dir options)
                              (sassc-options src-dir dest-dir options)))]
    result))

(defn- jsass
  "Compiles the source files with jsass"
  [^java.io.File src-dir ^java.io.File dest-dir options]
  (let [entrypoint (:arachne.sass.compiler-options/entrypoint options)
        load-path (:arachne.sass.compiler-options/load-path options)
        source-map (:arachne.sass.compiler-options/source-map options)
        source-comments (:arachne.sass.compiler-options/source-comments options)
        omit-map-comment (:arachne.sass.compiler-options/omit-map-comment options)
        output-to (:arachne.sass.compiler-options/output-to options)
        precision (:arachne.sass.compiler-options/precision options)
        sass (:arachne.sass.compiler-options/sass options)
        sass (if (nil? sass)
               (string/ends-with? entrypoint ".sass")
               sass)
        style (case (:arachne.sass.compiler-options/style options)
                :nested (OutputStyle/NESTED)
                :expanded (OutputStyle/EXPANDED)
                :compact (OutputStyle/COMPACT)
                :compressed (OutputStyle/COMPRESSED)
                nil)

        entrypoint-dir (.getParentFile (io/file entrypoint))
        entrypoint-path (apply io/file (filter identity [src-dir entrypoint-dir]))
        output-name (.getName (io/file output-to))
        source-map-to (io/file (str output-to ".map"))
        source-map-name (.getName source-map-to)

        ;; The locations we will read/write the input/output files
        input-file (io/file src-dir entrypoint)
        output-file (io/file dest-dir output-to)
        source-map-file (io/file dest-dir source-map-to)

        ;; JSASS options
        ;; All paths passed to jsass are relative to srd-dir because we want
        ;; source map to be correctly generated. jsass does not actually write
        ;; the output files for us, the input/output parameters are merely used
        ;; for generating relative paths etc in the source map
        jsass-source-map-root (.toURI entrypoint-path)
        jsass-source-map-file (.toURI (apply io/file (filter identity [entrypoint-path source-map-name])))
        jsass-include-paths (into [(io/file entrypoint-path)] (map #(io/file src-dir %) load-path))
        jsass-input-uri (.toURI (io/file src-dir entrypoint))
        jsass-output-uri (.toURI (io/file entrypoint-path output-name))

        jsass-options (io.bit3.jsass.Options.)
        jsass-compiler (io.bit3.jsass.Compiler.)]
    (try
      (.. jsass-options getIncludePaths (addAll jsass-include-paths))
      (.. jsass-options (setIsIndentedSyntaxSrc (boolean sass)))
      (when-not (nil? style)
        (.. jsass-options (setOutputStyle style)))
      (when-not (nil? precision)
        (.. jsass-options (setPrecision precision)))
      (when-not (nil? omit-map-comment)
        (.. jsass-options (setOmitSourceMapUrl (boolean omit-map-comment))))
      (when source-map
        (.. jsass-options (setSourceMapFile jsass-source-map-file))
        (.. jsass-options (setSourceMapRoot jsass-source-map-root))
        (.. jsass-options (setSourceMapContents true))
        (when-not (nil? source-comments)
          (.. jsass-options (setSourceComments (boolean source-comments)))))
      (let [entrypoint-contents (slurp input-file)
            output (.compileString jsass-compiler
                                   entrypoint-contents
                                   jsass-input-uri
                                   jsass-output-uri
                                   jsass-options)]
        (spit output-file (.getCss output))
        (spit source-map-file (.getSourceMap output)))
      (catch CompilationException e
        (error ::jsass-error (json/read-str (.getErrorJson e)))))))

(defn build-transducer
  "Return a transducer over filesets that builds SASS/SCSS files"
  [component]
  (let [options-entity (:arachne.sass.build/compiler-options component)
        out-dir (fs/tmpdir!)
        build-id (:arachne/id component)]
    (map (fn [input-fs]
           (let [src-dir (fs/tmpdir!)]
             (fs/commit! input-fs src-dir)
             (log/info :msg "Building SCSS/SASS" :build-id build-id)
             (let [started (System/currentTimeMillis)]
               ;; Create the output-dir if it doesn't exist
               (when-let [output-to-dir (.getParentFile (io/file (:arachne.sass.compiler-options/output-to options-entity)))]
                 (-> (append-path out-dir output-to-dir) io/file .mkdirs))
               (jsass src-dir out-dir options-entity)
               (let [elapsed (- (System/currentTimeMillis) started)
                     elapsed-seconds (float (/ elapsed 1000))]
                 (log/info :msg (format "SCSS/SASS build complete in %.2f seconds" elapsed-seconds)
                           :build-id build-id
                           :elapsed-ms elapsed)))
             (fs/add (fs/empty input-fs) out-dir))))))

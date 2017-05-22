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
   [arachne.log :as log]))

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
                        :arachne.sass.compiler-options/line-numbers
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

(defn extract [opts]
  (u/map-transform opts {}
                   :arachne.sass.compiler-options/entrypoint :entrypoint identity
                   :arachne.sass.compiler-options/output-to :output-to identity
                   :arachne.sass.compiler-options/style :style identity
                   :arachne.sass.compiler-options/line-numbers :line-numbers identity
                   :arachne.sass.compiler-options/load-path :load-path identity
                   :arachne.sass.compiler-options/plugin-path :plugin-path identity
                   :arachne.sass.compiler-options/source-map :source-map identity
                   :arachne.sass.compiler-options/omit-map-comment :omit-map-comment identity
                   :arachne.sass.compiler-options/precision :precision identity
                   :arachne.sass.compiler-options/sass :sass identity))

(defn build-transducer
  "Return a transducer over filesets that builds SASSC files"
  [component]
  (let [options-entity (:arachne.sass.build/compiler-options component)
        out-dir (fs/tmpdir!)
        build-id (:arachne/id component)]
    (map (fn [input-fs]
           (let [src-dir (fs/tmpdir!)]
             (fs/commit! input-fs src-dir)
             (log/info :msg "Building SASSC" :build-id build-id)
             (let [started (System/currentTimeMillis)]
               ;; Create the output-dir if it doesn't exist
               (when-let [output-to-dir (-> (io/file (:arachne.sass.compiler-options/output-to options-entity))
                                         (.getParentFile))]
                 (-> (append-path out-dir output-to-dir) io/file .mkdirs))
               (sassc src-dir out-dir options-entity)
               (let [elapsed (- (System/currentTimeMillis) started)
                     elapsed-seconds (float (/ elapsed 1000))]
                 (log/info :msg (format "SASSC build complete in %.2f seconds" elapsed-seconds)
                           :build-id build-id
                           :elapsed-ms elapsed)))
             (fs/add (fs/empty input-fs) out-dir))))))

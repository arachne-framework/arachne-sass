(ns fm.land.arachne.sass.build
  (:require
    [clojure.spec :as s]
    [fm.land.arachne.sass.schema :as schema]
    [arachne.error :as e :refer [deferror error]]
    [arachne.core.config :as cfg]
    [arachne.core.util :as u]
    [arachne.assets.pipeline :as p]
    [arachne.fileset :as fs]
    [clojure.java.io :as io]
    [arachne.log :as log]))

(defn- append-path
  "Apped the given path suffix to a base path (a File)."
  [base suffix]
  (let [suffix (or suffix "")
        suffix (if (= "/" suffix) "" suffix)]
    (.getCanonicalPath (io/file base suffix))))

(defn build-transducer
  "Return a transducer over filesets that builds SASSC files"
  [component]
  (let [options-entity (:fm.land.arachne.sass.build/compiler-options component)
        out-dir (fs/tmpdir!)
        build-id (:arachne/id component)]
    (map (fn [input-fs]
           (let [src-dir (fs/tmpdir!)]
             (fs/commit! input-fs src-dir)
             (log/info :msg "Building SASSC" :build-id build-id)
             (let [started (System/currentTimeMillis)
                   sass-opts (compiler-options options-entity out-dir)]
               (Thread/sleep 1000)
               (let [elapsed (- (System/currentTimeMillis) started)
                     elapsed-seconds (float (/ elapsed 1000))]
                 (log/info :msg (format "SASSC build complete in %.2f seconds" elapsed-seconds)
                           :build-id build-id
                           :elapsed-ms elapsed)))
             (fs/add (fs/empty input-fs) out-dir))))))

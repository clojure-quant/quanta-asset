(ns quanta.asset.seed
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [quanta.asset.datahike :as asset-db]))

(defn- read-edn-file [filename]
  (-> filename io/file slurp edn/read-string))

(defn seed-edn-assets-fn
  "Returns a seed fn that loads assets from an EDN file."
  [edn-filename]
  (fn [conn]
    (doseq [asset (read-edn-file edn-filename)]
      (println "seeding asset" (:asset/symbol asset))
      (asset-db/add-update-asset conn asset))))

(defn seed-edn-lists-fn
  "Returns a seed fn that loads quote-lists from EDN files in a directory."
  [lists-dir]
  (fn [conn]
    (doseq [^java.io.File f (-> lists-dir io/file .listFiles sort)
            :when (and f (.endsWith (.getName f) ".edn"))]
      (let [list-data (read-edn-file f)]
        (println "seeding list" (:lists/name list-data))
        (asset-db/add-update-list conn list-data)))))

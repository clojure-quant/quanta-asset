(ns demo.print
  (:require
   [quanta.asset.seed :as seed]
   [quanta.asset.datahike :as asset-db]
   [quanta.asset.schema :refer [schema]]
   [quanta.util.datahike :as datahike]))

(defn print-demo!
  "Start the asset db, print seeded assets and lists, then stop."
  [_]
  (let [db-path "asset-db"
        conn (datahike/db-start
              {:schema schema
               :db-path db-path
               :seed-fn [(seed/seed-edn-assets-fn "demo-assets.edn")
                         (seed/seed-edn-lists-fn "demo-lists")]})]
    (try
      (println "\n=== Assets ===")
      (doseq [asset (asset-db/query-assets conn {})]
        (println (:asset/symbol asset)
                 "-"
                 (:asset/name asset)
                 (select-keys asset [:asset/category :asset/exchange])))
      (println "\n=== Quote lists ===")
      (doseq [list-name (asset-db/list-names conn)
              :let [list (asset-db/get-list conn list-name)]]
        (println (:lists/name list) ":" (:lists/asset list)))
      (finally
        (datahike/db-stop conn)))))

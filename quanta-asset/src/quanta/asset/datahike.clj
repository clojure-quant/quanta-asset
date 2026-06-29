(ns quanta.asset.datahike
  (:require
   [taoensso.timbre :as timbre :refer [info]]
   [datahike.api :as d]))

(defn stop [conn]
  (when conn
    (info "disconnecting from datahike..")
    (d/release conn)
    (info "datahike stopped!")))

(defn add-asset-details [dbconn asset]
  (d/transact dbconn [(merge {:db/id [:asset/symbol (:asset/symbol asset)]}
                             asset)]))

(defn- conj-key-when [query k v]
  (if v
    (conj query `[~'?id ~k ~v])
    query))

(defn- conj-q-when [query q]
  (if q
    (-> query
        (conj '[?id :asset/name ?an]
              (conj `(~'or-join [~'?an ~'?as]
                                [(re-find ~'qr? ~'?an)]
                                [(re-find ~'qr? ~'?as)]))))
    query))

(defn query-assets [dbconn {:keys [q exchange category]}]
  (let [qr? (when q (re-pattern (str "(?i)" q)))]
    (-> '[:find [(pull ?id [*]) ...]
          :in $ qr?
          :where
          [?id :asset/symbol ?as]]
        (conj-key-when :asset/category category)
        (conj-key-when :asset/exchange exchange)
        (conj-q-when q)
        (d/q @dbconn qr?))))

(defn add-update-asset
  "[{:asset/symbol \"SPY\"
                  :asset/name \"Spiders S&P 500 ETF\"
                  :asset/exchange \"NYSE\"
                  :asset/category :etf}]"
  [dbconn asset]
  (if (map? asset)
    (d/transact dbconn [asset])
    (d/transact dbconn asset)))

(defn get-asset [dbconn asset-symbol]
  (-> '[:find [(pull ?id [*]) ...]
        :in $ ?asset-symbol
        :where
        [?id :asset/symbol ?asset-symbol]]
      (d/q @dbconn asset-symbol)
      first))

(defn get-asset-market [dbconn asset-symbol]
  (if-let [asset (get-asset dbconn asset-symbol)]
    (or (:asset/market asset) :us) ; todo: make sure this is not needed, by adding all recipies to have :market
    nil))

;; provider symbol mappings

(defn asset->provider [dbconn provider asset]
  (let [provider-k (keyword (symbol "asset" (name provider)))]
    (-> `[:find [(~'pull ~'?id [~provider-k]) ...]
          :in ~'$ ~'?asset-symbol
          :where
          [~'?id :asset/symbol ~'?asset-symbol]]
        (d/q @dbconn asset)
        first
        (get provider-k))))

(defn provider->asset [dbconn provider asset-provider]
  (let [provider-k (keyword (symbol "asset" (name provider)))]
    (-> `[:find [(~'pull ~'?id [:asset/symbol]) ...]
          :in ~'$ ~'?asset-provider
          :where
          [~'?id ~provider-k  ~'?asset-provider]]
        (d/q @dbconn asset-provider)
        first
        :asset/symbol)))

;; summary

(defn exchanges [dbconn]
  (let [query-exchanges
        '[:find [(pull ?id [:asset/exchange]) ...]
          :in $
          :where
          [?id :asset/exchange _]]]
    (->> (d/q  query-exchanges @dbconn)
         (map :asset/exchange)
         (into #{}))))

(defn categories [dbconn]
  (let [query '[:find [(pull ?id [:asset/category]) ...]
                :in $
                :where
                [?id :asset/exchange _]]]
    (->> (d/q  query @dbconn)
         (map :asset/category)
         (into #{}))))

;; LISTS

(defn tupelize-list [data]
  (update data :lists/asset #(into []
                                   (map-indexed (fn [idx asset]
                                                  [idx asset]) %))))

(defn untupelize-list [data]
  (update data :lists/asset #(into []
                                   (map (fn [[_idx asset]]
                                          asset) %))))

(defn- list-id [dbconn list-name]
  (d/q '[:find ?id .
         :in $ ?list-name
         :where
         [?id :lists/name ?list-name]]
       @dbconn list-name))

(defn add-update-list
  "Upserts a list by `:lists/name` and **replaces** `:lists/asset`."
  [dbconn data]
  (let [id (list-id dbconn (:lists/name data))
        tx (concat
            (if id
              [[:db/retractEntity id]]
              [])
            [(tupelize-list data)])]
    (d/transact dbconn (vec tx))))

(defn get-list [dbconn list-name]
  (-> '[:find [(pull ?id [:lists/name
                          [:lists/asset :limit nil]]) ...]
        :in $ ?list-name
        :where
        [?id :lists/name ?list-name]]
      (d/q @dbconn list-name)
      first
      untupelize-list))

(defn list-names [dbconn]
  (->> (d/q '[:find [?name ...]
              :where
              [?id :lists/name ?name]]
            @dbconn)
       sort))

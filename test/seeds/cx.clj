(ns seeds.cx
  (:require [crux.api :as x]
            [joplin.crux.database :as d]))

(defn run [target & args]
  (when-let [node (d/get-node (:db :conf target))]
    (let [txs [[:crux.tx/put {:crux.db/id (java.util.UUID/randomUUID)
                              :hamster/name "Farley Moat"
                              :hamster/age 12}]
               [:crux.tx/put {:crux.db/id (java.util.UUID/randomUUID)
                              :hamster/name "Barley Goat"
                              :hamster/age 3}]
               [:crux.tx/put {:crux.db/id (java.util.UUID/randomUUID)
                              :hamster/name "Gnarly Rote"
                              :hamster/age 99}]]
          tx (->> txs
                  (x/submit-tx node)
                  (x/await-tx node))]
      (when-not (x/tx-committed? node tx)
        (throw (Exception. (format "Seed '%s' failed to apply." (ns-name *ns*))))))))

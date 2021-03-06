(ns joplin.crux.database-test
  (:require [clojure.test :refer :all]
            [crux.api :as x]
            [joplin.crux.database :as sut]
            [joplin.alias :refer [*load-config*]]
            [joplin.repl :as repl]
            [seeds.cx]))

(def config (*load-config* "joplin-cx.edn"))

(defn crux-conf []
  (-> config :databases :cx-dev :conf))

(defn destroy-node []
  (reset! sut/crux-node nil)
  (sut/get-node (crux-conf)))

(defn query-migrations []
  (x/q (x/db (sut/get-node (crux-conf)))
       '{:find [id]
         :where [[e :migrations/id id]]}))

(deftest adding-migrations
  (testing "adds one migration"
    (repl/migrate config :dev)
    (is (= 1 (-> (query-migrations)
                 (count))))))

(deftest removing-migrations
  (testing "removes one migration from 'now'"
    (repl/migrate config :dev)
    (repl/rollback config :dev :cx-dev 1)
    (is (= 0 (-> (query-migrations)
                 (count)))))

  (testing "removing a migration does NOT remove it from valid-time history"
    (repl/migrate config :dev)
    (let [between (java.util.Date.)
          _ (Thread/sleep 500)]
      (is (= 1 (-> (query-migrations)
                   (count))))
      (repl/rollback config :dev :cx-dev 1)
      (is (= 1 (-> (x/q (x/db (sut/get-node (crux-conf)) between)
                        '{:find [e]
                          :where [[e :crux.db/id "20210302000000-test"]]})
                   (count)))))))

(deftest seeding
  (testing "adds seed data"
    (destroy-node)
    (repl/migrate config :dev)
    (repl/seed config :dev)
    (is (= 3 (-> (x/q (x/db (sut/get-node (crux-conf)))
                      '{:find [e]
                        :where [[e :hamster/name n]]})
                 (count))))))

;; (deftest resetting
;;   (repl/reset config :dev :cx-dev))

;; TODO:
;; (repl/seed)
;; (repl/reset)
;; (repl/pending)
;; (repl/create)
;; - use transaction fns
;; - extract a submit+await+entity+exception fn

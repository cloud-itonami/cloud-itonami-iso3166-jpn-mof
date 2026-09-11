(ns mofcompliance.store-contract-test
  "MemStore ≡ DatomicStore parity for the Store protocol."
  (:require [clojure.test :refer [deftest is testing]]
            [mofcompliance.store :as store]
            [mofcompliance.registry :as registry]))

(defn- exercise [s]
  (store/commit-record! s {:effect :engagement/upsert
                           :value {:id "eng-x" :operator "X KK"
                                   :base-fee 100 :monthly-rate 10 :monitoring-months 1
                                   :audit-export? false :export-fee nil :claimed-fee 110.0
                                   :unified-qualification-verified? true
                                   :tender-method-classified? true
                                   :foreign-investor? false
                                   :requires-fefta-screening? false :fefta-screening-verified? false
                                   :requires-customs-clearance? false :customs-clearance-verified? false
                                   :drafted? false :submitted? false
                                   :status :intake}})
  (store/commit-record! s {:effect :assessment/set
                           :path ["eng-x" :unified-qualification]
                           :payload {:track :unified-qualification :checklist ["a"] :spec-basis "x"}})
  (store/commit-record! s {:effect :engagement/mark-drafted :path ["eng-x" registry/compliance-track]})
  (store/commit-record! s {:effect :engagement/mark-submitted :path ["eng-x" registry/compliance-track]})
  (store/append-ledger! s {:t :committed :op :test})
  {:engagement (store/engagement s "eng-x")
   :assessment (store/assessment-of s "eng-x" :unified-qualification)
   :drafts (store/draft-history s)
   :submits (store/submit-history s)
   :ledger (store/ledger s)
   :drafted? (store/engagement-drafted? s "eng-x")
   :submitted? (store/engagement-submitted? s "eng-x")})

(deftest mem-and-datomic-parity
  (let [mem* (store/->MemStore (atom {:engagements {} :assessments {} :ledger []
                                      :draft-sequences {} :draft-records []
                                      :submit-sequences {} :submit-records []}))
        dat* (store/datomic-store {})
        m (exercise mem*)
        d (exercise dat*)]
    (is (= (:operator (:engagement m)) (:operator (:engagement d))))
    (is (true? (:drafted? m)) (true? (:drafted? d)))
    (is (true? (:submitted? m)) (true? (:submitted? d)))
    (is (= 1 (count (:drafts m))) (= 1 (count (:drafts d))))
    (is (= 1 (count (:submits m))) (= 1 (count (:submits d))))
    (is (= 1 (count (:ledger m))) (= 1 (count (:ledger d))))
    (is (= (:assessment m) (:assessment d)))))

(deftest seed-db-has-seven-engagements
  (testing "MemStore and DatomicStore demo seeds agree on the demo-data shape"
    (is (= 7 (count (store/all-engagements (store/seed-db)))))
    (is (= 7 (count (store/all-engagements (store/datomic-seed-db)))))))

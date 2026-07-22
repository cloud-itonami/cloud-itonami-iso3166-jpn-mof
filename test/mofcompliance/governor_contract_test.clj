(ns mofcompliance.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls implemented faithfully, and the integration test
  running the compiled StateGraph end-to-end. The single invariant
  under test:

    ProcurementCompliance-LLM never drafts or submits a compliance
    package the MOF Procurement Compliance Governor would reject,
    `:filing/draft`/`:filing/submit` NEVER auto-commit at any phase,
    `:engagement/intake` MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [mofcompliance.registry :as registry]
            [mofcompliance.store :as store]
            [mofcompliance.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :mof-compliance-operator :phase 3})
(def track registry/compliance-track)

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  [actor tid-prefix subject catalog-track]
  (exec-op actor (str tid-prefix "-assess-" (name catalog-track))
           {:op :compliance/assess :subject subject :track catalog-track} operator)
  (approve! actor (str tid-prefix "-assess-" (name catalog-track))))

(defn- assess-baseline!
  "unified-qualification + tender-method are ALWAYS applicable."
  [actor tid-prefix subject]
  (assess! actor tid-prefix subject :unified-qualification)
  (assess! actor tid-prefix subject :tender-method))

(defn- draft!
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-draft") {:op :filing/draft :subject subject :track track} operator)
  (approve! actor (str tid-prefix "-draft")))

(deftest clean-intake-auto-commits
  (testing "integration: engagement/intake at phase 3 auto-commits through the full compiled graph"
    (let [[db actor] (fresh)
          res (exec-op actor "t1"
                    {:op :engagement/intake :subject "eng-1"
                     :patch {:id "eng-1" :operator "Kita Procurement Advisory KK"}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "Kita Procurement Advisory KK" (:operator (store/engagement db "eng-1"))) "SSoT actually updated")
      (is (= 1 (count (store/ledger db)))))))

(deftest compliance-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :unified-qualification} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "eng-1" :unified-qualification)))))))

(deftest fabricated-track-is-held
  (testing "a compliance/assess proposal with no official spec-basis -> HOLD"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :compliance/assess :subject "eng-1" :track :unified-qualification :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-1" :unified-qualification)) "no assessment written"))))

(deftest corporate-number-misattribution-is-held-and-unoverridable
  (testing "treating the Corporate Number as MOF business registration -> HARD hold (flagship fabrication-trap defense)"
    (let [[db actor] (fresh)
          res (exec-op actor "t3b"
                    {:op :compliance/assess :subject "eng-2" :track :corporate-number-boundary :misattribute? true} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:corporate-number-misattribution} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-2" :corporate-number-boundary)) "no assessment written"))))

(deftest draft-without-assessment-is-held
  (testing "filing/draft before any compliance assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest unified-qualification-missing-is-held-and-unoverridable
  (testing "missing 全省庁統一資格 (unified qualification) -> HARD hold (unconditional)"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t5pre" "eng-4")
          _ (draft! actor "t5pre" "eng-4")
          res (exec-op actor "t5" {:op :filing/submit :subject "eng-4" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:unified-qualification-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest tender-method-undetermined-is-held-and-unoverridable
  (testing "undetermined 会計法/予算決算及び会計令 tender-method classification -> HARD hold (unconditional)"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t6pre" "eng-5")
          _ (draft! actor "t6pre" "eng-5")
          res (exec-op actor "t6" {:op :filing/submit :subject "eng-5" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:tender-method-undetermined} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest fefta-screening-missing-is-held-for-foreign-investor
  (testing "missing FEFTA prior-notification screening -> HARD hold, CONDITIONAL on :requires-fefta-screening?"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t7pre" "eng-6")
          _ (assess! actor "t7pre" "eng-6" :fefta-notification)
          _ (draft! actor "t7pre" "eng-6")
          res (exec-op actor "t7" {:op :filing/submit :subject "eng-6" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:fefta-screening-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest fefta-check-is-a-noop-for-domestic-engagement
  (testing "eng-1 (domestic, :requires-fefta-screening? false) never triggers the FEFTA gate"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t7bpre" "eng-1")
          _ (draft! actor "t7bpre" "eng-1")
          res (exec-op actor "t7b" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status res)) "clean submit still escalates for human approval, but is NOT held")
      (is (not (some #{:fefta-screening-missing} (mapcat :basis (store/ledger db))))))))

(deftest customs-clearance-missing-is-held-for-cross-border-goods
  (testing "missing customs/tariff import clearance -> HARD hold, CONDITIONAL on :requires-customs-clearance?"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t8pre" "eng-7")
          _ (assess! actor "t8pre" "eng-7" :customs-declaration)
          _ (draft! actor "t8pre" "eng-7")
          res (exec-op actor "t8" {:op :filing/submit :subject "eng-7" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:customs-clearance-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest customs-check-is-a-noop-for-services-only-engagement
  (testing "eng-1 (:requires-customs-clearance? false) never triggers the customs gate"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t8bpre" "eng-1")
          _ (draft! actor "t8bpre" "eng-1")
          res (exec-op actor "t8b" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status res)))
      (is (not (some #{:customs-clearance-missing} (mapcat :basis (store/ledger db))))))))

(deftest engagement-fee-mismatch-is-held
  (testing "claimed fee that doesn't equal base + months x rate (+ optional export) -> HOLD"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t9pre" "eng-3")
          _ (draft! actor "t9pre" "eng-3")
          res (exec-op actor "t9" {:op :filing/submit :subject "eng-3" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:engagement-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest submit-always-escalates-then-human-decides
  (testing "integration: a clean fully-assessed submit still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t10pre" "eng-1")
          _ (draft! actor "t10pre" "eng-1")
          r1 (exec-op actor "t10" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, submit record drafted"
        (let [r2 (approve! actor "t10")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:submitted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/submit-history db))) "one draft submit record"))))))

(deftest draft-always-escalates-then-human-decides
  (testing "a clean fully-assessed draft still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t11pre" "eng-1")
          r1 (exec-op actor "t11" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, draft record drafted"
        (let [r2 (approve! actor "t11")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:drafted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/draft-history db))) "one draft record"))))))

(deftest engagement-double-draft-is-held
  (testing "drafting the same engagement's compliance package twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t12pre" "eng-1")
          _ (draft! actor "t12pre" "eng-1")
          res (exec-op actor "t12" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-drafted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/draft-history db))) "still only the one earlier draft"))))

(deftest engagement-double-submit-is-held
  (testing "submitting the same engagement's compliance package twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess-baseline! actor "t13pre" "eng-1")
          _ (draft! actor "t13pre" "eng-1")
          _ (exec-op actor "t13a" {:op :filing/submit :subject "eng-1" :track track} operator)
          _ (approve! actor "t13a")
          res (exec-op actor "t13" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-submitted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/submit-history db))) "still only the one earlier submit"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :engagement/intake :subject "eng-1"
                          :patch {:id "eng-1" :operator "Kita Procurement Advisory KK"}} operator)
      (exec-op actor "b" {:op :compliance/assess :subject "eng-1" :track :unified-qualification :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))

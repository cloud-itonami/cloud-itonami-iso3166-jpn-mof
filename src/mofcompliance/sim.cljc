(ns mofcompliance.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean engagement through
  intake -> assess unified-qualification -> assess tender-method ->
  compliance-package filing draft (escalate/approve/commit) -> filing
  submit (escalate/approve/commit), then a foreign-investor engagement
  through the same lifecycle plus FEFTA assessment, then a cross-
  border-goods engagement through the same lifecycle plus customs
  assessment, then shows HARD-hold scenarios grounded in the dossier:
  fabrication defense, corporate-number-misattribution defense, fee
  mismatch, missing unified qualification, missing tender-method
  classification, missing FEFTA screening, missing customs clearance,
  and double-draft/double-submit."
  (:require [langgraph.graph :as g]
            [mofcompliance.registry :as registry]
            [mofcompliance.store :as store]
            [mofcompliance.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :mof-compliance-operator :phase 3})
(def track registry/compliance-track)

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== engagement/intake eng-1 (clean) ==")
    (println (exec-op actor "t1" {:op :engagement/intake :subject "eng-1"
                                  :patch {:id "eng-1" :operator "Kita Procurement Advisory KK"}} operator))

    (println "== compliance/assess eng-1/unified-qualification (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :unified-qualification} operator))
    (println (approve! actor "t2"))

    (println "== compliance/assess eng-1/tender-method (escalates -- human approves) ==")
    (println (exec-op actor "t2b" {:op :compliance/assess :subject "eng-1" :track :tender-method} operator))
    (println (approve! actor "t2b"))

    (println "== filing/draft eng-1 compliance-package (always escalates -- actuation/draft-filing) ==")
    (let [r (exec-op actor "t3" {:op :filing/draft :subject "eng-1" :track track} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t3")))

    (println "== filing/submit eng-1 compliance-package (always escalates -- actuation/submit-filing) ==")
    (let [r (exec-op actor "t4" {:op :filing/submit :subject "eng-1" :track track} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t4")))

    (println "== compliance/assess eng-2/unified-qualification (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :compliance/assess :subject "eng-2" :track :unified-qualification :no-spec? true} operator))

    (println "== compliance/assess eng-2/corporate-number-boundary (misattribution -> HARD hold) ==")
    (println (exec-op actor "t5b" {:op :compliance/assess :subject "eng-2" :track :corporate-number-boundary :misattribute? true} operator))

    (println "== compliance/assess eng-3 (unified-qualification + tender-method, sets up fee-mismatch) ==")
    (println (exec-op actor "t6" {:op :compliance/assess :subject "eng-3" :track :unified-qualification} operator))
    (println (approve! actor "t6"))
    (println (exec-op actor "t6b" {:op :compliance/assess :subject "eng-3" :track :tender-method} operator))
    (println (approve! actor "t6b"))
    (println (exec-op actor "t6c" {:op :filing/draft :subject "eng-3" :track track} operator))
    (println (approve! actor "t6c"))
    (println "== filing/submit eng-3 (fee mismatch -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :filing/submit :subject "eng-3" :track track} operator))

    (println "== compliance/assess eng-4 (sets up unified-qualification-missing) ==")
    (println (exec-op actor "t8" {:op :compliance/assess :subject "eng-4" :track :unified-qualification} operator))
    (println (approve! actor "t8"))
    (println (exec-op actor "t8b" {:op :compliance/assess :subject "eng-4" :track :tender-method} operator))
    (println (approve! actor "t8b"))
    (println (exec-op actor "t8c" {:op :filing/draft :subject "eng-4" :track track} operator))
    (println (approve! actor "t8c"))
    (println "== filing/submit eng-4 (unified-qualification-missing -> HARD hold) ==")
    (println (exec-op actor "t9" {:op :filing/submit :subject "eng-4" :track track} operator))

    (println "== compliance/assess eng-5 (sets up tender-method-undetermined) ==")
    (println (exec-op actor "t10" {:op :compliance/assess :subject "eng-5" :track :unified-qualification} operator))
    (println (approve! actor "t10"))
    (println (exec-op actor "t10b" {:op :compliance/assess :subject "eng-5" :track :tender-method} operator))
    (println (approve! actor "t10b"))
    (println (exec-op actor "t10c" {:op :filing/draft :subject "eng-5" :track track} operator))
    (println (approve! actor "t10c"))
    (println "== filing/submit eng-5 (tender-method-undetermined -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :filing/submit :subject "eng-5" :track track} operator))

    (println "== compliance/assess eng-6 (foreign investor, sets up fefta-screening-missing) ==")
    (println (exec-op actor "t12" {:op :compliance/assess :subject "eng-6" :track :unified-qualification} operator))
    (println (approve! actor "t12"))
    (println (exec-op actor "t12b" {:op :compliance/assess :subject "eng-6" :track :tender-method} operator))
    (println (approve! actor "t12b"))
    (println (exec-op actor "t12c" {:op :compliance/assess :subject "eng-6" :track :fefta-notification} operator))
    (println (approve! actor "t12c"))
    (println (exec-op actor "t12d" {:op :filing/draft :subject "eng-6" :track track} operator))
    (println (approve! actor "t12d"))
    (println "== filing/submit eng-6 (fefta-screening-missing -> HARD hold) ==")
    (println (exec-op actor "t13" {:op :filing/submit :subject "eng-6" :track track} operator))

    (println "== compliance/assess eng-7 (cross-border goods, sets up customs-clearance-missing) ==")
    (println (exec-op actor "t14" {:op :compliance/assess :subject "eng-7" :track :unified-qualification} operator))
    (println (approve! actor "t14"))
    (println (exec-op actor "t14b" {:op :compliance/assess :subject "eng-7" :track :tender-method} operator))
    (println (approve! actor "t14b"))
    (println (exec-op actor "t14c" {:op :compliance/assess :subject "eng-7" :track :customs-declaration} operator))
    (println (approve! actor "t14c"))
    (println (exec-op actor "t14d" {:op :filing/draft :subject "eng-7" :track track} operator))
    (println (approve! actor "t14d"))
    (println "== filing/submit eng-7 (customs-clearance-missing -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :filing/submit :subject "eng-7" :track track} operator))

    (println "== filing/draft eng-1 AGAIN (double-draft -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :filing/draft :subject "eng-1" :track track} operator))

    (println "== filing/submit eng-1 AGAIN (double-submit -> HARD hold) ==")
    (println (exec-op actor "t17" {:op :filing/submit :subject "eng-1" :track track} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft records ==")
    (doseq [r (store/draft-history db)] (println r))

    (println "== submit records ==")
    (doseq [r (store/submit-history db)] (println r))))

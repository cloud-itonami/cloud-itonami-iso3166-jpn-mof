(ns mofcompliance.mofcompliancellm
  "ProcurementCompliance-LLM client -- the *contained intelligence node*
  for the JPN-MOF (Ministry of Finance) compliance actor.

  It normalizes engagement intake, drafts a per-track (`:unified-
  qualification`/`:tender-method`/`:fefta-notification`/
  `:customs-declaration`/`:corporate-number-boundary`) compliance
  evidence checklist, drafts the compliance-package filing-draft
  action, and drafts the compliance-package filing-submit action.
  CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real MOF filing. Every output is censored
  downstream by `mofcompliance.governor` before anything touches the
  SSoT, and `:filing/draft`/`:filing/submit` proposals NEVER auto-commit
  at any phase -- see README Core Contract.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. Two test-only injection flags exist purely to exercise the
  governor's fabrication defenses without needing an actual bad LLM:
  `:no-spec?` (assess an unregistered track) and `:misattribute?`
  (assess `:corporate-number-boundary` while WRONGLY claiming the
  Corporate Number constitutes MOF business registration -- the
  governor's `corporate-number-misattribution` check must catch this)."
  (:require [mofcompliance.facts :as facts]
            [mofcompliance.registry :as registry]
            [mofcompliance.store :as store]))

(defn- normalize-intake
  [_db {:keys [patch]}]
  {:summary    (str "engagement intake record updated: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :engagement/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-track
  "Per-track (`:unified-qualification`/`:tender-method`/
  `:fefta-notification`/`:customs-declaration`/
  `:corporate-number-boundary`) compliance evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a track with NO official spec-basis.
  `:misattribute?` injects the OTHER failure mode: assessing
  `:corporate-number-boundary` while wrongly claiming the Corporate
  Number constitutes MOF business registration."
  [_db {:keys [track no-spec? misattribute?]}]
  (let [track (if no-spec? :unknown-track track)
        sb (facts/spec-basis track)]
    (cond
      (nil? sb)
      {:summary    (str (name track) " の公式spec-basisが見つかりません")
       :rationale  "mofcompliance.facts に未登録のトラック。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:track track :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}

      (and misattribute? (= track :corporate-number-boundary))
      {:summary    (str (name track) " について法人番号を法人登記の代替として扱う提案(検証用の誤った提案)")
       :rationale  "この提案は意図的に誤りを含む -- governorのcorporate-number-misattribution検査を試験するため"
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track :checklist (:required-evidence sb) :spec-basis (:provenance sb)
                    :claims-corporate-number-as-business-registration? true}
       :stake      nil
       :confidence 0.5}

      :else
      {:summary    (str (name track) " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-draft
  "Draft the actual compliance-package FILING-DRAFT action. ALWAYS
  `:stake :actuation/draft-filing`."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)
        track registry/compliance-track]
    {:summary    (str subject " 向けコンプライアンス・パッケージ提出ドラフト提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "track=" (name track) " portal=" (:portal e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-drafted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/draft-filing
     :confidence (if e 0.9 0.3)}))

(defn- propose-submit
  "Draft the actual compliance-package FILING-SUBMIT action. ALWAYS
  `:stake :actuation/submit-filing` -- a real-world MOF-facing
  compliance-package submission. Reflects readiness across ALL FOUR
  gates the governor independently re-verifies: 全省庁統一資格
  (unconditional), 会計法/予算決算及び会計令 tender-method classification
  (unconditional), FEFTA screening (conditional on `:requires-fefta-
  screening?`), and customs clearance (conditional on `:requires-
  customs-clearance?`)."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)
        track registry/compliance-track
        unified-ok? (:unified-qualification-verified? e)
        tender-ok? (:tender-method-classified? e)
        fefta-ok? (or (not (:requires-fefta-screening? e)) (:fefta-screening-verified? e))
        customs-ok? (or (not (:requires-customs-clearance? e)) (:customs-clearance-verified? e))]
    {:summary    (str subject " 向けコンプライアンス・パッケージ提出提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "unified-qualification-verified?=" (:unified-qualification-verified? e)
                        " tender-method-classified?=" (:tender-method-classified? e)
                        " fefta-screening-verified?=" (:fefta-screening-verified? e)
                        " customs-clearance-verified?=" (:customs-clearance-verified? e)
                        " claimed-fee=" (:claimed-fee e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-submitted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/submit-filing
     :confidence (if (and e unified-ok? tender-ok? fefta-ok? customs-ok?) 0.9 0.3)}))

(defprotocol Advisor
  (-advise [this db request] "Return a proposal map for `request`."))

(defrecord MockAdvisor []
  Advisor
  (-advise [_ db {:keys [op] :as request}]
    (case op
      :engagement/intake   (normalize-intake db request)
      :compliance/assess   (assess-track db request)
      :filing/draft        (propose-draft db request)
      :filing/submit       (propose-submit db request)
      {:summary "unknown op" :rationale "unsupported" :cites []
       :effect :noop :value {} :stake nil :confidence 0.0})))

(defn mock-advisor [] (->MockAdvisor))

(defn trace [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :track (:track request)
   :summary (:summary proposal)
   :confidence (:confidence proposal)
   :stake (:stake proposal)})

(ns mofcompliance.governor
  "MOF Procurement Compliance Governor -- the independent compliance
  layer that earns the ProcurementCompliance-LLM the right to commit.
  The LLM has no notion of what 全省庁統一資格 (Unified Qualification)
  actually requires, whether a contract's 会計法/予算決算及び会計令
  tender-method classification (一般競争入札/指名競争入札/随意契約) has
  actually been correctly determined, whether FEFTA prior-notification
  screening is actually complete for a foreign-investor engagement in a
  designated/core sector, whether customs/tariff import clearance is
  actually complete for a cross-border-goods engagement, whether a
  proposal is quietly treating the National Tax Agency's Corporate
  Number as if it were MOF performing business/commercial registration
  (a documented fabrication trap -- that authority belongs to the
  Ministry of Justice's Legal Affairs Bureau), whether a claimed
  engagement fee actually equals base + months x rate (+ optional
  export package), or when a draft stops being a draft and becomes a
  real-world MOF-facing filing, so this MUST be a separate system able
  to *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:mof-procurement-compliance-governor`
  (blueprint.edn).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual filing, registration, or compliance-program submission
  requires MOF Procurement Compliance Governor clearance and always
  escalates to human sign-off'; 'a false or fabricated regulatory-
  requirement claim is a HARD hold that cannot be overridden by human
  approval alone') names exactly the checks below.

  Ten checks, in priority order, ALL HARD violations except the
  confidence/actuation gate: a human approver CANNOT override the hard
  ones. The confidence/actuation gate is SOFT: it asks a human to look
  (low confidence / actuation), and the human may approve -- but see
  `mofcompliance.phase`: for `:stake :actuation/draft-filing`/
  `:actuation/submit-filing` NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                    -- did the compliance-track
                                         proposal cite an OFFICIAL
                                         source (`mofcompliance.facts`),
                                         or invent one?
    2. Evidence incomplete           -- for the compliance-package
                                         `:filing/draft`/`:filing/submit`,
                                         has EVERY applicable underlying
                                         regulatory track (unified-
                                         qualification + tender-method,
                                         plus fefta-notification/
                                         customs-declaration only when
                                         the engagement's own `:requires-*`
                                         flag says they apply) actually
                                         been assessed with a full
                                         evidence checklist on file?
    3. Corporate-number
       misattribution                 -- for `:compliance/assess`/
                                         `:filing/draft`/`:filing/submit`,
                                         does the proposal treat the
                                         NTA-issued Corporate Number as
                                         if it were MOF performing
                                         business/commercial
                                         registration? A documented
                                         fabrication trap this fleet has
                                         hit before with other
                                         jurisdictions' misattributed
                                         authorities -- registration
                                         belongs to the Ministry of
                                         Justice's Legal Affairs Bureau,
                                         never MOF.
    4. Unified-qualification missing -- for `:filing/submit`,
                                         UNCONDITIONALLY verify
                                         `:unified-qualification-
                                         verified?` is true. 全省庁統一資格
                                         is a documented prerequisite to
                                         any goods/services contract
                                         with a Japanese ministry
                                         (p-portal.go.jp).
    5. Tender-method undetermined    -- for `:filing/submit`,
                                         UNCONDITIONALLY verify
                                         `:tender-method-classified?`
                                         is true. 会計法/予算決算及び会計令
                                         require a correct
                                         一般競争入札/指名競争入札/随意契約
                                         classification before
                                         proceeding with any contract.
    6. FEFTA-screening missing       -- for `:filing/submit`, when the
                                         engagement declares
                                         `:requires-fefta-screening?
                                         true` (foreign investor,
                                         designated/core sector),
                                         INDEPENDENTLY verify
                                         `:fefta-screening-verified?`
                                         is true.
    7. Customs-clearance missing     -- for `:filing/submit`, when the
                                         engagement declares
                                         `:requires-customs-clearance?
                                         true` (cross-border goods),
                                         INDEPENDENTLY verify
                                         `:customs-clearance-verified?`
                                         is true.
    8. Engagement fee mismatch       -- for `:filing/submit`,
                                         INDEPENDENTLY recompute whether
                                         the engagement's own `:claimed-
                                         fee` equals `base-fee +
                                         monthly-rate x monitoring-
                                         months` (+ optional export-fee
                                         when `:audit-export?` is true).
    9. Confidence floor / actuation
       gate                            -- LLM confidence below
                                         threshold, OR the op is
                                         `:filing/draft`/`:filing/submit`
                                         (REAL acts) -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off the engagement's own `:drafted?`/`:submitted?` facts (never a
  `:status` value) -- unlike sibling actors with multiple independent
  filing tracks, this actor manages exactly ONE
  (`mofcompliance.registry/compliance-track`), so no per-track
  indirection is needed here either."
  (:require [kotoba.lang.text :as str]
            [mofcompliance.facts :as facts]
            [mofcompliance.registry :as registry]
            [mofcompliance.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real MOF compliance package and submitting it are the two
  real-world actuation events this actor performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent MOF's procurement/FEFTA/customs requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はコンプライアンス要件として扱えない"}]))))

(defn- applicable-tracks
  "Which underlying `mofcompliance.facts` catalog tracks must be
  assessed before the compliance-package may be drafted/submitted for
  `engagement`? Always `:unified-qualification` + `:tender-method`;
  `:fefta-notification`/`:customs-declaration` are added only when the
  engagement's own `:requires-*` flag says they apply."
  [engagement]
  (cond-> #{:unified-qualification :tender-method}
    (true? (:requires-fefta-screening? engagement))   (conj :fefta-notification)
    (true? (:requires-customs-clearance? engagement)) (conj :customs-declaration)))

(defn- evidence-incomplete-violations
  "For the compliance-package `:filing/draft`/`:filing/submit`, EVERY
  applicable underlying track's required evidence checklist must
  actually be satisfied."
  [{:keys [op subject track]} st]
  (when (and (contains? #{:filing/draft :filing/submit} op) (= track registry/compliance-track))
    (let [e (store/engagement st subject)
          needed (applicable-tracks e)
          missing (remove (fn [t]
                             (let [a (store/assessment-of st subject t)]
                               (and a (facts/required-evidence-satisfied? t (:checklist a)))))
                           needed)]
      (when (seq missing)
        [{:rule :evidence-incomplete
          :detail (str subject " の必要書類チェックリストが未充足のトラック: "
                      (str/join "," (map name (sort missing))))}]))))

(defn- corporate-number-misattribution-violations
  "For `:compliance/assess`/`:filing/draft`/`:filing/submit`, a
  proposal that claims the NTA-issued Corporate Number constitutes MOF
  performing business/commercial registration is a HARD violation --
  registration is the Ministry of Justice's Legal Affairs Bureau's
  authority, never MOF's (`mofcompliance.facts`'s
  `:corporate-number-boundary` entry is the citable spec-basis for
  REJECTING this claim, not for asserting it)."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (when (true? (:claims-corporate-number-as-business-registration? (:value proposal)))
      [{:rule :corporate-number-misattribution
        :detail "法人番号(国税庁による自動収録)を法務省の商業・法人登記そのもの、またはMOFによる事業者登録権限であるかのように扱う提案は認められない -- 登記の実施主体は常に法務省法務局"}])))

(defn- unified-qualification-missing-violations
  "For `:filing/submit`, UNCONDITIONALLY verify
  `:unified-qualification-verified?` is true -- 全省庁統一資格 is a
  documented prerequisite to any goods/services contract with MOF (or
  any ministry), not conditional on anything else about the
  engagement."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (true? (:unified-qualification-verified? e))
        [{:rule :unified-qualification-missing
          :detail (str subject " は全省庁統一資格(資格審査結果通知書)が未確認 -- 提出提案は進められない")}]))))

(defn- tender-method-undetermined-violations
  "For `:filing/submit`, UNCONDITIONALLY verify
  `:tender-method-classified?` is true -- 会計法/予算決算及び会計令に基づく
  契約方式区分(一般競争入札/指名競争入札/随意契約)の判定は、engagementの
  他の状況に関わらず常に必要。"
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (true? (:tender-method-classified? e))
        [{:rule :tender-method-undetermined
          :detail (str subject " は会計法・予算決算及び会計令に基づく契約方式区分(一般競争入札/指名競争入札/随意契約)が未判定 -- 提出提案は進められない")}]))))

(defn- fefta-screening-missing-violations
  "For `:filing/submit`, when the engagement declares
  `:requires-fefta-screening? true` (foreign investor, designated/core
  sector), INDEPENDENTLY verify `:fefta-screening-verified?` is true.
  CONDITIONAL on the engagement's own ground truth -- a no-op for a
  domestic-operator engagement."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-fefta-screening? e))
                 (not (true? (:fefta-screening-verified? e))))
        [{:rule :fefta-screening-missing
          :detail (str subject " は外国為替及び外国貿易法(FEFTA)の対内直接投資等事前届出/審査が未完了 -- 提出提案は進められない")}]))))

(defn- customs-clearance-missing-violations
  "For `:filing/submit`, when the engagement declares
  `:requires-customs-clearance? true` (cross-border goods),
  INDEPENDENTLY verify `:customs-clearance-verified?` is true.
  CONDITIONAL on the engagement's own ground truth -- a no-op for a
  services-only engagement with no cross-border goods dimension."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-customs-clearance? e))
                 (not (true? (:customs-clearance-verified? e))))
        [{:rule :customs-clearance-missing
          :detail (str subject " は関税定率法に基づく輸入申告・輸入許可(税関)が未完了 -- 提出提案は進められない")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate (+ optional
  export-fee)."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- already-drafted-violations
  "Refuses to draft the SAME engagement's compliance package twice."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/draft) (= track registry/compliance-track))
    (when (store/engagement-drafted? st subject)
      [{:rule :already-drafted
        :detail (str subject " は既にドラフト済み")}])))

(defn- already-submitted-violations
  "Refuses to submit the SAME engagement's compliance package twice."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track registry/compliance-track))
    (when (store/engagement-submitted? st subject)
      [{:rule :already-submitted
        :detail (str subject " は既に提出済み")}])))

(defn check
  "Censors a ProcurementCompliance-LLM proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c
  :escalate? bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (corporate-number-misattribution-violations request proposal)
                           (unified-qualification-missing-violations request st)
                           (tender-method-undetermined-violations request st)
                           (fefta-screening-missing-violations request st)
                           (customs-clearance-missing-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :track      (:track request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})

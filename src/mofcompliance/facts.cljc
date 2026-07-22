(ns mofcompliance.facts
  "Japan Ministry of Finance (財務省, MOF) public-sector procurement /
  foreign-investment-screening / customs compliance catalog -- the ONLY
  source of regulatory-requirement facts this actor is allowed to cite
  (`mofcompliance.governor`'s spec-basis check enforces that every
  proposal touching `:compliance/assess`, `:filing/draft`, or
  `:filing/submit` cites this catalog and nothing invented).

  Every fact below was verified via web search against `mof.go.jp`,
  `p-portal.go.jp`, `customs.go.jp`, `japaneselawtranslation.go.jp`, and
  `zen-shouchou.jp` government/quasi-official domains during this repo's
  research pass (2026-07-22/23). Five catalog entries, each with its own
  owner authority and legal basis -- do NOT merge them into one
  undifferentiated 'MOF requirement':

    :unified-qualification        -- 全省庁統一資格 (Unified
                                      Qualification for Competitive
                                      Participation across All
                                      Ministries and Agencies) --
                                      goods/services scope only
                                      (excludes construction works and
                                      survey/construction-consulting,
                                      which use separate systems).
                                      Foreign corporations ARE eligible,
                                      including those with no Japan
                                      branch.
    :tender-method                -- 会計法 (Accounts Act) +
                                      予算決算及び会計令 (Cabinet Order on
                                      Budget, Settlement of Accounts and
                                      Accounting) tender-method
                                      classification: 一般競争入札
                                      (general competitive) /
                                      指名競争入札 (designated
                                      competitive) / 随意契約
                                      (discretionary contract).
    :fefta-notification           -- 外国為替及び外国貿易法 (FEFTA)
                                      prior notification for inward
                                      direct investment, reviewed
                                      jointly by the Minister of Finance
                                      and the competent sectoral
                                      minister. MOF's administering unit
                                      is the Foreign Investment Policy
                                      and Review Office, Research
                                      Division, International Bureau.
    :customs-declaration          -- Customs Tariff Act import
                                      declaration + import permit,
                                      administered by MOF's internal
                                      Customs and Tariff Bureau (関税局)
                                      with Japan Customs as the
                                      field-execution arm.
    :corporate-number-boundary    -- a NEGATIVE/boundary catalog entry,
                                      `:filing-track? false` (nothing is
                                      ever drafted/submitted against
                                      it). It exists so
                                      `mofcompliance.governor`'s
                                      corporate-number-misattribution
                                      check has a citable spec-basis
                                      for REJECTING any proposal that
                                      treats the National Tax Agency's
                                      13-digit Corporate Number
                                      (法人番号) as MOF performing
                                      business/commercial registration
                                      -- that authority belongs to the
                                      Ministry of Justice's Legal
                                      Affairs Bureau (法務局); the NTA
                                      only auto-receives that
                                      registration data to assign the
                                      Corporate Number.

  What this catalog deliberately does NOT claim (fabrication traps this
  repo's research dossier explicitly flagged -- see README/docs):
    - does NOT attribute banking/financial-institution licensing or
      supervision to MOF (separated to the independent Financial
      Services Agency, fully consolidated 2000-07-01);
    - does NOT attribute company/commercial registration (登記) to MOF
      (Ministry of Justice's Legal Affairs Bureau; see
      `:corporate-number-boundary` above);
    - does NOT invent a specific WTO GPA coverage-threshold yen figure
      (Japan's 1994 GPA signatory status, in force 1996, is a narrative
      fact only; no numeric threshold was verified);
    - does NOT model a formal '郵便入札 (mail-in bidding) system' as a
      distinct qualification/portal -- it is a submission-method
      procedural detail only, never a compliance gate;
    - does NOT model 国債市場特別参加者制度 (JGB Market Special
      Participants / primary-dealer system) as a track -- out of scope
      for this pass (see docs/business-model.md Domain note); a future
      pass could add it as a sixth catalog entry if scope is extended
      to MOF-issued-debt-market counterparties.")

(def catalog
  {:unified-qualification
   {:name "全省庁統一資格 -- 物品の製造・販売、役務の提供、物品の買受けに関する競争参加資格"
    :name-en "Unified Qualification for Competitive Participation in All Ministries and Agencies -- goods manufacture/sale, service provision, goods purchase"
    :owner-authority "全省庁共通制度（調達ポータル運営）-- 財務省はこの制度の対象官庁の一つとして契約担当官を通じ運用に参加"
    :legal-basis "予算決算及び会計令 (昭和22年勅令第165号, 1947-04-30) に基づく全省庁統一の競争参加資格審査 -- 会計法 (昭和22年法律第35号, 1947-03-31公布) の委任先"
    :promulgated "1947-04-30"
    :official-portal "https://www.p-portal.go.jp/pps-web-biz/geps-chotatujoho/resources/app/html/shikaku.html"
    :provenance "https://www.p-portal.go.jp/pps-web-biz/geps-chotatujoho/resources/app/html/shikaku.html"
    :provenance-secondary
    ["https://www.japaneselawtranslation.go.jp/ja/laws/view/2970"
     "https://www.mof.go.jp/application-contact/procurement/index.html"
     "https://zen-shouchou.jp/16081818569630"]
    :scope-excludes ["工事" "測量・建設コンサルタント等業務(別制度で対象)"]
    :foreign-corporations-eligible? true
    :foreign-corporation-note "国内に登記された支店を持たない外国法人も対象 -- 本国の登記/納税証明書類を日本語訳して提出することが必要"
    :unified-qualification-required? true
    :process-description "物品の製造・販売、役務の提供、物品の買受けを内容とする契約について、年度サイクルで等級付けされた資格審査結果通知書の取得が前提となる。工事および測量・建設コンサルタント等業務は別制度で対象外。"
    :required-evidence
    ["調達ポータル上での資格審査申請記録"
     "資格審査結果通知書(等級・有効期間確認)"
     "(外国法人の場合)本国登記・納税証明書類の日本語訳添付記録"]}

   :tender-method
   {:name "会計法・予算決算及び会計令 -- 一般競争入札/指名競争入札/随意契約の契約方式区分"
    :name-en "Accounts Act + Cabinet Order on Budget, Settlement of Accounts and Accounting -- tender-method classification (general competitive / designated competitive / discretionary contract)"
    :owner-authority "財務省 (会計法令の所管官庁) -- 各省庁の契約担当官が個別契約ごとに運用"
    :legal-basis "会計法 (昭和22年法律第35号, 1947-03-31公布) / 予算決算及び会計令 (昭和22年勅令第165号, 1947-04-30) -- 予定価格・一般競争入札・指名競争入札・随意契約の各要件を規定"
    :promulgated "1947-03-31"
    :official-portal "https://www.mof.go.jp/application-contact/procurement/index.html"
    :provenance "https://www.japaneselawtranslation.go.jp/ja/laws/view/2970"
    :provenance-secondary
    ["https://www.mof.go.jp/application-contact/procurement/index.html"]
    :process-description "予算決算及び会計令は予定価格の算定、一般競争入札、指名競争入札、随意契約それぞれの適用要件を定める。契約担当官(または本actorのようなコンプライアンス補助)は個別契約がどの方式に該当するかを法令に基づき正しく判定しなければならない -- 判定なしに入札・契約手続を進めることはできない。"
    :required-evidence
    ["予定価格算定記録"
     "契約方式区分(一般競争入札/指名競争入札/随意契約)の判定根拠記録(会計法・予算決算及び会計令の該当条項引用)"]}

   :fefta-notification
   {:name "外国為替及び外国貿易法 -- 対内直接投資等の事前届出(財務大臣・事業所管大臣共管)"
    :name-en "Foreign Exchange and Foreign Trade Act (FEFTA) -- prior notification for inward direct investment"
    :owner-authority "財務省国際局調査課 (Foreign Investment Policy and Review Office, Research Division, International Bureau, MOF) + 事業所管大臣共管"
    :legal-basis "外国為替及び外国貿易法 -- 対内直接投資等の届出制度。2026年改正(法案2026-05-29可決、2026-06-05公布)で一定の間接取得(外国持株会社の議決権50%以上取得等)を対象に追加。"
    :amended "2026-06-05"
    :official-portal "https://www.mof.go.jp/policy/international_policy/gaitame_kawase/fdi/index.htm"
    :provenance "https://www.mof.go.jp/policy/international_policy/gaitame_kawase/fdi/index.htm"
    :provenance-secondary
    ["https://www.mof.go.jp/english/policy/international_policy/fdi/Related_Guidance_and_Documents/20240913.html"]
    :sensitivity-tiers ["非指定業種" "指定業種(コア業種外)" "指定業種(コア業種)"]
    :standard-review-window-days 30
    :indirect-acquisition-coverage-note
    "2026年改正により、外国持株会社の議決権50%以上取得など一定の間接取得も対象化された(2026-06-05公布)。"
    :process-description "上場会社を業種により3区分(非指定/指定業種コア業種外/指定業種コア業種)に分類し、対内直接投資等について財務大臣と事業所管大臣が共管で事前届出の要否と審査(標準審査期間約30日)を行う。国家安全保障・公の秩序・公衆の安全・我が国経済の円滑な運営の観点からの審査。"
    :required-evidence
    ["投資対象上場会社の業種区分(非指定/指定業種コア業種外/指定業種コア業種)確認記録"
     "事前届出書提出記録(該当する場合)、または届出不要の確認記録"
     "標準審査期間(約30日)内の審査結果記録"]}

   :customs-declaration
   {:name "関税定率法 -- 輸入申告・輸入許可(財務省関税局/税関)"
    :name-en "Customs Tariff Act -- import declaration and import permit (MOF Customs and Tariff Bureau / Japan Customs)"
    :owner-authority "財務省関税局 (Customs and Tariff Bureau, MOF内部部局) / 税関 (Japan Customs, 執行機関)"
    :legal-basis "関税定率法 (Customs Tariff Act)"
    :official-portal "https://www.customs.go.jp/english/zeikan/k-kikou_e.htm"
    :provenance "https://www.customs.go.jp/english/zeikan/k-kikou_e.htm"
    :provenance-secondary
    ["https://www.kanzei.or.jp/kanzei_law/143AC0000000054.en.html"]
    :process-description "貨物を輸入しようとする者は税関長に対して輸入申告を行い、審査及び関税等の納付を経て輸入許可を受ける必要がある。政府契約の履行として輸入される貨物も同じ手続の対象。"
    :required-evidence
    ["輸入申告記録(税関長宛)"
     "関税等納付記録"
     "輸入許可書取得記録"]}

   :corporate-number-boundary
   {:name "法人番号 -- 国税庁による自動収録であり、法務省の商業・法人登記そのものではない(境界確認用エントリ)"
    :name-en "Corporate Number (13-digit, NTA-assigned) -- automatically derived from Ministry of Justice commercial/corporate registration data; NOT a business-registration authority of MOF"
    :owner-authority "国税庁 (National Tax Agency, MOFの外局) -- 法人番号の指定・公表のみ。商業・法人登記そのものは法務省法務局(登記所)の所管であり、国税庁/MOFは登記機関ではない。"
    :legal-basis "行政手続における特定の個人を識別するための番号の利用等に関する法律(番号利用法) -- 法人番号の指定根拠。登記の根拠法(商業登記法等)は法務省所管で別法。"
    :official-portal "https://www.moj.go.jp/MINJI/minji06.html"
    :provenance "https://www.moj.go.jp/MINJI/minji06.html"
    :filing-track? false
    :process-description "法人番号は法務局が保有する商業・法人登記情報を国税庁が自動的に受け取り指定・公表するものであり、国税庁(ひいてはMOF)が商業・法人登記そのものを行う権限を持つわけではない。登記の実施主体は常に法務省法務局。法人番号の保有・提示を『MOFによる事業者登録』であるかのように扱う提案は誤り。"
    :required-evidence []}})

(def valid-tracks (set (keys catalog)))

(defn spec-basis [track] (get catalog track))

(defn coverage
  ([] (coverage (keys catalog)))
  ([tracks]
   (let [have (filter catalog tracks) missing (remove catalog tracks)]
     {:requested (count tracks) :covered (count have)
      :covered-tracks (vec (sort (map name have)))
      :missing-tracks (vec (sort (map name missing)))
      :note "R0 catalog seed -- unified-qualification + tender-method + fefta-notification + customs-declaration + corporate-number-boundary, JPN-MOF agency scope"})))

(defn required-evidence-satisfied? [track submitted]
  (when-let [{:keys [required-evidence]} (spec-basis track)]
    (= (count required-evidence) (count (filter (set submitted) required-evidence)))))

(defn evidence-checklist [track] (:required-evidence (spec-basis track) []))

(defn filing-track?
  "Does `track`'s catalog entry represent something that is ever itself
  drafted/submitted as a filing (as opposed to a citation-only /
  boundary entry like `:corporate-number-boundary`)? Defaults to true
  when the catalog entry does not say otherwise -- only
  `:corporate-number-boundary` opts out today."
  [track]
  (let [sb (spec-basis track)]
    (boolean (and sb (not (false? (:filing-track? sb)))))))

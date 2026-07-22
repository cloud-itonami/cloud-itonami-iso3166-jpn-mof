# cloud-itonami-iso3166-jpn-mof

Open ISO 3166 Agency Blueprint for **JPN-MOF**: Ministry of Finance
(財務省, MOF) — a Japan-agency-level LEAF under
the `cloud-itonami-iso3166-jpn` country-level coordinator.

This repository designs a forkable OSS business for an independent
compliance consultant: an already-incorporated operator (typically one
already using `cloud-itonami-iso3166-jpn` for general Japan market entry)
gets a Compliance Advisor + independent **MOF Procurement Compliance
Governor** to navigate 全省庁統一資格 (Unified Qualification for
Competitive Participation in All Ministries and Agencies) and 会計法
(Accounts Act) / 予算決算及び会計令 (Cabinet Order on Budget, Settlement
of Accounts and Accounting) tender-method classification for a
public-sector delivery contract, FEFTA (外国為替及び外国貿易法) prior
foreign-investment-notification screening when the operator is a
foreign investor in a designated/core sector, and customs/tariff import
declaration at the Customs and Tariff Bureau (財務省関税局) when the
delivery involves cross-border goods.

## No robotics premise — digital/data service exemption

Agency-specific compliance navigation is a pure data/software service with
no physical-domain work — the same exemption class as `cloud-itonami-6310`
and `cloud-itonami-gtin-*`. `blueprint.edn` sets
`:itonami.blueprint/robotics false` and `:required-technologies` lists only
real capabilities (`:identity`, `:forms`, `:dmn`, `:bpmn`, `:audit-ledger`),
no `:robotics`.

## Core Contract

```text
operator intake + prior filing/compliance history
        |
        v
Compliance Advisor -> MOF Procurement Compliance Governor -> compliance draft, or human sign-off
        |
        v
gated filing / registration / compliance-program submission + audit ledger
```

No automated proposal can submit a filing or registration the governor
refuses, suppress a compliance record, or claim a legal conclusion the
governor has not cleared. `:filing/submit` is never in any phase's `:auto`
set — it always requires human sign-off (mirrors `cloud-itonami-M6910`'s
`filing-submit-never-auto-at-any-phase` invariant).

## Implementation

`src/mofcompliance/` — a langgraph-clj StateGraph actor, same
containment shape as `cloud-itonami-iso3166-ago`'s `marketentry.*` /
`cloud-itonami-iso3166-jpn-digital`'s `digitalprocurement.*` /
`cloud-itonami-iso3166-jpn-mod`'s `defensecompliance.*` /
`cloud-itonami-iso3166-jpn-moe`'s `greenprocurement.*` (advisor sealed
to proposals-only, independent governor, append-only ledger, `Store`
protocol swap, phase gate):

- `facts.cljc` — the 全省庁統一資格 + 会計法/予算決算及び会計令
  tender-method + FEFTA prior-notification + customs/tariff catalog,
  the ONLY source of regulatory-requirement facts the actor may cite.
  Five entries: `:unified-qualification`, `:tender-method`,
  `:fefta-notification`, `:customs-declaration`, and a NEGATIVE/
  boundary entry `:corporate-number-boundary` (`:filing-track? false`)
  that exists only so the governor has a citable spec-basis for
  REJECTING any proposal that treats the National Tax Agency's
  Corporate Number as MOF performing business/commercial registration
  — that authority belongs to the Ministry of Justice's Legal Affairs
  Bureau, never MOF (see "Fabrication traps addressed" below).
- `governor.cljc` — the MOF Procurement Compliance Governor: a
  spec-basis/no-fabrication HARD check, an evidence-incomplete check
  (across every applicable underlying track for the engagement), a
  **corporate-number-misattribution** HARD check, an **全省庁統一資格
  missing** HARD check (`:filing/submit`, unconditional), an
  **会計法/予算決算及び会計令 tender-method undetermined** HARD check
  (`:filing/submit`, unconditional), a **FEFTA-screening missing** HARD
  check (`:filing/submit`, CONDITIONAL on the engagement's own
  `:requires-fefta-screening?`), a **customs-clearance missing** HARD
  check (`:filing/submit`, CONDITIONAL on `:requires-customs-clearance?`),
  an independently-recomputed engagement-fee-mismatch check (three
  revenue lines: base fee + monitoring subscription + optional
  audit-export package), a confidence-floor/actuation gate, and
  double-draft/double-submit guards.
- `store.cljc` — `MemStore`/`DatomicStore` (via
  `kotoba-lang/langchain-store`'s entity field-spec `map->tx`/
  `pull->map`/`pull-pattern` AND its identity-schema/event-log
  helpers, not a hand-rolled `enc`/`dec*` or hand-rolled tx/pull pair)
  for the `engagement` entity, which tracks the engagement-level
  unconditional gates (unified-qualification, tender-method) and
  conditional gates (FEFTA, customs) plus the single actionable filing
  track's actuation state.
- `registry.cljc` — pure-function filing-draft/filing-submit record
  construction for the single actionable filing track
  (`:compliance-package` — the operator's bundled compliance-readiness
  package, distinct from the regulatory catalog entries in
  `facts.cljc`; see its docstring for why this actor manages exactly
  ONE filing track rather than one per regulatory regime).
- `mofcompliancellm.cljc` — the Compliance Advisor (mock LLM,
  proposals only).
- `operation.cljc` — the StateGraph: intake → advise → govern → decide
  → [request-approval →] commit/hold, `interrupt-before` on human
  approval.
- `phase.cljc` — phase 0→3 rollout; `:filing/draft`/`:filing/submit`
  are permanently absent from every phase's `:auto` set.

Ops: `:engagement/intake`, `:compliance/assess` (per underlying
regulatory-catalog-track evidence checklist — `:unified-qualification`/
`:tender-method`/`:fefta-notification`/`:customs-declaration`/
`:corporate-number-boundary`), `:filing/draft`, `:filing/submit` (the
latter two always target the single `:compliance-package` track).

## Fabrication traps addressed

This research pass explicitly verified and defended against:

- MOF does **not** license or supervise banks/securities firms — that
  function was separated to the independent Financial Services Agency
  (fully consolidated 2000-07-01). This actor never models bank/
  securities licensing as an MOF function.
- MOF does **not** perform company/commercial registration (登記) —
  that is the Ministry of Justice's Legal Affairs Bureau. The National
  Tax Agency (MOF's external bureau) only auto-receives that
  registration data to assign the 13-digit Corporate Number (法人番号).
  `governor.cljc`'s corporate-number-misattribution check is a HARD,
  unoverridable defense against conflating "Corporate Number issuance"
  with "MOF business-registration authority."
- No invented WTO GPA coverage-threshold yen figure — Japan's 1994 GPA
  signatory status (in force 1996) is narrative-only; no numeric
  threshold is cited anywhere in this repo.
- No invented "郵便入札 (mail-in bidding) system" as a distinct
  qualification/portal — it is a submission-method detail, never
  modeled as a compliance gate.

## What this is NOT

- **Not Ministry of Finance (財務省) itself, and not the
  government of Japan.** See [`docs/business-model.md`](docs/business-model.md)
  for the boundary with `com-etzhayyim-ooyake`, `matsurigoto`,
  `com-etzhayyim-toritsugi`, `legal-entity.etzhayyim.com`,
  `cloud-itonami-M6910`, and the country-level `cloud-itonami-iso3166-jpn`.
- **Not legal or tax advice.** Every regulatory claim must cite the
  official MOF source and route final filings to
  Japan-licensed counsel or a registered agent where the law requires
  licensed representation.

## Capability layer

Resolves via [`kotoba-lang/iso3166`](https://github.com/kotoba-lang/iso3166)
(code `JPN-MOF`, `:parent "JPN"`, cross-referenced to ooyake's
`gov.jpn.mof`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.

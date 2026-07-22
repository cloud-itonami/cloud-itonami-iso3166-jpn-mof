# Business Model: Independent MOF Procurement, Foreign-Investment-Screening & Customs Compliance Service — Japan (MOF)

Implementation: `src/mofcompliance/` — see README.md's Implementation
section. The Trust Controls below are enforced in code by
`mofcompliance.governor` (spec-basis/no-fabrication HARD check,
evidence-incomplete check, corporate-number-misattribution HARD check,
unified-qualification-missing HARD check, tender-method-undetermined
HARD check, FEFTA-screening-missing HARD check, customs-clearance-
missing HARD check, engagement-fee-mismatch check, confidence-floor/
actuation gate, double-draft/double-submit guards) and
`mofcompliance.phase` (`:filing/submit` absent from every phase's
`:auto` set).

## Classification

- Repository: `cloud-itonami-iso3166-jpn-mof`
- ISO 3166 (agency-level): `JPN-MOF`, parent `JPN`
- Ooyake cross-reference: `gov.jpn.mof` (Ministry of Finance / 財務省)
- Activity: 全省庁統一資格 (Unified Qualification) registration-status
  verification for goods/services public-sector contracts, 会計法/
  予算決算及び会計令 tender-method classification (一般競争入札/
  指名競争入札/随意契約), FEFTA (外国為替及び外国貿易法) prior
  foreign-investment-notification screening for foreign-investor
  operators in a designated/core sector, and customs/tariff import
  declaration (財務省関税局/税関) for public-sector deliveries
  involving cross-border goods
- Social impact: [:procurement-access-clarity :foreign-investment-screening-transparency :customs-clarity :public-spend-transparency]

## Customer

- an operator (domestic or foreign) seeking to bid on or deliver a
  Japanese public-sector contract, confirming 全省庁統一資格 status and
  correct 会計法/予算決算及び会計令 tender-method classification before
  proceeding
- a foreign investor whose acquisition of a Japanese listed company
  falls in a FEFTA designated/core sector, confirming prior-notification
  screening status before closing
- an operator importing equipment or goods as part of a public-sector
  delivery, confirming customs/tariff import-declaration status

## Offer

- 全省庁統一資格 registration-status verification walkthrough
  (goods/services scope; foreign-eligible with translated home-country
  documents)
- 会計法/予算決算及び会計令 tender-method classification checklist
  (一般競争入札/指名競争入札/随意契約)
- FEFTA prior-notification screening-status checklist for foreign
  investors in designated/core sectors
- customs/tariff import-declaration checklist for cross-border-goods
  public-sector deliveries
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires MOF Procurement Compliance Governor clearance and always
  escalates to human sign-off (`:filing/submit` is never automated at
  any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold
  that cannot be overridden by human approval alone — it must be
  corrected against a cited MOF source first
- treating the National Tax Agency's Corporate Number as if it were
  MOF performing business/commercial registration is a HARD hold that
  cannot be overridden — that authority belongs to the Ministry of
  Justice's Legal Affairs Bureau
- this service does **not** provide legal or tax advice; characterization
  and filing on the client's behalf beyond checklist/draft assistance
  routes to Japan-licensed counsel or a registered agent
- every requirement cites the official MOF source or
  regulation, never invented

## Boundary with adjacent actors (read before forking)

- **`cloud-itonami-iso3166-jpn`**: the COUNTRY-level coordinator (general
  Japan public-sector market entry). This repo is a narrower, deeper
  AGENCY-level leaf — most operators need the country-level blueprint plus
  only the agency-level blueprints that actually apply to their contract.
- **`com-etzhayyim-ooyake`** (etzhayyim/root): read-only civic-wayfinding
  mirror of government structure, non-commercial, barred from acting as or
  for the government (G3 impersonation ban). This blueprint is commercial
  and never claims to be Ministry of Finance or an official channel.
- **`matsurigoto`** (etzhayyim/root): sovereign e-government statecraft —
  literally the government. This blueprint is an independent operator that
  engages with MOF under its public rules — never the
  agency itself.
- **`com-etzhayyim-toritsugi`** (etzhayyim/root): guides a consenting
  INDIVIDUAL citizen through their OWN procedure, non-profit,
  donation-only. This blueprint's client is a business operator, not an
  individual citizen, and it is commercial.
- **`cloud-itonami-M6910`**: helps a client BECOME a legal entity
  (incorporation, ISIC 6910) — a prior, different regulatory phase (company
  law). This blueprint assumes incorporation is already done and handles
  MOF-specific compliance (a different regulatory domain).

## Domain note (reconciled during implementation)

This blueprint's original text (pre-implementation, landed
2026-07-10) described a narrower "Customs & Tax Compliance" scope:
tariff classification at the Customs and Tariff Bureau (財務省関税局)
plus Qualified Invoice Issuer registration under the National Tax
Agency's consumption-tax invoice system (インボイス制度).

The verified research dossier available for the implementation pass
that produced `src/mofcompliance/` instead centered on **public-sector
procurement compliance**: 全省庁統一資格 (Unified Qualification), 会計法/
予算決算及び会計令 tender-method classification, and FEFTA
foreign-investment-screening (the single most consequential
MOF-administered market-entry fact for foreign entrants) — plus
customs/tariff import declaration, which genuinely overlaps with and
subsumes the original text's tariff-classification theme (both trace
to the Customs and Tariff Bureau / Customs Tariff Act).

This actor family's governing principle (see Trust Controls above) is
to never model a regulatory requirement without a verified source, so
the implemented scope followed the dossier — matching the same choice
`cloud-itonami-iso3166-jpn-moe` made when its own pre-implementation
text (環境影響評価法/廃棄物処理法) diverged from its implementation-pass
dossier (グリーン購入法/環境配慮契約法) — rather than inventing
インボイス制度 specifics (thresholds, forms, registration deadlines)
with no verified sourcing in this pass. The governor name changed
accordingly, from "Customs & Tax Compliance Governor" to "MOF
Procurement Compliance Governor," to accurately describe what is
actually implemented.

A future pass with a verified dossier for インボイス制度 (Qualified
Invoice Issuer registration) could add it as a sixth catalog entry in
`mofcompliance.facts`, or as a sibling blueprint. The same applies to
国債市場特別参加者制度 (JGB Market Special Participants / primary-dealer
system) — mentioned as a possible niche/optional scope item in the
research brief for this pass, but not implemented here because neither
this blueprint's pre-existing text nor its dossier indicated the scope
should cover MOF-issued-debt-market counterparties.

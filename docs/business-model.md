# Business Model: Independent MOF-Regulated Customs & Tax Compliance Service — Japan (MOF)

## Classification

- Repository: `cloud-itonami-iso3166-jpn-mof`
- ISO 3166 (agency-level): `JPN-MOF`, parent `JPN`
- Ooyake cross-reference: `gov.jpn.mof` (Ministry of Finance / 財務省)
- Activity: tariff classification at the Customs and Tariff Bureau (財務省関税局) for goods entering Japan as part of a government-contract delivery, and Qualified Invoice Issuer registration under the National Tax Agency's (国税庁, MOF's external bureau) consumption-tax invoice system (インボイス制度)
- Social impact: [:customs-clarity :tax-invoice-compliance :public-spend-transparency]

## Customer

- an operator importing equipment or goods as part of a public-sector delivery
- an operator needing Qualified Invoice Issuer registration to invoice a Japanese public-sector buyer
- a foreign supplier needing tariff-classification and customs-duty clarity before shipment

## Offer

- tariff-classification walkthrough for goods entering as part of a government contract
- Qualified Invoice Issuer (インボイス) registration checklist
- customs-duty and import-compliance checklist for public-sector deliveries
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires Customs & Tax Compliance Governor clearance and always escalates to human
  sign-off (`:filing/submit` is never automated at any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold that
  cannot be overridden by human approval alone — it must be corrected
  against a cited MOF source first
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

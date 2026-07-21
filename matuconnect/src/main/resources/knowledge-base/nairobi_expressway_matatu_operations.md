# Nairobi Expressway — Matatu Operations (2023–2026)

*Supplementary knowledge base for MatuConnect RAG pipeline. Covers when
and how matatus began legally using the Nairobi Expressway, and which
specific routes are documented as using it. This corrects a gap in
`nairobi_route_updates_2024_2026.md`, which covered CBD terminus
restructuring but not expressway PSV access.*

## 1. Timeline: the matatu ban and its lifting

- **2022**: Following a serious accident (a 32-seater matatu crash at the
  Mlolongo toll station injuring over 20 passengers), the Ministry of
  Transport temporarily banned PSVs with a capacity over 7 passengers
  from using the Nairobi Expressway.
- **2022–2023**: The ban remained in place for roughly a year. Some
  matatus were reported using the expressway regardless, despite the ban.
- **13 July 2023**: Transport Cabinet Secretary Kipchumba Murkomen
  **permanently lifted the PSV suspension**, effective immediately,
  conditional on operators observing traffic rules. This is a durable
  policy change, not a temporary trial — matatus have been legally
  permitted on the expressway continuously since this date.

**Implication for MatuConnect:** the 2019–2020 GTFS dataset predates the
expressway's very existence (opened May 2022) and predates this policy
change by over a year. Any route using the expressway is, by definition,
entirely absent from the structured graph data — this knowledge base is
currently the *only* source of that information in the system.

## 2. Confirmed operational routes using the expressway

| Route | Operator(s) | Notes |
|---|---|---|
| **Kitengela ↔ Westlands** | Super Metro | Runs specifically during morning and evening peak hours via the expressway. Confirmed via operator interview (conductor James Kamau, Super Metro). |
| **Kitengela / Mlolongo / Athi River ↔ CBD** (Route 110, non-expressway variant) | Rembo Shuttle | Uses the traditional surface route: Muthurwa – Jogoo Road – Lusaka Road – Mombasa Road. Boards near Railways bus stop, Haile Selassie Avenue. This is the "without the expressway" option on the same Kitengela corridor. |
| **Nairobi ↔ Kitengela** | Super Metro | A second, more recently publicized Super Metro service on the same corridor (2024), separate from the established Route 110 operators. |
| **Kikuyu ↔ Westlands ↔ Juja ↔ Thika** | Super Metro | A newer cross-town route (also referencing Route 105 westbound and Routes 236/237 toward Thika/Makongeni) — not expressway-dependent for its full length, but illustrates the broader trend of SACCOs launching new cross-town corridors since 2024, consistent with the CBD-decongestion gazette changes already in the knowledge base. |

**Fare note:** expressway fares carry a premium over the surface-route
equivalent — historically reported around USD 1.27 (~KES 160–200
depending on exchange rate and year) each way for the Kitengela–CBD
expressway trip, on top of the toll the vehicle itself pays. Treat any
specific fare figure as indicative and likely to have shifted with fuel
prices — Kenyan matatu fares are volatile and spike during fuel-price
disputes (e.g. a nationwide matatu strike over fuel prices in May 2026
temporarily tripled Kitengela–Nairobi fares from ~KES 100 to ~KES 300
before normalizing).

## 3. Utawala — established routes, expressway status unclear

Utawala (an estate in the Embakasi area) has long-established matatu
service via:
- **Route 34**, boarding at Kencom or Ambassador stage in the CBD
- **Tawala Sacco**, boarding on Tom Mboya Street, opposite the National
  Archives
- **Kani Transport Sacco**, running Muthurwa/Bus Station – Kayole – Utawala
- **City Hopper Ltd**, running a broader corridor: Bus Station –
  Kawangware – Satellite – Ngumo – KNH – Karen – Utawala – Umoja – Kencom
  – GPO

None of these are documented in available sources as specifically
routed via the expressway to Westlands. A commuter-reported "Utawala to
Westlands via the expressway" route may exist informally or have
emerged very recently, but it is **not independently confirmed** here —
unlike the Kitengela–Westlands route above, which has a named operator
and an on-record interview. This gap is worth noting explicitly in the
project report as a known limitation of publicly available secondary
sources, rather than the knowledge base asserting a route that couldn't
be verified.

## 4. Sources

- Kenyans.co.ke, "Murkomen Lifts Matatu Ban on Nairobi Expressway" (13 Jul 2023)
- The Standard, "CS Macharia temporarily bans matatus from expressway" (2022)
- english.scio.gov.cn (Xinhua), "Chinese-built expressway offers Kenya's
  minibuses savings amid high fuel costs" (20 Sep 2023) — source of the
  Super Metro Kitengela–Westlands operational detail
- Kenyans.co.ke, "Super Metro Announces Free Ride as Njugush Matatu
  Debuts Kitengela Route" (15 Apr 2024)
- Super Metro official Facebook post, "New Route Alert: Kikuyu–Westlands–
  Juja–Thika"
- howto.co.ke, "All Mombasa Road Matatu Saccos, Routes and Bus Stops"
- The Star, "Fare normalises in Kitengela as matatu operators call off
  anti-fuel hike protests" (19 May 2026)
- haofinder.com, Utawala neighbourhood guide (operator listing)

## 5. Known limitations

- This is a secondary-source compilation, not a primary GTFS-style
  dataset — it cannot generate new graph edges. It exists purely so the
  chat agent can answer narrative questions about expressway routes
  correctly, the same role `nairobi_route_updates_2024_2026.md` plays
  for the CBD terminus restructuring.
- Coverage is uneven by design — routes with an on-record operator
  interview (Kitengela–Westlands) are documented with confidence;
  routes only rumored or inferred (a possible Utawala–Westlands
  expressway service) are explicitly flagged as unconfirmed rather than
  asserted, to avoid the RAG pipeline confidently answering with
  information that hasn't actually been verified.
- Fares and even specific route offerings change frequently in this
  industry (strikes, fuel price shifts, new SACCO entrants) — treat
  figures here as illustrative of the pattern, not as current pricing.
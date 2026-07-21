# Nairobi Matatu Network — Route Updates (2024–2026)

*Supplementary knowledge base for MatuConnect RAG pipeline. This document covers
route and terminus changes that occurred after the 2019–2020 Digital Matatus GTFS
dataset was collected, and is intended to be embedded into pgvector alongside the
existing Nairobi Expressway knowledge base.*

## 1. December 2024 CBD Route Restructuring (Official Gazette Notice)

On **20 December 2024**, the Nairobi City County Executive Member for Mobility
and Works issued a gazette notice, acting under Section 4(1) of the Nairobi City
County Transport Act, 2020. The notice **degazetted Gazette Notice No. 4479 of
2017** and published a **revised Core Bus Route Network** for all Public Service
Vehicles (PSVs) operating to, from, and within Nairobi City County. The changes
took effect immediately upon publication.

The restructuring introduced **89 new and re-gazetted matatu routes**, primarily
aimed at:
- Reducing the number of routes that transit directly through the CBD
- Cutting travel times between outer estates by allowing cross-town routes that
  bypass the city centre entirely
- Formalizing routes that had evolved informally since the last gazettement in 2017

A January 2025 follow-up report describes the reorganization's target as a
**120-route Core Bus Route Network** overall — the 89 figure refers
specifically to the newly introduced/re-gazetted routes within that total,
not the full network size. Treat "89 new routes" and "120-route network"
as describing the same reform at different levels of granularity, not as
conflicting figures.

### Notable new or re-gazetted routes (examples)
- **Kikuyu Town ↔ Eastleigh Section 3** — via Kikuyu Road, Waithaka, Naivasha Road,
  Kawangware, Ngong Road, Junction Mall, Adams Arcade, Prestige Mall, KMTC,
  Community, Kenyatta Avenue, GPO, Ambassadeur, Ronald Ngala Street, Kariokor
- **Dandora ↔ Kibera/Olympic** (two re-gazetted routes)
- **Eastleigh Main ↔ Yaya Centre**
- **Githurai ↔ Kangemi**
- **JKIA ↔ Westlands** (loop route)
- **Kahawa West ↔ Kangemi**
- **Kariobangi ↔ Kibera/Olympic**
- **Mwiki/Kasarani ↔ Ngumo**
- **Kiambu ↔ Westlands**
- **Kitengela ↔ Westlands** and **Athi River ↔ Westlands**
- **Kitengela ↔ Ruiru** and **Kitengela ↔ Thika**
- **Umoja ↔ Kibera** via Industrial Area
- **Utawala ↔ Kangemi** (two routes)
- **Kileleshwa ↔ Komarock Estate**
- **Riruta Satellite ↔ Kahawa West**
- **Komarock ↔ Highridge**
- **Dandora ↔ Ngumo** and **Dandora ↔ South C**
- **Kariobangi ↔ Madaraka** and **Kariobangi ↔ Upperhill**
- **Huruma ↔ South B**
- **Dagoretti ↔ Bomas**

**Implication for MatuConnect:** these are direct cross-town connections that did
not exist in the 2019 GTFS feed at all — the old network largely required transiting
through the CBD to change routes. Any coverage-gap analysis using only the 2019
dataset will overstate how disconnected certain estate-to-estate pairs are.

## 2. CBD Terminus System (Decongestion Strategy)

Matatus are barred from terminating inside the CBD core. Instead, they terminate
at one of several designated termini on the city's edge, from which passengers
walk into town. As of 2026, the terminus assignments are:

| Terminus | Location | Routes served |
|---|---|---|
| **Green Park Terminus** | Former Lunar Park site, off Uhuru Highway near Railways | Route 111 (Ngong), Route 24 (Karen), Route 125 (Rongai), Route 126 (Kiserian), Lang'ata Road routes, Argwings Kodhek Road routes |
| **Desai & Park Road Termini** | Desai Road / Park Road junction, Eastlands (Ngara area) | Long-distance PSVs from Mt Kenya region (Nyeri, Karatina, Embu), Waiyaki Way / Uhuru Highway / Kipande Road / Limuru Road traffic, upcountry routes to Eldoret/Kisumu |
| **Muthurwa Market Terminus** | Along Landhies Road, Muthurwa area | Jogoo Road and Eastlands routes (Donholm, Kayole, Umoja), Lusaka Road traffic |
| **Bunyala & Workshop Roads Terminus** | Junction of Bunyala Road and Workshop Road, Industrial Area | Mombasa Road routes (South B, South C, Industrial Area, Imara Daima, Athi River, Kitengela, Machakos) |

Within the CBD itself, the traditional boarding stages still function for routes
that are permitted to enter (or for the walk-in leg from the outer termini):

| Stage | Location | Primary routes |
|---|---|---|
| **Railways Terminus** | Haile Selassie Avenue | Ngong, Karen, Rongai, Kitengela, Kiserian, Kikuyu, Kawangware, Kibera — **but see relocation notice in Section 3a** |
| **Kencom Stage** | City Hall Way | Riruta, Kibera (partial), Ngong Road corridor |
| **OTC (Open Trade Centre)** | Haile Selassie Avenue | Buru Buru, Donholm, Pipeline, Kayole, Komarock, Dandora, Umoja |
| **Odeon / Koja Stage** | Moi Avenue | Juja, Ruiru, Kahawa, Githurai, Kenyatta University |
| **Afya Centre Stage** | Aga Khan Road | Upper Hill, Kilimani |
| **Ambassador / Archives Stage** | Moi Avenue, near National Archives | Upper Hill, Kilimani, Kawangware, Kenyatta National Hospital |

**Implication for MatuConnect:** a route advisory answer that only names a route
number (e.g. "take Route 111") is incomplete post-2024 — the useful answer also
names the terminus/stage, since that determines where the commuter actually walks
to/from in the CBD.

## 3a. Railways Terminus Relocation (Nairobi Railway City Project)

The **Nairobi Railway City project** — a flagship UK-Kenya Strategic
Partnership initiative funded via public-private partnership (~KES 30
billion) — is redeveloping the 425-acre Nairobi Central Station site into
an expanded transit hub with new Bus Rapid Transit (BRT) lines and
stations, alongside a revamped commuter rail terminus.

As part of this project, **PSV operators currently based at Railways
Terminus (Haile Selassie Avenue) are being relocated to Green Park
terminus**. Kenya Railways confirmed an agreement with NTSA to move
operators once major construction begins, targeted for the 2025–2026
window as workshop relocation (Makadara) neared completion.

**Practical implication:** this doc's terminus table above lists Railways
Terminus as actively serving Ngong/Karen/Rongai/Kitengela/Kiserian/
Kikuyu/Kawangware/Kibera routes — that reflects the terminus assignment
*before* this relocation completes. Green Park terminus (already listed
separately in the table above, serving Ngong Road corridor routes) is
expected to absorb some or all of this traffic as Railways Terminus is
phased out for PSV use. Treat any answer naming "Railways Terminus" as
potentially transitional rather than a stable long-term fact, and prefer
directing commuters to confirm the current terminus in person during this
transition period.

## 3b. 2025 Permit to Operate Regulations

Nairobi County proposed the **Permit to Operate Regulations, 2025**,
introducing operational (not geographic) changes to how routes function:
- A cap on the number of vehicles permitted per route, reviewed annually
  based on passenger demand or population growth data
- Operators may vary fleet size up to 10% of their authorised route
  capacity without prior approval; beyond that requires County Executive
  Committee (CEC) authorization
- Mandatory cashless fare payment
- Operating permits valid for 5 years, required for all operators
  including SACCOs

These regulations faced organized opposition from matatu operators at a
public meeting held at Green Park Terminus, who criticized the lack of
stakeholder consultation. As of the most recent available reporting, the
regulations' final adopted form and enforcement timeline were still
contested — treat this as an evolving regulatory situation rather than
settled policy.

**Implication for MatuConnect:** this doesn't change route geography or
the graph model, but it's relevant context for narrative questions about
why service levels or fares on a given route might fluctuate — vehicle
count caps directly affect how frequently a route is served.

## 3. Mass Rapid Transit Context (Longer-Term, Background Only)

The Nairobi Metropolitan Area Transport Authority (NaMATA) has gazetted mobility
corridors intended for future Bus Rapid Transit (BRT) lines, which will eventually
run parallel to (and in some cases replace) matatu corridors on the busiest
routes. Relevant lines for context:

- **Line 3**: Githurai → Thika Road → Moi Avenue (CBD) → Kenyatta National Hospital
- **Line 4**: T-Mall → Jogoo Road (~14km)
- **Line 5**: Outering Road corridor

These are **not yet part of day-to-day matatu operations** as of this dataset and
should be treated as background/future-work context rather than active routing
data — do not represent BRT lines as currently operational matatu routes.

## 4. Sources

- Nairobi City County Gazette Notice, 20 December 2024 (Core Bus Route Network,
  degazetting Notice No. 4479 of 2017) — kenyalaw.org
- Eastleigh Voice, "Nairobi announces 89 new matatu routes aimed at improving
  access to CBD" (21 Dec 2024)
- Nairobi Wire, "Nairobi County Introduces New Matatu Routes to Ease CBD Access"
  (23 Dec 2024)
- Kenyans.co.ke, "Govt Announces New Matatu Routes in Nairobi" (21 Dec 2024)
- Nairobi Postal Codes, "Nairobi Matatu Routes 2026: All 133+ Routes, Stages,
  Fares & SACCOs" (published Dec 2025, updated Dec 2025)
- The Star, "Radical transport reforms to tame rogue matatu operators"
  (16 Jan 2025) — source of the 120-route network figure
- The Star, "Matatus to be moved as Sh30bn Nairobi railway project kicks
  off" (22 Feb 2025) — source of the Railways Terminus relocation
- Nairobi Wire, "Nairobi County's New Transport Rules Set to Transform
  Matatu Industry and Improve Commuter Experience" (22 May 2025) —
  source of the 2025 Permit to Operate Regulations
- The Star, "Fare normalises in Kitengela as matatu operators call off
  anti-fuel hike protests" (19 May 2026)

## 5. Known Limitations of This Supplement

- This document is a **manually curated summary**, not a machine-readable GTFS
  feed — it cannot be used to generate new graph edges automatically. It exists
  to let the chat agent answer *narrative* questions ("has this route changed?",
  "where do I catch a matatu to X now?") correctly, while the JGraphT graph
  continues to use the structured 2019 GTFS data for pathfinding and coverage
  analysis.
- The 89 re-gazetted routes are not exhaustively listed here — only the most
  significant/frequently referenced ones. The full table is in the official
  gazette notice.
- Matatu strikes and fare disruptions occur periodically and are not
  captured here as individual events (e.g. a February 2026 strike
  paralyzed Nairobi transport; a May 2026 fuel-price strike temporarily
  tripled Kitengela–Nairobi fares before normalizing). These are
  short-term disruptions, not structural route changes — the chat agent
  should not treat a strike-era fare or a temporary route suspension as
  a permanent change to the network.
- Terminus assignments can shift as construction of new termini completes
  (see Section 3a on the Railways Terminus relocation specifically);
  this snapshot reflects the 2026 status and should be reviewed if the
  system is deployed beyond the academic project timeline.
# Nairobi Transit Knowledge Base
## Supplementary context for MatuConnect RAG pipeline

This document contains curated, verified facts about Nairobi's transit
infrastructure that supplement the structured Digital Matatus GTFS dataset
(2019). It captures post-2019 developments — most notably the Nairobi
Expressway — and known local matatu/SACCO knowledge that is not present
in the structured GTFS graph. Each section below is written as an
independent chunk suitable for embedding into pgvector.

---

## Nairobi Expressway — Overview

The Nairobi Expressway is a 27.1 km toll road connecting Mlolongo to
Westlands, passing through Jomo Kenyatta International Airport (JKIA) and
the Central Business District (CBD). It runs along the Mombasa Road –
Uhuru Highway – Waiyaki Way corridor. The Expressway opened on May 14,
2022, and is operated by Moja Expressway, a subsidiary of China Road and
Bridge Corporation (CRBC), under a 27-year concession agreement.

## Nairobi Expressway — Toll Stations and Interchanges

The Expressway has 11 toll stations and 27 toll plazas along its length.
Major interchanges include: Mlolongo, Syokimau, SGR, JKIA, Southern
Bypass, Eastern Bypass, Cabanas/Outering Road, Haile Selassie, Museum
Hill, The Mall, and Westlands. Tolling uses an origin-destination pricing
model — commuters pay a single fare based on entry and exit points, not
per interchange passed through.

## Nairobi Expressway — Toll Pricing

Toll rates are gazetted by the Kenyan government and were last revised in
December 2023, taking effect January 1, 2024. Short trips between nearby
interchanges (e.g. Mlolongo to Syokimau) cost approximately KES 120.
Longer trips such as the full Mlolongo to Westlands journey cost up to
KES 330–500 depending on vehicle class. Minibuses and matatus fall under
Class 4 (light vehicles, two axles, high bonnet), which is priced higher
than standard Class 3 saloon cars. Payment methods include Electronic Toll
Collection (ETC) via an On-Board Unit (OBU), a Manual Toll Collection
(MTC) card, cash, and mobile money vouchers.

## Nairobi Expressway — Reliability Notes

Traffic on the Expressway grew from approximately 11,000 vehicles/day at
launch in 2022 to around 53,000 vehicles/day by 2024, reflecting strong
commuter adoption. The operator has occasionally suspended tolls
temporarily during flooding or construction disruptions (for example,
in May 2026 during Waiyaki Way upgrade works), offering free passage on
affected sections. This kind of temporary variability is consistent with
the broader informality of Nairobi's transport network.

## Athi River / Greatwall Gardens Corridor

Greatwall Gardens is a residential estate located along Mombasa Road in
Athi River (Mavoko), near the Namanga interchange. It is one of several
planned gated estates in the area, alongside Greenpark and Crystal
Rivers. The Nairobi Expressway's Mlolongo interchange provides direct
access to and from this corridor, making it possible for commuters
travelling from Greatwall Gardens to reach the Expressway without first
navigating into Nairobi's general Mombasa Road traffic.

## Athi River / Greatwall Gardens — Matatu Service

A matatu service known as **Makos** operates from Greatwall Gardens
(Athi River) to Nairobi town and Westlands. Makos matatus access the
Nairobi Expressway by entering through the Mlolongo toll station, and
the service uses the Expressway for the majority of the journey towards
town and Westlands. This route is a practical example of a matatu
service that has adapted to incorporate the Expressway since its 2022
opening — a change not reflected in the 2019 GTFS dataset. Commuters
travelling from Greatwall Gardens can also reach Nairobi via ordinary
(non-Expressway) matatus along Mombasa Road, though these trips take
considerably longer during peak hours, sometimes 90–120 minutes.

## Athi River — Alternative Transit Option (SGR)

The Standard Gauge Railway (SGR) also serves Athi River via its own
station, connecting to Nairobi Terminus in approximately 20 minutes.
This is a faster and more predictable alternative to road transport for
commuters near Athi River, particularly during peak traffic hours.

---

## Notes on Data Provenance

This document combines information verified through web sources (Nairobi
Expressway official site, toll rate guides, and transit news as of 2026)
and local knowledge provided directly by the developer as a Nairobi
resident (e.g. the Makos matatu service details). It is intended to
supplement, not replace, the structured GTFS dataset. As the matatu
network continues to evolve, this document should be periodically
reviewed and updated by an admin user of the MatuConnect system.
# Steam data features — implementation plan

Ground rule: SteamDB (steamdb.info) has no official API; scraping is fragile
and against ToS — skip. Everything below uses Steam Web API, stable store
endpoints, and SteamGridDB.

Already have: SGDB grids/heroes/logos, HLTB stats, GPU compatibility.

## P1 — achievement rarity (fits the new achievements surfaces)
- [x] `GetGlobalAchievementPercentagesForApp` (public, no key): fetch per appId
- [x] "Unlocked by X% of players" in details + % chip in rows
- [x] In-memory cache 24h
- [x] Tests: parsing

## P2 — native square icons for Switch/3DS modes
- [x] SGDB `/icons/steam/<appid>` provider + memory cache
- [x] Native icon in `CompactIconCell` + `DsGameCell`, capsule fallback
- [x] Tests: parse

## P3 — "playing now" chip on game page
- [x] `GetNumberOfCurrentPlayers` chip on game page
- [x] Fetch on game page open

## P4 — review score chip on game page
- [x] appreviews endpoint + sentiment colors
- [x] Review chip on game page
- [x] Tests: buckets + percent

## P5 — game news / patch notes
- [x] `GetNewsForApp` collapsible Updates section on game page
- [x] Collapsible header + ConsoleListRow items

## P6 — video hero on game page (optional, evaluate)
- [x] `appdetails` movies -> VideoHero trailer on game page (pinned, muted)
- [x] Reduced-motion gate (static hero fallback)

Order: P1 -> P2 -> P3 -> P4 -> P5 -> (P6 optional)

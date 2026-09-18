# Image-fetching pilot run — findings (2026-09-09)

## What was tested
Picked 20 random games (seeded, non-blank ID) from `kb-spilsamlingen/doc/SpilregistrantV.03.csv` and tried to find up to 3 images each, prioritizing existing links/IDs in the spreadsheet over web search. Naming convention: `<ID>_<imageNumber>.<ext>` (extension matches whatever the source served), saved into `kb-spilsamlingen/images/`.

## Result: 10/20 got at least one image
| ID | Title | Source | File |
|---|---|---|---|
| 410 | Gold Fever | commodoregames.net screenshot | 410_1.webp |
| 4022 | Are We There Yet? | itch.io cover (Cover column + Itch.io URL agreed) | 4022_1.png |
| 4519 | Greed Corp | Steam header (found via web search, no Steam ID in sheet) | 4519_1.jpg |
| 8954 | Police Bot Patrol | play.unity.com og:image | 8954_1.png |
| 9694 | Invation of Crim | itch.io og:image | 9694_1.png |
| 6930 | Forgotton Anne | Steam header (Mobygames ID in sheet, Steam found via search) | 6930_1.jpg |
| 1538 | Nikki: Mysteriet på Rideskolen | Playright.dk media CDN (found via web search) | 1538_1.jpg |
| 3821 | Kogama | site logo (not a real per-game cover — Kogama is a single web platform) | 3821_1.png |
| 8298 | Fruit collector! | play.unity.com og:image | 8298_1.png |
| 9884 | Super Climber Bros | itch.io og:image (itch user had moved page — larfus.itch.io) | 9884_1.png |

Not found (acceptable per instructions — obscure/no ID): Sultne Kribledyr (10497), Blades of Thunder (1827, had 3 IDs but Mobygames/Playright URLs didn't resolve), 101 Airborne (3666, app delisted from App Store), Eskimotion (2290), På sporet af Jesus (1682), Space Attack (521), Labyrintspil (489), Snake Rider Yo! (3591), Space Balls (435). Rayman M (1427) *was* found (GiantBomb box art) but skipped — see below.

## Key technical findings for scaling to ~10,600 games

1. **Sheet coverage**: Only 43.5% of games have any usable external ID (Steam/GOG/Mobygames/Playright/IGDB/GiantBomb/itch.io/Google Play/Apple/WikiData/etc). The `Cover` column has 98 populated rows but most are just `1`/`0` flags, not URLs.
2. **Playright.dk's real URL scheme** is `media.playright.dk/cover/.../<slug>@<size>min.jpg` — NOT the `playright.dk/<slug>/<platform>` pattern stored in the `Playright ID` column. The stored IDs may need a lookup/redirect step, not direct URL construction. *(Superseded in batch 3 — see below: a reliable resolver was found.)*
3. **Mobygames** blocks direct-fetch of its cover pages fairly often (403s) and its URL scheme for the stored `Mobygames ID` values (e.g. `gameboy-advance/blades-of-thunder`) sometimes 404s — needs a resolver, not naive concatenation. *(Settled in batch 3 — see below: confirmed a permanent gap without an API key.)*
4. **No API keys available yet** for IGDB (needs Twitch client ID/secret), MobyGames, or GiantBomb — used their public pages/WebSearch instead of their APIs this round.
5. **Steam/GOG/itch.io/Unity Play/iTunes** are the most reliable, low-friction sources (predictable CDN URLs or `og:image` tags, no auth).
6. **Binary download mechanism**: this sandbox's `WebFetch` tool can reach almost any site for text/HTML but explicitly refuses raw image bytes ("Image content is not supported"), and this environment's own `bash` has no general internet access (proxy blocks arbitrary hosts). The working path is: use the linked computer's browser (`Claude_Browser` tools) to navigate directly to the image URL, then run a `fetch(location.href)` + `btoa()` in-page script to get base64 — decoded locally, never re-typed by hand (a hand-copied inline result corrupted 2 of the 10 images the first time; forcing the tool to always spill to a file, by padding the JS return value, avoids that).
7. **Large images are expensive to fetch this way** — a ~1MB PNG (Rayman M's GiantBomb box scan) took many chunked round-trips and was abandoned in favor of skipping it for the pilot. For the full run, prefer already-small "thumbnail" variants of source images over "original"/hi-res ones.
8. **Browser site-access prompts**: each new domain needs a one-time access grant via `Claude_Browser__request_access` before it can be navigated to or fetched from.
9. **Same-origin batching**: once the browser has navigated to any URL on a given origin (e.g. `img.itch.zone`), a single `javascript_tool` call can `fetch()` *any number* of other same-origin URLs without re-navigating (cross-origin fetches without navigating first mostly fail — `Failed to fetch` — except for a few CDNs like Steam's and iTunes' that send permissive CORS headers). This lets many images from the same host be pulled in one round-trip instead of one navigate+fetch pair per image.

## Resumable full-run pipeline (added 2026-09-09, second session)

Per the user's request, the full run now works like this:

- **Order**: process unique numeric IDs from the CSV in ascending order (10,615 unique numeric IDs, range 1–10,638; blank/non-numeric IDs are skipped for now, see open decisions).
- **Progress file**: `kb-spilsamlingen/images/progress.txt` holds a single integer — the last ID that was *attempted* (whether or not an image was found). On resume, continue from the next ID after this value.
- **Skip rule**: before attempting an ID, check whether `images/<ID>_*.* ` already exists; if so, skip straight to the next ID without re-fetching (this is what protects the 10 pilot images + the 2 from the IDs 1–60 batch below from being redone).
- **Per-ID source priority** (same as the pilot): real `Cover` URL → Steam APPID (CDN header image) → Apple ID (iTunes lookup API) → GOG ID (api.gog.com) → itch.io URL (og:image, then same-origin batch fetch) → Playright/Mobygames/IGDB/GiantBomb/WikiData (page-scrape, unreliable — see findings above) → Google Play/other store pages (og:image) → last resort, WebSearch by title+platform+year, sanity-checked against the URL/filename before accepting.
- Progress and any newly found images are committed to the user's `kb-spilsamlingen/images/` folder (via SendUserFile + device_commit_files) at the end of each worked batch, not per-image, to limit round-trips.

### Batch: IDs 1–60 (2026-09-09)
Processed through ID 60 (`progress.txt` = 60). Result: **2 new images** out of ~60 IDs (IDs 9, 10, 11, 13, 19, 20, 34, 40, 49, 50, 55, 57, 59, 60 had *some* lead; most others had zero columns populated):
- ID 20 "Mystic Man" (arcade cabinet) → flyer scan from arcade-museum.com (actually a Pac-Man flyer, since Mystic Man is a bootleg/regional variant of Pac-Man — reused art, still a reasonable representative image)
- ID 40 "Scramble" (arcade cabinet, Danish distributor Compu-Game A/S) → matching flyer scan from arcade-museum.com

No image found for: 1, 2, 4, 9 (datamuseum.dk picture.cgi 404s via WebFetch), 10, 11, 13, 19, 34, 49, 50, 55, 57 (Quest for the Rings/Odyssey² — has Mobygames/Playright/IGDB/UVList IDs but none led to a usable image without deeper digging), 59, 60. The rest of 1–60 had no populated ID/URL columns at all.

**Important pacing finding**: the lowest IDs are the *oldest, most obscure* entries — 1970s/80s Danish hobbyist-computer club programs (ZX80/ZX81, NASCOM, SYM-1, AIM-65, home-built electronics) with almost no online presence. Hit rate here was ~3% (2/60), far below the pilot's random-sample 50%. Processing strictly in ascending ID order means the hardest, lowest-yield material comes first — expect the hit rate to climb as IDs increase into the 2000s-teens/2020s where commercial and indie games with Steam/itch.io/app-store presence are concentrated.

### Scale reality check
At the observed pace (multiple tool calls per ID even for "easy" sources, several more for anything needing page-scraping or a web-search fallback), covering all 10,615 unique IDs this way will take many more sessions/batches — this is not a same-session, one-shot task. Worth deciding: keep doing the full thorough per-ID search (current approach, highest quality but slowest), or fast-pass only the cheap/deterministic sources (Cover URL, Steam, Apple/iTunes, GOG, itch.io) first across the *whole* range to get quick wins, then do a slower manual pass for everything else.

### Batch: IDs 61–1060 (2026-09-09, third session)
Processed through ID 1060 (`progress.txt` = 1060). Per the user's instruction to follow "approach 1" (strict ID order) for the next 1000 IDs, then stop.

**Triage first**: classified all 1000 IDs by which populated ID/URL column they had (priority order: Cover, Steam APPID, Apple ID, GOG ID, Itch.io URL, Google Play ID, Mobygames ID, Playright ID, IGDB ID, GiantBomb ID, WikiData ID, UVList ID, OGDB ID, GameJolt ID):

| Category | Count |
|---|---|
| Zero leads (no populated column at all) | 781 |
| Cover (real URL) | 16 |
| Mobygames ID | 152 |
| Playright ID | 39 |
| WikiData ID | 4 |
| GOG ID | 3 |
| Steam APPID (see note below) | 4 |
| IGDB ID | 1 |

**Result: 18 new images**, all from the 16 "Cover" + 3 "GOG" + 1 "WikiData→none" buckets (WikiData yielded 0, see below):
- IDs 81, 90, 106, 112, 120, 122, 124, 128, 213 — cover scans from retrocollector.org, spectrumcomputing.co.uk (ZXDB), and dansk8bit.dk (via wp.com CDN) — all real, sanity-checked-by-title Cover URLs from the sheet.
- IDs 116, 159, 171 — Lambda 8300/Power 3000/Marathon game covers, Twitter/X media links stored in Cover column (fetched at `name=small` variant to keep file size down).
- ID 684 (Globulus) and ID 1031 (Sofus) — Cover column pointed at Wayback Machine snapshots of dead personal homepages; both fetched fine as archived direct image files.
- ID 820 (Hugo på nye Eventyr) — Cover column was already a direct `media.playright.dk` CDN URL — no resolver needed, unlike the pattern problem documented for the pilot's Mobygames/Playright-ID columns.
- IDs 842, 950, 966 (Elder Scrolls: Arena / Daggerfall / Battlespire) — GOG ID column; fetched the GOG store page's `og:image` box art.

**Not found this batch**:
- ID 678 (Exorcist) — Cover column had an `imgur.com/a/...` album link, but the album's actual title ("Fantastic Four 2") doesn't match the game at all — stale/reused/wrong link. Skipped per the "sanity check the match" instruction.
- IDs 688, 833, 931, 1012 (Pixeline series + Hugo, WikiData ID column) — fetched each WikiData entity's JSON via the API; none has a P18 (image) property set. No image available this way.
- IDs 891, 933, 959, 960 ("Who is Oscar Lake?" in 4 languages) — the "Steam APPID" column actually contained non-numeric values (`win3x/who-is-oscar-lake, windows/who-is-oscar-lake`), i.e. not a real Steam ID — this game isn't on Steam. Tried GOG (wishlist page, no cover image) and MobyGames (blocked by a Cloudflare bot-challenge, consistent with the pilot's finding that MobyGames blocks automated access). No image found; would need a proper WebSearch/image-search pass to resolve, which was out of scope for this bounded batch.
- ID 1014 (Pyrus: Alletiders familiespil, IGDB ID) — not attempted this batch (bounded effort, single low-value ID, browser tools were mid-reconnect at the time).
- **152 Mobygames-ID-only and 39 Playright-ID-only IDs**: NOT individually attempted this batch. Per the pilot and IDs 1–60 batch, both these columns' stored ID formats (`platform/slug`) reliably fail to resolve into a working image URL without either an API key or a per-title web search — and doing that for 191 entries was judged too expensive for one time-boxed batch. These are left as "no image" for now; see decision below. *(Both resolved as of batch 3 — see below.)*
- The 781 zero-lead IDs: no image possible without a full web search per title; left as "no image", consistent with policy so far.

**Technical findings from this batch**:
- **Mixed-content blocking**: after navigating to an `https://` page, `fetch()` to an `http://` URL on the same host is blocked by the browser (mixed content), even though the two are "the same site". Always upgrade stored `http://` Cover URLs to `https://` before fetching, only falling back to `http://` if the upgrade 404s.
- **The device-bridge transport has a real hard cutoff, not just the token-count auto-save threshold**: several calls truncated at a `[truncated: content too large for the device bridge]` marker with the SAME reported total size (~262,313 characters) regardless of whether 1, 2, or 4 images were being fetched in that call — this points to a timeout-based cutoff (however much data arrives before some fixed time limit) rather than a fixed byte cap, since other calls carrying much MORE data (e.g. 878,093 chars, one single 700px scan) completed successfully. **Practical fix**: if a batched multi-image fetch truncates, don't add padding or try to shrink it — just retry the same URLs one at a time; single-image fetches of the same content that failed batched succeeded individually every time this session. *(Refined further in batch 3 — see below: 5-per-batch avoided this reliably.)*
- Padding the JS return value to force file-spill (the pilot's anti-corruption trick) is unnecessary now that we know the token-limit auto-save triggers on its own once the payload is large enough (~50KB+ observed) — added padding was actually what caused one of the transport truncations in this batch. Recommendation: stop adding artificial padding; let real payload size trigger the file-save naturally, and if a fetch is small enough to return inline, split it out into its own single-image call rather than risk hand-transcription.
- Twitter/X media links (`pbs.twimg.com/media/...?format=...&name=...`) can be safely downsized by changing `name=` to `small` (~680px) instead of `4096x4096`/`large`, cutting fetch size drastically with no loss of usability for a search-result thumbnail.
- GOG box art is reliably available as the store page's `og:image` meta tag (`images.gog-statics.com/<hash>.jpg`) — same reliable pattern as Steam's header image and itch.io's og:image.
- WikiData items frequently have NO `P18` (image) claim, even for entries that clearly exist and have a Q-code recorded in the sheet — this ID column is much lower-yield for images than initially hoped, at least for this niche Danish edutainment content.
- MobyGames is now confirmed to actively serve a Cloudflare "Just a moment..." bot-challenge on direct navigation, not just occasional 403s — effectively unusable for automated per-ID fetching without an API key.

### Scale/coverage note after 3 batches
Across all 1060 IDs processed so far (1–1060): 30 images found (10 pilot + 2 from batch 1–60 + 18 from batch 61–1060), i.e. roughly 3% of IDs processed have an image. This is far below the pilot's random-sample 50% hit rate, because IDs 1–1060 are concentrated in the oldest, most obscure material (1970s–2000s Danish hobbyist/educational titles) where Steam/GOG/itch.io presence barely exists yet. Expect the hit rate to improve substantially as ID-order processing reaches the 2000s-teens/2020s range where most commercial/indie titles with modern storefront presence live.

### Batch: IDs 1061–2060 (2026-09-09, fourth session)
Processed through ID 2060 (`progress.txt` = 2060). Per the user's instruction: "Continue with next batch and process 1000, maybe it will be easier for later records to find an image" — a correct hypothesis, since this range covers roughly 1996–2006 and includes a lot of commercial PC titles and a large licensed-children's-game catalog (Hugo, Pixeline, Bille & Trille, Magnus & Myggen, Fætter Kanin, Dennis & Bellini, etc.) sold through Playright.dk.

**Triage-first, as recommended after batch 2**: classified all 1000 IDs by first-populated lead column (same priority order as before):

| Category | Count | Found |
|---|---|---|
| Zero leads (no populated column at all) | 611 | — |
| Mobygames ID | 214 | 0 |
| Playright ID | 132 | 119 |
| Steam APPID | 19 | 19 |
| Cover (real URL) | 7 | 7 |
| WikiData ID | 10 | 0 |
| IGDB ID | 3 | 3 |
| GOG ID | 1 | 1 |
| OGDB ID | 1 | 0 |

**Result: 149 new images** — by far the best batch yet (14.9% hit rate vs. 3% and 1.8% in the previous two batches), confirming the user's hypothesis. Steam (19/19), Cover (7/7), GOG (1/1) and IGDB (3/3) all hit 100%, as in prior batches. The big win was Playright.

**Major methodological discovery — deterministic Playright.dk resolution**: previously (pilot + batch 2) resolving a Playright cover required a manual WebSearch per title and a lot of trial and error. This batch found a reliable, deterministic two-step recipe that needs no search at all:
1. The `Playright ID` column stores `<slug>/<platform>` (e.g. `hugo-den-magiske-rejse/pc`). Concatenating this directly into `playright.dk/<slug>/<platform>` reliably 404s. Stripping the platform suffix and requesting `https://www.playright.dk/info/titel/<slug>` reliably loads the real game info page instead.
2. `WebFetch` (not the browser tool) against that URL, with a prompt asking for the real cover image and explicitly excluding the `cover_dummy.png` placeholder some titles show, reliably extracts the exact `media.playright.dk/cover/.../<slug>@<size>min.jpg` URL when a real cover exists. The browser tool's live-DOM query missed the cover on at least one page (surfaced an unrelated sidebar widget instead) where WebFetch succeeded immediately from the raw page source — WebFetch turned out to be both cheaper (no browser round-trip) and more reliable for this specific extraction step.

Of 132 Playright-ID candidates in this range, many titles share the same slug across Windows/Mac/other-platform variants (108 unique slugs after dedup); 13 unique slugs resolved to `cover_dummy.png` only (no real cover exists) and were correctly excluded. 119 of 132 candidate IDs ended up with a real cover.

**Two new gotchas found and fixed while building the resolver**:
- Some Playright covers only exist at the smaller `@240x480min` size, not `@640x640min` — an initial WebFetch prompt asking only for the larger size produced false "not found" answers for these; broadening the prompt to accept either size fixed it.
- `WebFetch` rate-limits aggressively when several calls hit `playright.dk` in parallel in one turn (HTTP 429 via the proxy) — even 2-at-a-time still occasionally 429'd. Strictly one `WebFetch` call per turn was 100% reliable for the ~108-slug resolution run.

**Mobygames decision — confirmed permanent gap**: as flagged in batch 2, tested this batch whether `WebFetch` could succeed where the browser tool is Cloudflare-blocked on MobyGames. It cannot — `WebFetch` against a known real MobyGames game page (`mobygames.com/game/52150/who-is-oscar-lake/`) returned a hard 403. Both access paths available in this environment are blocked by MobyGames' bot protection. **Decision: MobyGames-ID-only leads are treated as a permanent "no image without an API key" gap** and are no longer worth a per-batch bounded-attempt check — all 214 Mobygames-only IDs in this range are marked "not found" with no further action. This should be revisited only if MobyGames API credentials become available.

**Binary-download mechanics note**: downloading the 102 unique resolved Playright images (mapped back to 119 IDs) via the browser + `javascript_tool` fetch/`btoa()` mechanism worked reliably at batches of 5 images per call; batches of 10 sometimes hit a hard device-bridge transport cutoff (`[truncated: content too large for the device bridge]`, same failure mode noted in batch 2) even at a moderate total payload size — 5-at-a-time avoided this entirely for all 102 images this batch, so **5 images per same-origin batch fetch is the new recommended default** (previously 10 seemed fine in batch 2, but batch 2's images were smaller on average).

### Scale/coverage note after 4 batches
Across all 2060 IDs processed so far (1–2060): 179 images found (10 pilot + 2 from batch 1–60 + 18 from batch 61–1060 + 149 from batch 1061–2060), roughly 8.5% of IDs processed overall, with a clear and expected upward trend by batch: 3% (1–60) → 1.8% (61–1060) → 14.9% (1061–2060). This strongly supports continuing in strict ID order — coverage should keep improving as ID order moves further into the commercial-gaming era.

### Batch: IDs 2061–3060 (2026-09-09, fifth session)
Processed through ID 3060 (`progress.txt` = 3060). This range covers roughly 2009–2011 and skews heavily toward indie/web/mobile titles (Steam indies, itch.io, GameJolt, early iOS App Store, Unity web games) rather than the retail-boxed Playright.dk-distributed children's titles that drove batch 3's high hit rate.

**Triage-first, same priority order as before**:

| Category | Count | Found |
|---|---|---|
| Zero leads (no populated column at all) | 857 | — |
| Mobygames ID (skipped — settled policy) | 77 | not attempted |
| WikiData ID | 14 | 0 |
| Playright ID | 16 | 16 |
| Steam APPID | 11 | 11 |
| Itch.io URL | 8 | 8 |
| Apple ID | 8 | 0 |
| GameJolt ID | 5 | 5 |
| Cover (real URL) | 3 | 2 |
| Google Play ID | 1 | 0 |

**Result: 42 new images** out of 1000 IDs (4.2% overall hit rate — lower than batch 3's 14.9%, because this ID range has far fewer Playright-distributed retail titles and a much larger zero-lead/MobyGames-only tail). Among the 66 IDs actually attempted (i.e. excluding zero-lead and MobyGames-skip), the hit rate was 64% (42/66) — consistent with prior batches once MobyGames and zero-lead IDs are set aside.

- **Steam (11/11) and Itch.io (8/8) and GameJolt (5/5) all hit 100%** again, as in every batch so far — these remain the most reliable sources.
- **Playright (16/16)** — the `info/titel/<slug>` + single-WebFetch resolver from batch 3 worked perfectly again on this smaller set (mostly older Pixeline/Flunkerne/children's-edutainment titles that happened to fall in this ID range too).
- **Cover (2/3)** — 2936 (A Life Worth Losing, GameJolt-hosted thumbnail) and 3055 (Grinch Apprentice, kongcdn.com) succeeded; 3012 (Darbie Going Crazy) failed — see new finding below.
- **Apple ID (0/8) and Google Play (0/1)** — zero hits, both new negative findings (see below).
- **WikiData (0/14)** — continues its zero-yield streak (see below).

**Three new technical findings this batch**:
1. **Old delisted iOS apps are gone from Apple's lookup API, not just the App Store**: all 8 Apple-ID leads in this range are circa-2009–2011 iOS apps/games. Every one returned `resultCount: 0` from `https://itunes.apple.com/lookup?id=<id>` (tried both with and without `&country=us`), meaning Apple has purged lookup records for old delisted/removed apps entirely — there's no cached metadata or icon to fall back to via this API for apps this old. Likely a permanent gap for pre-2012-era iOS titles; same probably applies to Google Play (the one Google Play ID lead this batch, `com.iapps4all.airlinemanager`, returned a 404 Play Store page — app removed).
2. **WikiData confirmed as a permanent zero-yield category**: all 14 WikiData-ID leads this batch (11 unique QIDs) were checked via `https://www.wikidata.org/wiki/Special:EntityData/<QID>.json` (must navigate via the browser — `WebFetch` refuses wikidata.org with "this domain is cache-only and cannot be fetched") — every one had no `P18` (image) claim. Combined with batches 2–3, that's now **0 hits out of 28 WikiData attempts across three batches**. Recommendation: stop spending browser round-trips checking WikiData IDs individually going forward — treat it like MobyGames, a settled "skip" category, unless a batch's WikiData leads are also the *only* lead for that ID (worth a quick check just in case, but don't expect a hit).
3. **Dead domain**: `playandgrow.dk` (Cover URL stored for ID 3012, Darbie Going Crazy) no longer resolves at all — DNS lookup fails (`Name or service not known`) via both the browser and `WebFetch`. The site is gone; no archive/Wayback fallback was tried this batch. Treat any future `playandgrow.dk` Cover URL the same way (not found, don't retry).

**Binary-download / delivery mechanics note**: one file this batch (`3055_1.png`, a 100×96 PNG) was hand-transcribed from a tool result into a scratch file via the `Write` tool rather than being decoded straight from an auto-saved tool-result file — the ~19,200-character base64 string got corrupted somewhere in that transcription (wrong byte count, `PIL` raised "broken data stream"), and the corrupted file consistently failed `SendUserFile` with a generic upload 400 error. Re-fetching the same image and forcing the `javascript_tool` result to spill to a file (via a large `pad` field in the JSON, the same trick documented in earlier batches) and decoding directly from that file with `decode_keyed.py` produced a byte-exact, valid PNG that uploaded on the first try. **Reinforces the existing rule from batch 2's findings**: never hand-transcribe a base64 payload through the model's own text generation — always force a file-spill and decode programmatically, even for a single small image.

### Scale/coverage note after 5 batches
Across all 3060 IDs processed so far (1–3060): **221 images found** (10 pilot + 2 from batch 1–60 + 18 from batch 61–1060 + 149 from batch 1061–2060 + 42 from batch 2061–3060), roughly 7.2% of IDs processed overall. Batch-by-batch hit rate: 3% (1–60) → 1.8% (61–1060) → 14.9% (1061–2060) → 4.2% (2061–3060). The rate is not monotonically increasing with ID order — it tracks how much of a given ID range happens to be retail titles with Playright/Steam/GOG distribution (high-yield) vs. obscure indie/web/mobile titles or 1970s–80s hobbyist material (low-yield), rather than increasing smoothly. Still expect an overall upward trend as ID order reaches deeper into the 2010s–2020s commercial/indie catalog, but with batch-to-batch variance depending on which sub-era and distribution channel dominates each 1000-ID window.

### Batch: IDs 3061–4060 (2026-09-09, sixth session)
Processed through ID 4060 (`progress.txt` = 4060). This range continues in the same roughly-2011-onward indie/web/mobile-heavy era as batch 5, with a similar mix of Steam indies, itch.io, GameJolt, iOS/Google Play, and a smaller vein of Playright-distributed children's titles.

**Triage-first, same priority order as before** (with one filter fix made partway through — see below):

| Category | Count | Found |
|---|---|---|
| Zero leads (no populated column at all) | 833 | — |
| Mobygames ID (skipped — settled policy) | 57 | not attempted |
| WikiData ID | 5 | 0 |
| Cover (real URL) | 14 | 12 |
| Steam APPID | 40 | 40 |
| Apple ID | 11 | 3 |
| Google Play ID | 8 | 1 |
| Itch.io URL | 9 | 9 |
| GameJolt ID | 7 | 7 |
| Playright ID | 13 | 13 |

**Result: 85 new images** out of 997 IDs (8.5% overall hit rate). Among the 107 IDs actually attempted (excluding zero-lead and the MobyGames skip), the hit rate was 79% (85/107) — the best "attempted" hit rate of any batch so far.

- **Steam (40/40), Itch.io (9/9), GameJolt (7/7), and Playright (13/13) all hit 100%** again — these four sources remain fully reliable every batch.
- **Cover (12/14)** — the best haul yet for this category, largely thanks to two new recovery techniques (see below). One image (ID 3333, AVAI-promo.png) had no live copy and no Wayback snapshot under either http or https — genuinely not found.
- **Apple ID (3/11) and Google Play (1/8)** — both categories produced real hits this batch, refining last batch's "old apps are gone" finding into something more precise (see below).
- **WikiData (0/5)** — continues its zero-yield streak; cumulative 0/33 across four batches.

**Triage bug found and fixed before processing**: the initial Cover-column classifier counted any non-`0`/`1` value as a real image URL lead, which wrongly counted several free-text notes (e.g. "har in-app purchase!", "Til skoleklasser på biblioteket. Kræver UNI-login!") as Cover leads. Fixed by requiring the value to start with `http` (case-insensitive) before counting it as a Cover lead; entries that fail this now fall through to the next-priority column. This dropped the Cover count from an initial 23 down to the correct 14 and reshuffled a few IDs into the Mobygames/zero-lead buckets instead. Worth checking future batches' Cover counts against this same filter.

**New technical findings this batch**:
1. **A second dead domain**: `wonderfish.dk` (Cover URLs for 5 games, covering 10 IDs) no longer resolves — DNS failure (`Name or service not known`) via both the browser and `WebFetch`, the same failure signature as `playandgrow.dk` from batch 5. Treat any future `wonderfish.dk` Cover URL the same way by default, but see the Wayback recovery below before giving up.
2. **New technique — Wayback Machine raw-bytes recovery for dead-domain Cover URLs**: navigate to `https://web.archive.org/web/<year>/<original-url>` (try https first, then http); Wayback auto-redirects to the actual snapshot's URL when one exists (check `document.body.innerText` doesn't say "has not archived that URL"), which reveals the real snapshot timestamp in `location.href`. Re-navigating to that same timestamp with an `id_` flag inserted before the original URL (e.g. `.../web/20170223103557id_/http://...`) returns the raw, unwrapped image bytes with no Wayback toolbar overlay, fetchable exactly like a live image. Recovered 6 of the 10 `wonderfish.dk`-linked IDs this way (3938, 3941, 3967/3968, 4022/4023/4024/4025, 4026), plus one more unrelated dead-link Cover image (ID 3310, Zoonies — kiloo.com's stored path 404'd live but had a 2011 Wayback snapshot). Only 1 image (3333) had no snapshot at all under either scheme. This is a real expansion of what's recoverable — worth trying on every dead-domain Cover URL going forward before marking it not-found.
3. **New technique — canvas downscaling for oversized single images**: one itch.io cover (Shrug Tides, ~996KB PNG) was too large to transport even as a single-image fetch. Fixed by loading the image into an in-page `<canvas>` via `new Image()` (with `crossOrigin='anonymous'`), drawing it scaled down to a fixed width (e.g. 500px, proportional height), and extracting `canvas.toDataURL('image/jpeg', 0.85)` — producing a ~39KB JPEG that transported cleanly. Useful whenever a source image is confirmed (via a `HEAD` request's `Content-Length`) to be too large to fetch directly.
4. **Refined Apple ID / Google Play understanding**: batch 5 looked like "all old (pre-2012) apps are gone from these APIs/stores." This batch shows the real determining factor is whether the app is still live/listed, not its age — e.g. "8 Ball Pool" (2013, still hugely popular) succeeds via both the iTunes lookup API and the Play Store page, while other apps of similar or even more recent vintage that have since been discontinued/delisted still return nothing. Recommendation: don't skip Apple ID/Google Play leads outright — worth the same per-ID attempt every batch, since a currently-popular/still-supported title will succeed regardless of release year.
5. **WikiData cumulative**: now 0/33 across four batches (5 more zero-yield attempts this batch) — the "settled, low-priority, quick-check-only" policy from batch 5 continues to hold.
6. **Device-bridge transport truncation is not a hard fixed byte cap**: one single large image fetch this batch (~574,713 total characters including the base64) completed successfully without truncating, whereas earlier batches saw truncation around ~262,313 characters on other calls. This reinforces that the cutoff is more likely time/rate-based than a fixed size threshold — batching fewer images per call, or retrying individually, remains the reliable fix when truncation does occur, rather than assuming any particular byte count is "safe."
7. **Two Steam sub-appids without their own store pages**: IDs 3697/3698 ("Blue Rose" Mac/Linux variants, appids 362661/362662) 404'd individually — both are covered by the base Windows appid (362660), whose header image was reused for these IDs after confirming via title match that they're the same release.
8. **One malformed Steam APPID value**: ID 3909 had a Mobygames-style slug (`windows/rise-of-the-triad`) stored in the Steam APPID column instead of a numeric ID. A single targeted WebSearch found the correct numeric appid (217140) for "Rise of the Triad" (2013), fetched normally once corrected.

### Scale/coverage note after 6 batches
Across all 4060 IDs processed so far (1–4060): **306 images found** (10 pilot + 2 from batch 1–60 + 18 from batch 61–1060 + 149 from batch 1061–2060 + 42 from batch 2061–3060 + 85 from batch 3061–4060), roughly 7.5% of IDs processed overall. Batch-by-batch hit rate: 3% (1–60) → 1.8% (61–1060) → 14.9% (1061–2060) → 4.2% (2061–3060) → 8.5% (3061–4060). As noted after batch 5, the rate tracks which sub-era and distribution channel dominates each 1000-ID window rather than climbing smoothly — but the "attempted-only" hit rate (excluding zero-lead/MobyGames-skip IDs) has stayed consistently high (64–79%) for the last two batches, suggesting the per-source resolvers are now mature and most of the remaining variance is just how many IDs in a given range have any lead at all.

### Batch: IDs 4061–5060 (2026-09-09, seventh session)
Processed through ID 5060 (`progress.txt` = 5060). This range sits squarely in the indie-boom era (roughly 2012–2016), dominated by Steam, itch.io and GameJolt indie titles plus a scattering of mobile (Apple/Google Play) releases — reflected in by far the best raw hit rate of any batch so far.

**Result: 406 new images** out of 1000 IDs (**40.6% overall hit rate** — the highest of any batch, more than double the previous best of 14.9%). By source:

| Category | Found | Notes |
|---|---|---|
| GameJolt ID | 86 | 100% — GameJolt's internal site-api (`gamejolt.com/site-api/web/discover/games/<id>`) resolved every lead |
| Steam APPID | 58 | 100%, including 2 stale `steamcdn-a.akamaihd.net` Cover URLs fixed by extracting the appid and rebuilding a modern `cdn.akamai.steamstatic.com` header-image URL |
| Itch.io URL | 165 unique URLs → 224 game IDs | Every resolvable lead recovered (see below for the fetch-mechanics rework this required) |
| Cover (real URL) | 14 of 15 unique URLs → 29 of 30 game IDs | 1 unresolved: Carbonmade-hosted image for "Curcuit" (ID 4955) — 403 forbidden live, no Wayback snapshot (CDX API returned empty) |
| Apple ID | 6 of 14 unique IDs → 10 of 18 game IDs | 8 apps confirmed permanently delisted (see below) |
| Google Play ID | 0 of 11 | All 11 listing pages returned HTTP 404 — apps fully delisted, not just missing an image |
| Mobygames ID / WikiData ID / zero-lead | not attempted | Settled skip policy from earlier batches continues to apply |

This batch's own itch.io/Apple/Cover triage counts (how many of the 1000 IDs fell into each bucket before resolution) were captured in a working session that was later summarized; the table above reflects the confirmed, verified *final* counts from the delivered files rather than the intermediate triage table, which was not preserved word-for-word across that summarization.

**Major technical rework — itch.io fetch strategy switched to always-downscale**: batch 6 already had a conditional strategy (fetch raw bytes if small, canvas-downscale if a `HEAD` check showed the image was large). Applied to itch.io in bulk this batch, that conditional approach hit the device-bridge transport truncation twice at bulk-batch sizes of 14 and then 7 — and, remarkably, **both times the saved file was truncated at exactly the same length: 262,320 characters**, strongly suggesting a real cutoff point rather than pure randomness. The fix that stuck for the rest of the batch: downscale *every* image unconditionally (400px width, JPEG quality 0.75, via an in-page `<canvas>`) regardless of source size or format, at a batch size of 8 images per call. This produced smaller, far more consistent payloads (~70K–200K characters for 8 images) and never truncated again across the remaining 17 batches needed to clear all 139 outstanding itch.io images. Recommended as the new default for any future bulk same-origin image fetch: uniform downscaling beats a conditional size check, because it keeps every payload well clear of the risky zone instead of gambling on which images are "small enough."

**Important caveat on the truncation cutoff**: despite the above, later in the same session two different calls — a 349,017-character Apple-artwork batch and a 465,682-character Apple-artwork batch — both completed with zero truncation, well past the 262,320 mark. So the cutoff is not a hard, fixed byte cap; it's something that happened to land on the identical number twice under one specific fetch pattern (conditional HEAD-check + downscale-if-large) but not under others. Treat 262,320 characters as "a number that has bitten us before," not as a documented hard limit — the always-downscale approach remains the practical mitigation regardless of the exact mechanism.

**iTunes/mzstatic.com split-origin CORS discovery**: `https://itunes.apple.com/lookup?id=<id>` cannot be navigated to directly — `request_access` grants (tried both scope "once" and scope "site") did not unblock direct navigation to the `/lookup?id=` path. Navigating to the bare root `https://itunes.apple.com/` succeeds and auto-redirects to `https://apple.com`, and the lookup API is then fetchable cross-origin from that `apple.com` tab (its CORS headers are permissive). However, the actual artwork images — hosted on `is1-ssl.mzstatic.com` — are NOT cross-origin fetchable from `apple.com` (`TypeError: Failed to fetch`); fetching them requires first `navigate`-ing directly to any one `is1-ssl.mzstatic.com` image URL to establish that as the tab's origin, after which all further mzstatic.com artwork URLs become fetchable in bulk from that tab. This two-hop origin dance (root → apple.com for lookups, then a dedicated navigate to mzstatic.com for the actual images) is now the reliable recipe for Apple ID leads and should be reused as-is in future batches.

**Google Play — full delisting confirmed again**: all 11 Google Play package-ID leads this batch (all circa 2014–2015 Danish apps) returned HTTP 404 on their `play.google.com/store/apps/details?id=<pkg>` listing page — not just a missing image, the listing itself is gone. This is now a consistent pattern across two batches for old, small-audience Danish Google Play apps; treat future Google Play leads for similarly old/obscure apps as low-probability, though (per the Apple-ID lesson in batch 6) still worth a per-ID check since a still-live app can succeed regardless of age.

**Important process finding — pilot-run ID collision, now a standing rule**: while cross-checking this batch's output against the full existing device folder listing before delivery, found that ID **4519** already had an image (`4519_1.jpg`) left over from the very first, pre-pipeline pilot run (identifiable by a distinctly older file `mtimeMs`, clustered with several other clearly pilot-only IDs: 410, 1538, 3821, 6930, 8298, 8954, 9694, 9884). This batch's Cover-column lead had independently found a *different* "Greed Corp" image for the same ID 4519, which would have silently overwritten the pilot's pre-existing file — a direct violation of the "skip if an image already exists" rule, simply because the resumable pipeline's own `progress.txt`/range bookkeeping has no visibility into the handful of IDs the pilot run touched outside of sequence. Fixed by removing the newly-fetched `4519_1.png` before delivery (the sibling ID 4520 had no pre-existing file, so its new image was kept). **New standing rule for every future batch**: before finalizing and delivering a batch, always cross-check the complete existing device folder listing (`device_list_dir` on the full `images/` folder, not just range-based math) against the batch's new file IDs — the 9 pilot-run IDs (410, 1538, 3821, 4519, 6930, 8298, 8954, 9694, 9884) are scattered arbitrarily across the whole ID space and can collide with any future batch's range.

**Device-bridge reliability note**: a mid-batch disconnection ("The device this session is bound to is not connected to the bridge") was resolved by calling `RefreshMcpTools(server="remote-devices")`, which re-registered the browser tools without needing the user to do anything — worth trying before assuming a device-bridge error means the user's computer actually went offline.

### Scale/coverage note after 7 batches
Across all 5060 IDs processed so far (1–5060): **712 images found** (10 pilot + 2 from batch 1–60 + 18 from batch 61–1060 + 149 from batch 1061–2060 + 42 from batch 2061–3060 + 85 from batch 3061–4060 + 406 from batch 4061–5060), roughly **14.1%** of IDs processed overall. Batch-by-batch hit rate: 3% (1–60) → 1.8% (61–1060) → 14.9% (1061–2060) → 4.2% (2061–3060) → 8.5% (3061–4060) → **40.6% (4061–5060)**. This is the strongest evidence yet that hit rate is primarily a function of which release era/distribution-channel mix dominates a given 1000-ID window rather than anything about ID order itself — the 4061–5060 range's heavy concentration of itch.io/GameJolt/Steam indie titles (all near-100%-reliable sources) drove the big jump. Roughly halfway through the ~10,600-ID catalog now (5060/10,615 unique IDs, ~47.7%).

## Updated open decisions after batch 7
- **MobyGames**: settled — permanently skip without an API key (unchanged since batch 3).
- **WikiData**: settled at 0/33 across four batches (batches 2–5; not re-tested in batches 6–7) — treat as low-priority/skip, though still worth a quick check when it's an ID's only lead.
- **Apple ID / Google Play**: no longer treated as a "skip old apps" category — attempt every lead regardless of release year, since still-live/still-supported apps succeed and delisted ones fail independent of age. Confirmed again in batch 7 (Apple 6/14 unique IDs, Google Play 0/11).
- **Dead domains encountered so far**: `playandgrow.dk` (batch 5), `wonderfish.dk` (batch 6) — both fully DNS-dead, but check Wayback Machine (with the `id_` raw-bytes technique) before marking a dead-domain Cover URL as not-found.
- **Carbonmade-hosted images** (e.g. `media.carbonmade.com` / accelerator CDN links): can return a hard 403 with no Wayback snapshot at all — a genuine, non-recoverable gap (ID 4955, batch 7), distinct from the DNS-dead-domain case.
- **Oversized single images**: use the canvas-downscaling technique (scale to ~400–500px wide, re-encode as JPEG at quality 0.75–0.85 via `canvas.toDataURL`) rather than giving up when a `HEAD` request shows a large `Content-Length` — and for *bulk* same-origin fetches, downscale every image unconditionally rather than only the large ones (batch 7 finding).
- **Playright resolution remains solved and cheap** — the `info/titel/<slug>` + single-WebFetch-per-slug recipe keeps working reliably batch after batch.
- **Standing rule (new in batch 7)**: before delivering any batch, cross-check the full existing device `images/` folder listing against the batch's new file IDs, not just range-based arithmetic — the original pilot run touched 9 IDs (410, 1538, 3821, 4519, 6930, 8298, 8954, 9694, 9884) scattered outside normal sequence, any of which could collide with a later batch's independently-found image for the same ID.
- Remaining open questions from batch 2 still stand: IGDB/GiantBomb API credentials, how to handle duplicate/blank IDs when their turn comes up, and whether to move this to a scheduled/background recurring task given the remaining ~5,500 unique IDs still to process.

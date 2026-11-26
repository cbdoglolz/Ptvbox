# PTVBOX / PCBOX Media Matching Audit

Date: 2026-11-25  
Scope: `D:\Ptvbox` source, with `D:\tvboxwin` used as read-only PCBOX reference.  
Status: phase-0 audit only. No matching algorithm, database migration, external provider adapter or user data was changed for this audit.

## 1. Evidence and limitations

- Confirmed by source inspection: source-site matching, rating-provider matching, database schema, negative/manual links, artwork persistence and candidate probing.
- Confirmed by existing regression tests: title identity and season tests, protected TV identity tests, metadata/store tests and prior PCBOX matching regressions.
- Not claimed as this round's device proof: real search success, playback, ATV focus behavior or provider accuracy on the owner's TV box.
- The current PTVBOX implementation is materially simpler than PCBOX. Similar names and compatible-looking tests do not make it a parity implementation.

## 2. Current source-site matching flow

```text
Work selected
  -> scan()
  -> enabled/searchable sites, ordered by group priority
  -> per-site search(work.title)
  -> optional original title + first two aliases
  -> Identity.matches(Work, Vod)
  -> choose up to 5 site rows
  -> site detail()
  -> enumerate roads and requested episode
  -> probe media with HTTP Range 0-4095
  -> Candidate
```

Relevant code:

- `app/src/main/java/com/cbdog/ptvbox/PtvModel.kt`: `scan()`, site search, alias fan-out, candidate sorting and probing.
- `app/src/main/java/com/cbdog/ptvbox/Models.kt`: `Identity.matches`, title normalization and episode selection.
- `app/src/main/java/com/cbdog/ptvbox/Network.kt`: site parsing and probe.
- `app/src/main/java/com/cbdog/ptvbox/EpisodeCatalogRepository.kt`: detail-page catalog search.

Hard rules already present:

- Reject obvious previews/commentary/featurettes by title keywords.
- Reject a conflicting four-digit year.
- For TV, reject an explicit conflicting season; season 2+ does not accept a title with no season marker.
- Accept a source row only when a normalized local title/original/alias equals the normalized source title.
- Probe before exposing the candidate as playable.

## 3. Current rating-provider matching flow

```text
Work
  -> manual/preference link stored in SharedPreferences
  -> provider-native ID when the Work already belongs to that provider
  -> manual TMDB series ID for another provider
  -> first three title query variants
  -> provider search candidates
  -> RatingMatch055 exact normalized title + kind/year/season checks
  -> optionally enrich up to four likely candidates
  -> write only when exactly one candidate matches
  -> cache for 30 minutes and persist manual link
```

Relevant code:

- `Metadata.ratingWork()` and `ratingWorkUncached()` in `Network.kt`.
- `RatingMatch055.kt`.
- `Presentation050.kt`, `PresentationQueue050` and `presentation_v5`.
- `ProviderRatings.kt` for manual match, clear match and immediate UI update.
- `DetailMetadata040.kt` and `TmdbEpisodes.kt` for detail, people and episode metadata.

Current hard rules:

- TMDB matching requires a configured credential.
- `rating_link_<work>_<provider>` stores a numeric external ID.
- `rating_link_<work>_<provider> = none` suppresses automatic rematching.
- Exact normalized title equality is required; no fuzzy acceptance.
- Kind, year and explicit season conflicts are checked.
- More than one accepted candidate produces no automatic match.
- Manual match is applied immediately and invalidates the artwork presentation cache.

## 4. Current data model

### Work identity

`Work` is a flat immutable value containing:

- Provider, provider ID and `identityKey`.
- Title, original title, aliases, kind, season and catalog season.
- Year and release date.
- Poster, backdrop, overview, rating and votes.
- Genres, people and `WorkFacts`.

The effective key is `identityKey`, otherwise `provider:kind:id`. This prevents equal numeric IDs from different providers from colliding. It does not yet provide an internal media UUID as the sole identity, and it does not store a separate `source_item_id`.

### SQLite tables

Library schema version 3 creates:

- `media_v2(k PK, media_id UNIQUE, j JSON)`
- `aliases_v2(legacy, k)`
- `favorites_v2(k)`
- `state_v2(k, state)`
- `episodes_v2(k, episode_id UNIQUE, media_key, season, name)`
- `progress_v2(k, work, updated, position_ms, duration_ms, j)`
- `hidden_v2(k)`
- `catalog_v2(work, j)`
- `episode_sources_v2(work, group_key, name, episode_key)`
- `changes_v2(revision, device_id, session_id, kind, entity, updated)`
- `migration_issues(table_name, legacy_key, reason)`
- `episode_flags_v3(k, favorite, watched)`

`media_id` is a stable UUID, but external IDs are embedded in the `Work` JSON rather than normalized into a separate indexed table. There is no alias-language/source/type/confidence table, no relation graph, no source-item table, no candidate-score table and no match-history table.

### Existing external IDs

- `Work.id` plus `Work.provider` is the provider-native identity.
- `WorkFacts.douban` can retain IMDb ID exposed by Douban.
- `WorkFacts.tmdb` can retain TMDB and IMDb IDs.
- AniList and Bangumi IDs are currently encoded as anime source provider IDs or aliases, not a complete cross-provider mapping table.
- No unified Douban, TMDB, IMDb, AniList, MAL and Bangumi ID registry exists.

## 5. Current caches and correction memory

- `presentation_v5`: hashed presentation signature, derived ratings, Chinese poster, logo and backdrop, cached for up to 24 hours.
- In-memory `ratingCache057`: provider result cache for up to 30 minutes.
- `rating_link_*`: persistent numeric manual link or `none` negative lock.
- `tmdb_link_<work>`: TMDB series ID used by episode lookup.
- `aliases_v2`: migration mapping from legacy keys to current Work keys; it is not a multilingual alias index.
- Anime season JSON: 12-hour local cache keyed by year and quarter.

Manual links do not record source item, matched alias, score breakdown, rule version, actor or history. A manual correction is therefore durable but not auditable beyond its final value.

## 6. Current failure classes

### Confirmed by code

1. Source titles are accepted only by exact normalized equality. Minor punctuation or alternate translation differences can produce a complete miss.
2. Title variants are capped to two aliases during scan, and rating searches default to three names.
3. Roman numerals and `第2期`, `Part II`, `Cour`, installment and movie numbering are not modeled separately.
4. `Identity.base()` removes season markers but does not safely normalize all Roman/Unicode/full-width variants.
5. Provider matching requires exact title equality; it has no scored fallback or top-1/top-2 margin.
6. Cross-provider links are not linked if only one provider exposes a usable external ID.
7. Search returns only the first five matching source rows per site. This can hide a preferred version when a site returns many same-title variants.
8. `PresentationQueue` intentionally refuses source-site works; source-site cards therefore depend on display metadata available before normalized cross-provider linking.
9. Anime Chinese enrichment is a best-effort exact/containment supplement. If Bangumi returns a different original name, sparse infobox, or the candidate is not in the requested page, the poster/title can remain Japanese.
10. A source item that correctly supplies roads but reports an empty or noncanonical episode name can lose playback because episode selection relies on title/number parsing.

### Plausible and requiring a real request trace

- The three “药屋少女” source rows may be different resource entries, season labels, or sites returning incomplete detail responses. Source evidence is required before deciding whether the fault is query, detail parsing, or episode selection.
- A site may return the second season under a base title without a season marker. The current rule intentionally rejects it for season 2+, which favors safety over recall.
- AiYue, Girigiri and other sites may expose different titles between search and detail. The current code rechecks the detail title only indirectly and needs recorded request fixtures.

## 7. Difference from the PCBOX reference

PCBOX has stronger building blocks that should be reused conceptually, not copied blindly:

- External provider IDs are kept per provider.
- Manual clear uses provider-specific suppression and does not erase the other provider.
- Manual correction can preserve stable Work identity while replacing one provider link.
- Metadata query expansion includes aliases beyond the first three and strips `Part`-style suffixes for additional candidates.
- Identity checks distinguish provider-native types and namespaces; equal numeric IDs are not treated as the same media.
- Regression tests cover `II`/`2`, season/Cour, title aliases, manual clear/rebind, refresh protection and immediate score updates.

PTVBOX gaps against those ideas:

- No normalized external-ID table.
- No internal UUID as the primary matching key.
- No scored candidate model.
- No alias provenance/language fields.
- No separate installment/part/cour semantics.
- No persisted match history.
- Limited update protection during concurrent presentation work.

## 8. Reusable and required-new components

Reusable:

- `Work`, `WorkFacts`, `PersonCredit`, `CompanyCredit`.
- Stable provider key and `media_id` UUID.
- Manual link/negative-link behavior.
- `presentation_v5` invalidation pattern.
- Exact safety checks for kind, year and season.
- Existing protected backup format and merge semantics.

New, but not implemented in phase 0:

- `media_external_ids`.
- `media_title_aliases` with language/type/provider/confidence.
- `source_items` and `source_matches`.
- `match_candidates` and `match_history`.
- A pure title parser returning separate season/installment/part/cour/episode fields.
- Provider adapters for TMDB, Douban, AniList, Bangumi and optional data sources.
- A deterministic scorer with hard conflicts and top-1/top-2 margin.
- A 300-500 item reviewed fixture corpus.

Migration risks:

- Existing `Work.key`, `episodeRef`, favorites and progress keys must remain stable.
- Existing `rating_link_*` values must be imported as manual locks, including `none`.
- A failed migration must preserve the current database and migration backups.
- No automatic merge may collapse a movie, TV season, OVA or special solely because titles look similar.
- User media/subtitle caches must remain outside the identity migration transaction.

## 9. Phase-1 plan after approval

1. Add pure Unicode normalization and a title parser with explicit outputs:
   - base title;
   - season number;
   - installment number;
   - part number;
   - movie number;
   - cour number;
   - episode number;
   - parser confidence and unresolved text.
2. Preserve raw source titles and raw source metadata before creating search variants.
3. Generate bounded variants for Chinese, Japanese, Romaji, English, aliases, full title and base title.
4. Add unit fixtures for:
   - `2`, `II`, `Ⅱ`, full-width `２`;
   - `第二季`, `Season 2`, `第2期`, `Part II`, `第2クール`;
   - movie, TV, OVA, OAD, ONA, special and recap;
   - same title with different years and countries.
5. Add regression assertions ensuring current exact matches do not regress and ambiguous inputs remain reviewable.
6. Stop after phase 1 and report parser output, tests and known unresolved cases.

Initial test corpus should grow to 300-500 real source items before scored automatic binding is enabled. Until then, automatic source and rating matching should continue to favor no match over a wrong match.

## 10. Approval gate

The attached specification requires phase 0 to stop before matching-engine code starts. This audit satisfies that requirement. Phase 1 must not begin until the owner explicitly approves it.

# Adding more shows

The app is multi-show. A station is just **a catalog JSON** plus **one or more
Internet Archive item identifiers**, registered as a `Show` in
`ShowRegistry` (in `Episode.kt`). Adding a station is data work, not a
rewrite.

## What a show needs

1. **Public-domain audio on Archive.org** — one item (or several) with one
   mp3 per episode.
2. **A catalog**: a JSON array bundled in `app/src/main/assets/`, one object
   per episode:

```json
{
  "ep": 1,
  "title": "The Old Ones Are Hard to Kill",
  "date": "1974-01-06",
  "writer": "Henry Slesar",
  "desc": "A spry woman in her golden years rents a room to…",
  "fallbackFile": "CBS-Radio-Mystery-Theater_0001_1974-01-06_0001_The-Old-Ones-Are-Hard-to-Kill.mp3"
}
```

3. **A `Show` entry** in `ShowRegistry.all` (`Episode.kt`):

```kotlin
val MYSHOW = Show(
    id = "myshow",                 // stable key; prefixes per-show prefs
    name = "MY SHOW",              // brand-plate / station-picker label
    catalogAsset = "catalog_myshow.json",
    episodeCount = 123,
    items = listOf(
        ArchiveItem("Archive_Item_Identifier", 1, 123),  // id + ep range
    ),
    fileRegex = "...",             // see below; or omit for bundled filenames
)
```

## Two filename strategies

- **Runtime resolution** (`fileRegex` set): if the Archive filenames embed the
  episode number, give a regex where group 1 = the full filename and group 2 =
  the episode number, e.g. for `XMinusOne55-04-24001NoContact.mp3`:
  `"\"name\"\\s*:\\s*\"(XMinusOne\\d{2}-\\d{2}-\\d{2}(\\d{3})[^\"]*?\\.mp3)\""`.
  The app fetches each item's metadata once, maps episode → exact filename,
  and caches it. `fallbackFile` is only used if that fetch fails.

- **Bundled filenames** (`fileRegex = null`, the default): for shows with
  irregular names, dates, or spaces (e.g. Lights Out), store the exact,
  URL-encoded filename in each episode's `fallbackFile` and skip runtime
  resolution.

## Multi-item shows

`items` can list several Archive items with contiguous episode ranges (e.g.
Johnny Dollar spans seven items, one per lead actor). Runtime resolution
fetches every item and records which item each episode lives in, so the
ranges only steer the offline fallback.

Filenames with spaces/parentheses are URL-encoded automatically in
`streamUrl`; bundled `fallbackFile` values should be stored pre-encoded.

## Per-show state

Played set, resume point, and last browse index are stored in
SharedPreferences keyed by the show `id`, so each station is independent. No
code needed — it falls out of the `id`.

## Data-gathering tips

Catalogs in this project were compiled from Wikipedia episode lists, the
Jerry Haendiges Vintage Radio Logs (otrsite.com), and OTR Plot Spot
(otrplotspot.com). Cross-check Archive filenames against the logs — titles and
dates often differ slightly, so don't trust constructed names blindly.

Good public-domain candidates still on the bench: Suspense, The Mysterious
Traveler, Inner Sanctum, 2000 Plus, Space Patrol, The Whistler.

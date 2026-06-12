# Adding more shows

The app is built to be show-agnostic: a show is just **a catalog JSON** plus
**one or more Internet Archive item identifiers**. CBS Radio Mystery Theater
is simply the first preset.

## What a show needs

1. **Public-domain audio on Archive.org**, ideally one item (or a few) with
   one mp3 per episode and a consistent filename scheme that contains the
   episode number.
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

## Where the code touches the show

- `EpisodeRepository.kt` — `ITEMS` lists the Archive.org identifiers;
  `FILE_RX` parses episode numbers out of the item's mp3 filenames;
  `loadCatalog()` reads the asset.
- `PlayerViewModel.kt` — the 1288/1289 item-boundary in the fallback URL
  logic is CBSRMT-specific.
- `RadioScreen.kt` — the brand plate text and the year marks derive from
  the catalog dates automatically.

## Suggested path to multi-show support

1. Introduce a `Show` data class (name, asset file, items list, filename
   regex, episode→item mapping).
2. Move the CBSRMT constants out of `EpisodeRepository` into a `Show`
   registry.
3. Add a show selector — a "band switch" knob (AM/FM/SW style) on the
   radio face would stay true to the aesthetic.
4. Keep per-show SharedPreferences keys (played set, resume point) by
   prefixing them with the show id.

Good candidates with public-domain Archive.org collections: Suspense,
X Minus One, Dimension X, The Whistler, Lights Out, Inner Sanctum.

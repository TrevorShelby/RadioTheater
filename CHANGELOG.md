# Changelog

## v1.1 (2026-06-19)

The single-show player becomes a seven-station old-time-radio appliance.

### New: multi-station support
- Tap the brand plate to open the **STATIONS** picker and switch shows.
- Each station keeps its own browse position, resume point, and played-episode
  history. Existing CBS Radio Mystery Theater progress is migrated automatically.

### New shows
- **X Minus One** — 125 episodes (1955–1958), sci-fi.
- **Dimension X** — 50 episodes (1950–1951), sci-fi.
- **Lights Out** — 71 episodes (1935–1947), horror.
- **Quiet, Please** — 91 episodes (1947–1949), literary fantasy/horror.
- **Escape** — 218 episodes (1947–1954), high adventure.
- **Yours Truly, Johnny Dollar** — 809 episodes (1949–1962), detective, spanning
  all six lead actors; the five-part serials play straight through.

Roughly 2,760 episodes across seven stations.

### Fixes
- Tuning dial now tracks your touch immediately and lands where you tap
  (previously it lagged and jumped back toward the first episode). Rebuilt the
  gesture handling and made dial scrubbing lightweight for large catalogs.
- Year labels on the dial no longer overlap into an unreadable smear when a
  station has many episodes clustered in a few years.
- Hardened the playback path so a transient player error degrades to "STATIC"
  instead of crashing the app.

### Under the hood
- `Show` / `ArchiveItem` / `ShowRegistry` abstraction; per-show catalogs in
  `assets/`. Runtime filename resolution where filenames embed the episode
  number, bundled exact filenames otherwise; multi-item shows supported.
- Stream URLs are URL-encoded to support filenames with spaces.

## v1.0 (2026-06-12)

- Initial release: CBS Radio Mystery Theater (1,399 episodes), Bakelite radio
  UI, tuning dial + knobs, search, resume, played tracking, scrub/skip.

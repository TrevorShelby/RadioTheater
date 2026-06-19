# Radio Theater 📻

A 1950s Bakelite tabletop radio for the **Meta Portal** (and other Android
devices) that streams thousands of public-domain old-time-radio episodes —
mystery, science fiction, horror, and detective drama — straight from the
Internet Archive.

The entire screen is the radio: spin the tuning knob or drag the slide-rule
dial to click through the years, press the Bakelite PLAY button, and let the
golden age of radio do the rest. Tap the brand plate at the top to switch
between stations.

> *"Come in... welcome. I'm E.G. Marshall."*

## Why this exists

Meta unlocked sideloading on Portal devices in June 2026, leaving a lot of
perfectly good touchscreen speakers looking for a purpose. A huge swath of
classic radio drama is public domain and freely hosted on the Internet
Archive, so a dedicated old-time-radio appliance felt right: no accounts, no
ads, no subscriptions — just a warm amber dial and a creaking door.

It runs fine on any reasonably modern Android tablet too (landscape
orientation, minSdk 28).

## Stations

Tap the brand plate to open the **STATIONS** picker. Seven shows are built
in — about **2,760 episodes** in all:

| Station | Years | Episodes | Flavor |
|---|---|---|---|
| **CBS Radio Mystery Theater** | 1974–1982 | 1,399 | Mystery / suspense |
| **X Minus One** | 1955–1958 | 125 | Science fiction |
| **Dimension X** | 1950–1951 | 50 | Science fiction |
| **Lights Out** | 1935–1947 | 71 | Horror / supernatural |
| **Quiet, Please** | 1947–1949 | 91 | Literary fantasy/horror |
| **Escape** | 1947–1954 | 218 | High adventure |
| **Yours Truly, Johnny Dollar** | 1949–1962 | 809 | Detective / crime |

Each station keeps its own browse position, resume point, and played-episode
history, so you can hop between shows without losing your place in any of them.

## Features

- **Skeuomorphic radio UI** — tuning dial with year markings, rotary knobs
  with detent clicks, glowing episode display, speaker grille, chunky buttons
- **Seven stations** — tap the brand plate to switch shows; per-show memory
- **Browse without interrupting** — turning the dial only changes what's
  displayed; the current episode keeps playing until you press PLAY
- **Catalogs built in** — titles, air dates, writers, and (where available)
  descriptions, with instant search by title, writer, number, or year
- **Episode detail view** — tap the display for a full-screen episode card
- **Scrubbing & skips** — progress bar with drag-to-seek, ◀15s / 30s▶ jumps
- **Played tracking** — episodes you finish get a ✓ everywhere they appear
- **Resume** — reopens on the episode you left off, at the same position
- **Auto-advance** — episodes flow into each other like a real broadcast
  (the Johnny Dollar five-part serials play straight through)
- **Mechanical sounds** — synthesized detent ticks and button thunks

## How it works

- Each show is a `Show` in `Episode.kt`'s `ShowRegistry`: a bundled catalog
  JSON in `app/src/main/assets/` plus one or more Internet Archive item IDs.
- Audio streams directly from the Internet Archive. Shows whose Archive
  filenames embed the episode number resolve exact filenames at runtime from
  the metadata API (cached in filesDir); shows with irregular filenames bundle
  the exact name in the catalog.
- Playback is Media3 ExoPlayer with constant-bitrate seeking enabled (the
  archive mp3s carry no seek index).
- Nothing is downloaded or stored beyond the filename cache and your
  played/resume state. No analytics, no network calls other than Archive.org.

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md) for the controls, and
[docs/ADDING_SHOWS.md](docs/ADDING_SHOWS.md) for adding more shows.

## Building

Requirements: Android Studio (Ladybug or newer), JDK 17+. No API keys needed.

```
git clone <this repo>
# open in Android Studio, or:
./gradlew assembleDebug
adb install -r app/build.nosync/outputs/apk/debug/app-debug.apk
```

### Portal-specific notes

- Sideloading: enable ADB on the Portal, then deploy normally (device
  appears as "Facebook Portal"). The launcher requires the LAUNCHER+DEFAULT
  intent categories, already set in the manifest.
- minSdk 28 / targetSdk 29 matches Portal's Android build; the app uses no
  Google Mobile Services (Portals don't have them).
- If the project lives in an iCloud-synced folder, build output must stay in
  `build.nosync` (already configured) — iCloud duplicates intermediate build
  files mid-build and breaks dexing otherwise.

## Content & legality

All seven shows are in the public domain (or carry a Public Domain Mark) on
the Internet Archive and are legally free to stream. This app is an
independent fan project, not affiliated with CBS, NBC, Meta, or the Internet
Archive.

Episode metadata (titles, dates, writers, descriptions) is compiled from
Wikipedia, the Jerry Haendiges Vintage Radio Logs, and OTR Plot Spot.

## License

MIT — free to use, modify, and redistribute. See [LICENSE](LICENSE).

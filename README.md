# Radio Theater 📻

A 1950s Bakelite tabletop radio for the **Meta Portal** (and other Android
devices) that streams the complete run of **CBS Radio Mystery Theater** —
all 1,399 episodes broadcast between 1974 and 1982 — straight from the
Internet Archive.

The entire screen is the radio: spin the tuning knob or drag the slide-rule
dial to click through nine years of episodes, press the Bakelite PLAY button,
and let E.G. Marshall do the rest.

> *"Come in... welcome. I'm E.G. Marshall."*

## Why this exists

Meta unlocked sideloading on Portal devices in June 2026, leaving a lot of
perfectly good touchscreen speakers looking for a purpose. CBS Radio Mystery
Theater is public domain (Public Domain Mark 1.0) and freely hosted on the
Internet Archive, so a dedicated old-time-radio appliance felt right: no
accounts, no ads, no subscriptions — just a warm amber dial and a creaking
door.

It runs fine on any reasonably modern Android tablet too (landscape
orientation, minSdk 28).

## Features

- **Skeuomorphic radio UI** — tuning dial with year markings, rotary knobs
  with detent clicks, glowing episode display, speaker grille, chunky buttons
- **Browse without interrupting** — turning the dial only changes what's
  displayed; the current episode keeps playing until you press PLAY
- **Full catalog built in** — titles, air dates, writers, and descriptions
  for all 1,399 episodes; instant search by title, writer, number, or year
- **Episode detail view** — tap the display for a full-screen episode card
- **Scrubbing & skips** — progress bar with drag-to-seek, ◀15s / 30s▶ jumps
- **Played tracking** — episodes you finish get a ✓ everywhere they appear
- **Resume** — reopens on the episode you left off, at the same position
- **Auto-advance** — episodes flow into each other like a real broadcast
- **Mechanical sounds** — synthesized detent ticks and button thunks

## How it works

- `app/src/main/assets/catalog.json` holds the bundled episode catalog,
  compiled from Wikipedia's season-by-season episode lists.
- Audio streams from two Internet Archive items (the same source used by the
  popular Alexa skill): `APPBLY_radioMysteryTheater_0001` (episodes 1–1288)
  and `APPBLY_radioMysteryTheater_0002` (1289–1399).
- On first launch the app fetches the exact mp3 filename list from the
  Archive.org metadata API and caches it. If that fetch fails, filenames are
  constructed from the catalog (~97% accurate) with an automatic fallback
  retry on playback errors.
- Playback is Media3 ExoPlayer with constant-bitrate seeking enabled (the
  archive mp3s carry no seek index).
- Nothing is downloaded or stored beyond the filename cache and your
  played/resume state. No analytics, no network calls other than Archive.org.

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md) for the controls, and
[docs/ADDING_SHOWS.md](docs/ADDING_SHOWS.md) for extending it to other
old-time radio shows.

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

CBS Radio Mystery Theater episodes on the Internet Archive carry a
[Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/)
license — the catalog is legally free to stream. This app is an independent
fan project, not affiliated with CBS, Meta, or the Internet Archive.

Episode metadata (titles, dates, writers, descriptions) comes from
Wikipedia's episode lists (CC BY-SA 4.0).

## License

MIT — free to use, modify, and redistribute. See [LICENSE](LICENSE).

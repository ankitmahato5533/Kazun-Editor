# Kazun Editor

**Free, offline, open-source Android video editor.** Koi subscription nahi, koi API key nahi, koi data device se bahar nahi.

<p align="center">
  <a href="https://github.com/ankitmahato5533/Kazun-Editor/releases"><b>⬇ Download APK</b></a>
</p>

> **Status:** `0.2.0-alpha` — working core editor with **real GPU effects** in preview and export.

## Download

Sirf **GitHub Releases** — koi Play Store, koi F-Droid nahi.

```
https://github.com/ankitmahato5533/Kazun-Editor/releases
```

1. Latest release se `.apk` download karo
2. "Install unknown apps" allow karo (Android ek baar poochta hai)
3. Install → open → naya project

## Kya kaam karta hai

- **Projects** — 16:9, 9:16, 1:1, 4:5
- **Timeline** — multi-track, drag, trim, split, delete, zoom, undo/redo
- **Preview** — ExoPlayer playback, playhead sync
- **Effects** — 1,256 ka catalog, search, live parameter controls, **GPU pe render** (LUT grading, blur, RGB split)
- **Text** — overlay add karo
- **Export** — MP4 via Media3 Transformer, effects baked in, hardware encoder
- **Privacy** — no INTERNET permission, zero telemetry

Detailed status: [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)

## Build from source

```bash
git clone https://github.com/ankitmahato5533/Kazun-Editor.git
cd Kazun-Editor
gradle :app:assembleDebug        # needs JDK 17 + Android SDK 35
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Regenerate the effect catalog

```bash
node tools/generate_catalog.mjs   # writes app/src/main/assets/effects/catalog.json
```

1,256 effects = 156 primitives + 1,100 data presets. Deterministic output.

## Tech

Kotlin 2.0 · Jetpack Compose · Media3 (ExoPlayer) · MediaExtractor/MediaMuxer · kotlinx.serialization · Gradle 8.9 / AGP 8.7

## Docs

- [Technical blueprint](docs/BLUEPRINT.md) — architecture, data model, 1,256-effect catalog, roadmap
- [Implementation status](docs/IMPLEMENTATION.md) — what works, what is pending

## License

GPL-3.0 — FFmpeg/GPL compliance ke liye recommended.

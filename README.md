# Kazun Editor

Ek **free, offline, open-source Android video editor** — koi subscription nahi, koi API key nahi, koi data device se bahar nahi.

> **Status:** Planning stage. Code abhi shuru nahi hua.

## Kya hai ye

- **Free forever** — user ke liye free, developer ke liye ₹0 recurring cost
- **100% offline** — editing aur AI, dono phone ke andar
- **No third-party APIs** — koi cloud call nahi
- **Privacy by architecture** — video kabhi device chhodta hi nahi
- **Open source** — code public, **sirf GitHub Releases** pe distribute

## Kya banega

| Phase | Feature |
|---|---|
| 1 | Timeline editor — import, trim, split, filters, text, audio, export |
| 2 | Effects — transitions, keyframes, speed ramp, color grading, chroma key |
| 3 | AI on-device — auto-captions, silence cut, background removal, TTS |
| 4 | Polish — templates, onboarding, in-app updater, GitHub release |

## Tech

Kotlin · Jetpack Compose · Media3 (ExoPlayer + Transformer) · OpenGL ES 3.0 · Room · whisper.cpp · MediaPipe · FFmpeg

## Download

Sirf **GitHub Releases** se — koi Play Store, koi F-Droid nahi.

```
https://github.com/ankitmahato5533/Kazun-Editor/releases
```

- `arm64` APK — 99% phones ke liye (yehi download karo)
- `armv7` APK — purane phones ke liye
- Har release ke saath `SHA256SUMS.txt` — verify kar sakte ho

> Pehli baar install karte waqt Android "Install unknown apps" allow karne ko kahega — ye normal hai.

## Documentation

- [Technical Blueprint](docs/BLUEPRINT.md) — architecture, data model, module structure, feature spec, roadmap

## License

TBD — GPL-3.0 recommended (FFmpeg compliance ke liye).

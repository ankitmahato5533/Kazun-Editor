# Implementation status

> Sirf wahi likha hai jo **sach mein** kaam karta hai. Jo baaki hai, neeche "pending" mein hai.

**Version:** `0.1.0-alpha` · **Commit:** CI-built APK · **Tests:** 12 unit tests (timeline engine)

---

## Working today

| Area | Status | Detail |
|---|---|---|
| Project dashboard | ✅ | Create (4 aspect ratios), list, delete, rename-ready, JSON persistence |
| Media import | ✅ | SAF multi-select, duration/resolution probe, sequential placement |
| Timeline | ✅ | Multi-track, drag move, edge trim, split, delete, tap select, zoom 0.008–0.25 dp/ms |
| Overlap safety | ✅ | Gap-based placement: clips kabhi overlap nahi karte (property test se verified) |
| Undo / redo | ✅ | 60-step stack, drag operations coalesced |
| Preview | ✅ | ExoPlayer, play/pause, playhead sync, next-clip advance |
| Effects catalog | ✅ | 1,256 descriptors loaded from assets, search, 20 categories |
| Effect params | ✅ | Auto-generated UI (sliders / switches / enum chips) from descriptor |
| Text overlays | ✅ | Add text clip, live overlay on preview |
| Export | ✅ | MP4 stream-copy via MediaExtractor + MediaMuxer (video track) |
| Share | ✅ | FileProvider + system share sheet |
| Offline | ✅ | **No INTERNET permission** in manifest — app network access kar hi nahi sakta |
| CI/CD | ✅ | Push → test + APK; tag → GitHub Release with APK + SHA256 |

## Not wired yet (honest list)

| Area | Kya missing hai | Blueprint section |
|---|---|---|
| GPU effects | 1,256 effects **catalog aur params** hain, par GPU shader pipeline abhi nahi — preview/export pe visual apply nahi hota | §7.3 |
| Transitions | Catalog mein 148 hain, timeline pe drop karna baaki | §3.5 C |
| Audio | Audio track clips + mixing engine baaki | §8 |
| Export with effects | Abhi stream copy; effects bake karne ke liye re-encode path chahiye | §8.1 |
| AI | Model manager + Whisper/VAD wiring baaki | §9 |
| Text animations | 96 presets catalog mein, keyframe engine baaki | §3.6.7 |
| Custom presets | Save/share `.kazunpreset` baaki | §3.6.6 |
| Room DB | File-based JSON use ho raha hai (simpler + crash-safe); Room migration later | §5.1 |

## Why this split

Sab kuch ek saath likhna possible nahi tha, isliye **vertical slice** banaya: import → edit → effects select → export.
Ye slice real device pe chal sakta hai aaj. Baaki phases isi skeleton mein plug honge — data model aur catalog already final hain.

## Test coverage

```
TimelineOpsTest — 12 tests
├── add: placement, overlap push
├── trim: clamp to source, cannot cross
├── split: correctness, no-op outside bounds
├── move: free space, nearest gap, negative clamp
├── duration: longest track
├── clipAt: hit / gap
├── effects: 6-max cap
└── property test: 120 random ops, invariants hold
```

# Kazun Editor — Technical Blueprint

> **Status:** Planning only. Is doc mein koi code nahi hai — sirf architecture, scope, decisions.
> **Last updated:** 2026-10-23

---

## 1. Vision (ek line mein)

Ek **free, offline, fully open-source Android video editor** jo bina kisi subscription, bina kisi API key, aur bina internet ke kaam kare — AI features bhi phone ke andar.

**Non-negotiables:**

| Rule | Matlab |
|---|---|
| ₹0 cost | User ko free, developer ko ₹0 recurring cost |
| 100% offline | Editing aur AI dono bina internet |
| No third-party API | Koi OpenAI / Runway / ElevenLabs call nahi |
| No data leaves device | Privacy by architecture, policy se nahi |
| No ads | Free ka matlab free |
| Open source | Code public, koi bhi audit/build kar sake |

---

## 2. Platform decisions

| Decision | Value | Reason |
|---|---|---|
| Platform | Android only (v1) | User ka choice, aur mobile-first editing |
| UI toolkit | Kotlin + Jetpack Compose | Native performance, modern |
| minSdk | 26 (Android 8.0) | ~97% devices, MediaCodec mature |
| targetSdk | 36 (Android 16) | Future-proof, naye Android behavior ke liye tayyar |
| ABI | arm64-v8a only | 99% devices, APK size aadha |
| Package | **`com.kazun.editor`** | Final |
| Orientation | Portrait primary, landscape editor supported | Short-form + long-form dono |
| UI language | **English only** (v1) — strings `strings.xml` mein, future i18n ready | User ka choice |
| Distribution | **GitHub Releases only** | 100% free, no store account, full control |

### Device tiers

| Tier | Spec | Kya chalega |
|---|---|---|
| **Minimum** | **6GB RAM**, Android 8+ | 1080p source, 720p preview, 1080p export, saare effects (heavy ones slower) |
| **Recommended** | 8GB RAM, Android 11+ | 1080p preview 30fps, 4 tracks, all effects + AI comfortably |
| **Best** | 12GB+, flagship SoC | 4K source, 60fps preview, AI near-realtime |

> **Note:** 4K *source* supported hai, par 4K *export* sirf flagship pe. 6GB devices pe 1080p max — warna OOM/heat.
>
> **RAM gate:** First launch pe `ActivityManager.totalMem` check hoga. `< 5.5GB` mila toh warning screen: "Aapka device minimum requirement se kam hai — 720p mode mein chalega, heavy effects slow honge." App band nahi hota, bas honest expectation set hota hai.

---

## 3. Feature scope

### 3.1 In scope — Phase 1 (MVP)

- [ ] Project create / rename / delete / duplicate
- [ ] Video import from gallery (single + multi-select)
- [ ] Timeline: 2 tracks (video + audio), horizontal scroll, zoom, snapping
- [ ] Trim, split, delete, reorder (drag)
- [ ] Frame-accurate scrub (ExoPlayer seek + frame step)
- [ ] Basic filters: brightness, contrast, saturation, temperature
- [ ] Text overlays: font, size, color, position, background
- [ ] Music/audio add, per-clip volume, fade in/out
- [ ] Export 720p/1080p, 16:9 + 9:16, hardware encoder
- [ ] Autosave + project restore
- [ ] Undo/redo

### 3.2 In scope — Phase 2 (v1.0)

- [ ] 4 tracks: 2 video, 1 overlay, 1 audio
- [ ] Transitions: 148 variants (12 families) — §3.5 C
- [ ] Keyframe animation (position, scale, rotation, opacity)
- [ ] Speed ramp (0.25x – 4x), reverse, freeze frame
- [ ] Color grading: LUT support, curves, HSL
- [ ] Chroma key (green screen)
- [ ] Masks (linear, radial), blend modes (6)
- [ ] Aspect ratio crop, rotate, flip, PiP
- [ ] Audio: EQ (3-band), normalize, auto-ducking
- [ ] Waveform display
- [ ] Stickers, shapes, progress bar, watermark
- [ ] Export presets: Reels / Shorts / YouTube / WhatsApp
- [ ] GIF export, still frame, audio-only, SRT export
- [ ] Project templates (3 built-in)

### 3.3 In scope — Phase 3 (AI)

- [ ] Auto-captions (Whisper, on-device, Hindi + English + 90 languages)
- [ ] Caption editor + 6 styled presets + word highlight
- [ ] Silence auto-cut (Silero VAD)
- [ ] Background removal (MediaPipe selfie segmentation)
- [ ] Text-to-speech voiceover (Piper, Hindi + English voices)
- [ ] Beat detection (algorithmic) → auto-cut on beat
- [ ] Auto-reframe 16:9 → 9:16 (subject tracking)

### 3.4 Out of scope — honest list

| Feature | Kyun nahi |
|---|---|
| Demucs voice isolation | 300MB + heavy compute, phone pe impractical |
| Real-ESRGAN upscaling | 1 frame mein seconds, sirf still ke liye theek |
| Local LLM (title/hashtag) | 1-4GB RAM, sirf flagship, ROI kam |
| 4K export on mid-range | OOM + thermal throttle |
| Cloud sync / team collab | Internet + server cost — vision ke against |
| iOS version | Apple $99/yr, abhi budget nahi |
| Web version | User ne explicitly mana kiya |
| 3D / VFX compositing | Different product altogether |

### 3.5 Effects catalog — **1,256 effects**

#### Pehle honest baat

"1000 effects" ke do matlab ho sakte hain:

| Approach | Reality |
|---|---|
| 1,000 **haath se likhe shaders** | ~2 saal ka kaam, 1,000 GLSL files maintain karna impossible, user ko fayda zero |
| 1,000 **tappable effects** | ~156 primitives + data-driven presets — achievable, maintainable |

**Hum doosra kar rahe hain.** Ye wahi tarika hai jisse CapCut, Filmora, VN count karte hain. User ko 1,000 alag-alag cheezein dikhti hain jo tap kar sakta hai — andar se ~156 engine-level primitives hain jo milkar banti hain.

#### Breakdown: 1,256 kahan se aaye

| Layer | Count | Kya hai |
|---|---|---|
| **Shader / DSP primitives** | 156 | Asli code — GLSL shaders + audio DSP |
| **Data presets** | 1,100 | JSON files (params, curves, LUT refs) — naya code nahi |
| **Total tappable** | **1,256** | Jo user ko UI mein dikhta hai |

---

#### Cost legend (6GB device, 1080p preview)

- 🟢 **Cheap** — 1 pass, 1-3 texture reads → 30fps easily
- 🟡 **Medium** — 2-4 passes → 30fps, thoda heat
- 🔴 **Heavy** — many iterations → preview auto 540p, export theek

---

### A. Filter presets / LUTs — **320** 🟢

16 categories × 20 LUTs. Sabse sasta effect (ek texture read), sabse zyada visual impact.

| # | Category | Count | Example names |
|---|---|---|---|
| 1 | Cinematic | 20 | Teal & Orange, Moody Blue, Bleach Bypass, Film Noir, Cold Cinema, Golden Hour, Anamorphic, Blockbuster... |
| 2 | Film Stocks | 20 | Kodak Portra 400, Fuji Velvia, Ilford HP5, CineStill 800T, Kodachrome 64, Agfa Vista... |
| 3 | Vintage & Retro | 20 | Sepia, 70s Fade, Polaroid, Lomo, Cross Process, Retro Paper, Push Process... |
| 4 | Black & White | 20 | Classic, High Contrast, Silver, Selenium Tone, Platinum Print, Infrared... |
| 5 | Warm & Golden | 20 | Golden Hour, Sun Kissed, Amber, Honey, Desert Warm, Peach Glow... |
| 6 | Cool & Blue | 20 | Arctic, Moody Blue, Steel, Cyan Drift, Midnight, Glacier... |
| 7 | Pastel & Soft | 20 | Soft Pink, Aesthetic Pink, Cotton Candy, Dreamy, Matte Soft, Milk... |
| 8 | Neon & Cyber | 20 | Cyberpunk, Neon Nights, Blade, Synthwave, Miami Vice, Dark Neon... |
| 9 | Nature & Landscape | 20 | Forest, Ocean, Desert, Alpine, Jungle Green, Autumn Leaf... |
| 10 | Urban & Street | 20 | Gritty City, Concrete, Urban Decay, Subway, Graffiti, Night Street... |
| 11 | Portrait & Skin | 20 | Soft Skin, Beauty Glow, Warm Portrait, Natural Tone, Studio, Editorial... |
| 12 | Food & Product | 20 | Crisp Food, Fresh, Commercial, Menu Shot, Café, Bakery... |
| 13 | Travel & Sunset | 20 | Sunset, Tropical, Wanderlust, Island, Safari, Golden Beach... |
| 14 | Moody & Dark | 20 | Dark Matter, Underexposed, Shadow Play, Horror, Thriller, Goth... |
| 15 | Seasonal | 20 | Spring Bloom, Summer Heat, Monsoon, Winter Frost, Autumn, Festival... |
| 16 | Social & Clean | 20 | Clean Bright, VSCO Warm, Contrast Pop, Y2K Cool, Influencer, Story... |

**320 LUTs kaise banenge (honestly):**

| Source | Count | Method |
|---|---|---|
| **Procedural** | 260 | Color-science math se generate — 3-4 stop curve + channel mixer + saturation matrix. JSON mein define, first use pe 33³ texture bake hoke cache |
| **Measured film emulations** | 40 | Real `.cube` files (open-source packs + khud colorimeter se banaye) |
| **User imported** | ∞ | `.cube` import — user apni LUT add kar sake |

> **Kyun procedural?** 320 `.cube` files = ~30MB APK. Procedural = ~400KB JSON, aur baaki cache mein banta hai. Same look, 1/70th size. Aur ek LUT modify karni ho toh JSON edit, 33×33×33 values nahi.

---

### B. Quick Looks — **120** 🟢

LUT ke bina, sirf adjustment curves se — patli aur fast. 10 groups × 12.

| Group | Count | Examples |
|---|---|---|
| Exposure fixes | 12 | Brighten, Darken, Backlit Fix, Overexposed Fix... |
| Contrast looks | 12 | Punchy, Flat, Filmic, Hard, Soft... |
| Color pops | 12 | Vibrant, Vivid+, Color Boost, Pop... |
| Desaturate | 12 | Muted, Faded, Gray-ish, Subtle... |
| Skin tones | 12 | Warm Skin, Neutral Skin, Tan, Porcelain... |
| Night / low light | 12 | Night Boost, Neon Night, Shadow Lift... |
| Day / outdoor | 12 | Sunny, Hazy Fix, Blue Sky, Daylight... |
| Indoor | 12 | Tungsten Fix, Fluorescent Fix, Cozy... |
| HDR-ish | 12 | HDR Look, Dynamic, Clarity, Detail+... |
| Creative | 12 | Cross Tone, Split Tone, Duo Tone, Pastel Wash... |

---

### C. Transitions — **148** 🟡

| Group | Count | List |
|---|---|---|
| Basic dissolves | 8 | Cut, Cross Dissolve, Fade Black, Fade White, Blur Dissolve, Add Dissolve, Subtract, Luma Wipe |
| Directional | 32 | Slide / Push / Flip / Roll / Swing — har ek L, R, U, D (5 styles × 4 dir = 20) + 12 diagonal/combined |
| Wipes | 12 | Linear, Barn Door, Venetian, Blinds, Checkerboard, Clock, Radial, Spiral... |
| Shape reveals | 12 | Circle, Square, Diamond, Star, Heart, Hexagon, Triangle, Custom Mask... |
| Zoom / scale | 8 | Zoom In, Zoom Out, Punch, Spin Zoom, Bounce Zoom... |
| Blur / defocus | 8 | Blur Through, Focus Pull, Bokeh Shift, Gaussian Wipe... |
| Glitch / digital | 12 | RGB Split, Pixelate, Datamosh, Scanline, Static, Block, Slice... |
| Light / optical | 12 | Flash, Light Leak, Lens Flare, Film Burn, Glow, Exposure Bloom... |
| 3D / perspective | 12 | Cube, Page Turn, Flip 3D, Card, Door, Origami... |
| Mask / creative | 12 | Ink, Paint, Splatter, Torn Paper, Shatter, Puzzle... |
| Liquid / warp | 12 | Ripple, Wave, Melt, Liquify, Swirl, Water... |
| Camera | 8 | Whip Pan, Shake, Handheld, Crash Zoom, Sweep... |

Har transition: 0.1s–2.0s duration + ease curve (linear / ease-in-out / bounce / elastic).

---

### D. Text animations — **96**

| Group | Count | Examples |
|---|---|---|
| Entry | 24 | Fade, Slide (4 dir), Scale, Bounce, Blur In, Rotate, Flip, Drop, Pop, Zoom... |
| Exit | 24 | Same 24, reversed |
| Loop / ambient | 16 | Float, Pulse, Wiggle, Shine, Breathe, Sway... |
| Emphasis | 16 | Karaoke, Glow Pulse, Neon Flicker, Shake, Split Reveal, Underline Grow... |
| Typing | 8 | Typewriter, Letter Drop, Word Pop, Cursor Blink, Decrypt... |
| Utility | 8 | Countdown, Lower Third, Timer, Progress, Handwriting, Mask Reveal... |

---

### E. Text styles / presets — **48**

| Group | Count | Examples |
|---|---|---|
| Title | 12 | Bold Impact, Elegant Serif, Minimal, Cinematic, Editorial... |
| Subtitle / caption | 12 | Clean, Boxed, Outline, Karaoke, Bilingual, Shadow... |
| Callout | 12 | Sticker Text, Neon Sign, 3D Extrude, Graffiti, Comic... |
| Data / utility | 12 | Ticker, Label, Tag, Watermark, Credits, Numbers... |

Har style mein: font, size, weight, letter-spacing, stroke, shadow, background pill, gradient fill — sab pre-tuned.

---

### F. Motion presets — **48** 🟢 (keyframes, shader nahi)

| Group | Count | Examples |
|---|---|---|
| Ken Burns | 8 | In, Out, Diagonal ×4, Dual... |
| Zoom | 8 | Slow, Punch, Snap, Breathe, Spin... |
| Pan / slide | 8 | L, R, U, D, Diagonal ×2, Parallax... |
| Shake / handheld | 8 | Light, Medium, Heavy, Subtle, Camera, Earthquake... |
| Rotate | 8 | Slow, Fast, Tilt, Wobble, 360... |
| Float / idle | 8 | Float, Sway, Drift, Hover, Wave... |

Ek tap = poore keyframe curve bhar jate hain. User ko curve drag nahi karna padta (par kar sakta hai).

---

### G. Particles & overlays — **56** 🟡

7 types × 8 variants (density/color/speed). Sab procedural GLSL — koi video file bundle nahi.

| Type | Variants | Example |
|---|---|---|
| Snow | 8 | Light, Heavy, Blizzard, Slow, Blue, Warm... |
| Rain | 8 | Drizzle, Storm, Window, Night, Neon... |
| Sparks / embers | 8 | Campfire, Welding, Magic, Gold, Rising... |
| Dust | 8 | Floating, Old Film, Sunbeam, Room... |
| Confetti | 8 | Party, Celebration, Gold, Color... |
| Bokeh | 8 | Warm, Cool, Night, Soft, Circles... |
| Smoke | 8 | Fog, Mist, Steam, Haze, Dark... |
| Light rays | 8 | God Rays, Sunbeam, Spot, Volumetric... |

---

### H. Stickers & shapes — **52**

| Group | Count |
|---|---|
| Shapes (rect, circle, line, arrow, triangle, star, polygon...) | 24 |
| Arrows & pointers | 12 |
| Badges (subscribe-style, "NEW", "LIVE", emoji-style) | 16 |

Plus system emoji picker (free, no bundle) + custom PNG import.

---

### I. Audio presets — **56** ⚡

| Group | Count | Examples |
|---|---|---|
| Voice | 16 | Podcast, Vlog, Interview, Phone Call, Deep, Bright, Warm, Radio... |
| Music | 12 | Bass Boost, Treble Boost, Club, Lo-Fi, Vinyl, Concert... |
| Utility | 12 | Normalize, Noise Gate, De-Ess, Compress, Limit... |
| Creative / FX | 16 | Echo, Reverb, Underwater, Megaphone, Telephone, Robot, Chorus, Flanger... |

Sab DSP presets — EQ curve + compressor + reverb params ka JSON. User apna preset save kar sakta hai.

---

### J. Blur — **28**

Gaussian (5 strengths) 🟡 · Motion (4 dir) 🟡 · Radial 🟡 · Zoom 🟡 · Tilt-Shift 🟡 · Bokeh 🔴 · Pixelate (4 sizes) 🟢 · Soft Focus 🟢 · Defocus 🟡 · Prism Blur 🟡 · Path Blur 🟡

---

### K. Glitch & retro — **48**

| Type | Count |
|---|---|
| RGB split / chromatic aberration | 6 |
| Digital glitch (block displacement) | 8 |
| Scanlines / CRT | 6 |
| VHS | 6 |
| Old film (scratches, grain, jitter) | 6 |
| Datamosh / frame smear | 6 |
| Posterize / 8-bit | 4 |
| Camera shake | 6 |

---

### L. Light & stylize — **44**

Vignette (4 intensities) 🟢 · Film grain (4) 🟢 · Bloom / glow (4) 🟡 · Light leak (6) 🟡 · Lens flare (6) 🟡 · God rays (4) 🟡 · Neon glow (4) 🔴 · Flash / strobe (4) 🟢 · HDR look (4) 🟡 · Faded matte (4) 🟢

---

### M. Distortion — **44**

Mirror (h / v / quad / diag) 🟢 · Kaleidoscope (6) 🟡 · Wave / ripple (6) 🟢 · Twirl (4) 🟡 · Bulge / pinch (4) 🟢 · Fisheye (4) 🟢 · Stretch / squeeze (4) 🟢 · Perspective tilt (4) 🟢 · Slice displacement (4) 🟡 · Fractal zoom (4) 🔴

---

### N. Detail & texture — **16**

Sharpen (4) 🟡 · Clarity / local contrast (4) 🟡 · Soften (4) 🟢 · Texture / grain overlay (4) 🟢

---

### O. Gradient maps / duotone — **36** 🟢

2-color maps (12) · 3-color maps (12) · Tonal split (12). Har ek se 1 pass — sabse sasta "designer look".

---

### P. Split-screen layouts — **32** 🟢

2-way (8): vertical, horizontal, diagonal, ratio 1:2, 2:1, PiP corners
3-way (8): tri-split variants
4-way (8): 2×2, strips, mosaic
Creative (8): film strip, grid, collage, custom mask

---

### Q. Borders & frames — **24** 🟢

Solid (4) · Gradient (4) · Polaroid (4) · Film strip (4) · Rounded corners (4) · Shadow / lift (4)

---

### R. Chroma key & color key — **12** 🟡

Green screen (3 toughness levels) · Blue screen (3) · Color picker key (3) · Luma key (3)

---

### S. Speed / retime presets — **16** ⚡

Slow-mo (0.25x, 0.5x) · Fast (2x, 4x) · Speed ramp in/out · Freeze frame · Reverse · Jump cut · Velocity edit presets

---

### T. AI effects — **12** (Phase 3)

| # | Effect | Model |
|---|---|---|
| 1-6 | Auto-captions × 6 styles | Whisper |
| 7 | Silence auto-cut | Silero VAD |
| 8 | Background removal | MediaPipe |
| 9 | Background replace (blur/color/image) | MediaPipe + shader |
| 10 | Auto-reframe 16:9 → 9:16 | MediaPipe tracking |
| 11 | TTS voiceover | Piper |
| 12 | Beat-sync auto-cut | DSP |

---

### Total

| Category | Count |
|---|---|
| A. Filter presets / LUTs | 320 |
| B. Quick Looks | 120 |
| C. Transitions | 148 |
| D. Text animations | 96 |
| E. Text styles | 48 |
| F. Motion presets | 48 |
| G. Particles & overlays | 56 |
| H. Stickers & shapes | 52 |
| I. Audio presets | 56 |
| J. Blur | 28 |
| K. Glitch & retro | 48 |
| L. Light & stylize | 44 |
| M. Distortion | 44 |
| N. Detail & texture | 16 |
| O. Gradient maps | 36 |
| P. Split-screen layouts | 32 |
| Q. Borders & frames | 24 |
| R. Chroma / color key | 12 |
| S. Speed presets | 16 |
| T. AI effects | 12 |
| **TOTAL** | **1,256** |

**1,256 effects** — sab offline, sab free, zero API cost.

---

### 1,256 effects ka UI problem (aur solution)

1,256 items ek list mein daal do toh app unusable ho jayega. Isliye:

| Solution | Detail |
|---|---|
| **Category tabs** | 20 categories, top-level horizontal scroll |
| **Sub-chips** | Har category ke andar sub-groups |
| **Search** | Fuzzy search — "teal" likho, Teal & Orange aa jaye |
| **Favorites** | Star kar sakte ho, top pe pin |
| **Recents** | Last 12 automatically |
| **Live preview on hover** | Long-press pe split preview (before/after) |
| **Thumbnail lazy load** | Sirf visible items render, 128px thumbs cached |
| **"Similar"** | Ek effect ke neeche 6 similar suggestions |

> Bina iske, 1,000 effects 100 se bhi bura experience dete hain. Ye UI kaam **introvert nahi** — Phase 2 ka 1/3 time yahin jayega.

---

### APK size impact

| Component | Size |
|---|---|
| 156 primitives (shaders + Kotlin) | ~1.5MB |
| 1,100 JSON presets | ~2.5MB |
| 40 measured `.cube` LUTs (compressed) | ~4MB |
| Thumbnails (lazy, on-device generated) | 0 (runtime) |
| **Total added** | **~8MB** |

APK ~33MB. Acceptable.

> `.cube` files ke bajaye procedural LUTs use nahi karte toh 320 LUTs = ~30MB. Isliye procedural approach.

---

### Performance rules (1,256 ke saath bhi)

1. **Max 6 effects per clip** — GPU budget fixed
2. **Uber shader** — 12 color params + LUT ek hi pass mein
3. **Preview 540p** jab heavy chain ho, export full res
4. **Fixed composite order** — LUT → color → detail → distortion → light → particles → transition
5. **Heavy (🔴) warning** — preview quality kam hone ka toast
6. **Effect presets** — user apna combo save kar sake

---

### Phase distribution

| Phase | Effects | Count |
|---|---|---|
| **1 (MVP)** | Color adjust params, 20 LUTs, fade, volume | ~40 |
| **2a** | Saare LUTs + Quick Looks + transitions + motion | ~700 |
| **2b** | Text anims, particles, stickers, audio, blur, glitch, light, distortion, splits | ~504 |
| **3 (AI)** | 12 AI effects | 12 |

**Honest note:** Phase 2 ab **8-10 weeks** ka hai (pehle 3-4 tha). Isme ~3 weeks customization engine (§3.6) ka hai, baaki content tuning — har preset ko acha dikhna chahiye. Code se zyada *taste* ka kaam hai.

### 3.6 Customization model — har effect tweakable

**Rule:** App mein **koi bhi effect "fixed" nahi hai.** Jo bhi effect hai, uske parameters user badal sakta hai. 1,256 preset sirf **starting points** hain — infinite variations user bana sakta hai.

#### 3.6.1 Har effect ke 3 levels

| Level | Kya milta hai | Kiske liye |
|---|---|---|
| **L1 — Quick** | Intensity / Mix slider (0-100%) | Beginners — ek slider, bas |
| **L2 — Parameters** | Saare params, grouped, live preview | Normal users |
| **L3 — Keyframes** | Kisi bhi param ko animate karo | Power users |

Default view **L1** hai. "Advanced" tap karo → L2. Param ke aage ⏱ icon → L3. Isse 1,256 effects ka overload nahi hota.

#### 3.6.2 Universal controls (har effect pe, bina exception)

| Control | Kya karta hai |
|---|---|
| **Intensity** | 0-100% — effect ko original footage ke saath mix karta hai (`mix(orig, fx, t)`) |
| **Bypass (A/B)** | Hold karo → original dikhe, chhodo → effect. Compare karne ke liye |
| **Reset** | Defaults pe wapas |
| **Randomize** 🎲 | Params ko random valid values pe set kare (seed-based) |
| **Copy settings** | Ek clip ka effect config doosre clip pe paste |
| **Save as preset** | Tera combination ek named preset ban jaye |
| **Favorite** | Star → top pe pin |
| **Keyframe toggle** | ⏱ har param ke aage — us param ko animate karo |

#### 3.6.3 Parameter types (12 widget, 10,000+ controls)

Har effect ka ek **descriptor JSON** hai. UI usse **auto-generate** hoti hai — maine 1,256 effects ke liye 1,256 panels nahi likhne.

```json
{
  "id": "light.vignette",
  "name": "Vignette",
  "category": "light",
  "cost": "cheap",
  "version": 2,
  "params": [
    { "key": "amount",   "label": "Amount",   "type": "FLOAT",  "default": 0.5, "min": 0, "max": 1, "step": 0.01, "group": "Shape" },
    { "key": "softness", "label": "Softness", "type": "FLOAT",  "default": 0.6, "min": 0.1, "max": 1, "step": 0.01, "group": "Shape" },
    { "key": "roundness","label": "Roundness","type": "FLOAT",  "default": 1.0, "min": 0, "max": 1, "group": "Shape" },
    { "key": "color",    "label": "Color",    "type": "COLOR",  "default": "#000000", "group": "Color" },
    { "key": "center",   "label": "Center",   "type": "POINT2D","default": [0.5, 0.5], "group": "Shape" }
  ]
}
```

| Type | Widget | Example use |
|---|---|---|
| `FLOAT` | Slider + numeric input | Blur radius, opacity |
| `INT` | Slider + stepper | Pixel size, iterations |
| `BOOL` | Switch | Invert, grayscale |
| `COLOR` | Color picker + **eyedropper (video frame se)** | Vignette color, glow tint |
| `ENUM` | Segmented chips | Direction L/R/U/D, blend mode |
| `ANGLE` | Dial pad | Rotation, light angle |
| `POINT2D` | XY pad + **preview par drag** | Center, offset, light position |
| `CURVE` | Bezier editor | Ease, response curve |
| `GRADIENT` | 2-3 stop editor | Gradient map, light leak color |
| `FILE` | Import button | Custom LUT, custom mask, image |
| `SEED` | Dice button 🎲 | Glitch/particle randomness |
| `RANGE` | Dual-handle slider | Shadows/highlights band |

**12 widget components → 10,000+ controls.** Yehi reason hai ki 1,256 effects feasible hain.

#### 3.6.4 Category-wise: kya customize hota hai

| Category | Customizable params |
|---|---|
| **LUT / filters (320)** | Intensity, **hue shift, temperature, saturation, contrast, fade**, custom `.cube` import |
| **Quick Looks (120)** | Saare curve points, per-channel mixer |
| **Transitions (148)** | Duration, **direction, ease curve, softness, color, rotation** |
| **Text anims (96)** | Speed, delay, easing, direction, stagger, loop |
| **Text styles (48)** | Font, size, weight, tracking, stroke, shadow, bg, gradient |
| **Motion (48)** | Intensity, direction, easing, duration, loop |
| **Particles (56)** | Density, size, speed, **color, wind, gravity, lifetime, spread, seed** |
| **Stickers (52)** | Position, scale, rotate, color tint, opacity |
| **Audio (56)** | Saare DSP params: gain, EQ points, threshold, ratio, attack, release, reverb size... |
| **Blur (28)** | Radius, direction, samples, mask |
| **Glitch (48)** | Amount, block size, speed, RGB offset, seed |
| **Light (44)** | Intensity, position, angle, color, falloff |
| **Distortion (44)** | Strength, center, frequency, phase |
| **Gradient maps (36)** | 2-3 stops, positions, blend |
| **Split-screen (32)** | Ratio, gap, border, radius, background |
| **Borders (24)** | Thickness, color, radius, shadow |
| **Chroma key (12)** | Color, threshold, softness, spill |
| **Speed (16)** | Rate, curve points, preserve-pitch toggle |
| **AI (12)** | Style, threshold, language, model size, target fps |

**Bottom line:** koi effect "preset only" nahi hai. Transitions ke duration bhi change honge, LUT ki intensity bhi, particle ka gravity bhi.

#### 3.6.5 Effect stack (per clip)

| Rule | Value |
|---|---|
| Max effects per clip | 6 |
| Same effect twice? | ✅ Allowed (different params — jaise 2 blurs) |
| Reorder | Drag & drop |
| Master intensity | Poori stack pe ek slider |
| Stack bypass | Ek tap mein saare effects off (A/B compare) |
| Stack save | "Mera look" — 6 effects ka combo ek preset ke roop mein |

**Composite order** (fixed, predictable):
`LUT → Color → Detail → Distortion → Light → Particles → Transition`

#### 3.6.6 Custom presets

| Feature | Detail |
|---|---|
| **Save** | Kisi bhi effect ya poore stack ko named preset bana do |
| **Unlimited** | Jitne chahiye — Room DB mein, koi limit nahi |
| **Organize** | Categories/folders + search |
| **Share** | `.kazunpreset` file (JSON) export → WhatsApp/Telegram pe bhejo, doosra import kar le |
| **Import** | File picker, validation ke saath (malformed file se crash nahi) |
| **Community packs** | GitHub pe preset packs host kar sakte ho — free distribution, same updater infra |
| **Thumbnail** | Auto-generated 128px preview (clip ka mid-frame + effect) |

**Count:** 1,256 built-in + **∞ user presets**. Isliye actual number 1,256 nahi, "1,256 + tere apne" hai.

#### 3.6.7 Keyframes (L3)

| Aspect | Value |
|---|---|
| Kaunse params | Jo descriptor mein `keyframable: true` hai (default sab) |
| Interpolation | Linear, ease-in, ease-out, ease-in-out, **bezier (custom)**, hold (step) |
| Editor | Full curve editor — 2D graph, drag points, bezier handles |
| Copy | Curve ek clip se doosre pe copy (same effect ho toh) |
| Presets | 8 easing presets (smooth, snap, bounce, elastic...) |
| Max keyframes | 200 per param (practical limit) |

#### 3.6.8 Technical: auto UI + shader binding

```
descriptor.json  ──►  Compose UI (auto-generated)
       │
       └───────────►  UniformBinder (auto-generated)
                          │
                          ▼
                    GLSL shader uniforms
```

| Concern | Handling |
|---|---|
| Param ↔ shader mismatch | **Build-time CI check** — JSON ke saare params shader mein hone chahiye, warna build fail |
| Missing uniform | Runtime pe default value, crash nahi |
| Slider drag performance | Uniform update only (shader **recompile nahi** hota) |
| Enum change | Kuch effects ke liye shader variant — lazily compile, cache |
| Param change → preview | 30fps max debounce, drag ke dauraan **540p scrub mode** |
| Old project load | Descriptor `version` field — purane params naye defaults se fill ho jate hain |
| Unknown effect (imported preset) | Placeholder card, project khulta hai, effect skipped |

#### 3.6.9 Data model impact

`clips.effects_json` ka structure:

```json
[
  {
    "effectId": "light.vignette",
    "version": 2,
    "enabled": true,
    "intensity": 0.8,
    "params": { "amount": 0.6, "softness": 0.4, "color": "#101010" },
    "keyframes": {
      "amount": [
        { "t": 0,   "v": 0.2, "ease": "EASE_OUT" },
        { "t": 800, "v": 0.9, "ease": "BEZIER", "h1": [0.3,0.1], "h2": [0.7,0.9] }
      ]
    }
  }
]
```

Naye tables `custom_presets` (user presets) aur `effect_usage` (favorites/recents) — poori schema §5.1 mein hai.

#### 3.6.10 UX: overload se bachne ke liye

1,256 effects × 8 params = **10,000+ controls**. Sab ek saath dikha do toh user bhaag jayega. Isliye:

| Pattern | Detail |
|---|---|
| **Progressive disclosure** | L1 (1 slider) → L2 (params) → L3 (keyframes) |
| **Smart defaults** | Har preset already ek achha look hai — tweak optional |
| **Groups** | Params "Shape / Color / Motion" mein grouped, collapsible |
| **Top-3 first** | Sabse important 3 params pehle, baaki "More" ke andar |
| **Live preview** | Har change turant preview pe (no Apply button) |
| **Undo-friendly** | Param change bhi undo stack mein (debounced — ek drag = ek undo) |
| **Search params** | 12+ params wale effects mein search box |

#### 3.6.11 Cost

| Kaam | Time |
|---|---|
| Descriptor schema + parser | 3 days |
| 12 widget components | 4 days |
| Auto-generated param panel | 3 days |
| Keyframe editor (curve UI) | 5 days |
| Custom preset CRUD + import/export | 3 days |
| Uniform binder + CI validation | 2 days |
| **Total** | **~3 weeks** |

> Ye 3 weeks **bachate** bhi hain — warna 1,256 effects ke manual panels banane mein 6+ mahine lagte, aur har naye effect pe 2 din.

---

## 4. Architecture

### 4.1 Layered view

```
┌─────────────────────────────────────────────────────────┐
│  UI LAYER (Jetpack Compose)                             │
│  Projects · Editor · Captions · Export · Settings       │
└──────────────────────────┬──────────────────────────────┘
                           │ ViewModel (StateFlow)
┌──────────────────────────▼──────────────────────────────┐
│  DOMAIN LAYER                                           │
│  UseCases: AddClip, SplitClip, ApplyEffect, Export...   │
│  TimelineEngine (pure Kotlin, no Android deps)          │
│  UndoRedoManager (command pattern)                      │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  DATA LAYER                                             │
│  Repositories → Room DB + File storage + MediaStore     │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  MEDIA ENGINE                                            │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌───────────┐  │
│  │ Preview  │ │ Renderer  │ │ Exporter │ │ AI Engine │  │
│  │ (Media3) │ │ (GL ES 3) │ │(Media3 + │ │(JNI/ONNX) │  │
│  │          │ │           │ │ FFmpeg)  │ │           │  │
│  └──────────┘ └───────────┘ └──────────┘ └───────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Gradle modules

```
kazun-editor/
├── app/                      # Application, DI, navigation host
├── core/
│   ├── model/                # Pure Kotlin data classes (no Android)
│   ├── common/               # Extensions, Result types, dispatchers
│   ├── database/             # Room entities, DAOs, migrations
│   ├── datastore/            # Preferences (settings, onboarding)
│   ├── data/                 # Repositories, mappers
│   ├── media/                # Probe, thumbnail, waveform, MediaStore
│   ├── timeline/             # TimelineEngine — pure Kotlin ⭐
│   ├── render/               # OpenGL compositor, shaders, LUT
│   ├── export/               # Transformer + FFmpeg pipelines
│   ├── ai/                   # Model manager, inference wrappers
│   └── ui/                   # Design system: theme, components
├── feature/
│   ├── projects/             # Project dashboard
│   ├── editor/               # Timeline editor (biggest module)
│   ├── captions/             # Transcript + caption editor
│   ├── export/               # Export sheet + progress
│   └── settings/             # Settings, model downloads, licenses
├── native/
│   └── whisper/              # CMake, whisper.cpp JNI bridge
└── benchmark/                # Macrobenchmark + baseline profiles
```

**Rule:** `core:timeline` aur `core:model` mein **zero Android imports**. Isse pure JVM unit tests likh sakte hain (JUnit, no Robolectric) — timeline logic bugs sabse zyada yahan hote hain.

---

## 5. Core data model

### 5.1 Room schema

```sql
-- Project
CREATE TABLE projects (
    id              TEXT PRIMARY KEY,       -- UUID
    name            TEXT NOT NULL,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    aspect_ratio    TEXT NOT NULL,          -- "16:9" | "9:16" | "1:1" | "4:5"
    width           INTEGER NOT NULL,       -- output width
    height          INTEGER NOT NULL,
    fps             INTEGER NOT NULL,       -- 24 | 30 | 60
    thumbnail_path  TEXT,
    duration_ms     INTEGER NOT NULL DEFAULT 0,
    settings_json   TEXT NOT NULL DEFAULT '{}'
);

-- Track
CREATE TABLE tracks (
    id          TEXT PRIMARY KEY,
    project_id  TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,              -- VIDEO | AUDIO | TEXT | OVERLAY
    sort_order  INTEGER NOT NULL,
    is_muted    INTEGER NOT NULL DEFAULT 0,
    is_locked   INTEGER NOT NULL DEFAULT 0,
    volume      REAL NOT NULL DEFAULT 1.0
);

-- Clip (ek media ka ek slice)
CREATE TABLE clips (
    id              TEXT PRIMARY KEY,
    track_id        TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    source_uri      TEXT NOT NULL,          -- content:// URI
    source_duration_ms INTEGER NOT NULL,
    trim_start_ms   INTEGER NOT NULL DEFAULT 0,
    trim_end_ms     INTEGER NOT NULL,
    timeline_start_ms INTEGER NOT NULL,     -- position on timeline
    speed           REAL NOT NULL DEFAULT 1.0,
    volume          REAL NOT NULL DEFAULT 1.0,
    transform_json  TEXT NOT NULL DEFAULT '{}',  -- x, y, scale, rotation
    opacity         REAL NOT NULL DEFAULT 1.0,
    blend_mode      TEXT NOT NULL DEFAULT 'NORMAL',
    effects_json    TEXT NOT NULL DEFAULT '[]',
    keyframes_json  TEXT NOT NULL DEFAULT '[]',
    sort_order      INTEGER NOT NULL
);

-- Transition (do clips ke beech)
CREATE TABLE transitions (
    id            TEXT PRIMARY KEY,
    track_id      TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    after_clip_id TEXT NOT NULL,
    type          TEXT NOT NULL,
    duration_ms   INTEGER NOT NULL
);

-- Text overlay properties (clip type TEXT ke liye 1:1)
CREATE TABLE text_props (
    clip_id     TEXT PRIMARY KEY REFERENCES clips(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    font_family TEXT NOT NULL,
    font_size   REAL NOT NULL,
    color       INTEGER NOT NULL,
    bg_color    INTEGER,
    alignment   TEXT NOT NULL DEFAULT 'CENTER',
    style_json  TEXT NOT NULL DEFAULT '{}'
);

-- Background task (thumbnail, transcribe, export)
CREATE TABLE tasks (
    id          TEXT PRIMARY KEY,
    project_id  TEXT REFERENCES projects(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,              -- THUMBNAIL | WAVEFORM | TRANSCRIBE | EXPORT
    status      TEXT NOT NULL,              -- QUEUED | RUNNING | DONE | FAILED
    progress    INTEGER NOT NULL DEFAULT 0,
    payload_json TEXT NOT NULL,
    error       TEXT,
    created_at  INTEGER NOT NULL
);

-- Caption segment (Whisper output)
CREATE TABLE captions (
    id          TEXT PRIMARY KEY,
    project_id  TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    start_ms    INTEGER NOT NULL,
    end_ms      INTEGER NOT NULL,
    text        TEXT NOT NULL,
    word_timings_json TEXT
);
```

-- User-made effect/stack presets (§3.6.6)
CREATE TABLE custom_presets (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    category     TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    thumbnail    TEXT,
    created_at   INTEGER NOT NULL,
    usage_count  INTEGER NOT NULL DEFAULT 0,
    is_favorite  INTEGER NOT NULL DEFAULT 0
);

-- Favorites / recents for built-in effects
CREATE TABLE effect_usage (
    effect_id   TEXT PRIMARY KEY,
    is_favorite INTEGER NOT NULL DEFAULT 0,
    use_count   INTEGER NOT NULL DEFAULT 0,
    last_used   INTEGER NOT NULL
);
```

**Indexes:** `clips(track_id, timeline_start_ms)`, `tasks(project_id, status)`, `captions(project_id, start_ms)`, `custom_presets(category, usage_count)`.

### 5.2 File storage

```
filesDir/
├── projects/{projectId}/thumbnail.jpg
├── cache/thumbs/{hash}.jpg          # clip thumbnails, evictable
├── cache/waveforms/{hash}.dat       # downsampled peaks
├── models/whisper-tiny-q8.bin       # ~42MB
├── models/silero-vad.onnx           # ~2MB
├── models/selfie_seg.onnx           # ~250KB
└── exports/{projectId}_{timestamp}.mp4
```

Media **copy nahi hota** — sirf `content://` URI store hota hai. User ne file delete ki toh clip missing mark hogi, crash nahi.

---

## 6. Timeline engine (sabse critical part)

### 6.1 Model

```kotlin
// core:model — pure Kotlin, no Android
data class TimelineState(
    val projectId: String,
    val tracks: List<Track>,
    val durationMs: Long,
    val playheadMs: Long,
    val selection: Set<String>,   // clip ids
    val zoom: Float
)

data class Track(
    val id: String,
    val type: TrackType,
    val clips: List<Clip>,        // sorted by timelineStartMs, non-overlapping
    val muted: Boolean,
    val locked: Boolean
)

data class Clip(
    val id: String,
    val sourceUri: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val timelineStartMs: Long,
    val speed: Float,
    val effects: List<Effect>,
    val keyframes: List<Keyframe>,
    val transform: Transform
) {
    val durationMs: Long get() = ((trimEndMs - trimStartMs) / speed).toLong()
    val timelineEndMs: Long get() = timelineStartMs + durationMs
}
```

### 6.2 Rules (invariants)

1. Ek track pe clips **overlap nahi** kar sakte — move karte waqt auto-shift ya reject
2. `trimStartMs < trimEndMs <= sourceDurationMs` — hamesha
3. Gapless optional: default mein gaps allowed (user ki marzi)
4. Track duration = `max(clip.timelineEndMs)`, project duration = `max(track duration)`
5. Text/overlay clips ki koi source duration nahi — user-defined length

### 6.3 Edit operations (command pattern)

```kotlin
interface EditCommand {
    fun apply(state: TimelineState): TimelineState
    fun revert(state: TimelineState): TimelineState
    val label: String   // "Split clip" — undo toast ke liye
}
```

**Commands:** `AddClip`, `RemoveClip`, `MoveClip`, `TrimStart`, `TrimEnd`, `SplitClip`, `SetSpeed`, `AddEffect`, `RemoveEffect`, `UpdateKeyframe`, `AddTransition`, `SetVolume`, `ReorderTrack`.

**Undo/redo:** `ArrayDeque<EditCommand>` (max 100). Har command immutable state return karta hai — **bug-proof** kyunki purana state kabhi mutate nahi hota.

**Persistence:** har 2 second (debounced) mein poora timeline Room mein write. Command stack memory mein — process death pe undo history jaaye toh chalega, project data nahi jaana chahiye.

### 6.4 Split logic

```
Split(clip, atMs):
  local = (atMs - clip.timelineStartMs) * speed + clip.trimStartMs
  left  = clip.copy(id = new(), trimEndMs = local)
  right = clip.copy(id = new(), trimStartMs = local, timelineStartMs = atMs)
  keyframes/effects ko position ke hisaab se baanto
```

### 6.5 Snapping

Snap targets: playhead, clip edges (dono taraf), track start, 0ms, markers.
Threshold: **8dp** screen space (zoom-independent). Magnetic feel ke liye haptic feedback.

---

## 7. Preview & rendering

### 7.1 Reality check

`ExoPlayer` ek time pe **ek** video decode karta hai. Multi-layer compositing ke liye custom GL pipeline chahiye.

### 7.2 Two-stage approach

**Phase 1 — Fast path (MVP):**
- Active video track ka current clip → `ExoPlayer` → `SurfaceView`
- Text/filters preview: same surface pe Compose overlay + `RenderEffect` (Android 12+) ya simple GL pass
- Limitations accept karo: ek video layer only

**Phase 2 — Full compositor:**

```
MediaCodec decoder (Video A) ──► SurfaceTexture ──┐
MediaCodec decoder (Video B) ──► SurfaceTexture ──┤
Bitmap (text/sticker) ────────────────────────────┼──► GL Compositor ──► SurfaceView
LUT texture ──────────────────────────────────────┘       │
                                                          ├─ per-layer transform
                                                          ├─ blend mode
                                                          ├─ effect chain
                                                          └─ transition mix
```

**Frame clock:** `Choreographer` pe driven, project fps se locked. Audio master clock (`AudioTrack` position) se sync — drift ho toh video frame skip/dup karo, audio kabhi na todo.

### 7.3 Shader architecture

```
Vertex: standard quad, MVP matrix per layer
Fragment (chained FBOs):
  [decode tex] → [color grade] → [LUT] → [effect chain] → [transform] → [blend] → [transition] → screen
```

Har effect ek fragment shader file — `assets/shaders/effects/*.glsl` — plus ek `descriptor.json` (§3.6). Naya effect add karne ke liye sirf shader + JSON chahiye, **Kotlin code nahi** — UI bhi auto-generate ho jati hai.

```
assets/effects/light.vignette/
├── vignette.frag        # GLSL
├── vignette.vert        # shared quad vertex
└── descriptor.json      # params, ranges, UI groups, cost class
```

**Build-time validation:** CI script har descriptor ke params ko shader uniforms se match karta hai. Mismatch = build fail. Runtime pe missing uniform = default value, crash nahi.

### 7.4 Performance budget

| Metric | Target |
|---|---|
| Preview FPS (1080p, 2 layers) | 30 fps stable |
| Seek latency | < 150ms |
| Memory (editor screen) | < 400MB |
| Cold start → project list | < 1.5s |
| Timeline scroll | 60 fps |

**Tools:** Macrobenchmark + baseline profile. Low-end device pe **preview resolution 540p**, export 1080p — best of both.

---

## 8. Export pipeline

### 8.1 Primary: Media3 Transformer

Kyun: phone ka **hardware encoder** (MediaCodec) use karta hai → 1080p export ~1-2x realtime, kam heat.

```kotlin
val composition = Composition.Builder(
    EditedMediaItemSequence.Builder(videoItems)
        .addItem(audioItem)
        .build()
).build()

Transformer.Builder(context)
    .setVideoMimeType(MimeTypes.VIDEO_H264)
    .setAudioMimeType(MimeTypes.AUDIO_AAC)
    .addListener(progressListener)
    .build()
    .start(composition, outputPath)
```

Transformer `GlEffect` interface deta hai — hamare shaders seedhe yahan plug ho jate hain, matlab **preview aur export same shader** use karte hain. Ye consistency ka biggest win hai.

### 8.2 Fallback: FFmpeg

Kab use hoga:
- Unsupported codec (rare, jaise HEVC 10-bit)
- GIF / SRT / audio-only export
- Advanced filters jo Transformer support nahi karta

**FFmpegKit retirement issue:** original `com.arthenica:ffmpeg-kit` Jan 2025 mein retire [1], binaries April 2025 mein hata diye. Options:

| Option | Verdict |
|---|---|
| `arthenica/ffmpeg-kit-next` (official continuation, source-only) | ✅ Recommended — khud build karo |
| `ffmpegkit-maintained/ffmpeg` (community fork, Maven Central, 16KB ready) | ✅ Fastest migration |
| Khud ka NDK build | Full control, zyada kaam |

**Decision:** FFmpegKitNext se shuru, apne CI mein build. Dependency ko `core:export` ke peeche abstract rakho (`VideoEncoder` interface) — library badalni ho toh ek jagah change.

### 8.3 Export settings

| Preset | Resolution | Bitrate | FPS |
|---|---|---|---|
| WhatsApp | 720p | 3 Mbps | 30 |
| Reels / Shorts | 1080x1920 | 8 Mbps | 30 |
| YouTube 1080p | 1920x1080 | 12 Mbps | 30/60 |
| High quality | 1920x1080 | 16 Mbps | source |
| Custom | user | user | user |

**Foreground service** mandatory hai — 10+ min export background mein chale, notification ke saath progress. `WAKE_LOCK` + `FOREGROUND_SERVICE_MEDIA_PROCESSING` (Android 14+).

### 8.4 Export job flow

```
User taps Export
  → validate timeline (missing media? empty?)
  → build Composition
  → start ForegroundService
  → Transformer progress → notification + UI
  → on success → MediaStore insert → share sheet
  → on fail → error + retry option (partial file delete)
```

---

## 9. AI architecture

### 9.1 Principle

**Sab on-device.** Model download optional hai (ek baar, Wi-Fi), uske baad hamesha offline. User ke video kabhi device se bahar nahi jate — ye marketing line bhi hai aur sach bhi.

### 9.2 Model manager

```kotlin
interface AiModel {
    val id: String            // "whisper-tiny-q8"
    val sizeBytes: Long
    val sha256: String
    val downloadUrl: String   // GitHub Releases asset
    fun isInstalled(): Boolean
    suspend fun download(onProgress: (Int) -> Unit)
    fun delete()
}
```

Download **GitHub Releases** se — free hosting, no CDN cost, versioned.

| Model | Size | License | Feature |
|---|---|---|---|
| whisper-tiny-q8 | 42MB | MIT | Fast captions |
| whisper-base-q8 | 78MB | MIT | Accurate captions |
| silero-vad | 2MB | MIT | Silence detection |
| selfie_segmentation | 250KB | Apache 2.0 | Background removal |
| piper-hi + en | 60MB each | MIT | TTS |

### 9.3 Captions pipeline

```
Video → extract audio (16kHz mono WAV, FFmpeg)
      → VAD se speech segments (Silero)
      → har segment Whisper pe (whisper.cpp JNI, int8)
      → word timestamps merge
      → Room captions table
      → editable transcript UI
```

**Performance (measured on similar setups):** Pixel 8 pe base-q4 ~1.2x realtime, Redmi Note 12 pe tiny-q8 ~1.4x realtime. Matlab 5 min video ≈ 4-6 min process — background service mein, progress ke saath. User ko clear expectation dikhana hoga.

**Language:** multilingual model (Hindi + English + 90 more). `whisper-tiny.en` sirf English ke liye faster hai — optional download.

### 9.4 Silence detection

Silero VAD ONNX → speech probability per 30ms frame → threshold 0.5 → segments < 300ms gap ko "silence" maano → auto-cut commands generate karo (user preview kare, phir apply).

**Important:** destructive nahi — generated cuts ek "pending edit" hote hain jo user approve/reject kare.

### 9.5 Background removal

MediaPipe `ImageSegmenter` (selfie multiclass) → per-frame mask → GL shader mein `mix(video, background, mask)`.

- Preview: 30fps pe possible on mid-range (GPU delegate)
- Export: Transformer ke `GlEffect` ke through same shader
- Alternative background: color, blur, image — sab shader se

### 9.6 TTS

Piper ONNX Runtime → 22kHz WAV → audio track pe clip. Hindi + English voices. Model ~60MB per voice, optional download.

### 9.7 Beat detection

Pure DSP — no AI. Energy-based onset detection on low-pass filtered audio → peaks → BPM estimate → cut markers. Free, instant, offline.

---

## 10. Screens & navigation

### 10.1 Navigation graph

```
Splash
  └─► Onboarding (first run only)
        └─► Projects (home)
              ├─► Editor/{projectId}
              │     ├─► MediaPicker (bottom sheet)
              │     ├─► Captions (full screen)
              │     ├─► Export (bottom sheet → progress screen)
              │     ├─► Effects (bottom sheet, tabs)
              │     └─► Settings (per project)
              └─► Settings
                    ├─► AI Models
                    ├─► Storage
                    ├─► Licenses (OSS + model licenses)
                    └─► About
```

### 10.2 Screen list

| # | Screen | Purpose | Complexity |
|---|---|---|---|
| 1 | Onboarding | 3 slides, permissions, model setup | Low |
| 2 | Projects | Grid of projects, FAB "New" | Low |
| 3 | Media Picker | Gallery grid, multi-select, permission | Medium |
| 4 | **Editor** | The product | **Very high** |
| 5 | Captions | Transcript + styled preview | Medium |
| 6 | Export | Presets + progress | Low |
| 7 | Effects | Bottom sheet: filters, transitions, text, audio | High |
| 8 | Settings | Preferences, models, storage | Low |

### 10.3 Editor wireframe (portrait)

```
┌──────────────────────────────┐
│ ←  Project name        ⋮  ⤴  │  ← top bar
├──────────────────────────────┤
│                              │
│                              │
│        VIDEO PREVIEW         │  ← 16:9, black bg
│        (tap = fullscreen)    │
│                              │
│                              │
├──────────────────────────────┤
│  ▶  ⏮  ⏭   00:12.4 / 01:30  │  ← transport
├──────────────────────────────┤
│  ✂  ⧉  🗑   🔍 ─────●────    │  ← tool bar + zoom
├──────────────────────────────┤
│ V2 │    [clip A]   [clip B]  │  ← video track 2
│ V1 │ [=== clip A ===][===B=] │  ← video track 1 (thumbs)
│ A1 │ ~~~~ waveform ~~~~~~~~  │  ← audio
│ T1 │   [text]      [text]    │  ← text/overlay
│    │▲                        │  ← playhead
├──────────────────────────────┤
│  🎨  📝  🎵  ⚡  ⏱  🤖      │  ← bottom action bar
└──────────────────────────────┘
```

**Bottom action bar:** Filters · Text · Audio · Effects · Speed · AI

### 10.4 Editor layout rules

- Preview area: **40%** of screen height (portrait), fixed
- Timeline: remaining space, **min 3 tracks visible**
- Track header: 44dp fixed width, horizontally pinned
- Clip min width: 24dp (tap target), max zoom pe thumbnail har 1s
- Playhead always center? **No** — free-moving with auto-scroll on playback (like CapCut), predictable
- Selected clip: 2dp accent border + handles
- Empty state: "Tap + to add media" dashed placeholder

### 10.5 Effects panel — layout

Bottom sheet, 3 snap points (peek 25% / half 55% / full 90%).

```
┌──────────────────────────────────┐
│  ── drag handle ──        ✕      │
│  [ Filters ][ Effects ][ Text ]  │  ← top tabs
│  🔍 Search effects...            │
├──────────────────────────────────┤
│  FAVORITES ★  RECENTS  ↑         │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐    │  ← 128px thumbs
│  │Teal│ │Noir│ │VHS │ │Neon│    │
│  └────┘ └────┘ └────┘ └────┘    │
│  ── Cinematic 20 ──              │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐    │
│  ...                             │
├──────────────────────────────────┤
│  AFTER SELECTING AN EFFECT:      │
│  Intensity ──────●──── 70%       │  ← L1
│  ▸ Advanced (8 params)           │  ← L2
│  ⏱ Animate                       │  ← L3
│  [ Reset ]  [ Save preset ]  🎲  │
└──────────────────────────────────┘
```

**Rules:**
- Effect tap → turant apply + preview (no confirm button)
- Long-press → before/after split view
- Drag & drop → reorder stack
- Stack chips upar dikhte hain (max 6), tap → params, ✕ → remove

### 10.6 Interaction details

| Gesture | Action |
|---|---|
| Tap clip | Select |
| Drag clip body | Move (snap) |
| Drag clip edge | Trim |
| Pinch timeline | Zoom |
| Two-finger drag timeline | Scroll + zoom |
| Tap playhead area | Seek |
| Long-press clip | Context menu (split/delete/duplicate) |
| Double-tap preview | Fit / fill toggle |
| Swipe up on preview | Fullscreen |

**Haptics:** snap pe light tick, trim limit pe double-buzz, split pe medium.

---

## 11. Tech stack — exact versions

| Component | Library | Version (approx) |
|---|---|---|
| Language | Kotlin | 2.1.x |
| UI | Compose BOM | 2026.x |
| Player | Media3 ExoPlayer | 1.6+ |
| Export | Media3 Transformer | 1.6+ |
| DB | Room | 2.7+ |
| DI | Hilt | 2.55+ |
| Async | Coroutines + Flow | 1.10+ |
| Images | Coil | 3.x |
| Navigation | Navigation Compose | 2.8+ |
| Settings | DataStore | 1.1+ |
| GL | OpenGL ES 3.0 (built-in) | — |
| Whisper | whisper.cpp (NDK, CMake) | latest |
| VAD/Segmentation | ONNX Runtime Mobile / MediaPipe Tasks | 1.20+ / 0.10+ |
| FFmpeg | FFmpegKitNext | 8.x |
| Tests | JUnit5, Turbine, Roborazzi (screenshot) | — |
| Build | Gradle KSP, version catalog | 8.x |

**Compose Multiplatform?** Nahi. Android-only hai, abhi KMP ka overhead faayda nahi.

---

## 12. Performance & quality strategy

### 12.1 The hard constraints

| Constraint | Approach |
|---|---|
| Phone RAM | Decode at preview resolution (540p), not source |
| Thermal | Export chunked, `Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND)`, cooldown check |
| Battery | Hardware encoder only when possible, no software fallback without warning |
| Storage | Thumbnails 128px, waveform downsampled to 200 peaks/sec, cache eviction 500MB cap |
| ANR | Har heavy kaam `ForegroundService` + `WorkManager` |
| OOM | Bitmap pooling, `inSampleSize`, no full-res bitmap in memory |

### 12.2 Testing

| Layer | Test type | Coverage goal |
|---|---|---|
| Timeline engine | Pure JUnit | 90%+ (split/trim/move edge cases) |
| Repositories | Room in-memory | 80% |
| ViewModels | Turbine + coroutines test | 70% |
| UI | Compose UI tests | Critical flows only |
| Visual | Roborazzi screenshots | Editor states |
| Device | Macrobenchmark | Startup, scroll, export |

**Timeline property tests:** random sequence of 100 operations → invariants always hold (no overlap, valid trims, duration consistent). Ye bugs ka 80% pehle hi pakad lega.

### 12.3 Crash & telemetry policy

**Zero telemetry by default.** "No data leaves device" ka matlab sach mein zero.
Optional, off-by-default, explicit opt-in: crash reports. Even then — no video content, no file names, no project data. Sirf stack trace + device model.

---

## 13. Licensing (free public distribution ke liye)

### 13.1 App license

**Recommendation: GPL-3.0**
Kyun: FFmpeg agar GPL components (x264) ke saath build hua toh poori app GPL honi chahiye. App free + open source hai toh koi dikkat nahi — aur GitHub pe public hone se license compliance dikhane mein easy rehta hai.

**Alternative:** LGPL FFmpeg build (no x264, hardware encoding only) → app Apache-2.0 rakh sakte. Zyada freedom, thoda kam compatibility.

### 13.2 Third-party obligations

| Dependency | License | Obligation |
|---|---|---|
| FFmpeg (`x264` ke saath) | GPL-3.0 | App GPL, source publish |
| FFmpeg (LGPL build) | LGPL-2.1 | Notice + relinkable |
| Media3 | Apache-2.0 | Notice |
| Whisper (model) | MIT | Attribution |
| whisper.cpp | MIT | Notice |
| Silero VAD | MIT | Notice |
| MediaPipe | Apache-2.0 | Notice |
| Piper | MIT | Notice |
| OpenGL ES | — | Platform |
| Jetpack/Compose | Apache-2.0 | Notice |

**Action:** `feature:settings → Licenses` screen mein auto-generated list (plugin se) + full texts. `LICENSE` file repo root mein (GPL-3.0 full text).

### 13.3 Model weights

Models app ke saath **bundle nahi** honge — runtime pe download (GitHub Releases assets). Isse:
- APK chhota (25MB vs 200MB+)
- Sirf zaroori model download
- Model license separately displayed

---

## 14. Distribution — GitHub Releases only

**Decision:** Sirf GitHub Releases. F-Droid nahi, Play Store nahi, koi store account nahi.

| Channel | Status | Cost |
|---|---|---|
| **GitHub Releases** | ✅ Only channel | ₹0 |
| F-Droid | ❌ Dropped | — |
| Play Store | ❌ Dropped | — |
| APK mirrors (APKMirror etc.) | ❌ Nahi | — |

### 14.1 Isse kya badal gaya

| Pehle F-Droid ke liye chahiye tha | Ab zaroorat nahi |
|---|---|
| Reproducible builds | ❌ Hata diya |
| No proprietary blobs rule | ❌ Hata diya (MediaPipe rakh sakte hain) |
| Exact upstream source tagging | ❌ Hata diya |
| FOSS-only dependency audit | ⚠️ Still good practice, par blocker nahi |
| Model weights alag rakhna | ⚠️ Ab APK mein bundle *kar sakte hain*, par size (200MB+) — recommendation: on-demand download hi rakho |

**Ek tradeoff jo aa gaya:** store nahi hai, matlab **auto-update nahi milega**. User ko naya APK khud install karna padega. Isliye §14.3 ka in-app updater zaroori ho gaya.

### 14.2 Release artifacts

| File | Content | Size |
|---|---|---|
| `Kazun-Editor-{version}-arm64.apk` | 99% devices | ~25-30MB |
| `Kazun-Editor-{version}-armv7.apk` | Purane phones (optional) | ~22MB |
| `Kazun-Editor-{version}-universal.apk` | Dono ABIs (optional) | ~40MB |
| `SHA256SUMS.txt` | Checksums | <1KB |
| `CHANGELOG.md` | Kya badla | — |

**APK install karne ke liye user ko:** 1 baar "Install unknown apps" allow karna padta hai. Onboarding screen mein step-by-step help + screenshot rakhna hoga — warna 50% users yahin ruk jayenge.

### 14.3 In-app update checker (naya requirement)

Store nahi hai, matlab update khud check karna hoga.

```
App start (max 1x/day) → GET https://api.github.com/repos/{owner}/Kazun-Editor/releases/latest
  → compare tag_name with BuildConfig.VERSION_NAME
  → naya hai? → Settings mein badge + optional dialog
  → user tap → browser mein release page (ya direct APK download)
  → FileProvider + PackageInstaller intent se install
```

| Concern | Handling |
|---|---|
| API rate limit | 60 req/hour/IP unauthenticated — hum 1/day hi call karte hain, safe |
| Privacy | Ye **optional** check hai, default OFF. On kare toh sirf ek public API call — koi device ID nahi bhejte |
| Offline | Fail ho toh silently ignore, retry next launch |
| Beta channel | GitHub pre-releases se — Settings mein "Beta updates" toggle |

### 14.4 Signing

Ek keystore, GitHub Secrets mein. **Lose karna = app kabhi update nahi kar sakte** (Android signature check fail karega, user ko uninstall karna padega).

**Backup protocol:**
1. `kazun-release.jks` — 2 offline copies (pendrive + encrypted cloud)
2. Password — password manager mein
3. Ye 3 cheezein repo mein **kabhi nahi** (`.gitignore` mein confirm)

### 14.5 Version scheme

`MAJOR.MINOR.PATCH` + monotonically increasing `versionCode`.
`0.1.0` = first public alpha. `1.0.0` = feature complete.

### 14.6 Release cadence

| Type | Kab | Tag |
|---|---|---|
| Alpha | Har phase ke end pe | `v0.1.0-alpha`, `v0.2.0-alpha`... |
| Beta | AI phase ke baad | `v0.9.0-beta` |
| Stable | Polish ke baad | `v1.0.0` |

Har release mein: signed APK + SHA256 + changelog + (screenshots optional).

---

## 15. Build & CI

```
.github/workflows/
├── ci.yml          # PR: lint, unit tests, assemble debug
└── release.yml     # tag: build release APK (arm64 + armv7), sign, SHA256, GitHub Release
```

**Release trigger:** `git tag v1.0.0 && git push origin v1.0.0` → CI khud release bana deta hai. Manual upload ki zaroorat nahi.

**CI steps:** JDK 21 → Gradle cache → `./gradlew lint testDebugUnitTest assembleRelease` → sign → upload artifacts. Total ~12 min free tier pe.

**Native builds:** whisper.cpp aur FFmpeg prebuilt artifacts CI mein cache honge — har build pe 40 min compile nahi.

---

## 16. Roadmap

| Phase | Duration | Deliverable | Definition of done |
|---|---|---|---|
| **0. Foundation** | 1 week | Project skeleton, DI, theme, navigation, CI | App install hota hai, empty screens |
| **1. Core editor** | 4-5 weeks | Import → timeline → trim/split → export | 1080p video edit + export ho jaye |
| **2. Effects** | 8-10 weeks | 1,256 effects + customization engine (auto UI, keyframes, presets) | Har effect tweakable |
| **3. AI** | 3-4 weeks | Captions, silence cut, background removal, TTS | 5 min video ki captions 6 min mein |
| **4. Polish** | 2-3 weeks | Templates, onboarding, a11y, perf, in-app updater | Public GitHub Release 1.0 |

**Total: ~14-17 weeks** (part-time solo ke hisaab se realistic; full-time aadha).

**Har phase ke end pe:**
- Working APK (GitHub Release, `v0.x.0-alpha`)
- Demo video
- Test checklist pass

---

## 17. Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Low-end devices pe crash | High | Device tier gating, preview 540p, memory tests on real 4GB device |
| Export slow / heat | High | Hardware encoder first, chunked export, thermal pause |
| GL compositor complexity | High | Phase 1 simple path, Phase 2 incremental — ek saath nahi |
| FFmpeg licensing mistake | Medium | LGPL build + hardware encode, legal review before release |
| Whisper accuracy (Hindi) | Medium | base model option, manual caption editing always |
| Timeline bugs (edge cases) | High | Property-based tests, immutable state, command pattern |
| Storage bloat | Medium | Cache cap, auto-evict, storage screen |
| Google Play rejection (later) | Low | 16KB alignment, targetSdk, data safety form |
| Solo dev burnout | Real | Phase-wise shipping, har phase ke baad break |

---

## 18. Decisions — resolved & remaining

### 18.1 Resolved

| Decision | Value | Date |
|---|---|---|
| Package name | `com.kazun.editor` | 2026-10-23 |
| UI language | English only (v1) | 2026-10-23 |
| Minimum device | 6GB RAM | 2026-10-23 |
| Distribution | GitHub Releases only | 2026-10-23 |
| Cost | ₹0 everywhere | 2026-10-23 |

### 18.2 Main ne khud decide kiya (user ne "baki sab tu decide kar" bola)

| Decision | Value | Kyun |
|---|---|---|
| **Templates (3)** | ① Reels Hook (9:16, 15s) ② YouTube Intro (16:9, 5s) ③ Photo Slideshow (16:9, 30s + music) | Teen sabse common use-cases cover ho jate hain |
| **Telemetry** | **Zero.** Koi crash SDK nahi. | "No data leaves device" ka promise sach rahe. Settings mein "Copy debug info" button — user khud paste kare agar bug report karna ho |
| **License** | GPL-3.0 | FFmpeg (x264) compliance |
| **Theme** | Dark-first, Material 3, accent color `#FF4D5A` (Kazun red) | Video editing mein dark default, eyes par kam load |
| **Fonts** | 2 bundled (Inter, Bebas Neue) + system fallback | APK size vs choice ka balance |
| **Default export** | 1080p 30fps, 16:9, 8 Mbps | Sabse safe default |
| **Min SDK** | 26, with RAM gate at 5.5GB | 6GB devices mostly Android 10+, par code compatible rakho |
| **Autosave** | 2 sec debounce, 3 backup slots | Crash pe maximum 2 sec ka loss |
| **Onboarding** | 3 screens, skippable | Permissions + model download explain karna zaroori |

---

## 19. Ye doc kya nahi hai

- Koi code nahi — implementation Phase 0 mein
- Koi UI mockup nahi — wireframe-level only (actual design baad mein)
- Koi timeline estimate guarantee nahi — estimates hain, commitments nahi

---

## 20. Decision log

| # | Decision | Value | Date | Kyun |
|---|---|---|---|---|
| 1 | Platform | Android only | 2026-10-23 | User ka choice — mobile app, web nahi |
| 2 | Cost model | 100% free (user + dev) | 2026-10-23 | "Apas futi kaudi nahi" — ₹0 budget |
| 3 | AI approach | On-device only, no APIs | 2026-10-23 | Sab kuch apna ho, koi bill na aaye |
| 4 | Distribution | **GitHub Releases only** | 2026-10-23 | Store account/paisa nahi, full control |
| 5 | Export engine | Media3 Transformer primary | — | Hardware encoder = fast + kam heat |
| 6 | FFmpeg | FFmpegKitNext | — | Original FFmpegKit retired Jan 2025 |
| 7 | Background removal | MediaPipe (not BiRefNet) | — | 250KB vs 900MB, realtime on phone |
| 8 | Package name | `com.kazun.editor` | 2026-10-23 | User confirmed |
| 9 | UI language | English only | 2026-10-23 | User confirmed |
| 10 | Minimum device | 6GB RAM | 2026-10-23 | User confirmed |
| 11 | Effects count | **1,256** (1,244 core + 12 AI) | 2026-10-23 | §3.5 catalog — 156 primitives + 1,100 presets |
| 14 | Customization | **Har effect fully tweakable** — 3 levels, 12 widget types, keyframes, custom presets | 2026-10-23 | §3.6 |
| 12 | Telemetry | Zero | 2026-10-23 | Privacy promise |
| 13 | Templates | Reels Hook / YouTube Intro / Photo Slideshow | 2026-10-23 | Most common use-cases |

---

## References

1. FFmpegKit retirement aur alternatives — https://www.itpathsolutions.com/ffmpegkit-shutdown-what-to-do-next
2. whisper.cpp Android performance numbers — https://mvpfactory.io/blog/fine-tuning-whisper-cpp-for-on-device-speech-to-text-in-kmp-quantization
3. MediaPipe Selfie Segmentation — https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter
4. Media3 Transformer — https://developer.android.com/media/media3/transformer
5. FFmpegKitNext — https://github.com/arthenica/ffmpeg-kit-next

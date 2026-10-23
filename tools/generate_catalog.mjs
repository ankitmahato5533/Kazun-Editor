#!/usr/bin/env node
/**
 * Generates app/src/main/assets/effects/catalog.json
 * 1,256 effects = 156 primitives + 1,100 data presets.
 * Deterministic output — same input always produces the same file.
 */
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

const out = [];
let uid = 0;
const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');

function add(category, name, opts = {}) {
  uid += 1;
  out.push({
    id: `${category}.${slug(name)}.${uid}`,
    name,
    category,
    cost: opts.cost ?? 'cheap',
    description: opts.description ?? '',
    params: opts.params ?? defaultParams(category),
    keywords: opts.keywords ?? [],
  });
}

const P = {
  amount: (def = 0.7) => ({ key: 'amount', label: 'Amount', type: 'FLOAT', default: def, min: 0, max: 1, step: 0.01, group: 'Main' }),
  intensityPct: (def = 100) => ({ key: 'intensityPct', label: 'Intensity', type: 'FLOAT', default: def, min: 0, max: 100, step: 1, group: 'Main' }),
  softness: () => ({ key: 'softness', label: 'Softness', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Shape' }),
  speed: () => ({ key: 'speed', label: 'Speed', type: 'FLOAT', default: 1, min: 0.1, max: 4, step: 0.05, group: 'Motion' }),
  direction: () => ({ key: 'direction', label: 'Direction', type: 'ENUM', options: ['Left', 'Right', 'Up', 'Down'], defaultOption: 'Left', group: 'Main' }),
  color: (def = '#FFFFFF') => ({ key: 'color', label: 'Color', type: 'COLOR', defColor: def, group: 'Color' }),
  angle: (def = 0) => ({ key: 'angle', label: 'Angle', type: 'ANGLE', default: def, min: 0, max: 360, step: 1, group: 'Main' }),
  seed: () => ({ key: 'seed', label: 'Random seed', type: 'SEED', default: 1, min: 1, max: 9999, step: 1, group: 'Main' }),
  density: () => ({ key: 'density', label: 'Density', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Particles' }),
  size: () => ({ key: 'size', label: 'Size', type: 'FLOAT', default: 0.5, min: 0.05, max: 2, step: 0.01, group: 'Particles' }),
  gravity: () => ({ key: 'gravity', label: 'Gravity', type: 'FLOAT', default: 0.3, min: -1, max: 1, step: 0.01, group: 'Physics' }),
  wind: () => ({ key: 'wind', label: 'Wind', type: 'FLOAT', default: 0, min: -1, max: 1, step: 0.01, group: 'Physics' }),
  gain: () => ({ key: 'gain', label: 'Gain (dB)', type: 'FLOAT', default: 0, min: -24, max: 12, step: 0.5, group: 'Level' }),
  eqLow: () => ({ key: 'eqLow', label: 'Low', type: 'FLOAT', default: 0, min: -12, max: 12, step: 0.5, group: 'EQ' }),
  eqMid: () => ({ key: 'eqMid', label: 'Mid', type: 'FLOAT', default: 0, min: -12, max: 12, step: 0.5, group: 'EQ' }),
  eqHigh: () => ({ key: 'eqHigh', label: 'High', type: 'FLOAT', default: 0, min: -12, max: 12, step: 0.5, group: 'EQ' }),
  mix: () => ({ key: 'mix', label: 'Mix', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Main' }),
  radius: () => ({ key: 'radius', label: 'Radius', type: 'FLOAT', default: 0.3, min: 0, max: 1, step: 0.01, group: 'Main' }),
  threshold: () => ({ key: 'threshold', label: 'Threshold', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Main' }),
  rate: () => ({ key: 'rate', label: 'Rate', type: 'FLOAT', default: 1, min: 0.25, max: 4, step: 0.05, group: 'Main' }),
  thickness: () => ({ key: 'thickness', label: 'Thickness', type: 'FLOAT', default: 0.1, min: 0, max: 1, step: 0.01, group: 'Shape' }),
};

function defaultParams(category) {
  switch (category) {
    case 'luts': return [P.intensityPct(80), P.mix(), { key: 'hue', label: 'Hue shift', type: 'FLOAT', default: 0, min: -180, max: 180, step: 1, group: 'Color' }, { key: 'temp', label: 'Temperature', type: 'FLOAT', default: 0, min: -100, max: 100, step: 1, group: 'Color' }, { key: 'sat', label: 'Saturation', type: 'FLOAT', default: 0, min: -100, max: 100, step: 1, group: 'Color' }];
    case 'looks': return [P.intensityPct(60), { key: 'contrast', label: 'Contrast', type: 'FLOAT', default: 0, min: -100, max: 100, step: 1, group: 'Tone' }, { key: 'brightness', label: 'Brightness', type: 'FLOAT', default: 0, min: -100, max: 100, step: 1, group: 'Tone' }, { key: 'fade', label: 'Fade', type: 'FLOAT', default: 0, min: 0, max: 100, step: 1, group: 'Tone' }];
    case 'transitions': return [{ key: 'durationMs', label: 'Duration (ms)', type: 'FLOAT', default: 500, min: 100, max: 2000, step: 50, group: 'Timing' }, P.direction(), { key: 'ease', label: 'Ease', type: 'ENUM', options: ['Linear', 'EaseInOut', 'Bounce', 'Elastic'], defaultOption: 'EaseInOut', group: 'Timing' }, P.softness()];
    case 'textanim': return [{ key: 'speed', label: 'Speed', type: 'FLOAT', default: 1, min: 0.25, max: 3, step: 0.05, group: 'Timing' }, { key: 'delayMs', label: 'Delay (ms)', type: 'FLOAT', default: 0, min: 0, max: 2000, step: 10, group: 'Timing' }, { key: 'stagger', label: 'Stagger', type: 'FLOAT', default: 0.2, min: 0, max: 1, step: 0.01, group: 'Timing' }];
    case 'textstyle': return [{ key: 'fontSize', label: 'Size', type: 'FLOAT', default: 48, min: 12, max: 200, step: 1, group: 'Type' }, { key: 'tracking', label: 'Tracking', type: 'FLOAT', default: 0, min: -5, max: 20, step: 0.5, group: 'Type' }, P.color('#FFFFFF'), { key: 'stroke', label: 'Stroke', type: 'FLOAT', default: 0, min: 0, max: 20, step: 1, group: 'Type' }];
    case 'motion': return [P.amount(0.5), P.speed(), { key: 'loop', label: 'Loop', type: 'BOOL', default: 0, group: 'Timing' }];
    case 'particles': return [P.density(), P.size(), P.speed(), P.wind(), P.gravity(), P.color('#FFFFFF'), P.seed()];
    case 'stickers': return [{ key: 'scale', label: 'Scale', type: 'FLOAT', default: 1, min: 0.1, max: 4, step: 0.05, group: 'Transform' }, { key: 'rotation', label: 'Rotation', type: 'ANGLE', default: 0, min: 0, max: 360, step: 1, group: 'Transform' }, P.color('#FFFFFF')];
    case 'audio': return [P.gain(), P.eqLow(), P.eqMid(), P.eqHigh(), P.mix()];
    case 'blur': return [P.radius(), P.angle(), { key: 'samples', label: 'Quality', type: 'INT', default: 9, min: 3, max: 33, step: 2, group: 'Quality' }];
    case 'glitch': return [P.amount(0.5), P.speed(), { key: 'blockSize', label: 'Block size', type: 'FLOAT', default: 0.1, min: 0.01, max: 0.5, step: 0.01, group: 'Main' }, P.seed()];
    case 'light': return [P.amount(0.5), P.angle(), P.color('#FFF2C4'), { key: 'x', label: 'X', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Position' }, { key: 'y', label: 'Y', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Position' }];
    case 'distort': return [P.amount(0.4), { key: 'frequency', label: 'Frequency', type: 'FLOAT', default: 0.3, min: 0.05, max: 2, step: 0.01, group: 'Main' }, { key: 'phase', label: 'Phase', type: 'FLOAT', default: 0, min: 0, max: 6.28, step: 0.01, group: 'Main' }];
    case 'detail': return [P.amount(0.5), P.radius(), { key: 'threshold', label: 'Threshold', type: 'FLOAT', default: 0.05, min: 0, max: 0.5, step: 0.01, group: 'Main' }];
    case 'gradient': return [{ key: 'colorA', label: 'Color A', type: 'COLOR', defColor: '#FF6B6B', group: 'Colors' }, { key: 'colorB', label: 'Color B', type: 'COLOR', defColor: '#4D96FF', group: 'Colors' }, P.angle(), P.mix()];
    case 'split': return [{ key: 'gap', label: 'Gap', type: 'FLOAT', default: 8, min: 0, max: 60, step: 1, group: 'Layout' }, P.thickness(), { key: 'radius', label: 'Corner radius', type: 'FLOAT', default: 0, min: 0, max: 60, step: 1, group: 'Layout' }, P.color('#000000')];
    case 'border': return [P.thickness(), P.color('#FFFFFF'), { key: 'radius', label: 'Radius', type: 'FLOAT', default: 16, min: 0, max: 120, step: 1, group: 'Shape' }, { key: 'shadow', label: 'Shadow', type: 'FLOAT', default: 0, min: 0, max: 1, step: 0.01, group: 'Shape' }];
    case 'chroma': return [P.color('#00FF00'), P.threshold(), P.softness(), { key: 'spill', label: 'Spill', type: 'FLOAT', default: 0.3, min: 0, max: 1, step: 0.01, group: 'Main' }];
    case 'speed': return [P.rate(), { key: 'preservePitch', label: 'Preserve pitch', type: 'BOOL', default: 1, group: 'Audio' }];
    case 'ai': return [{ key: 'confidence', label: 'Confidence', type: 'FLOAT', default: 0.5, min: 0, max: 1, step: 0.01, group: 'Model' }, { key: 'language', label: 'Language', type: 'ENUM', options: ['Auto', 'English', 'Hindi', 'Hinglish'], defaultOption: 'Auto', group: 'Model' }];
    default: return [P.amount()];
  }
}

/* ---------------- A. LUTs & filters: 320 = 16 x 20 ---------------- */
const lutGroups = {
  'Cinematic': ['Teal & Orange', 'Moody Blue', 'Bleach Bypass', 'Film Noir', 'Cold Cinema', 'Golden Hour', 'Anamorphic', 'Blockbuster', 'Silver Screen', 'Night Drama'],
  'Film Stocks': ['Kodak Portra 400', 'Fuji Velvia', 'Ilford HP5', 'CineStill 800T', 'Kodachrome 64', 'Agfa Vista', 'Fuji Superia', 'Ektar 100', 'Tri-X 400', 'Vista Vision'],
  'Vintage': ['Sepia', '70s Fade', 'Polaroid', 'Lomo', 'Cross Process', 'Retro Paper', 'Push Process', 'Faded Print', 'Analog Warm', 'Old Magazine'],
  'Black & White': ['Classic B&W', 'High Contrast B&W', 'Silver', 'Selenium Tone', 'Platinum Print', 'Infrared', 'Noir Hard', 'Soft Grain', 'Charcoal', 'Ivory Mono'],
  'Warm & Golden': ['Golden Hour', 'Sun Kissed', 'Amber', 'Honey', 'Desert Warm', 'Peach Glow', 'Caramel', 'Warm Sand', 'Copper', 'Sunset Film'],
  'Cool & Blue': ['Arctic', 'Moody Blue', 'Steel', 'Cyan Drift', 'Midnight', 'Glacier', 'Cold Steel', 'Blue Hour', 'Ice Tone', 'Deep Sea'],
  'Pastel & Soft': ['Soft Pink', 'Aesthetic Pink', 'Cotton Candy', 'Dreamy', 'Matte Soft', 'Milk', 'Lavender Haze', 'Peach Pastel', 'Soft Mint', 'Cloud Nine'],
  'Neon & Cyber': ['Cyberpunk', 'Neon Nights', 'Blade', 'Synthwave', 'Miami Vice', 'Dark Neon', 'Electric Violet', 'Toxic Green', 'Hot Pink City', 'Chrome Dreams'],
  'Nature': ['Forest', 'Ocean', 'Desert', 'Alpine', 'Jungle Green', 'Autumn Leaf', 'Meadow', 'Canyon', 'Misty Pine', 'Coastal'],
  'Urban': ['Gritty City', 'Concrete', 'Urban Decay', 'Subway', 'Graffiti', 'Night Street', 'Rooftop', 'Asphalt', 'Downtown', 'Alleyway'],
  'Portrait': ['Soft Skin', 'Beauty Glow', 'Warm Portrait', 'Natural Tone', 'Studio', 'Editorial', 'Golden Skin', 'Fresh Face', 'Soft Light', 'Clean Beauty'],
  'Food': ['Crisp Food', 'Fresh', 'Commercial', 'Menu Shot', 'Cafe', 'Bakery', 'Sizzle', 'Farm Table', 'Bright Bite', 'Delicious'],
  'Travel': ['Sunset', 'Tropical', 'Wanderlust', 'Island', 'Safari', 'Golden Beach', 'Road Trip', 'Mountain Air', 'Blue Lagoon', 'Hidden Gem'],
  'Moody': ['Dark Matter', 'Underexposed', 'Shadow Play', 'Horror', 'Thriller', 'Goth', 'Bruised', 'Smoke', 'Blackout', 'Storm'],
  'Seasonal': ['Spring Bloom', 'Summer Heat', 'Monsoon', 'Winter Frost', 'Autumn', 'Festival', 'Harvest', 'Snowfall', 'Cherry Blossom', 'Golden Fall'],
  'Social': ['Clean Bright', 'VSCO Warm', 'Contrast Pop', 'Y2K Cool', 'Influencer', 'Story', 'Reel Ready', 'Feed Fresh', 'Trendy', 'Bright Feed'],
};
const lutVariants = ['', ' Deep', ' Soft', ' Punch', ' Film'];
for (const [group, bases] of Object.entries(lutGroups)) {
  const names = [];
  for (const v of lutVariants) for (const b of bases) { if (names.length < 20) names.push(b + v); }
  for (const n of names) add('luts', n, { keywords: [group.toLowerCase(), ...n.toLowerCase().split(' ')] });
}

/* ---------------- B. Quick looks: 120 = 10 x 12 ---------------- */
const lookGroups = {
  'Exposure': ['Brighten', 'Darken', 'Backlit Fix', 'Overexposed Fix', 'Lift', 'Crush', 'Auto Tone', 'Fill Light', 'Dim', 'Glow', 'Shadow Lift', 'Highlight Roll'],
  'Contrast': ['Punchy', 'Flat', 'Filmic', 'Hard', 'Soft', 'Deep Blacks', 'Low Contrast', 'Snap', 'Rich', 'Muted', 'High Key', 'Low Key'],
  'Color Pop': ['Vibrant', 'Vivid+', 'Color Boost', 'Pop', 'Saturated', 'Juicy', 'Bold', 'Rich Color', 'Chroma+', 'Vivid Warm', 'Vivid Cool', 'Electric'],
  'Desaturate': ['Muted', 'Faded', 'Gray-ish', 'Subtle', 'Low Saturation', 'Washed', 'Barely There', 'Tone Down', 'Neutral', 'Pastel Wash', 'Dust', 'Pale'],
  'Skin': ['Warm Skin', 'Neutral Skin', 'Tan', 'Porcelain', 'Golden Skin', 'Soft Skin', 'Even Skin', 'Fresh Skin', 'Rosy', 'Bronze', 'Natural Skin', 'Glow Skin'],
  'Night': ['Night Boost', 'Neon Night', 'Shadow Lift', 'Low Light', 'City Night', 'Moonlight', 'Night Vision', 'Dark Boost', 'Blue Night', 'Street Night', 'Club', 'Midnight Fix'],
  'Outdoor': ['Sunny', 'Hazy Fix', 'Blue Sky', 'Daylight', 'Golden Out', 'Beach Day', 'Bright Out', 'Cloudy Fix', 'Green Out', 'Desert Sun', 'Snow Bright', 'Park'],
  'Indoor': ['Tungsten Fix', 'Fluorescent Fix', 'Cozy', 'Warm Indoor', 'Candle', 'Office', 'Lamp Glow', 'Room Light', 'Soft Indoor', 'Cafe Light', 'Studio Fix', 'Home'],
  'HDR': ['HDR Look', 'Dynamic', 'Clarity', 'Detail+', 'HDR Soft', 'HDR Strong', 'Local Tone', 'Balanced', 'Range+', 'Natural HDR', 'Flat HDR', 'Deep Detail'],
  'Creative': ['Cross Tone', 'Split Tone', 'Duo Tone', 'Pastel Wash', 'Amber Steel', 'Pink Teal', 'Sunset Split', 'Cold Warm', 'Candy', 'Retro Split', 'Mint Rose', 'Violet Gold'],
};
for (const [group, names] of Object.entries(lookGroups)) {
  for (const n of names) add('looks', n, { keywords: [group.toLowerCase(), 'look', 'quick'] });
}

/* ---------------- C. Transitions: 148 ---------------- */
const family = (prefix, count, build, cost = 'medium') => {
  for (let i = 0; i < count; i++) {
    const n = build(i);
    add('transitions', n, { cost, keywords: ['transition', prefix.toLowerCase()] });
  }
};
family('dissolve', 8, (i) => ['Cross Dissolve', 'Fade to Black', 'Fade to White', 'Blur Dissolve', 'Add Dissolve', 'Subtract Dissolve', 'Luma Wipe', 'Film Dissolve'][i]);
family('directional', 32, (i) => {
  const styles = ['Slide', 'Push', 'Flip', 'Roll', 'Swing'];
  const dirs = ['Left', 'Right', 'Up', 'Down'];
  const s = styles[Math.floor(i / 4) % 5];
  const d = dirs[i % 4];
  const extra = i >= 20 ? [' Diagonal', ' Combined'][Math.floor((i - 20) / 6) % 2] : '';
  return `${s} ${d}${extra}`;
});
family('wipe', 12, (i) => ['Linear Wipe', 'Barn Door', 'Venetian Blinds', 'Blinds', 'Checkerboard', 'Clock Wipe', 'Radial Wipe', 'Spiral Wipe', 'Grid Wipe', 'Square Wipe', 'Angle Wipe', 'Soft Wipe'][i]);
family('shape', 12, (i) => ['Circle Open', 'Square Open', 'Diamond Open', 'Star Open', 'Heart Open', 'Hexagon Open', 'Triangle Open', 'Custom Mask', 'Iris', 'Ring Open', 'Polygon Open', 'Cross Open'][i]);
family('zoom', 8, (i) => ['Zoom In', 'Zoom Out', 'Punch Zoom', 'Spin Zoom', 'Bounce Zoom', 'Warp Zoom', 'Snap Zoom', 'Depth Zoom'][i]);
family('defocus', 8, (i) => ['Blur Through', 'Focus Pull', 'Bokeh Shift', 'Gaussian Wipe', 'Soft Dissolve', 'Double Blur', 'Lens Defocus', 'Dream Fade'][i]);
family('glitch', 12, (i) => ['RGB Split', 'Pixelate', 'Datamosh', 'Scanline', 'Static', 'Block Glitch', 'Slice', 'VHS Cut', 'Digital Tear', 'Corrupt', 'Frame Drop', 'Signal Loss'][i], 'heavy');
family('light', 12, (i) => ['Flash', 'Light Leak', 'Lens Flare', 'Film Burn', 'Glow Flash', 'Exposure Bloom', 'Sun Flare', 'Spot Flash', 'Prism', 'Bokeh Light', 'Warm Leak', 'Cool Leak'][i]);
family('perspective', 12, (i) => ['Cube', 'Page Turn', 'Flip 3D', 'Card', 'Door', 'Origami', 'Tilt', 'Swing 3D', 'Fold', 'Pan 3D', 'Rotate 3D', 'Depth Slide'][i]);
family('mask', 12, (i) => ['Ink', 'Paint', 'Splatter', 'Torn Paper', 'Shatter', 'Puzzle', 'Brush', 'Mosaic Reveal', 'Strip Reveal', 'Zigzag', 'Wave Mask', 'Noise Reveal'][i]);
family('liquid', 12, (i) => ['Ripple', 'Wave', 'Melt', 'Liquify', 'Swirl', 'Water', 'Paint Drip', 'Oil', 'Bubble', 'Flow', 'Undertow', 'Droplet'][i], 'heavy');
family('camera', 8, (i) => ['Whip Pan', 'Shake Cut', 'Handheld', 'Crash Zoom', 'Sweep', 'Snap Pan', 'Tilt Cut', 'Rush'][i]);

/* ---------------- D. Text animations: 96 = 6 x 16 ---------------- */
const textGroups = {
  'Entry': ['Fade In', 'Slide In Left', 'Slide In Right', 'Slide In Up', 'Slide In Down', 'Scale In', 'Bounce In', 'Blur In', 'Rotate In', 'Flip In', 'Drop In', 'Pop In', 'Zoom In', 'Rise In', 'Expand In', 'Unfold'],
  'Exit': ['Fade Out', 'Slide Out Left', 'Slide Out Right', 'Slide Out Up', 'Slide Out Down', 'Scale Out', 'Bounce Out', 'Blur Out', 'Rotate Out', 'Flip Out', 'Drop Out', 'Pop Out', 'Zoom Out', 'Sink Out', 'Collapse Out', 'Fold'],
  'Loop': ['Float', 'Pulse', 'Wiggle', 'Shine', 'Breathe', 'Sway', 'Drift', 'Hover', 'Wave', 'Glow Loop', 'Tilt Loop', 'Jitter', 'Bob', 'Rotate Loop', 'Scale Loop', 'Blink'],
  'Emphasis': ['Karaoke', 'Glow Pulse', 'Neon Flicker', 'Shake', 'Split Reveal', 'Underline Grow', 'Highlight Sweep', 'Color Shift', 'Impact', 'Stomp', 'Flash', 'Bold Swap', 'Box Pop', 'Stamp', 'Slam', 'Echo'],
  'Typing': ['Typewriter', 'Letter Drop', 'Word Pop', 'Cursor Blink', 'Decrypt', 'Scramble', 'Fast Type', 'Slow Type', 'Handwrite', 'Reveal Line', 'Marquee', 'Ticker', 'Chain', 'Stack', 'Cascade', 'Ripple Type'],
  'Utility': ['Countdown', 'Timer', 'Lower Third', 'Progress Bar', 'Mask Reveal', 'Credit Roll', 'Subtitle Pop', 'Caption Fade', 'Number Flip', 'Score Roll', 'Cursor Follow', 'Label Slide', 'Badge Pop', 'Sticker Text', 'Callout', 'HUD'],
};
for (const [g, names] of Object.entries(textGroups)) for (const n of names) add('textanim', n, { keywords: ['text', 'animation', g.toLowerCase()] });

/* ---------------- E. Text styles: 48 = 4 x 12 ---------------- */
const styleGroups = {
  'Title': ['Bold Impact', 'Elegant Serif', 'Minimal', 'Cinematic', 'Editorial', 'Brutalist', 'Luxury', 'Sport', 'Retro Title', 'Modern', 'Magazine', 'Documentary'],
  'Subtitle': ['Clean', 'Boxed', 'Outline', 'Karaoke', 'Bilingual', 'Shadow', 'Pill', 'Gradient', 'Minimal Caption', 'Bold Caption', 'Neon Caption', 'Paper Caption'],
  'Callout': ['Sticker Text', 'Neon Sign', '3D Extrude', 'Graffiti', 'Comic', 'Handwritten', 'Marker', 'Chalk', 'Glitch Text', 'Metal', 'Gold Foil', 'Outline Pop'],
  'Utility': ['Ticker', 'Label', 'Tag', 'Watermark', 'Credits', 'Numbers', 'HUD', 'Badge', 'Timestamp', 'Lower Third', 'CTA', 'Subscribe'],
};
for (const [g, names] of Object.entries(styleGroups)) for (const n of names) add('textstyle', n, { keywords: ['text', 'style', g.toLowerCase()] });

/* ---------------- F. Motion presets: 48 = 6 x 8 ---------------- */
const motionGroups = {
  'Ken Burns': ['Ken Burns In', 'Ken Burns Out', 'Ken Burns Diagonal A', 'Ken Burns Diagonal B', 'Ken Burns Dual', 'Ken Burns Slow', 'Ken Burns Fast', 'Ken Burns Soft'],
  'Zoom': ['Slow Zoom', 'Punch Zoom', 'Snap Zoom', 'Breathe Zoom', 'Spin Zoom', 'Double Zoom', 'Rack Zoom', 'Pop Zoom'],
  'Pan': ['Pan Left', 'Pan Right', 'Pan Up', 'Pan Down', 'Pan Diagonal A', 'Pan Diagonal B', 'Parallax Pan', 'Slow Drift'],
  'Shake': ['Light Shake', 'Medium Shake', 'Heavy Shake', 'Subtle Shake', 'Camera Shake', 'Earthquake', 'Impact Shake', 'Handheld'],
  'Rotate': ['Slow Rotate', 'Fast Rotate', 'Tilt Rotate', 'Wobble', 'Spin 360', 'Swing', 'Rock', 'Tumble'],
  'Idle': ['Float', 'Sway', 'Drift', 'Hover', 'Wave Idle', 'Bounce Idle', 'Pulse Idle', 'Glide'],
};
for (const [g, names] of Object.entries(motionGroups)) for (const n of names) add('motion', n, { keywords: ['motion', 'keyframe', g.toLowerCase()] });

/* ---------------- G. Particles: 56 = 7 x 8 ---------------- */
const particles = {
  'Snow': ['Light Snow', 'Heavy Snow', 'Blizzard', 'Slow Snow', 'Blue Snow', 'Warm Snow', 'Night Snow', 'Sparkle Snow'],
  'Rain': ['Drizzle', 'Storm', 'Window Rain', 'Night Rain', 'Neon Rain', 'Tropical Rain', 'Fine Rain', 'Rain Drop'],
  'Sparks': ['Campfire', 'Welding', 'Magic', 'Gold Sparks', 'Rising Sparks', 'Embers', 'Fireflies', 'Electric'],
  'Dust': ['Floating Dust', 'Old Film', 'Sunbeam Dust', 'Room Dust', 'Dust Storm', 'Fine Dust', 'Dust Motes', 'Warm Dust'],
  'Confetti': ['Party', 'Celebration', 'Gold Confetti', 'Color Confetti', 'Confetti Burst', 'Ribbon', 'Streamer', 'Blast'],
  'Bokeh': ['Warm Bokeh', 'Cool Bokeh', 'Night Bokeh', 'Soft Bokeh', 'Circles', 'Light Balls', 'Dreamy', 'Prism'],
  'Smoke': ['Fog', 'Mist', 'Steam', 'Haze', 'Dark Smoke', 'Low Fog', 'Smoke Plume', 'Vapor'],
};
for (const [g, names] of Object.entries(particles)) for (const n of names) add('particles', n, { cost: 'medium', keywords: ['particles', g.toLowerCase(), 'overlay'] });

/* ---------------- H. Stickers & shapes: 52 (24 + 12 + 16) ---------------- */
const shapes = ['Rectangle', 'Circle', 'Line', 'Triangle', 'Star', 'Polygon', 'Diamond', 'Heart', 'Hexagon', 'Ring', 'Rounded Rect', 'Square', 'Oval', 'Cross', 'Plus', 'Arrow Shape', 'Speech Bubble', 'Frame', 'Border Box', 'Banner Ribbon', 'Badge Circle', 'Burst', 'Blob', 'Wave Shape'];
shapes.forEach((n) => add('stickers', n, { keywords: ['shape', 'sticker', 'graphic'] }));
const arrows = ['Arrow Right', 'Arrow Left', 'Arrow Up', 'Arrow Down', 'Arrow Diagonal', 'Curved Arrow', 'Pointer Hand', 'Caret', 'Chevron', 'Triangle Pointer', 'Dashed Arrow', 'Callout Pointer'];
arrows.forEach((n) => add('stickers', n, { keywords: ['arrow', 'pointer', 'sticker'] }));
const badges = ['Subscribe Badge', 'New Badge', 'Live Badge', 'Sale Badge', 'Trending Badge', 'Like Badge', 'Follow Badge', 'Share Badge', 'Comment Badge', 'Alert Badge', 'Check Badge', 'Star Badge', 'Hot Badge', 'Free Badge', 'Pro Badge', 'Custom Badge'];
badges.forEach((n) => add('stickers', n, { keywords: ['badge', 'sticker', 'label'] }));

/* ---------------- I. Audio: 56 = 16 + 12 + 12 + 16 ---------------- */
const audioGroups = {
  'Voice': ['Podcast', 'Vlog', 'Interview', 'Phone Call', 'Deep Voice', 'Bright Voice', 'Warm Voice', 'Radio Host', 'Studio Voice', 'Narration', 'Whisper', 'Shout', 'Clean Voice', 'Vocal Boost', 'Speech Clarity', 'Voice Focus'],
  'Music': ['Bass Boost', 'Treble Boost', 'Club', 'Lo-Fi', 'Vinyl', 'Concert', 'Warm Music', 'Wide Stereo', 'Mono', 'Loud', 'Balanced', 'Cinematic'],
  'Utility': ['Normalize', 'Noise Gate', 'De-Ess', 'Compress', 'Limit', 'Loudness Match', 'Silence Trim', 'DC Offset', 'Hum Remove', 'Clip Fix', 'Auto Gain', 'Gate Soft'],
  'FX': ['Echo', 'Reverb', 'Underwater', 'Megaphone', 'Telephone', 'Robot', 'Chorus', 'Flanger', 'Tremolo', 'Distortion', 'Bitcrush', 'Pitch Up', 'Pitch Down', 'Slow Down', 'Speed Up', 'Reverse'],
};
for (const [g, names] of Object.entries(audioGroups)) for (const n of names) add('audio', n, { cost: 'cheap', keywords: ['audio', 'sound', g.toLowerCase()] });

/* ---------------- J. Blur: 28 ---------------- */
const blurs = [
  ...['Gaussian Blur Light', 'Gaussian Blur Medium', 'Gaussian Blur Strong', 'Gaussian Blur Dreamy'].map((n) => [n, 'medium']),
  ...['Motion Blur Horizontal', 'Motion Blur Vertical', 'Motion Blur Diagonal', 'Motion Blur Radial'].map((n) => [n, 'medium']),
  ['Radial Blur', 'medium'], ['Zoom Blur', 'medium'],
  ['Tilt Shift', 'medium'], ['Bokeh Blur', 'heavy'], ['Bokeh Soft', 'heavy'],
  ...['Pixelate Small', 'Pixelate Medium', 'Pixelate Large', 'Pixelate Mosaic'].map((n) => [n, 'cheap']),
  ['Soft Focus', 'cheap'], ['Defocus', 'medium'], ['Prism Blur', 'medium'], ['Path Blur', 'medium'],
  ['Gaussian Portrait', 'medium'], ['Background Blur', 'medium'], ['Face Blur', 'medium'], ['Edge Blur', 'medium'],
  ['Streak Blur', 'medium'], ['Glass Blur', 'heavy'], ['Frosted Glass', 'heavy'],
];
blurs.forEach(([n, c]) => add('blur', n, { cost: c, keywords: ['blur', 'soft', 'focus'] }));

/* ---------------- K. Glitch: 48 ---------------- */
const glitches = [
  ...['RGB Split Light', 'RGB Split Medium', 'RGB Split Strong', 'Chromatic Aberration', 'Chroma Shift', 'Color Fringe'].map((n) => [n, 'cheap']),
  ...['Digital Glitch', 'Block Glitch', 'Datamosh', 'Pixel Sort', 'Slice Glitch', 'Data Corruption', 'Frame Glitch', 'Signal Break'].map((n) => [n, 'medium']),
  ...['Scanlines', 'CRT Lines', 'Scanline Fade', 'Interlace', 'VHS Lines', 'Tracking Lines'].map((n) => [n, 'cheap']),
  ...['VHS', 'VHS Warm', 'VHS Cold', 'VHS Noise', 'Tape Wobble', 'VHS Tracking'].map((n) => [n, 'medium']),
  ...['Old Film', 'Film Scratches', 'Film Jitter', 'Dust & Hair', 'Film Flicker', 'Super 8'].map((n) => [n, 'medium']),
  ...['Frame Smear', 'Ghost Frame', 'Echo Frame', 'Motion Trail', 'Stutter', 'Skip Frame'].map((n) => [n, 'medium']),
  ...['Posterize', '8-bit', 'Bitcrush', 'Threshold'].map((n) => [n, 'cheap']),
  ...['Camera Shake Soft', 'Camera Shake Hard', 'Impact Shake', 'Handheld Cam', 'Earthquake', 'Micro Shake'].map((n) => [n, 'cheap']),
];
glitches.forEach(([n, c]) => add('glitch', n, { cost: c, keywords: ['glitch', 'retro', 'vhs'] }));

/* ---------------- L. Light: 44 ---------------- */
const lights = [
  ...['Vignette Soft', 'Vignette Medium', 'Vignette Strong', 'Vignette Oval'].map((n) => [n, 'cheap']),
  ...['Film Grain Fine', 'Film Grain Medium', 'Film Grain Heavy', 'Film Grain Color'].map((n) => [n, 'cheap']),
  ...['Bloom Soft', 'Bloom Strong', 'Glow', 'Dream Glow'].map((n) => [n, 'medium']),
  ...['Light Leak Warm', 'Light Leak Cool', 'Light Leak Edge', 'Light Leak Sweep', 'Light Leak Flash', 'Light Leak Film'].map((n) => [n, 'medium']),
  ...['Lens Flare', 'Flare Anamorphic', 'Flare Circle', 'Flare Streak', 'Flare Prism', 'Flare Sun'].map((n) => [n, 'medium']),
  ...['God Rays', 'Sun Rays', 'Spot Light', 'Volumetric Light'].map((n) => [n, 'medium']),
  ...['Neon Glow', 'Neon Edge', 'Neon Pulse', 'Neon Outline'].map((n) => [n, 'heavy']),
  ...['Flash', 'Flash Soft', 'Strobe', 'Exposure Pulse'].map((n) => [n, 'cheap']),
  ...['HDR Look', 'HDR Soft', 'HDR Strong', 'Local Contrast'].map((n) => [n, 'medium']),
  ...['Faded Matte', 'Matte Lift', 'Washed', 'Milky'].map((n) => [n, 'cheap']),
];
lights.forEach(([n, c]) => add('light', n, { cost: c, keywords: ['light', 'glow', 'flare'] }));

/* ---------------- M. Distortion: 44 ---------------- */
const distortions = [
  ...['Mirror Horizontal', 'Mirror Vertical', 'Mirror Quad', 'Mirror Diagonal'].map((n) => [n, 'cheap']),
  ...['Kaleidoscope 4', 'Kaleidoscope 6', 'Kaleidoscope 8', 'Kaleidoscope Soft', 'Kaleido Tri', 'Kaleido Hex'].map((n) => [n, 'medium']),
  ...['Wave Small', 'Wave Large', 'Ripple Slow', 'Ripple Fast', 'Liquid Warp', 'Jelly'].map((n) => [n, 'cheap']),
  ...['Twirl Soft', 'Twirl Strong', 'Spiral', 'Vortex'].map((n) => [n, 'medium']),
  ...['Bulge', 'Pinch', 'Bulge Soft', 'Pinch Hard'].map((n) => [n, 'cheap']),
  ...['Fisheye', 'Fisheye Wide', 'Lens Barrel', 'Lens Pincushion'].map((n) => [n, 'cheap']),
  ...['Stretch Horizontal', 'Stretch Vertical', 'Squeeze', 'Elastic'].map((n) => [n, 'cheap']),
  ...['Perspective Tilt', 'Tilt Left', 'Tilt Right', 'Keystone'].map((n) => [n, 'cheap']),
  ...['Slice Displace', 'Slice Shift', 'Row Shift', 'Column Shift'].map((n) => [n, 'medium']),
  ...['Fractal Zoom', 'Fractal Soft', 'Infinite Zoom', 'Recursive'].map((n) => [n, 'heavy']),
];
distortions.forEach(([n, c]) => add('distort', n, { cost: c, keywords: ['distort', 'warp', 'geometry'] }));

/* ---------------- N. Detail: 16 ---------------- */
['Sharpen Light', 'Sharpen Medium', 'Sharpen Strong', 'Sharpen Edge'].forEach((n) => add('detail', n, { cost: 'medium', keywords: ['sharpen', 'detail'] }));
['Clarity Light', 'Clarity Medium', 'Clarity Strong', 'Local Contrast'].forEach((n) => add('detail', n, { cost: 'medium', keywords: ['clarity', 'detail'] }));
['Soften Light', 'Soften Medium', 'Soften Strong', 'Dream Soften'].forEach((n) => add('detail', n, { keywords: ['soften', 'detail'] }));
['Texture Grain', 'Texture Paper', 'Texture Canvas', 'Texture Noise'].forEach((n) => add('detail', n, { keywords: ['texture', 'grain'] }));

/* ---------------- O. Gradient maps: 36 = 12 + 12 + 12 ---------------- */
const duos = [['Sunset', '#FF7E5F', '#FEB47B'], ['Ocean', '#2E3192', '#1BFFFF'], ['Candy', '#FF6EC7', '#7873F5'], ['Fire', '#F12711', '#F5AF19'], ['Mint', '#43C6AC', '#F8FFAE'], ['Violet', '#7F00FF', '#E100FF'], ['Steel', '#485563', '#29323C'], ['Peach', '#FFAFBD', '#FFC3A0'], ['Night', '#0F2027', '#2C5364'], ['Gold', '#F7971E', '#FFD200'], ['Rose', '#ED4264', '#FFEDBC'], ['Forest', '#134E5E', '#71B280']];
duos.forEach(([n, a, b]) => add('gradient', `${n} Duotone`, { params: [{ key: 'colorA', label: 'Color A', type: 'COLOR', defColor: a, group: 'Colors' }, { key: 'colorB', label: 'Color B', type: 'COLOR', defColor: b, group: 'Colors' }, P.angle(), P.mix()], keywords: ['gradient', 'duotone'] }));
duos.forEach(([n, a, b]) => add('gradient', `${n} Trilogy`, { params: [{ key: 'colorA', label: 'Color A', type: 'COLOR', defColor: a, group: 'Colors' }, { key: 'colorB', label: 'Color B', type: 'COLOR', defColor: b, group: 'Colors' }, { key: 'colorC', label: 'Color C', type: 'COLOR', defColor: '#FFFFFF', group: 'Colors' }, P.mix()], keywords: ['gradient', 'trilogy'] }));
['Shadows Warm', 'Shadows Cool', 'Highlights Warm', 'Highlights Cool', 'Split Warm Cool', 'Split Cool Warm', 'Tone Map Soft', 'Tone Map Hard', 'Luma Map', 'Chroma Map', 'Blackpoint', 'Whitepoint'].forEach((n) => add('gradient', n, { keywords: ['gradient', 'tone', 'split'] }));

/* ---------------- P. Split screens: 32 = 4 x 8 ---------------- */
const splits = {
  'Two': ['Vertical Split', 'Horizontal Split', 'Diagonal Split', 'Ratio 1:2', 'Ratio 2:1', 'PiP Corner', 'PiP Center', 'Side by Side'],
  'Three': ['Tri Vertical', 'Tri Horizontal', 'Tri Diagonal', 'Tri Staggered', 'Tri Focus', 'Tri Wide', 'Tri Narrow', 'Tri Feature'],
  'Four': ['Grid 2x2', 'Strips', 'Mosaic', 'Four Corner', 'Quad Focus', 'Quad Wide', 'Quad Equal', 'Quad Stagger'],
  'Creative': ['Film Strip', 'Collage', 'Card Stack', 'Polaroid Wall', 'Hex Grid', 'Circle Grid', 'Free Mask', 'Masonry'],
};
for (const [g, names] of Object.entries(splits)) for (const n of names) add('split', n, { keywords: ['split', 'screen', 'layout', g.toLowerCase()] });

/* ---------------- Q. Borders: 24 = 6 x 4 ---------------- */
const borderStyles = ['Solid', 'Gradient', 'Polaroid', 'Film Strip', 'Rounded', 'Shadow'];
['Border', 'Frame', 'Edge', 'Outline'].forEach((suffix, i) => borderStyles.forEach((s) => add('border', `${s} ${suffix}`, { keywords: ['border', 'frame', s.toLowerCase()] })));

/* ---------------- R. Chroma: 12 ---------------- */
['Green Screen Soft', 'Green Screen Medium', 'Green Screen Strong'].forEach((n) => add('chroma', n, { cost: 'medium', keywords: ['chroma', 'key', 'green'] }));
['Blue Screen Soft', 'Blue Screen Medium', 'Blue Screen Strong'].forEach((n) => add('chroma', n, { cost: 'medium', keywords: ['chroma', 'key', 'blue'] }));
['Color Key Pick', 'Color Key Soft', 'Color Key Hard'].forEach((n) => add('chroma', n, { cost: 'medium', keywords: ['chroma', 'key', 'color'] }));
['Luma Key Bright', 'Luma Key Dark', 'Luma Key Range'].forEach((n) => add('chroma', n, { cost: 'medium', keywords: ['chroma', 'key', 'luma'] }));

/* ---------------- S. Speed: 16 ---------------- */
['Slow Motion 0.25x', 'Slow Motion 0.5x', 'Slow Motion 0.75x', 'Speed Up 1.5x', 'Speed Up 2x', 'Speed Up 4x', 'Speed Ramp In', 'Speed Ramp Out', 'Speed Ramp Both', 'Freeze Frame', 'Reverse Clip', 'Jump Cut', 'Velocity Edit', 'Timelapse', 'Hyperlapse', 'Boomerang'].forEach((n) => add('speed', n, { keywords: ['speed', 'retime', 'ramp'] }));

/* ---------------- T. AI: 12 ---------------- */
['Auto Captions Clean', 'Auto Captions Bold', 'Auto Captions Boxed', 'Auto Captions Karaoke', 'Auto Captions Bilingual', 'Auto Captions Outline', 'Silence Auto-Cut', 'Background Removal', 'Background Replace', 'Auto Reframe 9:16', 'TTS Voiceover', 'Beat Sync Cut'].forEach((n) => add('ai', n, { cost: 'heavy', keywords: ['ai', 'auto', 'smart'] }));

/* ---------------- write ---------------- */
const counts = out.reduce((acc, e) => { acc[e.category] = (acc[e.category] || 0) + 1; return acc; }, {});
const total = out.length;
const ids = new Set(out.map((e) => e.id));
console.log('counts', JSON.stringify(counts, null, 0));
console.log('TOTAL', total, 'unique ids', ids.size);

const target = new URL('../app/src/main/assets/effects/catalog.json', import.meta.url).pathname;
mkdirSync(dirname(target), { recursive: true });
writeFileSync(target, JSON.stringify({ version: 1, effects: out }));
console.log('written', target);

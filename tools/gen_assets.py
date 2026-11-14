#!/usr/bin/env python3
"""Generates the mod's textures and sounds (pip install pillow numpy soundfile).

Run from the repository root:  python tools/gen_assets.py
The faceless Steve skin (textures/entity/kill000145236.png) is a hand-made file and is not touched.
"""
import json
import os
import random

import numpy as np
import soundfile as sf
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'findiaries')
TEX_BLOCK = os.path.join(ROOT, 'textures', 'block')
TEX_ENTITY = os.path.join(ROOT, 'textures', 'entity')
SOUNDS = os.path.join(ROOT, 'sounds')
for d in (TEX_BLOCK, TEX_ENTITY, SOUNDS):
    os.makedirs(d, exist_ok=True)

rnd = random.Random(145236)


def animated(name, frames, frametime, painter):
    img = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                img.putpixel((x, f * 16 + y), painter(f, x, y))
    img.save(os.path.join(TEX_BLOCK, name + '.png'))
    with open(os.path.join(TEX_BLOCK, name + '.png.mcmeta'), 'w') as fh:
        json.dump({'animation': {'frametime': frametime}}, fh, indent=2)
        fh.write('\n')


# --- wonderland.avi portal: TV static ---------------------------------------------------------
def static(f, x, y):
    v = rnd.choice((12, 40, 90, 150, 205, 245))
    if (y + f * 3) % 16 in (5, 6):  # rolling scan line
        v = min(255, v + 60)
    return (v, v, v, 235)


animated('wonderland_portal', 8, 2, static)


# --- cabin portal: slow dark wood-coloured murk -----------------------------------------------
def murk(f, x, y):
    v = 10 + int(14 * (1 + np.sin(x * 0.9 + f * 1.57) * np.cos(y * 0.7 - f * 0.8))) + rnd.randint(0, 6)
    return (v + 8, int(v * 0.7), int(v * 0.4), 245)


animated('hall_portal', 4, 8, murk)

# --- portal made of coloured dots ---------------------------------------------------------------
DOT_COLOURS = [(255, 40, 40), (40, 255, 60), (60, 90, 255), (255, 240, 40), (255, 60, 230), (40, 240, 255), (255, 140, 20), (255, 255, 255)]
dot_frames = []
for f in range(8):
    dots = {}
    for _ in range(34):
        dots[(rnd.randrange(16), rnd.randrange(16))] = rnd.choice(DOT_COLOURS)
    dot_frames.append(dots)


def dots(f, x, y):
    c = dot_frames[f].get((x, y))
    return (c[0], c[1], c[2], 255) if c else (0, 0, 0, 70)


animated('dots_portal', 8, 3, dots)

# --- white portal ---------------------------------------------------------------------------------
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        v = rnd.randint(246, 255)
        img.putpixel((x, y), (v, v, v, 255))
img.save(os.path.join(TEX_BLOCK, 'white_portal.png'))


# --- entity skins (64x64 player layout, see TallHumanModel) -----------------------------------
def skin(name, base, jitter, patches=()):
    img = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            j = rnd.randint(-jitter, jitter)
            img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))
    for (x0, y0, x1, y1, colour) in patches:
        for y in range(y0, y1):
            for x in range(x0, x1):
                j = rnd.randint(-10, 10)
                img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in colour) + (255,))
    img.save(os.path.join(TEX_ENTITY, name + '.png'))


skin('watcher', (9, 11, 22), 3)
skin('blue_stranger', (14, 28, 92), 5)
# danger_12: black, with one hand glowing blue (left arm = texture 32..48 x 48..64)
skin('danger_12', (5, 5, 8), 2, patches=[
    (32, 59, 48, 64, (40, 80, 255)),   # lower end of the arm, all four sides
    (40, 48, 44, 52, (60, 110, 255)),  # underside of the hand
    (32, 56, 48, 59, (16, 30, 110)),   # fade
])

# --- sounds -------------------------------------------------------------------------------------
SR = 44100
nprng = np.random.default_rng(145236)


def save(name, data):
    data = np.asarray(data, dtype=np.float32)
    data = data / max(1e-6, np.max(np.abs(data))) * 0.89
    path = os.path.join(SOUNDS, name + '.ogg')
    with sf.SoundFile(path, 'w', samplerate=SR, channels=1, format='OGG', subtype='VORBIS') as fh:
        for i in range(0, len(data), SR):  # small blocks: old libsndfile crashes on big vorbis writes
            fh.write(data[i:i + SR])


def envelope(n, attack, release):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    e[:a] = np.linspace(0, 1, a)
    e[-r:] = np.linspace(1, 0, r)
    return e


# chirp: a dry electronic chirring / trilling, like a huge insect or a broken modem, over a low drone
dur = 7.0
n = int(dur * SR)
t = np.arange(n) / SR
trill_rate = 23 + 9 * np.sin(2 * np.pi * 0.31 * t)
gate = (np.sin(2 * np.pi * np.cumsum(trill_rate) / SR) > 0.15).astype(float)
gate = np.convolve(gate, np.ones(60) / 60, mode='same')
carrier_f = 2900 + 700 * np.sin(2 * np.pi * 1.7 * t) + 350 * np.sin(2 * np.pi * 11 * t)
carrier = np.sin(2 * np.pi * np.cumsum(carrier_f) / SR) + 0.4 * np.sin(2 * np.pi * np.cumsum(carrier_f * 1.51) / SR)
bursts = (np.sin(2 * np.pi * 0.9 * t + 0.6) > -0.35).astype(float)
bursts = np.convolve(bursts, np.ones(2000) / 2000, mode='same')
chirp = carrier * gate * bursts * 0.6
chirp += 0.10 * nprng.standard_normal(n) * gate * bursts
chirp += 0.35 * np.sin(2 * np.pi * 47 * t) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t))
save('chirp', chirp * envelope(n, 0.4, 1.5))

# hum: slow beating drone with a thin whine on top
dur = 12.0
n = int(dur * SR)
t = np.arange(n) / SR
hum = np.sin(2 * np.pi * 55 * t) + 0.9 * np.sin(2 * np.pi * 57.3 * t) + 0.5 * np.sin(2 * np.pi * 110.7 * t) + 0.3 * np.sin(2 * np.pi * 165 * t + np.sin(2 * np.pi * 0.2 * t) * 3)
hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.13 * t)
hum += 0.05 * np.sin(2 * np.pi * (3100 + 40 * np.sin(2 * np.pi * 0.4 * t)) * t)
noise = np.convolve(nprng.standard_normal(n), np.ones(400) / 400, mode='same') * 6
hum += 0.35 * noise
save('hum', hum * envelope(n, 2.5, 3.5))

# steps: something with very long legs running up to you, very fast, then nothing
dur = 4.2
n = int(dur * SR)
steps = np.zeros(n)
pos = 0.15
k = 0
while pos < 3.3:
    i = int(pos * SR)
    m = int(0.11 * SR)
    tt = np.arange(m) / SR
    thud = np.sin(2 * np.pi * (95 - 180 * tt) * tt) * np.exp(-tt * 38)
    click = np.convolve(nprng.standard_normal(m), np.ones(18) / 18, mode='same') * np.exp(-tt * 70) * 1.6
    loud = 0.15 + 0.85 * (pos / 3.3) ** 2
    steps[i:i + m] += (thud + click) * loud * (1.0 if k % 2 == 0 else 0.85)
    pos += 0.105 + 0.012 * nprng.standard_normal() - 0.012 * (pos / 3.3)
    k += 1
save('steps', steps)

print('assets written to', os.path.normpath(ROOT))

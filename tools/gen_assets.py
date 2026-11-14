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

# --- creative tab icons for the event items -------------------------------------------------------
TEX_ITEM = os.path.join(ROOT, 'textures', 'item')
os.makedirs(TEX_ITEM, exist_ok=True)
ICON_COLOURS = {
    '.': (0, 0, 0, 0), 'k': (8, 8, 11, 255), 'd': (40, 44, 52, 255), 's': (95, 104, 116, 255),
    'g': (63, 210, 79, 255), 'o': (232, 130, 46, 255), 'y': (245, 214, 32, 255), 'p': (220, 202, 204, 255),
    'q': (190, 170, 176, 255), 'c': (122, 122, 122, 255), 'w': (200, 200, 200, 255), 'b': (120, 85, 50, 255),
    'G': (60, 150, 50, 255), 'S': (191, 143, 120, 255), 'T': (0, 168, 168, 255), 'B': (74, 58, 191, 255),
    'm': (105, 125, 95, 255),
}
ICONS = {
    'event_triangle': [
        "................", "................", ".......kk.......", ".......kk.......", "......kkkk......", "......kkkk......",
        ".....kkkkkk.....", ".....kkkkkk.....", "....kkkkkkkk....", "....kkkkkkkk....", "...kkkkkkkkkk...", "...kkkkkkkkkk...",
        "..kkkkkkkkkkkk..", "..dddddddddddd..", "................", "................"],
    'event_slab': [
        "................", "................", "................", "................", "................", "..dddddddddddd..",
        ".dgddoddgddoddd.", ".dgddoddgddoddd.", ".dddddddddddddd.", "..kkkkkkkkkkkk..", "................", "................",
        "................", "................", "................", "................"],
    'event_ufo': [
        "................", "................", "....dddddddd....", "...ddsdddddsd...", "..dddsdddsdddd..", ".ddsdddsdddsddd.",
        ".dddddsddddddsd.", ".dsdyyddsddsddd.", ".ddyyyydddsdddd.", ".ddyyyydsdddsdd.", "..ddyydddddsdd..", "...dsddydsddd...",
        "....dddddddd....", "................", "................", "................"],
    'event_mushrooms': [
        "................", ".....pppppp.....", "...pppppppppp...", "..pppppppppppp..", "..pppppppppppp..", "...qqqqqqqqqq...",
        "......qqqq......", "....qqq..qqq....", "...qq.q..q.qq...", "..qq..q..q..qq..", "..q...q..q...q..", ".qq...q..q...qq.",
        ".q....q..q....q.", ".q....q..q....q.", ".q....q..q....q.", "................"],
    'event_tower': [
        ".....dddddd.....", ".....dydddd.....", ".y...dyddyd.....", ".....ddddyd..y..", ".....dyddds.....", ".....dydddd.....",
        ".....dddyds.....", "..y..dsdydd.....", ".....dddddd...y.", ".....dyddyd.....", ".....dydddd.....", ".....ddddyd.....",
        ".....dsddyd.....", ".....dddddd.....", "..cccccccccccc..", "................"],
    'event_longlegs': [
        "......bbbb......", "......SSSS......", "......SSSS......", ".....TTTTTT.....", ".....TTTTTT.....", ".....STTTTS.....",
        ".....SBBBBS.....", "......B..B..GG..", "......B..B.GGGG.", "......B..B.GGGG.", "......B..B..GG..", "......B..B..bb..",
        "......B..B..bb..", "......B..B..bb..", "......c..c..bb..", "................"],
    'event_portal': [
        "................", "..mmmmmmmmmmmm..", "..mmmmmmmmmmmm..", "..mmwkwswkwkmm..", "..mmkswkkwswmm..", "..mmwkkwskkwmm..",
        "..mmskwkwwskmm..", "..mmwwkskwkwmm..", "..mmkwswkkswmm..", "..mmwkkwwskkmm..", "..mmskwkswkwmm..", "..mmwskwkkwsmm..",
        "..mmmmmmmmmmmm..", "..mmmmmmmmmmmm..", "................", "................"],
}
for name, rows in ICONS.items():
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), ICON_COLOURS[ch])
    img.save(os.path.join(TEX_ITEM, name + '.png'))
print('item icons written')

# =====================================================================================================
# update 0.3.0
# =====================================================================================================
from PIL import ImageDraw  # noqa: E402

rnd3 = random.Random(30)


def block_tex(name, painter, size=16):
    im = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            im.putpixel((x, y), painter(x, y))
    im.save(os.path.join(TEX_BLOCK, name + '.png'))


# cobblestone pattern: a few irregular stones separated by dark seams
STONES = [
    "aaaabbbbbcccccda",
    "aaaabbbbbcccccdd",
    "eaaabbbbfccccddd",
    "eeeffffffgggdddd",
    "eeeffffffggghhhh",
    "eeefffffgggghhhh",
    "iiiijjjjgggghhhk",
    "iiiijjjjjlllhkkk",
    "iiiijjjjjllllkkk",
    "mmiijjjjnlllllkk",
    "mmmmoooonnnlllpp",
    "mmmmooooonnnpppp",
    "mmmmooooonnnpppp",
    "qqqrrroooonnpppa",
    "qqqrrrrrbbccccaa",
    "aaqrrrbbbbccccda",
]


def cobble(base, seam, blood=None):
    shades = {ch: rnd3.randint(-22, 22) for ch in 'abcdefghijklmnopqr'}

    def paint(x, y):
        ch = STONES[y][x]
        edge = STONES[y][(x + 1) % 16] != ch or STONES[(y + 1) % 16][x] != ch
        if edge:
            c = seam
            j = rnd3.randint(-5, 5)
        else:
            c = base
            j = shades[ch] + rnd3.randint(-7, 7)
        col = [max(0, min(255, v + j)) for v in c]
        if blood and ((x * 7 + y * 3) % 11 in (0, 1) and y % 5 != 4 or (edge and rnd3.random() < 0.5)):
            col = [max(0, min(255, v + rnd3.randint(-12, 12))) for v in blood]
        return tuple(col) + (255,)
    return paint


block_tex('red_cobblestone', cobble((112, 34, 30), (52, 12, 12)))
block_tex('blood_cobblestone', cobble((84, 16, 16), (30, 4, 6), blood=(170, 8, 12)))


# hellway portal: slowly boiling red
def hell(f, x, y):
    v = 0.5 + 0.5 * np.sin(x * 0.8 + f * 0.785 + np.sin(y * 0.6 + f * 1.57) * 2.0)
    r = int(90 + 150 * v) + rnd.randint(-8, 8)
    g = int(40 * v * v)
    return (max(0, min(255, r)), g, int(g * 0.4), 240)


animated('hellway_portal', 8, 3, hell)

# eye flower: two block tall red flower with an eye in the middle
GREEN, DGREEN, RED, DRED, WHITE, PUPIL = (58, 132, 44, 255), (34, 92, 30, 255), (196, 22, 26, 255), (128, 10, 16, 255), (236, 232, 222, 255), (12, 8, 10, 255)
bottom = Image.new('RGBA', (16, 16))
d = ImageDraw.Draw(bottom)
d.line([(7, 0), (7, 15)], fill=GREEN)
d.line([(8, 0), (8, 15)], fill=DGREEN)
d.line([(6, 9), (3, 6)], fill=GREEN)
d.line([(3, 6), (2, 7)], fill=DGREEN)
d.line([(9, 12), (12, 9)], fill=GREEN)
d.line([(12, 9), (13, 10)], fill=DGREEN)
bottom.save(os.path.join(TEX_BLOCK, 'eye_flower_bottom.png'))
top = Image.new('RGBA', (16, 16))
d = ImageDraw.Draw(top)
d.line([(7, 11), (7, 15)], fill=GREEN)
d.line([(8, 11), (8, 15)], fill=DGREEN)
d.ellipse([2, 1, 13, 12], fill=RED, outline=DRED)
for (px, py) in ((1, 6), (14, 6), (7, 0), (8, 13), (3, 2), (12, 2), (3, 11), (12, 11)):
    top.putpixel((px, py), DRED)
d.ellipse([5, 4, 10, 9], fill=WHITE)
d.rectangle([7, 6, 8, 7], fill=PUPIL)
top.putpixel((5, 4), RED), top.putpixel((10, 4), RED), top.putpixel((5, 9), RED), top.putpixel((10, 9), RED)
top.save(os.path.join(TEX_BLOCK, 'eye_flower_top.png'))
top.save(os.path.join(TEX_ITEM, 'eye_flower.png'))

# --- entity skins ---------------------------------------------------------------------------------
skin('wanderer', (10, 11, 16), 3)
skin('she', (0, 0, 0), 0)
# red demon: dark red, ribs, burning eyes (head front = 8..16 x 8..16), horns use the head top (8..16 x 0..8)
skin('red_demon', (70, 8, 8), 8, patches=[
    (8, 0, 16, 8, (30, 2, 2)),
    (9, 11, 11, 13, (255, 190, 60)), (13, 11, 15, 13, (255, 190, 60)),
    (10, 14, 14, 15, (20, 0, 0)),
    (20, 21, 28, 22, (150, 30, 24)), (20, 23, 28, 24, (150, 30, 24)), (20, 25, 28, 26, (150, 30, 24)), (20, 27, 28, 28, (150, 30, 24)),
])


def rect(im, x0, y0, x1, y1, colour, jitter=6):
    for y in range(y0, y1):
        for x in range(x0, x1):
            j = rnd3.randint(-jitter, jitter)
            im.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in colour) + (255,))


# sky person: Steve's head on a grey suit with a traffic light on the chest
sp = Image.new('RGBA', (64, 64))
rect(sp, 0, 0, 64, 64, (128, 128, 130), 5)
SKIN_C, HAIR, EYE_W, EYE_B, BEARD = (187, 137, 108), (48, 32, 20), (240, 240, 240), (73, 58, 160), (106, 64, 48)
rect(sp, 0, 0, 32, 16, SKIN_C, 4)        # whole head
rect(sp, 8, 0, 16, 8, HAIR, 4)           # top
rect(sp, 0, 8, 8, 11, HAIR, 4)           # right side hair
rect(sp, 16, 8, 24, 11, HAIR, 4)         # left side hair
rect(sp, 24, 8, 32, 15, HAIR, 4)         # back
rect(sp, 8, 8, 16, 10, HAIR, 4)          # fringe
rect(sp, 8, 10, 9, 11, HAIR, 4), rect(sp, 15, 10, 16, 11, HAIR, 4)
rect(sp, 9, 12, 10, 13, EYE_W, 0), rect(sp, 10, 12, 11, 13, EYE_B, 0)
rect(sp, 13, 12, 14, 13, EYE_B, 0), rect(sp, 14, 12, 15, 13, EYE_W, 0)
rect(sp, 11, 13, 13, 14, (150, 100, 78), 3)   # nose
rect(sp, 10, 14, 14, 15, BEARD, 3), rect(sp, 10, 15, 11, 16, BEARD, 3), rect(sp, 13, 15, 14, 16, BEARD, 3)
# chest panel (body front = 20..28 x 20..32)
rect(sp, 22, 21, 26, 30, (52, 52, 56), 3)
rect(sp, 23, 22, 25, 24, (235, 30, 30), 0)
rect(sp, 23, 25, 25, 27, (70, 220, 50), 0)
rect(sp, 23, 28, 25, 29, (30, 30, 32), 0)
# darker bands on legs and arms
for (x0, x1) in ((0, 16), (16, 32)):
    rect(sp, x0, 28 if x0 == 0 else 60, x1, 30 if x0 == 0 else 62, (84, 84, 88), 3)
rect(sp, 40, 28, 56, 30, (84, 84, 88), 3)
rect(sp, 32, 60, 48, 62, (84, 84, 88), 3)
sp.save(os.path.join(TEX_ENTITY, 'sky_person.png'))

# flower man (own layout, see FlowerManModel): 16^3 body at 0,0; leg 3x30x3 at 0,32; flower plane 8x8 at 16,32
fm = Image.new('RGBA', (64, 64))
DIRT, GRASS_C = (121, 85, 58), (86, 152, 52)
rect(fm, 0, 0, 64, 32, DIRT, 14)
rect(fm, 16, 0, 32, 16, GRASS_C, 12)          # top face
rect(fm, 0, 16, 64, 19, GRASS_C, 12)          # grass rim on the sides
for x in range(64):
    for y in range(19, 19 + rnd3.randint(0, 3)):
        fm.putpixel((x, y), tuple(max(0, min(255, c + rnd3.randint(-12, 12))) for c in GRASS_C) + (255,))
rect(fm, 0, 32, 12, 64, (70, 140, 44), 12)    # legs
# the yellow flower
fl = ImageDraw.Draw(fm)
fl.line([(19, 37), (19, 39)], fill=(40, 110, 30, 255))
fl.line([(20, 36), (20, 39)], fill=(52, 130, 36, 255))
fl.line([(18, 38), (17, 37)], fill=(52, 130, 36, 255))
fl.line([(21, 38), (22, 37)], fill=(40, 110, 30, 255))
fl.rectangle([18, 33, 21, 35], fill=(250, 220, 30, 255))
fl.point([(17, 34), (22, 34), (19, 32), (20, 32)], fill=(255, 236, 80, 255))
fl.point([(19, 34), (20, 34)], fill=(230, 160, 20, 255))
fm.save(os.path.join(TEX_ENTITY, 'flower_man.png'))

# --- scream ---------------------------------------------------------------------------------------
dur = 2.6
n = int(dur * SR)
t = np.arange(n) / SR
f0 = 520 + 420 * np.exp(-((t - 0.5) ** 2) / 0.12) + 60 * np.sin(2 * np.pi * 9 * t) - 140 * t
ph = 2 * np.pi * np.cumsum(f0) / SR
voice = sum(np.sin(ph * k + 0.4 * np.sin(2 * np.pi * 37 * t)) / k for k in (1, 2, 3, 4, 5, 6.02, 7.1))
rasp = np.sign(np.sin(2 * np.pi * 61 * t)) * 0.35 + 1.0
breath = np.convolve(nprng.standard_normal(n), np.ones(9) / 9, mode='same') * 1.4
scream = np.tanh((voice * rasp + breath * 0.6) * 1.6)
scream += 0.5 * np.sin(2 * np.pi * 43 * t) * np.exp(-t * 0.6)
save('scream', scream * envelope(n, 0.05, 1.1))

# --- more event icons, drawn with shapes ------------------------------------------------------------
BLUE, LIGHT, ORANGE = (30, 63, 143, 255), (120, 170, 220, 255), (232, 130, 46, 255)


def icon(name, painter):
    im = Image.new('RGBA', (16, 16))
    painter(ImageDraw.Draw(im), im)
    im.save(os.path.join(TEX_ITEM, name + '.png'))


def tri_base(dr, colour=BLUE):
    dr.polygon([(8, 1), (1, 13), (14, 13)], fill=colour)


def i_triangle(dr, im):
    tri_base(dr)
    dr.polygon([(5, 9), (8, 9), (5, 11)], fill=LIGHT)
    dr.polygon([(10, 10), (12, 11), (10, 12)], fill=LIGHT)
    im.putpixel((8, 4), ORANGE)


def i_wanderer(dr, im):
    dr.polygon([(8, 0), (3, 6), (13, 6)], fill=BLUE)
    dr.line([(8, 7), (8, 15)], fill=(10, 11, 16, 255), width=2)
    dr.line([(1, 9), (14, 9)], fill=(10, 11, 16, 255))


def i_lying(dr, im):
    dr.polygon([(1, 10), (14, 8), (14, 12)], fill=BLUE)
    dr.line([(1, 14), (14, 14)], fill=(200, 200, 200, 255))
    im.putpixel((11, 10), ORANGE)


def i_star(dr, im):
    tri_base(dr)
    for (x, y) in ((8, 5), (5, 10), (11, 10), (8, 9), (3, 2), (13, 3), (1, 6)):
        im.putpixel((x, y), (255, 255, 255, 255))


def i_eyes(dr, im):
    tri_base(dr)
    dr.rectangle([5, 8, 6, 9], fill=(255, 255, 255, 255))
    dr.rectangle([9, 8, 10, 9], fill=(255, 255, 255, 255))
    im.putpixel((6, 9), (0, 0, 0, 255)), im.putpixel((10, 9), (0, 0, 0, 255))


def i_castle(dr, im):
    c, k = (122, 122, 122, 255), (70, 70, 70, 255)
    dr.rectangle([2, 6, 13, 14], fill=c)
    for x in (2, 5, 8, 11):
        dr.rectangle([x, 4, x + 1, 5], fill=c)
    dr.rectangle([1, 3, 3, 14], fill=k), dr.rectangle([12, 3, 14, 14], fill=k)
    dr.rectangle([7, 10, 8, 14], fill=(0, 0, 0, 255))


def i_hell(dr, im):
    dr.rectangle([3, 1, 12, 14], fill=(84, 16, 16, 255))
    dr.rectangle([5, 3, 10, 12], fill=(235, 60, 20, 255))
    dr.rectangle([6, 5, 9, 10], fill=(255, 150, 40, 255))


def i_house(dr, im):
    dr.polygon([(8, 1), (1, 7), (14, 7)], fill=(120, 85, 50, 255))
    dr.rectangle([3, 7, 12, 14], fill=(170, 136, 78, 255))
    dr.rectangle([6, 10, 8, 14], fill=(235, 60, 20, 255))
    dr.rectangle([10, 9, 11, 10], fill=(160, 210, 240, 255))


def i_ship(dr, im):
    dr.ellipse([1, 7, 14, 12], fill=(200, 200, 205, 255))
    dr.ellipse([5, 4, 10, 9], fill=(130, 200, 240, 255))
    im.putpixel((3, 10), (235, 30, 30, 255)), im.putpixel((7, 11), (70, 220, 50, 255)), im.putpixel((12, 10), (235, 30, 30, 255))
    dr.line([(12, 2), (14, 0)], fill=ORANGE)


def i_flowerman(dr, im):
    dr.rectangle([4, 4, 11, 8], fill=(121, 85, 58, 255))
    dr.rectangle([4, 3, 11, 4], fill=(86, 152, 52, 255))
    dr.line([(6, 9), (6, 15)], fill=(70, 140, 44, 255))
    dr.line([(9, 9), (9, 15)], fill=(70, 140, 44, 255))
    dr.rectangle([7, 0, 8, 1], fill=(250, 220, 30, 255))
    im.putpixel((7, 2), (52, 130, 36, 255))


for nm, fn in (('event_triangle', i_triangle), ('event_triangle_wanderer', i_wanderer), ('event_triangle_lying', i_lying),
               ('event_triangle_star', i_star), ('event_triangle_eyes', i_eyes), ('event_castle', i_castle),
               ('event_hell_portal', i_hell), ('event_wrong_house', i_house), ('event_spaceship', i_ship),
               ('event_flower_man', i_flowerman)):
    icon(nm, fn)
print('0.3.0 assets written')

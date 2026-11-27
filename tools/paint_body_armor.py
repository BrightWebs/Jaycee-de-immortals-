# paints body armor's textures (our own art, in the style of an armorsaurus: deep blue scales,
# charcoal under-layer, bone horns, red accents). run from the repo root
import random, os
from PIL import Image
R = 'jaycee/src/main/resources/assets/jaycee/textures/'
random.seed(7)
BLUES = [(30, 44, 98), (38, 56, 120), (46, 66, 138), (34, 50, 110), (52, 74, 150)]
EDGE, HI, GAP, RED = (16, 22, 52), (92, 120, 196), (26, 58, 50), (140, 30, 42)

def scale_px(x, y, seed=0):
    row = y // 2
    off = (row % 2) * 2
    cx, ix, iy = (x + off) // 4, (x + off) % 4, y % 2
    h = hash((cx, row, seed)) % 1000
    base = BLUES[h % len(BLUES)]
    if iy == 1 and ix in (0, 3): return EDGE
    if iy == 1 and ix == 0 and h % 7 == 0: return GAP
    if iy == 0 and ix == 1: return tuple(min(255, (b + c) // 2) for b, c in zip(base, HI))
    if h % 53 == 0 and ix == 2: return RED
    return base

def fill(img, x0, y0, x1, y1, f, seed=0):
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = f(x, y, seed)
            img.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def box_regions(u, v, w, h, d):
    return [(u, v, u + 2 * (w + d), v + d + h)]

def bone(x, y, s=0):
    t = (y * 3 + x) % 9
    c = (232, 226, 207) if t < 6 else (205, 197, 174)
    if y % 5 == 4: c = (169, 159, 134)
    return c

def dark(x, y, s=0):
    c = (42, 45, 56) if (x + y) % 3 else (34, 36, 46)
    if (x * 7 + y * 3) % 23 == 0: c = RED
    return c

# ---- the suit itself (vanilla armor layout, 64x32)
suit = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
fill(suit, 0, 0, 32, 16, scale_px, 1)            # helmet
fill(suit, 16, 16, 40, 32, scale_px, 2)          # chest
fill(suit, 40, 16, 56, 32, scale_px, 3)          # arms
fill(suit, 0, 16, 16, 32, scale_px, 4)           # boots
# face: open visor so you can still see whose face it is, with a nose guard
for y in range(11, 16):
    for x in range(9, 15):
        suit.putpixel((x, y), (0, 0, 0, 0))
for y in range(10, 14):
    for x in (11, 12): suit.putpixel((x, y), EDGE + (255,))
for x in range(8, 16): suit.putpixel((x, 10), HI + (255,))
# chest: darker centre plate with a red seam
for y in range(20, 30):
    for x in range(22, 26): suit.putpixel((x, y), (24, 34, 78, 255) if (x + y) % 2 else (30, 42, 92, 255))
for y in range(20, 30): suit.putpixel((24, y), RED + (255,))
# boots: charcoal soles
for y in range(28, 32):
    for x in range(0, 16): suit.putpixel((x, y), dark(x, y) + (255,))
os.makedirs(R + 'entity/equipment/humanoid', exist_ok=True)
suit.save(R + 'entity/equipment/humanoid/body_armor.png')

legs = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
fill(legs, 0, 16, 16, 32, scale_px, 5)
for y in range(16, 32):
    for x in range(0, 16):
        if y % 6 == 5: legs.putpixel((x, y), GAP + (255,))
# belt
for y in range(28, 32):
    for x in range(16, 40): legs.putpixel((x, y), dark(x, y) + (255,))
for y in range(20, 22):
    for x in range(16, 40): legs.putpixel((x, y), (0, 0, 0, 0))
os.makedirs(R + 'entity/equipment/humanoid_leggings', exist_ok=True)
legs.save(R + 'entity/equipment/humanoid_leggings/body_armor.png')

# ---- the 3d bits: rows 0-23 scales, 24-47 bone, 48-63 charcoal
extra = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
fill(extra, 0, 0, 64, 24, scale_px, 9)
fill(extra, 0, 24, 64, 48, bone)
fill(extra, 0, 48, 64, 64, dark)
extra.save(R + 'entity/body_armor_extra.png')

# ---- item icons: our palette mapped onto simple 16x16 shapes
SHAPES = {
 'helmet': ["B..............B", ".B............B.", "..BXXXXXXXXXXB..", "..XXXXXXXXXXXX..", ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.",
            ".XXXX......XXXX.", ".XXX...XX...XXX.", ".XXX...XX...XXX.", "..XX........XX..", "..X..........X.."],
 'chestplate': ["..XXX......XXX..", ".XXXXX....XXXXX.", ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.", "..XXXXXXXXXXXX..", "...XXXXXXXXXX...",
                "...XXXXXXXXXX...", "...XXXXXXXXXX...", "...XXXXXXXXXX...", "...XXXXXXXXXX...", "...XXXXXXXXXX...", "....XXXXXXXX...."],
 'leggings': ["...XXXXXXXXXX...", "...XXXXXXXXXX...", "...XXXXXXXXXX...", "...XXXX..XXXX...", "...XXXX..XXXX...", "...XXXX..XXXX...",
              "...XXXX..XXXX...", "...XXX....XXX...", "...XXX....XXX...", "...XXX....XXX...", "...XXX....XXX..."],
 'boots': ["................", "................", "..XXX......XXX..", "..XXX......XXX..", "..XXX......XXX..", "..XXX......XXX..",
           ".XXXX.....XXXX..", "XXXXX....XXXXX..", "XXXXX....XXXXX.."],
 'gauntlet': ["................", "..B.B.B.........", "..B.B.B.........", ".XXXXXXX........", ".XXXXXXXX.......", ".XXXXXXXXX......",
              ".XXXXXXXXX......", "..XXXXXXXX......", "..XXXXXXX.......", "...XXXXX........", "...XXXXX........", "...DDDDD........"],
}
os.makedirs(R + 'item', exist_ok=True)
for name, rows in SHAPES.items():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    top = (16 - len(rows)) // 2
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == 'X':
                edge = (x == 0 or row[x - 1] != 'X' or x == 15 or (x + 1 < len(row) and row[x + 1] != 'X')
                        or y == 0 or rows[y - 1][x] != 'X' or y == len(rows) - 1 or rows[y + 1][x] != 'X')
                c = EDGE if edge else scale_px(x, y, 11)
            elif ch == 'B': c = bone(x, y)
            elif ch == 'D': c = dark(x, y)
            else: continue
            im.putpixel((x, y + top), c + (255,))
    im.save(R + 'item/body_armor_' + name + '.png')
print('painted')

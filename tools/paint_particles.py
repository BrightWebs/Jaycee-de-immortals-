# our own particle sprites in the style tensura / manas reborn use (small outlined pixel sprites that shrink
# through their frames). drawn from scratch here, not copied. run from the repo root
import math, random, os, json
from PIL import Image, ImageDraw

OUT = 'brightcore/src/main/resources/assets/brightcore/textures/particle/'
JSON = 'brightcore/src/main/resources/assets/brightcore/particles/'
os.makedirs(OUT, exist_ok=True); os.makedirs(JSON, exist_ok=True)

def hexc(h, a=255): return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)
def mix(a, b, k): return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3)) + (255,)
def dark(c, k=0.45): return tuple(int(v * k) for v in c[:3]) + (255,)

def outline(img, col):
    # 1px outline around everything drawn
    w, h = img.size; px = img.load(); out = img.copy(); po = out.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3]: continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] and px[nx, ny][:3] != col[:3]:
                    po[x, y] = col; break
    return out

def save(name, frames):
    for i, f in enumerate(frames): f.save(f'{OUT}{name}_{i}.png')
    json.dump({'textures': [f'brightcore:{name}_{i}' for i in range(len(frames))]}, open(f'{JSON}{name}.json', 'w'))

# ---- lightning sparks: a jagged bolt with a bright core that breaks into dots as it dies
def spark_frames(mid, seed):
    rnd = random.Random(seed)
    core = mix(mid, (255, 255, 255), 0.75)
    # zigzags corner to corner, big kinks like a real bolt
    pts = [(2, 3)]
    for i in range(5):
        x = min(13, pts[-1][0] + rnd.choice([2, 2, 3]))
        y = min(13, max(2, pts[-1][1] + (rnd.choice([3, 4]) if i % 2 == 0 else -rnd.choice([1, 2]))))
        pts.append((x, y))
    branch = [pts[2], (pts[2][0] + 1, pts[2][1] - 3)]
    frames = []
    for f in range(8):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        if f < 5:
            n = min(len(pts), 2 + f * 2)
            d.line(pts[:n], fill=mid, width=2 if f in (1, 2, 3) else 1)
            if f in (2, 3): d.line(branch, fill=mid, width=1)
            if f in (1, 2, 3): d.line(pts[:n], fill=core, width=1)
        else:
            k = f - 4
            for (x, y) in pts[::2]:
                r = max(0, 2 - k // 2)
                d.rectangle([x - r // 2, y - r // 2, x + r // 2, y + r // 2], fill=core if k < 3 else mid)
        frames.append(outline(im, dark(mid)))
    return frames

SPARKS = {'spark_yellow': '#f1fd85', 'spark_blue': '#9185fd', 'spark_purple': '#c185fd', 'spark_dark_purple': '#7a1ec0',
          'spark_dark_red': '#c0201a', 'spark_red': '#fd858b', 'spark_green': '#9ffd85', 'spark_black': '#3a3d55'}
for i, (n, c) in enumerate(SPARKS.items()): save(n, spark_frames(hexc(c), 100 + i))

# ---- fire: a small flame that flickers down to a spark as it rises
def fire_frames(core, mid, edge):
    frames = []
    for f in range(7):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        s = 1 - f / 7.5
        h = max(2, int(10 * s)); w = max(1, int(6 * s))
        cx, base = 8, 13
        top = base - h
        d.polygon([(cx - w // 2, base), (cx + w // 2, base), (cx + w // 2, base - h // 2), (cx + (f % 2), top), (cx - w // 2, base - h // 2)], fill=mid)
        if h > 3: d.ellipse([cx - max(0, w // 4), base - h // 2, cx + max(0, w // 4), base - 1], fill=core)
        frames.append(outline(im, edge))
    return frames
FIRES = {'fire_red': ('#ffe08a', '#ff7a2a', '#9a2e0a'), 'fire_black': ('#9ab0ff', '#2c3a8a', '#07070f'), 'fire_plasma': ('#ffd0ff', '#a23cee', '#3c0a6a'),
         'fire_illusion': ('#fff0a0', '#ff9a2a', '#8a4210'), 'fire_soul': ('#e0ffff', '#3fd8ff', '#0a3a5a'), 'fire_holy': ('#ffffff', '#ffe98a', '#9a7a20')}
for n, (a, b, c) in FIRES.items(): save(n, fire_frames(hexc(a), hexc(b), hexc(c)))

# ---- heat: blobs of heat that break into specks
def heat_frames():
    frames = []; rnd = random.Random(7)
    for f in range(6):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        for b in range(max(1, 4 - f // 2)):
            x, y, r = 4 + rnd.randint(0, 7), 4 + rnd.randint(0, 7), max(1, 3 - f // 2)
            d.ellipse([x - r, y - r, x + r, y + r], fill=hexc('#f39639'))
        frames.append(outline(im, hexc('#a0500e')))
    return frames
save('heat', heat_frames())

# ---- shockwave rings: grow out (or pull in) and thin into an arc
def ring_frames(color, n=8, reverse=False):
    frames = []
    for f in range(n):
        im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        r = 2 + f * 1.8
        w = 3 if 2 <= f <= 4 else 2 if f < 6 else 1
        if f < n - 2: d.ellipse([16 - r, 16 - r, 16 + r, 16 + r], outline=color, width=w)
        else: d.arc([16 - r, 16 - r, 16 + r, 16 + r], 200 + f * 10, 340, fill=color, width=1)
        if 2 <= f <= 4: d.ellipse([16 - r + 3, 16 - r + 3, 16 + r - 3, 16 + r - 3], outline=mix(color, (0, 0, 0), 0.25), width=1)
        frames.append(im)
    return frames[::-1] if reverse else frames
save('shock_ring', ring_frames(hexc('#f4f4f4')))
save('shock_ring_in', ring_frames(hexc('#e4e4e4'), reverse=True))

# ---- slashes: a crescent that sweeps open then fades
def slash_frames(col, edge, thin=False):
    frames = []
    for f in range(8):
        im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        span = min(170, 40 + f * 30)
        start = 190 - span // 2 + (f * 4 if f > 4 else 0)
        width = (2 if thin else 4) if f < 6 else 1
        d.arc([4, 4, 28, 28], start, start + span, fill=col, width=width)
        if not thin and f < 6: d.arc([5, 5, 27, 27], start + 10, start + span - 10, fill=edge, width=1)
        frames.append(im)
    return frames
save('slash_full', slash_frames(hexc('#fafafa'), hexc('#c8d0e0')))
save('slash_light', slash_frames(hexc('#fddfaf'), hexc('#e8b070'), thin=True))

# ---- snowflake: six arms
def flake(size):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for k in range(6):
        a = k * math.pi / 3
        d.line([(8, 8), (8 + math.cos(a) * size, 8 + math.sin(a) * size)], fill=hexc('#e8fbff'), width=1)
        mx, my = 8 + math.cos(a) * size * 0.6, 8 + math.sin(a) * size * 0.6
        d.point([(mx + math.cos(a + 1.2), my + math.sin(a + 1.2)), (mx + math.cos(a - 1.2), my + math.sin(a - 1.2))], fill=hexc('#c2eef6'))
    return outline(im, hexc('#5a9ab0'))
save('snowflake', [flake(6), flake(4)])

# ---- bubble that pops into bits
def bubble_frames():
    frames = []
    for f in range(6):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        if f == 0:
            d.ellipse([3, 3, 12, 12], outline=hexc('#d8f0ff'), width=1); d.point([(6, 6), (6, 7), (7, 6)], fill=hexc('#ffffff'))
        else:
            r = 4 + f
            for k in range(8 - f):
                a = k * math.pi * 2 / (8 - f) + f
                d.point([(8 + math.cos(a) * r * 0.7, 8 + math.sin(a) * r * 0.7)], fill=hexc('#d8f0ff'))
        frames.append(im)
    return frames
save('bubble_pop', bubble_frames())

# ---- solar flash: a soft glow
def glow(col, size=16):
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0)); px = im.load(); c = size / 2
    for y in range(size):
        for x in range(size):
            dd = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            if dd < 1: px[x, y] = mix(col, (255, 255, 255), max(0, 0.8 - dd))[:3] + (int(255 * (1 - dd) ** 1.6),)
    return im
save('solar_flash', [glow(hexc('#fff3b0'))])

# ---- sonic boom: a curl that spins down to a point
def sonic_frames():
    frames = []
    for f in range(8):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        r = 6 - f * 0.7
        if r > 1: d.arc([8 - r, 8 - r, 8 + r, 8 + r], f * 40, f * 40 + 220, fill=hexc('#e8e8e8'), width=1)
        else: d.point([(8, 8)], fill=hexc('#e8e8e8'))
        frames.append(im)
    return frames
save('sonic_curl', sonic_frames())

# ---- soul: a dark wisp whose eye lights up
def soul_frames():
    frames = []
    for f in range(8):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        h = 4 + f // 2
        d.ellipse([5, 13 - h - 3, 11, 13 - h + 3], fill=hexc('#141418'))
        d.polygon([(6, 13 - h), (10, 13 - h), (9 - f % 2, 14), (7, 12)], fill=hexc('#1e1e24'))
        if f >= 4: d.point([(7, 13 - h), (9, 13 - h)], fill=hexc('#ff5fd0'))
        frames.append(outline(im, hexc('#000000')))
    return frames
save('soul_wisp', soul_frames())

# ---- bats, two wing poses
def bat(up):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = hexc('#0a0a0c')
    d.ellipse([6, 7, 9, 10], fill=c)
    if up: d.polygon([(7, 8), (1, 3), (3, 8)], fill=c); d.polygon([(8, 8), (14, 3), (12, 8)], fill=c)
    else: d.polygon([(7, 8), (1, 12), (3, 9)], fill=c); d.polygon([(8, 8), (14, 12), (12, 9)], fill=c)
    d.point([(7, 8), (8, 8)], fill=hexc('#ff2a2a'))
    return im
save('bat', [bat(True), bat(False)])

# ---- blossom: a little glowing flower
def blossom():
    im = Image.new('RGBA', (8, 8), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for (x, y) in ((3, 1), (1, 3), (5, 3), (3, 5)): d.rectangle([x, y, x + 1, y + 1], fill=hexc('#e488e8'))
    d.rectangle([3, 3, 4, 4], fill=hexc('#ffe0ff'))
    return im
save('blossom', [blossom()])

# ---- drops that fall and splash (acid, heal)
def drops(col):
    a = Image.new('RGBA', (8, 8), (0, 0, 0, 0)); d = ImageDraw.Draw(a)
    d.polygon([(4, 1), (6, 5), (4, 7), (2, 5)], fill=col); d.point([(3, 4)], fill=mix(col, (255, 255, 255), 0.6))
    b = Image.new('RGBA', (8, 8), (0, 0, 0, 0)); d = ImageDraw.Draw(b)
    for (x, y) in ((1, 6), (6, 6), (3, 5), (5, 4)): d.point([(x, y)], fill=col)
    return [a, b]
save('acid_drop', drops(hexc('#9be33b')))
save('heal_drop', drops(hexc('#7dff9a')))
print('painted')

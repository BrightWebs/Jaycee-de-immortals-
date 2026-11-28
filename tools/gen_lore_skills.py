# builds jaycee's lore_skills.json: every ability from the icon pack manifest that isn't hand made yet.
# numbers come from tensura's own configs when tensura has that skill, otherwise from the rank defaults below.
# looks come from brightcore's fx_recipes.json (the particle recipe every skill already has).
#
#   python3 tools/gen_lore_skills.py <manifest.json> <tensura_cfg.json> <tensura_abil.json> <existing_ids.txt>
import json, re, sys, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
man = json.load(open(sys.argv[1]))['abilities']
# sins the icon pack missed (tensura and manas reborn both have them)
_have = {a['id'] for a in man}
for _id, _nm in [('wrath', 'Wrath'), ('envy', 'Envy')]:
    if _id not in _have: man.append({'id': _id, 'display_name': _nm, 'variant': None, 'categories': ['unique'], 'sources': []})
tcfg = json.load(open(sys.argv[2]))
tab = json.load(open(sys.argv[3]))
existing = set(open(sys.argv[4]).read().split())
fx = json.load(open(os.path.join(ROOT, 'brightcore/src/main/resources/assets/brightcore/fx_recipes.json')))

def norm(s): return re.sub(r'[^a-z0-9]', '', s.lower())

# tensura lookups by squashed name
tab_by = {}
for k, v in tab.items():
    tab_by.setdefault(norm(k), v)
cfg_by = {norm(k): v for k, v in tcfg.items()}

# what each rank costs and hits for when tensura has no number (tensura scale: magicules, hp)
RANK = {
    #            type          mp     ap    dmg  cd(s)
    'common':   ('COMMON',     50,    0,    10,  2),
    'intrinsic':('INTRINSIC',  100,   0,    12,  2),
    'extra':    ('EXTRA',      1000,  0,    25,  4),
    'sub':      ('EXTRA',      1500,  0,    35,  5),
    'arts':     ('ARTS',       0,     300,  30,  3),
    'magic':    ('MAGIC',      2000,  0,    40,  3),
    'unique':   ('UNIQUE',     5000,  0,    60,  6),
    'ultimate': ('ULTIMATE',   20000, 0,    150, 10),
    'resistance': ('RESISTANCE', 0, 0, 0, 0),
    'nullification': ('NULLIFICATION', 0, 0, 0, 0),
}

def rank_of(cats):
    c = cats[0]
    if c.startswith('ultimate'): return 'ultimate'
    if c.startswith('unique'): return 'unique'
    if c.startswith('arts'): return 'arts'
    if c.startswith('magic'): return 'magic'
    if c.startswith('subskills'): return 'sub'
    if c == 'resistance': return 'resistance'
    if c == 'nullification': return 'nullification'
    if 'extra' in cats: return 'extra'
    if 'common' in cats: return 'common'
    return 'intrinsic' if 'intrinsic' in cats else 'extra'

def magic_class(cats, tensura_pkg):
    c = ' '.join(cats)
    if tensura_pkg.startswith('magic/summon') or any(k in c for k in ('summon', 'creation', 'legion')): return 'summoning'
    if tensura_pkg.startswith('magic/spiritual') or any(k in c for k in ('element', 'spirit', 'schools')): return 'elemental'
    if any(k in c for k in ('miracle', 'holy', 'divine')): return 'holy'
    if tensura_pkg.startswith('magic/aspectual') or any(k in c for k in ('fire', 'ice', 'earth', 'water', 'wind', 'light', 'dark', 'gravity',
            'barrier', 'enhancement', 'explosive', 'nuclear', 'lightning', 'recovery', 'space', 'mental', 'illusion', 'optical', 'life', 'slowing')):
        return 'aspectual'
    return 'other'

# keyword hints on the name beat the recipe shape
KEY_ARCH = [
    (r'breath|blizzard_breath', 'cone'),
    (r'barrier|wall|shield|protection|armou?r|_guard|fortress|defen', 'guard'),
    (r'heal|recovery|regenerat|cure|antidote|restor|revive|resurrect', 'heal'),
    (r'teleport|warp|_step|movement|flight|_dash|instant_move|transfer|gate|_move', 'move'),
    (r'summon|create_|creation|conjur|army|legion|familiar', 'summon'),
    (r'sense|perception|detect|_eye|sight|clairvoy|analy|appraisal|prediction|insight|search', 'sense'),
    (r'beam|_ray|laser|cannon|lance', 'beam'),
    (r'slash|blade|sword|cut|strike|punch|kick|thrust|fist|palm|claw', 'slash'),
    (r'_ball|bullet|shot|arrow|spear|needle|bolt|orb|missile', 'projectile'),
    (r'storm|rain|field|area|zone|explosion|burst|wave|quake|collapse|meteor|domain', 'area'),
]

def arch_for(id_, shape, t):
    if t:
        if t.get('projectiles'):
            return 'cone' if any('Breath' in p for p in t['projectiles']) else 'projectile'
    for pat, a in KEY_ARCH:
        if re.search(pat, id_): return a
    if t and t.get('toggle'): return 'aura'
    return shape

OFFENSIVE = {'projectile', 'beam', 'cone', 'area', 'slash'}
# extra modes for uniques/ultimates, picked by what the skill's particles are made of
POOL = {
    'flame': ['projectile', 'area', 'cone'], 'black_flame': ['beam', 'area', 'slash'], 'ember': ['projectile', 'cone', 'area'],
    'frost': ['projectile', 'area', 'guard'], 'spark': ['beam', 'area', 'move'], 'dark_spark': ['beam', 'area', 'slash'],
    'void': ['area', 'beam', 'move'], 'holy': ['beam', 'heal', 'guard'], 'heal': ['heal', 'guard', 'sense'],
    'petal': ['heal', 'slash', 'area'], 'acid': ['projectile', 'cone', 'area'], 'bubble': ['projectile', 'guard', 'move'],
    'drop': ['projectile', 'heal', 'cone'], 'gust': ['slash', 'move', 'area'], 'mist': ['sense', 'move', 'area'],
    'soul': ['beam', 'sense', 'summon'], 'spirit': ['sense', 'guard', 'beam'], 'rune': ['sense', 'area', 'guard'],
    'shard': ['projectile', 'slash', 'guard'], 'blood': ['slash', 'heal', 'beam'], 'slash': ['slash', 'beam', 'move'],
    'shockwave': ['area', 'slash', 'guard'], 'aura': ['awaken', 'guard', 'slash'],
}
MODE_NAME = {'projectile': 'shot', 'beam': 'ray', 'cone': 'breath', 'area': 'storm', 'slash': 'strike', 'aura': 'aura',
             'guard': 'guard', 'sense': 'sight', 'heal': 'restore', 'move': 'shift', 'summon': 'summon', 'awaken': 'awaken'}

def first(d, *keys):
    for k in keys:
        if d and k in d and isinstance(d[k], (int, float)) and not isinstance(d[k], bool): return d[k]
    return None

out = []
for e in man:
    id_ = e['id']
    if id_ in existing: continue
    cats = e['categories']
    rk = rank_of(cats)
    typ, mp, ap, dmg, cd = RANK[rk]
    rec = fx.get(id_, {'families': ['aura'], 'shape': 'aura'})
    fam = rec['families']
    # find tensura's version: same id, same squashed name, or the display name
    t = tab_by.get(norm(id_)) or tab_by.get(norm(e['display_name'])) or tab_by.get(norm(re.sub(r'_(arts|magic|skill)_.*$', '', id_)))
    c = None
    if t:
        for cn in t.get('cfg', []):
            if norm(cn) in cfg_by: c = cfg_by[norm(cn)]; break
    if c is None: c = cfg_by.get(norm(id_)) or cfg_by.get(norm(e['display_name']))
    src = 'tensura' if (t or c) else 'lore'

    s = {'id': id_, 'name': e['display_name'], 'type': typ, 'rank': rk, 'elem': fam[0], 'acc': fam[-1], 'src': src}
    if rk in ('resistance', 'nullification'):
        s['arch'] = 'resist'
        out.append(s); continue

    arch = arch_for(id_, rec['shape'], t)
    v = first(c, 'magiculeCost', 'minCost')
    if v is not None: mp = v
    v = first(c, 'auraCost')
    if v is not None: ap = v
    if rk == 'arts' and first(c, 'magiculeCost') is None: mp = 0
    v = first(c, 'magicDamage', 'damage', 'baseDamage', 'dashDamage', 'flameDamage', 'fireDamage')
    if v is not None:
        dmg = v + (first(c, 'fireDamage') or 0 if 'magicDamage' in (c or {}) else 0)
    v = first(c, 'cooldown')
    if v is not None: cd = v
    s.update(arch=arch, mp=round(mp), ap=round(ap), dmg=round(float(dmg), 1), cd=round(float(cd) * 20))
    v = first(c, 'cooldownMastered')
    s['cdm'] = round(float(v) * 20) if v is not None else max(0, s['cd'] // 2)
    v = first(c, 'range', 'maxDistance', 'distance', 'dashDistance')
    s['range'] = round(float(v), 1) if v is not None else {'beam': 30, 'projectile': 40, 'area': 20, 'move': 12, 'cone': 8, 'slash': 4}.get(arch, 16)
    v = first(c, 'radius', 'explosionRadius', 'flameRadius', 'size')
    s['radius'] = round(float(v), 1) if v is not None else (6 if rk == 'ultimate' else 4 if rk == 'unique' else 3)
    v = first(c, 'castTime')
    if typ == 'MAGIC':
        s['cast'] = round(v) if v is not None else 20
        v2 = first(c, 'castTimeMastered')
        s['castm'] = round(v2) if v2 is not None else s['cast'] // 2
        s['mclass'] = magic_class(cats, (t or {}).get('pkg', ''))
    v = first(c, 'effectTime', 'duration', 'strengthenTime', 'blossomDuration')
    s['dur'] = round(v) if v is not None else (1200 if arch == 'awaken' else 600)

    # one-mode skills that just stay on are toggles. uniques/ultimates get a few modes
    if rk in ('unique', 'ultimate'):
        pool = [arch] + [a for f in fam for a in POOL.get(f, [])] + ['awaken', 'sense', 'guard']
        modes = []
        for a in pool:
            if a not in modes: modes.append(a)
            if len(modes) == (4 if rk == 'ultimate' else 3): break
        s['modes'] = modes
    elif arch in ('aura', 'guard', 'sense') and typ != 'MAGIC':
        s['toggle'] = True
    out.append(s)

dst = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/lore_skills.json')
json.dump(out, open(dst, 'w'), separators=(',', ':'))
import collections
print(len(out), 'skills;', collections.Counter(x['src'] for x in out), collections.Counter(x['arch'] for x in out).most_common())

# ---- names, descriptions and icons
lang_p = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/lang/en_us.json')
lang = json.load(open(lang_p))
ELEM = {'flame': 'burns', 'ember': 'burns', 'black_flame': 'burns with black flame and withers', 'frost': 'freezes and slows',
        'spark': 'paralyses for a moment', 'dark_spark': 'blinds with darkness and withers', 'void': 'withers', 'acid': 'poisons',
        'mist': 'blinds', 'soul': 'weakens', 'spirit': 'weakens', 'rune': 'slows their hands', 'shard': 'slows', 'blood': 'heals you for part of it',
        'petal': 'heals you a little', 'heal': 'heals you a little', 'holy': 'marks them (double against undead)',
        'gust': 'blows them back', 'shockwave': 'knocks them back', 'bubble': 'knocks them back', 'drop': 'knocks them back'}
def money(x):
    x = int(x)
    return f'{x:,}'
def cost(sp, mult=1):
    bits = []
    if sp.get('mp'): bits.append(money(sp['mp'] * mult) + ' magicules')
    if sp.get('ap'): bits.append(money(sp['ap'] * mult) + ' aura')
    return ' and '.join(bits) if bits else 'nothing'
def what(sp, a):
    d, el = sp.get('dmg', 0), ELEM.get(sp['elem'], '')
    hit = f' and {el}' if el else ''
    r = sp.get('range', 16); rad = sp.get('radius', 3)
    return {
        'projectile': f'Fires a bolt up to {r:g} blocks for {d:g} damage{hit}.',
        'beam': f'A ray up to {r:g} blocks that hits everything in the line for {d:g}{hit}.',
        'cone': f'A blast in front of you for {d:g} damage{hit}.',
        'area': f'Hits everything within {rad:g} blocks of where you look for {d:g}{hit}.',
        'slash': f'A sweep in front of you for {d:g} plus your attack{hit}.',
        'aura': f'Your power pours out: more attack, and your hits {el or "hit harder"}.',
        'guard': 'Cuts the damage you take and keeps you steady.',
        'sense': 'You can see every living thing around you, even through walls, and in the dark.',
        'heal': f'Heals you {max(6, d):g} and people near you half that.',
        'move': f'Jumps you up to {r:g} blocks where you look, or right behind someone.',
        'summon': 'Calls helpers that fight for you for a while.',
        'awaken': 'Lets your full power out for a while: stronger, tougher, faster.',
    }[a]
for sp in out:
    k = 'ability.jaycee.' + sp['id']
    lang[k] = sp['name']
    if sp['arch'] == 'resist':
        nul = sp['type'] == 'NULLIFICATION'
        desc = ('Always on. Blocks this kind of harm completely (unless they have twice your EP).' if nul
                else 'Always on. Halves this kind of damage (big hits over half your health still go through), and sometimes shrugs off its effects.')
    elif sp.get('modes'):
        desc = ' '.join(f'{MODE_NAME[m].title()}: {what(sp, m)}' for m in sp['modes']) + f' Costs {cost(sp)}.'
    else:
        desc = what(sp, sp['arch'])
        if sp.get('toggle'): desc += f' Toggle, costs {cost(sp)} to start and a tenth of that every second.'
        elif sp['type'] == 'MAGIC': desc += f" Hold to cast ({sp['cast']/20:g}s, {sp['castm']/20:g}s mastered), let go to fire. Costs {cost(sp)}."
        else: desc += f' Costs {cost(sp)}.'
    lang[k + '.desc'] = desc
for m, n in MODE_NAME.items(): lang['jaycee.lore.mode.' + m] = n.title()
lang.update({'jaycee.lore.cast_stopped': '%s: let go too early, the cast fell apart.',
             'jaycee.lore.cast_ready': '%s is ready. Let go to cast!', 'jaycee.lore.casting': 'Casting %s... %ss',
             'jaycee.lore.blocked': 'Something is in the way.'})
json.dump(lang, open(lang_p, 'w'), indent=1, ensure_ascii=False)

if len(sys.argv) > 5:
    import zipfile, io
    from PIL import Image
    z = zipfile.ZipFile(sys.argv[5])
    files = {e['id']: e['file'] for e in man}
    dst_dir = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/textures/ability')
    n = 0
    for sp in out:
        dst = os.path.join(dst_dir, sp['id'] + '.png')
        if os.path.exists(dst) or sp['id'] not in files: continue
        try:
            im = Image.open(io.BytesIO(z.read(files[sp['id']]))).convert('RGBA').resize((64, 64), Image.LANCZOS)
            im.save(dst, optimize=True); n += 1
        except KeyError:
            pass
    print('icons', n)

# gives every skill its own look and hit behaviour: two colours, two particle sprites, a pattern and tags.
# order of trust: tensura's code for that skill, then manas reborn's, then the lore (what the name says), then
# a spread so no two neighbours look the same. user notes from the review bench win over all of it (OVERRIDES).
#   python3 tools/gen_lore_fx.py <mod_fx.json> <mod_particle_colors.json>
import json, re, os, sys, hashlib, colorsys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
mod_fx = json.load(open(sys.argv[1])); mod_col = json.load(open(sys.argv[2]))
def norm(s): return re.sub(r'[^a-z0-9]', '', s.lower())
mod_by = {}
for k, v in mod_fx.items():
    mod_by.setdefault(norm(k.replace('@mr', '')), []).append(v)

# what each of their particles looks like in our terms: (our sprite family, colour)
P = {
 'RED_FIRE': ('flame', '#ff7a2a'), 'FLAME': ('flame', '#ff9a3a'), 'BLACK_FIRE': ('black_flame', '#2c1f4a'), 'PLASMA_FIRE': ('flame', '#a23cee'),
 'ILLUSION_FIRE': ('flame', '#c67323'), 'HEAT_EFFECT': ('ember', '#f39639'), 'LAVA': ('ember', '#ff6a00'), 'SOUL_FIRE_FLAME': ('soul', '#4fe3ff'),
 'LIGHTNING_SPARK': ('spark', '#9185fd'), 'LIGHTNING_EFFECT': ('spark', '#9185fd'), 'YELLOW_LIGHTNING_SPARK': ('spark', '#f1fd85'),
 'PURPLE_LIGHTNING_SPARK': ('dark_spark', '#c185fd'), 'DARK_PURPLE_LIGHTNING_SPARK': ('dark_spark', '#47056d'), 'DARK_RED_LIGHTNING_SPARK': ('spark', '#6d0906'),
 'RED_LIGHTNING_SPARK': ('spark', '#fd858b'), 'GREEN_LIGHTNING_SPARK': ('spark', '#9ffd85'), 'BLACK_LIGHTNING_SPARK': ('dark_spark', '#2b2d3a'),
 'BLACK_LIGHTNING_EFFECT': ('dark_spark', '#2b2d3a'), 'ELECTRIC_SPARK': ('spark', '#c7f7ff'), 'SNOWFLAKE': ('frost', '#c2eef6'), 'SNOWFLAKE_EFFECT': ('frost', '#c2eef6'),
 'SOLAR_FLASH': ('holy', '#fff6c4'), 'FLASH': ('holy', '#ffffff'), 'END_ROD': ('holy', '#fffbe0'), 'INSTANT_EFFECT': ('holy', '#ffffff'),
 'TOTEM_OF_UNDYING': ('holy', '#f5e04a'), 'WAX_ON': ('holy', '#ffcc55'), 'WAX_OFF': ('holy', '#f4f4f4'), 'BLOSSOM': ('petal', '#c66ccc'),
 'CHERRY_LEAVES': ('petal', '#ffb7d5'), 'SOUL': ('void', '#1e1e1e'), 'SCULK_SOUL': ('soul', '#3fe0d0'), 'BATS_MODE': ('mist', '#030302'),
 'FALLING_ACID': ('acid', '#9be33b'), 'LANDING_ACID': ('acid', '#9be33b'), 'FALLING_HEAL_DROP': ('heal', '#7dff9a'), 'LANDING_HEAL_DROP': ('heal', '#7dff9a'),
 'COMPOSTER': ('heal', '#8bd14b'), 'HAPPY_VILLAGER': ('heal', '#6dff6d'), 'HEART': ('heal', '#ff4d6d'), 'ANGRY_VILLAGER': ('blood', '#c03030'),
 'DAMAGE_INDICATOR': ('blood', '#8a0000'), 'CRIMSON_SPORE': ('blood', '#c83a3a'), 'WARPED_SPORE': ('soul', '#2ee0c0'), 'GLOW': ('soul', '#88ffff'),
 'ENCHANT': ('rune', '#b68cff'), 'PORTAL': ('void', '#a64dff'), 'REVERSE_PORTAL': ('void', '#a64dff'), 'WITCH': ('void', '#b04dff'),
 'DRAGON_BREATH': ('void', '#d060ff'), 'RAID_OMEN': ('void', '#3b5bff'), 'TRIAL_OMEN': ('void', '#2a6cff'), 'SWEEP_ATTACK': ('slash', '#f0f0f0'),
 'CRIT': ('slash', '#fff3b0'), 'ENCHANTED_HIT': ('slash', '#7ad7ff'), 'EXPLOSION': ('shockwave', '#ffffff'), 'EXPLOSION_EMITTER': ('shockwave', '#ffe9c4'),
 'SQUID_INK': ('mist', '#101010'), 'SMOKE': ('mist', '#3a3a3a'), 'LARGE_SMOKE': ('mist', '#2e2e2e'), 'CLOUD': ('mist', '#e8e8e8'), 'POOF': ('mist', '#dddddd'),
 'WHITE_ASH': ('mist', '#e0e0e0'), 'ASH': ('mist', '#555555'), 'SPIT': ('mist', '#dddddd'), 'SPLASH': ('bubble', '#3fa0ff'), 'BUBBLE': ('bubble', '#6fc0ff'),
 'FISHING': ('drop', '#3fa0ff'), 'DOLPHIN': ('drop', '#3f8fff'), 'DRIPPING_WATER': ('drop', '#3f8fff'), 'GUST': ('gust', '#e0f5f0'),
 'GUST_EMITTER_SMALL': ('gust', '#e0f5f0'), 'GUST_EMITTER_LARGE': ('gust', '#e0f5f0'), 'SONIC_BOOM': ('sonic', '#99ffff'), 'BLOCK': ('shard', '#8b6b4a'),
 'ITEM': ('shard', '#9a9a9a'), 'FALLING_DUST': ('shard', '#a88b6a'), 'WEATHER_MANIPULATION': ('gust', '#cfe8ff'), 'SHOCK_WAVE': ('shockwave', '#f0f0f0'),
}
# lore: words in the name -> (sprite family, colour 1, colour 2, element for hits). first match wins, so specific beats general
LORE = [
 (r'black.*(thunder|lightning)|(thunder|lightning).*black', 'dark_spark', '#2a0d3d', '#7a2bc0', 'dark_spark'),
 (r'black_?flame|hell_?fire|hellfire|black_fire', 'black_flame', '#2c1f4a', '#8a3cff', 'black_flame'),
 (r'thunder|lightning|electr|plasma|volt|storm_?god|raijin', 'spark', '#9fe0ff', '#ffffff', 'spark'),
 (r'flame|fire|inferno|blaze|burn|heat|magma|lava|solar|sun|ignis|pyro|explos|nuclear|napalm', 'flame', '#ff7a2a', '#ffd36b', 'flame'),
 (r'ice|frost|cold|freez|blizzard|glacier|snow|cocytus|icicle|winter', 'frost', '#9fe6ff', '#e8fbff', 'frost'),
 (r'water|aqua|tide|wave|hydro|rain|sea|ocean|flood|bubble|mist_?form', 'drop', '#3f8fff', '#a8e1ff', 'drop'),
 (r'wind|air|gale|tornado|cyclone|gust|sky|hurricane|breeze|flight', 'gust', '#dff5ef', '#8fe0c8', 'gust'),
 (r'earth|stone|rock|mud|sand|quake|metal|iron|steel|golem|mountain|terra', 'shard', '#9c7a52', '#d6b98c', 'shard'),
 (r'gravity|graviton|weight|burden', 'void', '#3a2a6a', '#b9a8ff', 'void'),
 (r'space|spatial|dimension|gate|warp|teleport|portal|void|abyss|null', 'void', '#4a2a9c', '#c8b6ff', 'void'),
 (r'time|chrono|future|past|eternity|eternal|clock', 'rune', '#f5d76e', '#ffffff', 'rune'),
 (r'holy|sacred|divine|angel|heaven|hero|saint|god|light|radian|celestial|miracle|bless|pure|truth|justice|immortal|absolute', 'holy', '#ffe98a', '#ffffff', 'holy'),
 (r'dark|shadow|night|demon|evil|devil|hell|death|despair|curse|fear|nightmare|necro|undead|lich|grave|doom', 'void', '#2a1238', '#9c4dff', 'void'),
 (r'poison|venom|toxic|acid|corros|decay|rot|plague|miasma', 'acid', '#7bd13b', '#c4ff6b', 'acid'),
 (r'blood|vampir|drain|life_?steal|sangu|crimson', 'blood', '#a8102a', '#ff5577', 'blood'),
 (r'soul|spirit|ghost|phantom|wraith|astral|ether', 'soul', '#6fe7ff', '#d0fff9', 'soul'),
 (r'dragon|wyrm|drake', 'spark', '#3b7dd8', '#bfe0ff', 'spark'),
 (r'beast|wolf|fang|claw|feral|tiger|lion|ogre|berserk|savage|wild', 'ember', '#c9572a', '#ffcf8a', 'slash'),
 (r'gluttony|predat|devour|eat|beelzebub|hunger|absorb', 'mist', '#2a0f3a', '#a04dff', 'void'),
 (r'wrath|rage|satan', 'flame', '#b00a0a', '#ff4a2a', 'flame'),
 (r'pride|lucifer', 'holy', '#ffd34a', '#fff7c4', 'holy'),
 (r'envy|leviathan|jealous', 'acid', '#2f8f4f', '#9bff9b', 'acid'),
 (r'lust|asmodeus|charm|seduc|love', 'petal', '#ff5fae', '#ffc0e0', 'petal'),
 (r'sloth|belphegor|lazy|sleep|dream', 'mist', '#8a8fa8', '#d8dbea', 'mist'),
 (r'greed|mammon|wealth|gold', 'holy', '#e0b030', '#fff0a0', 'holy'),
 (r'plant|flower|petal|blossom|nature|forest|tree|leaf|bloom|vine|wood|sakura|cherry', 'petal', '#ff8fc7', '#8fe38f', 'petal'),
 (r'music|sound|voice|sonic|song|roar|howl|scream|echo|ultrasonic', 'sonic', '#ffffff', '#bfa8ff', 'gust'),
 (r'heal|recover|regenerat|restor|cure|revive|resurrect|vital|health', 'heal', '#6dff9a', '#ffffff', 'heal'),
 (r'mind|mental|thought|wisdom|sage|analy|insight|eye|sense|percep|detect|predict|calcul|know|apprais|clairvoy|search', 'rune', '#6ea8ff', '#d6e6ff', 'rune'),
 (r'chain|bind|seal|lock|shackle|thread|prison|jail|cage|restrain|string|web', 'rune', '#b0b8c8', '#ffffff', 'rune'),
 (r'alter|change|transform|mimic|copy|mirror|shape|morph|disguise|illusion|deceiv|trick', 'rune', '#c47dff', '#7de0ff', 'rune'),
 (r'dissolv|disintegrat|decompos|annihil|erase|destroy|ruin|break|crush|collapse', 'void', '#9aa0b5', '#ffffff', 'void'),
 (r'star|cosmic|galax|moon|astro|comet|meteor', 'holy', '#2a3a8a', '#fff8c2', 'holy'),
 (r'slash|blade|sword|cut|edge|katana|sever', 'slash', '#e8f0ff', '#9ab8ff', 'slash'),
 (r'strength|power|might|body|muscle|steel_?body|strong', 'aura', '#ff9a4a', '#ffffff', 'shockwave'),
 (r'speed|swift|haste|fast|instant|flash|blink|accel', 'gust', '#bff3ff', '#ffffff', 'gust'),
 (r'shield|barrier|wall|protect|guard|defen|armor|armour|fortress', 'rune', '#7fb4ff', '#ffffff', 'rune'),
 (r'magic|mana|arcane|spell|sorcer|witch|wizard', 'rune', '#8f6bff', '#d8c8ff', 'rune'),
]
# hit behaviours (mechanics), also from the name
TAGS = [
 ('control', r'domina|control|manipulat|command|rule|order|subjugat|king|monarch|lord'), ('gravity', r'gravit|weight|burden|crush'),
 ('time', r'time|chrono|stop|freez_?time|accelerat|thought'), ('space', r'space|spatial|dimension|gate|warp|teleport|portal'),
 ('drain', r'drain|absorb|predat|glutton|devour|eat|steal|vampir|beelzebub|hunger|life'), ('bind', r'chain|bind|seal|lock|shackle|thread|prison|jail|restrain|string|web|paraly'),
 ('undying', r'immortal|undying|regenerat|eternal|absolute|resurrect|revive'), ('beast', r'beast|wolf|fang|claw|feral|ogre|berserk|wild'),
 ('fear', r'fear|despair|terror|dread|nightmare|curse|haki|coerc|intimid'), ('erase', r'dissolv|disintegrat|annihil|erase|destroy|ruin|nullif|decompos'),
 ('blind', r'light|flash|solar|radian|illusion|mist|fog|smoke|shadow'), ('chain', r'thunder|lightning|electr|chain'),
]
PATTERNS = {
 'projectile': ['orb', 'lance', 'star', 'triple', 'helix', 'meteor', 'crescent', 'barrage'],
 'beam': ['straight', 'helix', 'strike', 'zigzag', 'wide'],
 'cone': ['spray', 'vortex', 'fan', 'roar'],
 'area': ['pillar', 'rain', 'nova', 'dome', 'vortex', 'quake', 'meteors', 'field'],
 'slash': ['arc', 'cross', 'flurry', 'cleave', 'wave', 'spin'],
 'aura': ['rise', 'orbit', 'halo', 'crackle', 'drip', 'wings', 'mist', 'runes'],
 'guard': ['dome', 'hex', 'shards', 'wall', 'cocoon'],
 'sense': ['pulse', 'eye', 'radar', 'threads'],
 'heal': ['spiral', 'rain', 'bloom', 'crosses'],
 'move': ['blink', 'trail', 'shadow', 'dash', 'flash'],
 'summon': ['portal', 'rise', 'call'],
 'awaken': ['pillar', 'explode', 'wings', 'crown', 'storm'],
 'resist': ['ripple'],
}
PAT_WORD = [
 (r'rain|shower|downpour', {'area': 'rain', 'heal': 'rain'}), (r'pillar|tower|column|pillar', {'area': 'pillar', 'awaken': 'pillar'}),
 (r'wall|barrier|fortress', {'guard': 'wall'}), (r'dome|sphere|field|zone|domain|territory|area', {'guard': 'dome', 'area': 'field'}),
 (r'wing|angel|harpy|flight|fly', {'aura': 'wings', 'awaken': 'wings', 'move': 'dash'}), (r'eye|sight|vision|clairvoy', {'sense': 'eye'}),
 (r'spear|lance|javelin|needle|arrow|bolt|icicle', {'projectile': 'lance', 'beam': 'straight'}), (r'meteor|comet|star_?fall|fall', {'projectile': 'meteor', 'area': 'meteors'}),
 (r'nova|explos|burst|blast|bomb|nuclear', {'area': 'nova', 'projectile': 'orb', 'awaken': 'explode'}), (r'vortex|tornado|whirl|cyclone|spiral|maelstrom', {'area': 'vortex', 'cone': 'vortex', 'projectile': 'helix'}),
 (r'quake|earth|tremor|fissure|crack', {'area': 'quake'}), (r'thunder|lightning|electr', {'beam': 'zigzag', 'aura': 'crackle', 'awaken': 'storm', 'area': 'pillar', 'cone': 'fan'}),
 (r'cross|x_', {'slash': 'cross'}), (r'flurry|hundred|thousand|rush|barrage|rapid|eight|five|petal', {'slash': 'flurry', 'projectile': 'barrage'}),
 (r'cleave|guillotine|break|crush|smash', {'slash': 'cleave'}), (r'wave|crescent|moon', {'slash': 'wave', 'projectile': 'crescent'}),
 (r'spin|whirlwind|cyclone|round', {'slash': 'spin'}), (r'crown|king|monarch|lord|ruler|emperor|god', {'awaken': 'crown', 'aura': 'halo'}),
 (r'halo|saint|holy|angel', {'aura': 'halo'}), (r'chain|thread|string|bind|web', {'sense': 'threads', 'guard': 'cocoon'}),
 (r'shadow|dark|night', {'move': 'shadow', 'aura': 'mist'}), (r'flash|instant|light_?speed|blink', {'move': 'flash'}),
 (r'heal|bloom|flower|blossom', {'heal': 'bloom'}), (r'rune|magic|circle|seal', {'aura': 'runes', 'sense': 'radar'}),
 (r'drip|blood|acid|poison', {'aura': 'drip'}), (r'gate|portal|summon|call', {'summon': 'portal'}), (r'roar|howl|voice|sonic|sound', {'cone': 'roar'}),
 (r'breath', {'cone': 'spray'}), (r'scale|shell|armor|armour|skin', {'guard': 'hex', 'aura': 'orbit'}),
]
# what you told me on the review bench
OVERRIDES = {
 'black_flame_thunder': {'f1': 'dark_spark', 'f2': 'black_flame', 'c1': '#1a0726', 'c2': '#5a1690', 'pat': {'*': 'zigzag', 'aura': 'crackle', 'awaken': 'storm', 'area': 'pillar'}},
 'dragon_body': {'f1': 'spark', 'f2': 'spark', 'c1': '#2f7dff', 'c2': '#bfe0ff', 'pat': {'*': 'crackle'}, 'note': 'blue lightning running over the body'},
 'ogre_berserker': {'f1': 'flame', 'f2': 'blood', 'c1': '#c01010', 'c2': '#ff4a2a', 'pat': {'*': 'rise'}},
}

def h(s): return int(hashlib.md5(s.encode()).hexdigest()[:8], 16)
def shade(hexc, k, seed):
    r, g, b = (int(hexc[i:i + 2], 16) / 255 for i in (1, 3, 5))
    hh, l, s = colorsys.rgb_to_hls(r, g, b)
    hh = (hh + ((seed % 21) - 10) / 360.0) % 1.0
    l = max(0.06, min(0.94, l + (((seed >> 5) % 13) - 6) / 100.0 * k))
    r, g, b = colorsys.hls_to_rgb(hh, l, s)
    return '#%02x%02x%02x' % (int(r * 255), int(g * 255), int(b * 255))

def look(sid, name, arch, modes, fam0, fam1):
    key = sid + ' ' + norm(name)
    text = sid
    src = 'lore'; f1, f2, c1, c2, elem = fam0, fam1, None, None, None
    ref = []
    m = mod_by.get(norm(sid)) or mod_by.get(norm(name))
    if m:
        GENERIC = {'EXPLOSION', 'EXPLOSION_EMITTER', 'INSTANT_EFFECT', 'FLASH', 'SMOKE', 'LARGE_SMOKE', 'CLOUD', 'POOF', 'SWEEP_ATTACK', 'CRIT',
                   'BLOCK', 'ITEM', 'FALLING_DUST', 'WAX_ON', 'WAX_OFF', 'END_ROD', 'ENCHANTED_HIT', 'SHOCK_WAVE', 'WHITE_ASH', 'SPIT', 'ANGRY_VILLAGER'}
        parts = [p for v in m for p in v['particles'] if p in P and p not in GENERIC]
        ref = sorted({p for v in m for p in v['particles']}) + sorted({'circle:' + c for v in m for c in v['circle']})
        if parts:
            src = m[0]['mod']
            # the most telling particle leads (lightning, fire, frost... before anything else they spawn)
            order = ['DARK_PURPLE', 'BLACK', 'PURPLE', 'DARK_RED', 'RED_L', 'YELLOW', 'GREEN', 'LIGHTNING', 'FIRE', 'FLAME', 'SNOW', 'SOLAR', 'ACID', 'HEAL', 'BLOSSOM', 'SOUL']
            parts.sort(key=lambda p: next((i for i, k in enumerate(order) if k in p), 99))
            a = P[parts[0]]
            others = [P[p] for p in parts if P[p] != a]
            b = others[0] if others else a
            f1, c1, f2, c2 = a[0], a[1], b[0], b[1]
    for pat, fa, ca, cb, el in LORE:
        if re.search(pat, text):
            if c1 is None: f1, c1, f2, c2 = fa, ca, (fa if f2 == fam1 else f2), cb
            elem = el
            break
    if c1 is None:
        # nothing to go on: keep the recipe's sprites, pick colours from them, spread the hue
        base = {'spirit': '#a9fff0', 'aura': '#c9a7ff', 'spark': '#fff27a', 'slash': '#f2f2f2', 'rune': '#8c7dff', 'holy': '#fff4c2', 'void': '#5a2d82',
                'mist': '#b8c0cc', 'ember': '#ff5a1f', 'bubble': '#8fd3ff', 'dark_spark': '#9b5cff', 'shockwave': '#ffffff', 'flame': '#ff8a2b', 'shard': '#d8e6ff',
                'gust': '#dfeeee', 'heal': '#7dff9a', 'drop': '#4aa8ff', 'frost': '#bfe9ff', 'acid': '#9be33b', 'soul': '#6fe7ff', 'black_flame': '#3b1d5c',
                'petal': '#ff9ecb', 'blood': '#c0182c'}
        c1, c2 = base.get(fam0, '#c9a7ff'), base.get(fam1, '#ffffff')
    if elem is None: elem = f1 if f1 not in ('aura', 'sonic') else fam0
    seed = h(sid)
    c1, c2 = shade(c1, 1, seed), shade(c2, 0.5, seed >> 8)
    # pattern per kind the skill can do
    pats = {}
    for k in set([arch] + list(modes or [])):
        pool = PATTERNS.get(k, ['rise'])
        pick = None
        for pw, mp in PAT_WORD:
            if k in mp and re.search(pw, text): pick = mp[k]; break
        pats[k] = pick or pool[seed % len(pool)]
    tags = [t for t, pw in TAGS if re.search(pw, text)][:3]
    o = OVERRIDES.get(sid)
    if o:
        f1, f2, c1, c2 = o.get('f1', f1), o.get('f2', f2), o.get('c1', c1), o.get('c2', c2)
        for k in pats: pats[k] = o['pat'].get(k, o['pat'].get('*', pats[k]))
        if src == 'lore': src = 'your notes'
        else: src += ' + your notes'
    return {'f1': f1, 'f2': f2, 'c1': c1, 'c2': c2, 'pat': pats, 'elem': elem, 'tags': tags, 'src': src, 'ref': ref[:8]}

if __name__ == '__main__':
    p = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/lore_skills.json')
    specs = json.load(open(p))
    import collections
    for s in specs:
        s['fx'] = look(s['id'], s['name'], s['arch'], s.get('modes'), s['elem'], s['acc'])
        s['elem'] = s['fx']['elem']
    json.dump(specs, open(p, 'w'), separators=(',', ':'))
    combos = collections.Counter((s['arch'], s['fx']['f1'], s['fx']['c1'], tuple(sorted(s['fx']['pat'].items()))) for s in specs)
    print(len(specs), 'skills;', len(combos), 'different looks; sources', collections.Counter(s['fx']['src'].split(' ')[0] for s in specs))
    print('elements', collections.Counter(s['elem'] for s in specs).most_common(12))

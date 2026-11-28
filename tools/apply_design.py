# puts skill_design.py onto lore_skills.json: every skill gets 'acts' (its real modes, each with an action)
#   python3 tools/apply_design.py <ref_modes.json>
import json, re, sys, os
sys.path.insert(0, os.path.dirname(__file__))
from skill_design import U, act_for, KIND, TOGGLE, FIX, SKIP_MODE, MODE_FIX, DESC, ULT_OF, SKILL_DESC
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = json.load(open(sys.argv[1]))
def norm(s): return re.sub(r'[^a-z0-9]', '', s.lower())
OLD = {'projectile': 'bolt', 'beam': 'beam', 'cone': 'breath', 'area': 'blast', 'slash': 'strike', 'aura': 'buff', 'guard': 'barrier',
       'sense': 'sense', 'heal': 'heal', 'move': 'dash', 'summon': 'summon', 'awaken': 'awaken'}
p = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/lore_skills.json')
specs = json.load(open(p))
review = []
for s in specs:
    if s['arch'] == 'resist': continue
    sid, name = s['id'], s['name']
    acts = []
    if sid in U:
        for mstr in U[sid]:
            parts = mstr.split('|')
            acts.append({'n': parts[0], 'a': parts[1], 'p': float(parts[2]) if len(parts) > 2 else 1.0})
        src = 'hand'
    else:
        refs = REF.get(norm(sid)) or REF.get(norm(name)) or []
        mods = [m for r in refs for m in r['modes'] if m[1] and not SKIP_MODE.search(m[1])]
        base = FIX.get(sid) or act_for(sid, name) or OLD.get(s['arch'], 'buff')
        if len(mods) > 1:
            seen = set()
            for mm in mods:
                if mm[1] in seen: continue
                seen.add(mm[1])
                acts.append({'n': mm[1], 'a': MODE_FIX.get(mm[1]) or act_for(mm[0], mm[1]) or base, 'p': 1.0})
            acts = acts[:6]
            src = 'mod modes'
        else:
            acts = [{'n': name, 'a': base, 'p': 1.0}]
            src = 'rule'
        review.append((s['rank'], sid, name, [(a['n'], a['a']) for a in acts], src))
    for i, a in enumerate(acts): a['k'] = re.sub(r'[^a-z0-9]+', '_', a['n'].lower()).strip('_')[:24] or 'mode%d' % i
    s['acts'] = acts
    s['arch'] = KIND.get(acts[0]['a'], 'aura')
    s['modes'] = [KIND.get(a['a'], 'aura') for a in acts] if len(acts) > 1 else None
    s.pop('toggle', None)
    if len(acts) == 1 and acts[0]['a'] in TOGGLE and s['type'] != 'MAGIC': s['toggle'] = True
    if s.get('modes') is None: s.pop('modes', None)
json.dump(specs, open(p, 'w'), separators=(',', ':'))
# mode descriptions for the hand-built ones
lp = os.path.join(ROOT, 'jaycee/src/main/resources/assets/jaycee/lang/en_us.json')
lang = json.load(open(lp))
for s in specs:
    if s['id'] in SKILL_DESC: lang['ability.jaycee.%s.desc' % s['id']] = SKILL_DESC[s['id']]
    d = DESC.get(ULT_OF.get(s['id'], s['id']))
    if not d: continue
    for a in s['acts']:
        if a['n'] in d:
            lang['ability.jaycee.%s.mode.%s' % (s['id'], a['k'])] = a['n']
            lang['ability.jaycee.%s.mode.%s.desc' % (s['id'], a['k'])] = d[a['n']]
json.dump(lang, open(lp, 'w'), indent=1, ensure_ascii=False)
json.dump(review, open('/home/claude/work/gen/design_review.json', 'w'))
import collections
print('acts:', collections.Counter(a['a'] for s in specs for a in s.get('acts', [])).most_common(70))

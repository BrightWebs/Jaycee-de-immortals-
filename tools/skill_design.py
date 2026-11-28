# what every lore skill actually does, mode by mode.
# uniques and ultimates are written by hand: mode names from tensura / manas reborn where they have the skill,
# the lore (web novel / light novel) where they don't. everything else goes through RULES (name -> action), and
# the review list at the bottom gets read by eye so names and actions match.
#
# a mode is "Name|action" or "Name|action|power". actions are what LoreActions.java knows:
#   offence: bolt lance barrage meteor beam chain breath blast nova blackhole strike flurry cleave execute explode
#   moving:  dash blink behind teleport swap
#   control: pull repel bind imprison sleep timestop fear charm dominate dispel suppress gravity
#   taking:  drain essence copy devour isolate
#   you:     buff rage haste invis barrier reflect protect heal rest regen undying awaken mimic
#   other:   analyze storage craft sense summon clone field weather weapon music
import re

U = {}  # uniques and ultimates, by id

def m(sid, *modes):
    U[sid] = list(modes)

# ------------------------------------------------------------ uniques (tensura / manas reborn names first)
m('envy', 'Absorb|absorb', 'Strength Sap|sap', 'Envious Field|envy_field', 'Resentful Barrier|resent', 'Disordered Desire|desire')
m('wrath', 'Breeder Reactor|breeder', 'Enrage|enrage', 'Seismic Fury|fury', 'Growing Rampage|rampage_cast', 'Crimson Rupture|rupture')
m('absolute_end', 'Ending|execute|1.4', 'Finality Field|field', 'Last Rites|suppress')
m('adapter', 'Adapt|buff', 'Resistance Growth|barrier', 'Evolve|awaken')
m('all_rounder', 'Versatile Strike|strike', 'Versatile Shot|bolt', 'Versatile Guard|barrier')
m('analyst', 'Analyze|analyze', 'Analytical Appraisal|copy')
m('avenger', 'Vengeance|reflect', 'Retribution|strike|1.3', 'Grudge|buff')
m('awakener', 'Awaken Self|awaken', 'Awaken Ally|protect', 'Inspire|buff')
m('berserk', 'Mad Ogre|rage', 'Rage|awaken')
m('berserker', 'Berserk|rage', 'Frenzied Strike|flurry')
m('bewilder', 'Charm|charm', 'Target|bind', 'Area|field', 'Kill|execute')
m('carnation', 'Blooming Thorns|blast', 'Petal Storm|barrage', 'Life Bloom|heal')
m('chef', 'Cook|craft', 'Feast|heal', 'Seasoned Blade|strike')
m('chosen_one', "Hero's Charisma|protect", "Hero's Haki|fear")
m('combatant', 'Combat Instinct|haste', 'Strike|flurry', 'Guard|barrier')
m('commander', 'Communication: Movement|protect', 'Communication: Targeting|dominate', 'Thought Domination|charm')
m('cook', 'Chaotic Fate|execute', 'Seize Fate|dispel')
m('craftsman', 'Craft|craft', 'Create Weapon|weapon', 'Reinforce|buff')
m('creator', 'Analytical Appraisal|analyze', 'Skill Creation|copy', 'Create|craft')
m('decipherer', 'Decipher|analyze', 'Unravel|dispel', 'Insight|sense')
m('degenerate', 'Crafting|craft', 'Separate|drain', 'Synthesise|essence')
m('dilettante', 'Dabble|copy', 'Imitation Strike|strike', 'Quick Study|analyze')
m('divine_berserker', 'Divine Rage|rage', 'Godslayer Strike|cleave|1.5', 'Unbreakable|undying')
m('eminence', 'Presence|fear', 'Command|dominate', 'Majesty|protect')
m('engorger', 'Engorge|devour', 'Bloat|buff', 'Regurgitate|blast')
m('falsifier', 'Presence Concealment|invis', 'Fake Death|undying', 'Illusion|clone')
m('fusionist', 'Fuse|craft', 'Disassemble|drain', 'Stone|barrier')
m('give_me_strength', 'Borrow Strength|essence', 'Empower|rage')
m('glutton', 'Gobble|devour', 'Digest|heal', 'Stomach|storage')
# gluttony: tensura's 7 modes (predation, stomach, mimicry, isolation, corrosion, receive, provide) + manas reborn's grasp and void
m('gluttony', 'Predation|devour', 'Stomach|storage', 'Mimicry|mimicry', 'Isolation|isolate', 'Corrosion|corrosion', 'Receive|receive', 'Provide|provide', 'Ravenous Grasp|grasp', 'Devouring Void|void')
m('gourmand', 'Devour|devour', 'Savour|heal', 'Stockpile|storage')
m('gourmet', 'Taste|analyze', 'Delicacy|heal', 'Devour|devour')
m('greed', 'Spiritual Domination|domination', 'Greed Flare|flare', 'Death Wish|death_wish', 'Energy Seize|seize', 'Endless Avarice|avarice')
m('guardian', 'Grant Protection|protect', 'Iron Wall|barrier')
m('guide', 'Guidance|sense', 'Path|teleport', 'Lead|protect')
m('healer', 'Heal|heal', 'Plague|field', 'Infection|chain')
m('imitator', 'Imitate|copy', 'Mimic|mimic', 'Copycat Strike|strike')
m('infinity_prison', 'Imaginary Space|field', 'Imprison|imprison')
m('intruder', 'Intrude|behind', 'Infiltrate|invis', 'Sabotage|suppress')
m('investigator', 'Investigate|analyze', 'Search|sense', 'Expose|dispel')
m('leader', 'Rally|protect', 'Command|dominate', 'Charge|dash')
m('limiter', 'Limit|suppress', 'Release Limit|awaken', 'Restrain|bind')
m('looter', 'Loot|essence', 'Snatch|copy', 'Stash|storage')
m('lord_of_magewolves', 'Wolf Pack|summon', 'Howl|fear', 'Lunar Fang|strike')
m('lust', 'Drain|lust_drain', 'Invigorate|invigorate', 'Rebirth|rebirth', 'Embracing Drain|embrace', 'Death Blessing|blessing', 'Spiritual Manipulation|manipulate', 'Transmission|transmit', 'Siphoning|siphon', 'Implode|implode')
m('manipulator', 'Manipulate|charm', 'Puppet|dominate', 'String Bind|bind')
m('manufacturer', 'Manufacture|craft', 'Create Weapon|weapon', 'Mass Produce|barrage')
m('martial_master', 'Thousand Cuts|flurry', 'Ultra Acceleration|haste', 'Martial Stance|barrier')
m('mathematician', 'Calculate|analyze', 'Predict|sense', 'Optimal Path|blink')
m('megalomania', 'Grandeur|awaken', 'Domineer|fear', 'Delusion|charm')
m('merciless', 'Soul Steal|drain', 'Soul Consume|execute')
m('milim_ear', 'Hear All|sense', 'Danger Detection|reflect')
m('milim_eye', 'Dragon Eye|sense', 'Insight|analyze')
m('motherly_queen', 'Nurture|heal', 'Protective Embrace|protect', 'Brood|summon')
m('murderer', 'Assassinate|execute', 'Bloodlust|rage', 'Shadow Step|behind')
m('musician', 'Sound Wave|breath', 'Sonic Blast|blast', 'Mind Requiem|sleep', 'March|music', 'Rock|nova')
m('observer', 'Presence Detection|sense', 'Danger Detection|reflect')
m('oppressor', 'Oppress|gravity', 'Attract|pull', 'Repel|repel', 'Bleve|nova', 'Flicker|blink')
m('optimist', 'Bright Side|heal', 'Lucky Break|undying', 'Cheer|protect')
m('overcomer', 'Overcome|undying', 'Adversity|rage', 'Second Wind|heal')
m('paranoia', 'Suspicion|sense', 'Persecution|fear', 'Defensive Instinct|reflect')
m('perceiver', 'Perceive|sense', 'Discern|analyze', 'Foresight|reflect')
m('planner', 'Plan|analyze', 'Trap|imprison', 'Contingency|undying')
m('pride', 'Copy|pride_copy', 'Declare Supremacy|declare', 'Supreme Order|order', 'Imperial Ascension|ascension')
m('reaper', 'Attack|cleave', 'Infinite Eater|devour', 'Harvest|execute')
m('recorder', 'Record|analyze', 'Replay|copy', 'Archive|storage')
m('reflector', 'Echo Counter|reflect', 'Echo Reflection|barrier')
m('regressor', 'Regress|rest', 'Rewind|heal', 'Reset|dispel')
m('researcher', 'Research|analyze', 'Experiment|craft', 'Breakthrough|copy')
m('responder', 'Respond|reflect', 'Counter|strike', 'Adapt|barrier')
m('reverser', 'Inverted Fusion (Buff)|buff', 'Inverted Fusion (Debuff)|dispel', 'Alignment Reverse|charm')
m('royal_beast_form', 'Beast King Form|awaken', 'Royal Roar|fear', 'Beast Claws|flurry')
m('scavenger', 'Scavenge|essence', 'Pick Clean|devour', 'Hoard|storage')
m('seeker', 'Seek|sense', 'Find Weakness|analyze', 'Pinpoint|lance')
m('seer', 'Foresee|sense', 'Prophecy|reflect', 'Clairvoyance|analyze')
m('sentient_being', 'Sentience|sense', 'Self Will|undying', 'Mind Over Matter|barrier')
m('severer', 'Severance|cleave|1.4', 'Blade Storm|flurry', 'Dummy Sword|weapon')
m('shadow_striker', 'Espionage|invis', 'Insta-kill|execute', 'Ultra Acceleration|haste')
m('sloth', 'Deep Hypno|deep_hypno', 'Fallen Hypno|fallen_hypno', 'Deprive|deprive', 'Rest|sloth_rest', 'Phantasmal Style|phantasmal', 'Fallen Strike|fallen_strike', 'Lethargy|lethargy', 'Effort|effort', 'Final Lullaby|lullaby')
m('sniper', 'Spatial Manipulation|lance|1.3', 'Create Weapon|weapon', 'Deadeye|sense')
m('spearhead', 'Spearhead|dash', 'Piercing Charge|lance', 'Vanguard|protect')
m('spiral_body', 'Spiral Body|barrier', 'Spiral Strike|strike', 'Coil|regen')
m('starved', 'Corrosion|corrosion', 'Receive|receive', 'Spiritual Domination|dominate')
m('supporter', 'Support|protect', 'Reinforce|buff', 'Mend|heal')
m('suppressor', 'Spatial Suppression|suppress', 'Spatial Motion|blink', 'Swap|swap')
m('survivor', 'Survive|undying', 'Endure|barrier', 'Recover|regen')
m('tempter', 'Temptation|charm', 'Corrupt|debuff', 'Whisper|sleep')
m('thrower', 'Throw|bolt', 'Barrage|barrage', 'Return|pull')
m('time_traveler', 'Stop Time|timestop', 'Rewind|heal', 'Time Skip|blink')
m('tough_guy', 'Tough Skin|barrier', 'Brawl|flurry', 'Stand Firm|undying')
m('traveler', 'Instant Motion|blink', 'Stardust Arrow|lance', 'Stardust Rain|meteor', 'Teleport|teleport', 'Portal Cut|cleave', 'Swap|swap')
m('tuner', 'Tune|buff', 'Resonance|music', 'Discord|debuff')
m('tyrant', 'Tyranny|fear', 'Subjugate|dominate', 'Crush|gravity')
m('unyielding', 'Backup|undying', 'Endure|barrier', 'Unbowed|rage')
m('usurper', 'Rob|copy', 'Force Takeover|dominate', 'Copy|analyze')
m('villain', "Villain's Charisma|charm", "Demon Lord's Haki|fear")
m('voracious_resurrection', 'Devour|devour', 'Resurrect|undying', 'Renewal|heal')
m('wisher', 'Wish|buff', 'Wish for Power|awaken', 'Wish for Safety|barrier')

# ------------------------------------------------------------ ultimates (the lore's own abilities for each)
m('abaddon', 'Abyssal Destruction|nova', 'Locust Swarm|summon', 'Bottomless Pit|blackhole', 'Devour|devour')
m('agastya', 'Ocean Drink|essence', 'Tidal Sage|heal', 'Sea Wave|breath', 'Wisdom|analyze')
m('agni', 'Sacred Fire|beam', 'Fire Domination|blast', 'Consuming Flame|drain', 'Flame Body|barrier')
m('ahura_mazda', 'Light of Wisdom|beam', 'Truth|analyze', 'Ahura Shield|barrier', 'Purge Evil|dispel')
m('alternative', 'Alternate Self|clone', 'Swap Reality|swap', 'Alternate Fate|undying', 'Parallel Thought|haste')
m('amalthea', 'Horn of Plenty|heal', 'Nurture|regen', 'Bounty|protect', 'Goat Charge|dash')
m('amaterasu', 'Sun Goddess|beam', 'Heavenly Radiance|nova', 'Sacred Mirror|reflect', 'Divine Light|heal')
m('amon', 'Hellfire|blast', 'Knowledge of Past and Future|sense', 'Reconcile|charm', 'Infernal Wolf|summon')
m('angra_mainyu', 'Destructive Spirit|nova', 'Corruption|debuff', 'Darkness|field', 'Evil Domination|dominate')
m('artist', 'Create|craft', 'Masterpiece|summon', 'Paint Reality|field', 'Brushstroke|strike')
m('asmodeus', 'Drain|lust_drain', 'Invigorate|invigorate', 'Rebirth|rebirth', 'Embracing Drain|embrace', 'Death Blessing|blessing', 'Spiritual Manipulation|manipulate', 'Transmission|transmit', 'Siphoning|siphon', 'Implode|implode')
m('astaroth', 'Truth Seeing|analyze', 'Serpent Breath|breath', 'Fallen Knowledge|copy', 'Duke of Hell|fear')
m('astarte', 'Love and War|buff', 'Morning Star|lance', 'Fertility|heal', 'Charm|charm')
m('azathoth', 'Void Manipulation|blackhole', 'Void Collapse|devour', 'Absolute Defense|barrier', 'Spatial Domination|teleport', 'Disintegration|execute')
m('azazel', 'Scapegoat|reflect', 'Forbidden Arts|craft', 'Fallen Watcher|sense', 'Desert Exile|imprison')
m('azi_dahaka', 'Three Heads|barrage', 'Serpent Breath|breath', 'Thousand Senses|sense', 'Endless Body|regen')
m('azrael', 'Angel of Death|execute', 'Soul Reaping|drain', 'Record of Souls|analyze', 'Wings of Death|nova')
m('bahamut', 'Mega Flare|beam|1.4', 'Dragon Scale|barrier', 'Dragon King|fear', 'Gigaflare|nova')
# beelzebub, lord of gluttony: everything gluttony has, at ultimate strength (the show: predation, stomach, isolation, mimicry,
# corrosion, and the food chain - receiving and providing skills through your subordinates)
m('beelzebub', 'Soul Devour|soul_devour', 'Predation|devour', 'Gluttonous Darkness|darkness', 'Imaginary Space|storage', 'Mimicry|mimicry', 'Corrosion|corrosion', 'Soul Steal|soul_steal', 'Food Chain|food_chain', 'Imaginary Space Isolation|isolate')
m('beelzebuth', 'Lord of Flies|summon', 'Ravenous Plague|field', 'Devour|devour', 'Gluttonous Void|blackhole')
m('belial', 'Lord of Lies|clone', 'Worthless|suppress', 'Deceit|invis', 'Wicked Flame|blast')
m('belphegor', 'Deep Hypno|deep_hypno', 'Fallen Hypno|fallen_hypno', 'Deprive|deprive', 'Rest|sloth_rest', 'Phantasmal Style|phantasmal', 'Fallen Strike|fallen_strike', 'Lethargy|lethargy', 'Effort|effort', 'Final Lullaby|lullaby')
m('blade_transformation', 'Blade Body|barrier', 'Sword Form|awaken', 'Thousand Blades|flurry', 'Blade Storm|nova')
m('cthugha', 'Living Flame|blast', 'Fomalhaut|beam', 'Flame Body|barrier', 'Fire Vampires|summon')
m('cthulhu', 'Dreaming God|sleep', "R'lyeh Rising|summon", 'Madness|fear', 'Tentacles|bind')
m('deus_ex_machina', 'Divine Machine|summon', 'Create Weapon|weapon', 'Calculation|analyze', 'Machine Cannon|beam')
m('djibril', 'Revelation|analyze', 'Messenger|teleport', 'Holy Strength|buff', 'Trumpet of Judgment|nova')
m('eternal', 'Eternity|undying', 'Endless|regen', 'Timeless|timestop', 'Unchanging|barrier')
m('faust', 'Analytical Appraisal|analyze', 'Research|copy', 'Truth Seeking|sense', 'Faustian Pact|essence')
m('fortuna', 'Lucky Strike|execute', 'Fortune|buff', 'Twist of Fate|reflect', 'Wheel of Fortune|dispel')
m('gabriel', 'Light of Hope|beam', 'Absolute Defense|barrier', 'Blessing|protect', 'Annunciation|heal')
m('grimoire', 'Spell Library|bolt', 'Forbidden Page|blast', 'Rewrite|dispel', 'Magic Archive|copy')
m('hamiel', 'Grace|heal', 'Divine Favor|protect', 'Venus Light|beam', 'Love|charm')
m('haniel', 'Joy|buff', 'Glory of God|nova', 'Grace|heal', 'Holy Barrier|barrier')
m('hastur', 'Unspeakable|fear', 'Wind Walker|dash', 'Yellow Sign|suppress', 'Gale|breath')
m('immortal', 'Immortality|undying', 'Infinite Regeneration|regen', 'Deathless|barrier', 'Rise Again|heal')
m('indra', 'Vajra|chain', 'Thunder God|beam', 'King of Heaven|fear', 'Storm Call|weather')
m('israfil', 'Trumpet of Resurrection|heal', 'Trumpet of the End|nova', 'Angel of Music|music', 'Burning Wings|dash')
m('jibril', 'Divine Message|analyze', 'Holy Spear|lance', 'Archangel Wings|dash', 'Revelation|dispel')
m('leviathan', 'Absorb|absorb', 'Strength Sap|sap', 'Envious Field|envy_field', 'Resentful Barrier|resent', 'Disordered Desire|desire', 'Tidal Wave|breath')
m('lucifer', 'Copy|pride_copy', 'Declare Supremacy|declare', 'Supreme Order|order', 'Imperial Ascension|ascension', 'Morning Star|beam')
m('mammon', 'Spiritual Domination|domination', 'Greed Flare|flare', 'Death Wish|death_wish', 'Energy Seize|seize', 'Endless Avarice|avarice')
m('melchizedek', 'King of Righteousness|protect', 'Priestly Blessing|heal', 'Judgment|beam', 'Covenant|bind')
m('mephisto', 'Contract|charm', 'Curse|debuff', 'Hellfire|blast', 'Soul Price|drain')
m('metatron', 'Purifying Light|beam', 'Absolute Defense|barrier', 'Purification|dispel', 'Light Manipulation|nova')
m('metis', 'Wisdom|analyze', 'Cunning|copy', 'Counsel|protect', 'Foresight|reflect')
m('michael', 'Castle Guard|protect', 'Kingdom Domination|dominate', 'Absolute Defense|barrier', 'Angelic Legion|summon', 'King of Justice|beam')
m('mood_maker', 'Lift Spirits|protect', 'Cheer|heal', 'Infectious Joy|field', 'Hype|buff')
m('multiple_weapons', 'Create Weapons|weapon', 'Weapon Rain|meteor', 'Thousand Arms|flurry', 'Arsenal|barrage')
m('necronomicon', 'Raise Dead|summon', 'Forbidden Curse|debuff', 'Death Spell|execute', 'Black Pages|analyze')
m('nodens', 'Lord of the Great Abyss|pull', 'Hunt|behind', 'Elder Sign|barrier', 'Night-gaunts|summon')
m('nyarlathotep', 'Thousand Faces|mimic', 'Crawling Chaos|field', 'Black Pharaoh|dominate', 'Madness|fear')
m('orpheus', 'Song of Orpheus|sleep', 'Descent|teleport', 'Lyre|music', 'Lament|heal')
m('proserpina', 'Spring Return|heal', 'Queen of the Underworld|summon', 'Pomegranate|bind', 'Seasons|weather')
m('raguel', 'Friendship|protect', 'Vengeance of God|strike|1.4', 'Bond|heal', 'Absolute Defense|barrier')
m('rex', 'Royal Decree|dominate', 'King Strike|cleave|1.3', 'Royal Guard|summon', 'Majesty|fear')
m('samael', 'Venom of God|chain', 'Angel of Death|execute', 'Blind Angel|debuff', 'Fall|gravity')
m('sandalphon', 'Prayer|heal', 'Gather Prayers|protect', 'Giant Angel|awaken', 'Holy Song|music')
m('sariel', 'Judgment|beam|1.3', 'Absolute Severance|cleave|1.5', 'Absolute Defense|barrier', 'Moon Gaze|sense')
m('satanael', 'Breeder Reactor|breeder', 'Enrage|enrage', 'Seismic Fury|fury', 'Growing Rampage|rampage_cast', 'Crimson Rupture|rupture', 'Absolute Annihilation|nova|1.4')
m('sephirot', 'Tree of Life|heal', 'Ten Lights|barrage', 'Kether|awaken', 'Malkuth|barrier')
m('shub_niggurath', 'Creation|craft', 'Thousand Young|summon', 'Fertility|regen', 'Black Goat|devour')
m('small_world', 'Pocket Space|storage', 'Shrink|gravity', 'Small World|imprison', 'Spatial Step|blink')
m('surya', 'Sun Chariot|dash', 'Solar Beam|beam', 'Radiance|nova', 'Healing Sun|heal')
m('susanoo', 'Storm God|weather', 'Sword of Gathering Clouds|cleave', 'Tempest|breath', 'Eight Headed Slayer|flurry')
m('terra_mater', 'Mother Earth|barrier', 'Earthquake|nova', 'Life Giver|heal', 'Stone Prison|imprison')
m('true_hero', "Hero's Haki|fear", 'Holy Sword|cleave|1.4', "Hero's Charisma|protect", 'Never Give Up|undying')
m('tsukuyomi', 'Moon Reading|sense', 'Night Domain|field', 'Lunar Blade|strike', 'Moonlight Sleep|sleep')
m('uriel', 'Absolute Defense|barrier', 'Infinite Prison|imprison', 'Spatial Domination|teleport', 'Law Manipulation|suppress', 'Absolute Severance|cleave|1.4')
m('vayu', 'Wind God|breath', 'Life Breath|heal', 'Gale Step|dash', 'Tornado|blackhole')
m('veldora', 'Storm Breath|breath|1.3', 'Black Lightning|chain', 'Storm Dragon|awaken', 'Tempest|weather', 'Faust|analyze')
m('velgrynd', 'Scorch Breath|breath|1.3', 'Heat Haze|invis', 'Parallel Existence|clone', 'Flare Circle|nova', 'Burning Dragon|awaken')
m('world_map', 'Map|sense', 'Waypoint|teleport', 'Survey|analyze', 'Border|barrier')
m('yaoyorozu', 'Eight Million Gods|summon', 'Divine Blessing|protect', 'Myriad Strikes|barrage', 'Kami Shield|barrier')
m('yog_sothoth', 'Key and Gate|teleport', 'Space-Time Domination|timestop', 'All in One|clone', 'Spatial Annihilation|blackhole')
m('yog_sotohort', 'Gate|teleport', 'Time Domination|timestop', 'Void Step|blink', 'Collapse|blackhole')

# ------------------------------------------------------------ everything else: name -> action. order matters, first match wins
RULES = [
 # utilities and senses first, so "eye" / "sense" names never turn into attacks
 (r'apprais|analy|decipher|investig|calculat|insight|research', 'analyze'),
 (r'sense|perception|detect|_eye$|^eye|vision|sight|clairvoy|search|observ|presence_detect|hear|scan|radar|magic_perception', 'sense'),
 (r'storage|stomach|pocket', 'storage'), (r'craft|forge|synthes|refin|alchem|cook|brew|manufact|create_item', 'craft'),
 (r'predat|devour|gluttony|eat$|_eat_|consume', 'devour'), (r'isolat', 'isolate'), (r'mimic|disguise|imitat|shapeshift', 'mimic'),
 (r'copy|steal_skill|rob', 'copy'),
 # moving
 (r'teleport|warp|gate|portal|transfer|spatial_motion|spatial_movement|dimension_move', 'teleport'), (r'swap|exchange', 'swap'),
 (r'shadow_motion|shadow_step|blink|flicker|instant_move|instant_motion|flash_step|shukuchi', 'blink'),
 (r'dash|charge|rush|lunge|leap|sprint|boost', 'dash'), (r'flight|fly|float|levitat|wing', 'haste'),
 # control
 (r'time_stop|stop_time|chrono|freeze_time|time', 'timestop'), (r'gravit|weight|burden|press|oppress|crush', 'gravity'),
 (r'attract|pull|grasp|magnet', 'pull'), (r'repel|push|deflect_away|knockback', 'repel'),
 (r'prison|jail|cage|imprison|seal|confine', 'imprison'), (r'chain|bind|lock|shackle|thread|web|restrain|string|tie|snare|entangle|earth_lock|mud_hand', 'bind'),
 (r'sleep|hypno|lullab|dream|slumber', 'sleep'), (r'fear|terror|dread|haki|coerc|intimid|menace|despair', 'fear'),
 (r'charm|seduc|temptat|allure|enchant_mind|mind_control|confus|illusion', 'charm'), (r'domina|command|rule|subjugat|control|manipulat|order', 'dominate'),
 (r'dispel|nullif|cancel|negat|purif|cleans|exorc|anti_magic|counter_magic', 'dispel'), (r'suppress|silence|disrupt|jam|interfere|limit', 'suppress'),
 # taking
 (r'drain|sap|siphon|absorb|leech|vampir|steal|deprive|plunder|harvest', 'drain'),
 # you
 (r'regenerat|recover|restor|mend|rejuven|vitality', 'regen'), (r'heal|cure|antidote|revive|resurrect|medic|treat|first_aid|life', 'heal'),
 (r'rest$|meditat|calm', 'rest'), (r'immortal|undying|endur|surviv|unyield|last_stand|backup', 'undying'),
 (r'reflect|mirror|counter|echo|parry|riposte', 'reflect'),
 (r'barrier|wall|shield|protect|guard|defen|armou?r|fortress|scale|skin|shell|body_harden|iron_body|steel_body|bulwark', 'barrier'),
 (r'conceal|stealth|invis|hide|vanish|phantom|ghost_step|formhide|haze|camouflag', 'invis'),
 (r'accelerat|haste|speed|swift|agility|reflex|instinct', 'haste'), (r'rage|berserk|frenzy|fury|wrath|enrage', 'rage'),
 (r'awaken|transform|release|ascen|true_form|evolv|liberat|unleash|over_drive|overdrive|limit_break', 'awaken'),
 (r'strength|power|might|reinforc|enhanc|empower|boost|bless|buff|aura_sword|harden', 'buff'),
 (r'summon|call|conjur|create_golem|familiar|undead|create_|raise|army|legion|clone|doppel|copy_self', 'summon'),
 (r'music|song|melody|rhythm|harmon|requiem|march', 'music'), (r'weather|rain_call|storm_call|tempest_call', 'weather'),
 (r'field|domain|zone|area|territory|sanctuary|miasma|fog|mist', 'field'),
 # attacks, the shape from the name
 (r'breath|roar|howl|scream|voice|sonic|wave$|sound', 'breath'),
 (r'beam|ray|laser|cannon|ray$|flare|nuclear|solar|light_magic_light|light$', 'beam'),
 (r'thunder|lightning|electr|spark|volt|plasma', 'chain'),
 (r'meteor|comet|falling_star|star_fall|rain|shower|downpour|hail', 'meteor'),
 (r'storm|explos|burst|blast|bomb|nova|eruption|annihilat|collapse|destruct|cataclysm|apocalyp', 'blast'),
 (r'black_hole|void|abyss|vortex|maelstrom|whirlpool', 'blackhole'),
 (r'needle|arrow|bullet|shot|spear|lance|javelin|bolt|dart|pierce|thrust|snipe|missile|icicle', 'lance'),
 (r'barrage|volley|rapid|thousand|hundred|flurry|eight|five|petals|multi|rush', 'flurry'),
 (r'slash|cut|blade|sword|edge|sever|cleav|guillotine|katana|scythe|axe|claw|fang|talon', 'strike'),
 (r'punch|kick|fist|palm|strike|smash|break|crush|blow|hit|tackle|impact|stomp|earthshatter', 'strike'),
 (r'ball|orb|sphere|fire$|ice$|water$|wind$|earth$|stone|rock|shot|throw|launch', 'bolt'),
 (r'kill|death|execut|assassin|end$|doom|reap', 'execute'),
 (r'curse|hex|poison|venom|toxic|plague|acid|corros|decay|rot|weaken|slow|debuff|disease|infect', 'field'),
]

def act_for(sid, name):
    text = sid + ' ' + re.sub(r'[^a-z_]', '', name.lower().replace(' ', '_'))
    for pat, act in RULES:
        if re.search(pat, text): return act
    return None

# which look pattern an action draws with (LoreFx's kinds)
KIND = {
 'bolt': 'projectile', 'lance': 'projectile', 'barrage': 'projectile', 'meteor': 'projectile',
 'beam': 'beam', 'chain': 'beam', 'breath': 'cone', 'blast': 'area', 'nova': 'area', 'blackhole': 'area', 'field': 'area', 'gravity': 'area',
 'explode': 'area', 'strike': 'slash', 'flurry': 'slash', 'cleave': 'slash', 'execute': 'slash',
 'dash': 'move', 'blink': 'move', 'behind': 'move', 'teleport': 'move', 'swap': 'move',
 'pull': 'area', 'repel': 'area', 'bind': 'sense', 'imprison': 'guard', 'sleep': 'aura', 'timestop': 'area', 'fear': 'awaken',
 'charm': 'aura', 'dominate': 'awaken', 'dispel': 'aura', 'suppress': 'aura',
 'drain': 'aura', 'essence': 'aura', 'copy': 'sense', 'devour': 'aura', 'isolate': 'aura',
 'buff': 'aura', 'rage': 'awaken', 'haste': 'aura', 'invis': 'aura', 'barrier': 'guard', 'reflect': 'guard', 'protect': 'guard',
 'heal': 'heal', 'rest': 'heal', 'regen': 'heal', 'undying': 'awaken', 'awaken': 'awaken', 'mimic': 'aura',
 'analyze': 'sense', 'storage': 'sense', 'craft': 'sense', 'sense': 'sense', 'summon': 'summon', 'clone': 'summon',
 'weather': 'area', 'weapon': 'summon', 'music': 'aura',
}
# actions that stay on until you turn them off when they're a skill's only thing
TOGGLE = {'buff', 'haste', 'invis', 'barrier', 'reflect', 'regen', 'sense', 'music', 'protect', 'mimic', 'rage'}

# ------------------------------------------------------------ single skills the rules got wrong (read through by hand)
FIX = dict(
 bunker_break='blast', great_secret_art='awaken', mystery_art='awaken', the_tough='barrier', ace='buff', fortitude='barrier', invincible='barrier',
 jupiter='gravity', knowledge_browsing='analyze', re_birth='undying', reset='dispel', reverse_fate='undying', sky_walk='haste', snow_crystal='bolt',
 spatial_connection='teleport', spiritual_dive='invis', chaotic_fate='execute', endless_loop='timestop', green_mile='execute', my_code_nazgul='summon',
 tachyon_paradox='timestop', beginning_eden='heal', initialize_heaven='nova', battlewill='buff', divine_battlewill='awaken', hazy_style='invis',
 phantasmal_style='clone', kamehameha='beam', hadoken='bolt', tetsuzanko='dash', anaphylaxy_shock='blast', angry_splatter='blast',
 breakdown_nostalgia='dispel', catastrophia_eclipse='nova', chaos_eater='devour', crazed_vendetta_expiation='flurry', damned_blood='drain',
 delete_barricade='dispel', mini_barricade='barrier', devastator_virus='field', dimension_fault='blast', einherjar='summon',
 evil_dragon_beast_production='summon', fatal_lost='execute', fertile_paradox='heal', infinite_eater='devour', insanity_hash='fear', love_energy='heal',
 marionette_dance='dominate', martial_dance='flurry', mort_en_ciel='execute', nihility_supply='essence', photon_slaughter='beam', quantum_splash='blast',
 quasar_break='beam', rink_ring='imprison', secret_method_of_the_cycle='regen', celestial='beam', secret_method_of_the_infinite_corridor='imprison',
 severed_realm='cleave', snatch_bites='drain', spiritron_torpedo='bolt', tatsumaki_seikentsuki='flurry', tepes='blast', zodiarch='meteor', minus_break='dispel',
 ground_zero='nova', spiral_penetrator='lance', planetary_assimilation='devour', diamond_path='buff', modelwill='buff', violent_break='rage',
 ogre_sword_guillotine='buff', earthshatter_kick='nova', freezing_white_flame='breath', bloody_bite='drain', spatial_severance='cleave', dimension_cut='cleave',
 chant_annulment='chant', enhanced_body_double='clone', holy_demonic_inversion='dispel', immovable='barrier', majesty='fear', parallel_existence='clone',
 parallel_thought='haste', perfect_memory='analyze', ultra_intuition='reflect', ultra_smell='sense', ultra_vibration='sense', telepathy='sense',
 water_current_motion='haste', absorption='drain', alteration='mimic', beast_unification='awaken', beastialize='awaken', dark_night_cycle='invis',
 dissolve='execute', paralyze='bind', plant_whisper='sense', possess='dominate', attribute_neutralization='dispel', desire='charm',
 elemental_communication='summon', espionage='invis', fallen_catastrophe='blast', fallen_thanatos='execute', imaginary_space='imprison', inspire_forces='protect',
 portfolio='analyze', pursuit_of_truth='analyze', solicitation='charm', substitution='swap', reinforce_structure='barrier', contract='bind', preservation='storage',
 transportation='teleport', auto_mapping='sense', blind='debuff', silent='invis', divination='sense', secret_of_faith_and_grace='heal', deception='invis',
 infiltration='invis', magic_sensor='sense', thoughtography='analyze', day_night_inversion='weather', death_blessing='execute', dimension_rupture='blast',
 eraser='execute', judgment='beam', necrosis='field', remove='dispel', eternal_regression='heal', evolution='awaken', duplicate_mass_production='summon',
 power_absorption='drain', soul_consumption='drain', scorch_excitation='rage', complete_fixation='bind', inverted_fusion='dispel', solidification='barrier',
 judgment_of_heaven='beam', armageddon='meteor', regalia_dominion='dominate', ultimate_dominion='dominate', weakness_identification='analyze',
 magicule_breeder_reactor='rage', stampede='dash', hero_s_blessing='protect', hero_s_charisma='protect', headwind_type_magic='breath', seize='copy',
 force_takeover='dominate', provide='protect', receive='copy', concept_sharing='protect', destiny_alteration='undying', lethal_attack='execute',
 unique_laws_ignoring='dispel', optimal_action='haste', transmutation='craft', separation='craft', pseudo_degenerate='craft', age_determination='analyze',
 absolute_taste='analyze', possession_fusion='dominate', marionette='dominate', remote_marionette='dominate', mana_measurement='analyze', silent_motion='invis',
 unexpected_effects='debuff', hostility_identification='sense', scorch_type_magic='breath', enemy_identification='sense', gallery_fake='clone',
 future_precognition='reflect', indiscriminate_absorption='devour', cocytus='blast', air_buster='beam', air_shut='bind', airflow_shut='bind', sacrifice='rage',
 tetanus_darkness='debuff', holy_bell='nova', possession='dominate', forbidden='blast', nihility='execute', nihilistic_banish='execute', nihilistic_parade='barrage',
 argos='sense', blizzard='blast', death_heralding_wind='breath', elephant_stampede='barrage', sudden_slice='strike', primordial_flame='beam', ice_veil='barrier',
 necromancy='summon', megiddo_revised='beam', liquidize='bind', demon_marionette='dominate', reincarnation='undying', secret_art_of_implantation='copy',
 sacred_birthday='heal', secret_art_of_revival='heal', secret_art_of_spiritualization='awaken', dance_with_wolves='summon', alignment_jacket='barrier',
 saint_barrel='beam', virtual_world='field', eternal_world='field', fantasy_world='field', nihilistic_world='field', after_world='field',
 weapon_destruction='dispel', deceleration='timestop', exhaustion='debuff', evil_law_of_contract='bind', body_snatch='dominate', armor_destruction='strike',
 absolute_offense_defense_sword_area='field', flame_manipulation='blast', water_manipulation='breath', wind_manipulation='repel', metal_manipulation='barrage',
 quantum_manipulation='blast', spatial_manipulation='teleport', temporal_manipulation='timestop', thought_manipulation='charm', mana_manipulation='essence',
 light_and_heat_domination='beam', sound_and_wind_domination='breath', water_and_thunder_domination='chain', weather_domination='weather',
 leyline_manipulation='field', earth_element_manipulation='bind', airflow_manipulation='repel', organic_domination='bind', mineral_domination='bind',
 law_domination='suppress', life_domination='heal', deceased_domination='summon', magic_insect_domination='summon', military_command='protect',
 flame_domination='blast', control_heart='charm', marionette_heart='dominate', sage='analyze', thought_reading='analyze', army_move='teleport',
 transport_technique='teleport', membrane_creation='barrier', birthday='heal', turn_undead='nova', army_destroyer_quaking_blast='nova', greed_flare='nova',
 flare_circle='nova', solar_rain='meteor', bound_spell='bind', burning_embrace='field', roaring_lion_punch='strike', magisteel_ultra_microwave='beam',
 dress_change='mimic', true_love='heal', destiny_change='undying', food_chain='copy', mystic_thread_shredder='strike', mystic_thread_slash='strike',
 mystic_thread_slicer='strike', plum_blossoms_five_petals_thrust='flurry', icicle_sword='strike', reduce_speed='debuff', slowing_magic='debuff',
 flowing_water_slash='strike', light_speed_slash='dash', wing_blade='strike', instinct_evasion='reflect', anti_poison='dispel', lucky_field='protect',
 mental_crush='fear', flashbang='blast', escape='blink', evil_crushing_beacon_prayer='beam', evil_crushing_holy_spear='lance', time_leap='blink',
 dimension_leap='teleport', spacetime_leap='teleport', cicada_flash='blink', skill_creation='copy', skill_duplication='copy', skill_gift='protect',
 skill_gifting='protect', skill_synchronization='buff', anti_skill='suppress', absolute_superiority_over_magic_and_unique_skills='dispel',
 veldora_style_killing_arts='flurry', death_world='field', death_streak='chain', hobgoblin_haze_shadow_strike='behind', hobgoblin_haze_wolf_fang='strike',
 haze_sudden_slice='behind', haze_style_sundering_falling_leaves='flurry', haze_style_dance_of_autumn_leaves='flurry', haze_style_war_god_s_roar='breath',
 invisible_blade='strike', nihility_collapse_haze_myriad_variety_petals='flurry', disintegration='execute', trinity_disintegration='execute',
 life_extermination='execute', holy_ark='barrier', skill_steal='copy', spacetime_connection='teleport', spacetime_continuous_attack='flurry',
 eight_impervious_gates='barrier', warp_shot='lance', mirage='clone', mirage_orage='clone', mirage_re_orage='clone', icicle_rain='meteor',
 limited_hell_flare='beam', limit_break='awaken', limit_end='execute', isolated_battlefield='imprison', sound_isolation_barrier='barrier', rain_maker='weather',
 willow_seven_petals_calm='reflect', light_magic_light='sense', floor_light='sense', aura_sword='buff', vortex_crash='blackhole',
)
# mode names from the mods that are really settings or chat messages, not modes
SKIP_MODE = re.compile(r'\.|^This |^There |%|Power$|^Default$')
MODE_FIX = {'Repeat': 'lance', 'Single': 'lance', 'Condensed': 'blast', 'Spread': 'blast', 'Chain Mode': 'lance', 'Limited Hell Flare': 'beam',
            'Blocks Break': 'strike', 'Earth Pit': 'bind', 'Movement Behaviour': 'protect', 'Targeting Behaviour': 'dominate', 'Block': 'strike', 'Entity': 'bind',
            'Auto Target': 'beam', 'Single Target': 'beam', 'Communion': 'heal', 'Contribution': 'protect', 'Retribution': 'reflect', 'Rain': 'weather',
            'Clone Control': 'clone', 'Coating': 'buff', 'Holy': 'beam', 'Space': 'blink', 'Raise': 'summon', 'Mass Raise': 'summon', 'Self-lock': 'barrier',
            'Slinger': 'lance', 'Steel': 'strike', 'Sticky': 'bind', 'Arcane Thread Fetters': 'bind', 'Dress Change': 'mimic', 'Spatial Bag': 'storage'}
KIND['chant'] = 'aura'; KIND['debuff'] = 'aura'
KIND.update({'void': 'area', 'corrosion': 'aura', 'grasp': 'aura', 'receive': 'sense', 'provide': 'sense', 'mimicry': 'sense'})
TOGGLE.add('chant')
# beelzebuth's own (Gluttonous.java)
KIND.update({'darkness': 'area', 'soul_steal': 'aura', 'soul_devour': 'aura', 'food_chain': 'sense'})
# the sins (Sins.java)
KIND.update({a: 'aura' for a in ['absorb', 'ascension', 'avarice', 'blessing', 'breeder', 'death_wish', 'declare', 'deep_hypno', 'deprive', 'desire', 'domination', 'effort', 'embrace', 'enrage', 'envy_field', 'fallen_hypno', 'fallen_strike', 'flare', 'fury', 'implode', 'invigorate', 'lethargy', 'lullaby', 'lust_drain', 'manipulate', 'order', 'phantasmal', 'pride_copy', 'rampage_cast', 'rebirth', 'resent', 'rupture', 'sap', 'seize', 'siphon', 'sloth_rest', 'transmit']})

# a few mod skills whose modes need a better fit than the names alone give
m('thunder_rain', 'Thunder Rain|meteor', 'Coating|buff')
m('fire_lance', 'Single|lance', 'Repeat|barrage')
m('firestorm', 'Condensed|blast', 'Spread|nova')
m('mud_spears', 'Targeted Mode|lance', 'Spread Mode|barrage')
m('stone_shot', 'Single|lance', 'Spread Mode|barrage')
m('explosion', 'Explosion Strike|blast', 'Explosion Trap|field')
m('black_lightning', 'Black Lightning|chain', 'Death Storm|blast')

# ------------------------------------------------------------ what each hand-built mode does (shown in the library)
# keyed by the base skill; its ultimate uses the same text (it's the same power, stronger)
ULT_OF = {'satanael': 'wrath', 'leviathan': 'envy', 'asmodeus': 'lust', 'mammon': 'greed', 'lucifer': 'pride', 'belphegor': 'sloth'}
DESC = {
 'beelzebub': {
  'Soul Devour': 'switch on: every hit you land eats at the soul (soul drain II for 5s)',
  'Predation': 'hold: breathe out the devouring mist, 15 blocks (30 mastered, scroll to change). 150 a hit, 30% to take up to 3 skills, everything it kills is yours',
  'Gluttonous Darkness': 'hold: a black hole in front of you (sneak: above you) that grows as you hold. drags everything in, drains it, eats its soul and swallows projectiles',
  'Imaginary Space': 'your stomach: a storage space that never fills',
  'Mimicry': 'take the form of the last thing you devoured',
  'Corrosion': 'hold: everything within 12 rots, 50 every half second. kills give up their skills and all their EP',
  'Soul Steal': 'hold: anything within 60 that is nearly dead, far weaker than you, or frozen with fear has its soul eaten whole',
  'Food Chain': 'take a skill your subordinate learned. sneak: teach it one of yours',
  'Imaginary Space Isolation': 'seal harmful effects away from your target (or you), or dissolve the item in your hand into magicules',
 },
 'wrath': {
  'Breeder Reactor': 'hold: pump 2% of your max magicules into yourself every 5s, past your max. each pump can stack Rampage (3%, 6% while over max)',
  'Enrage': 'hold: everything within 7 flies into a Rampage and attacks whatever is near it, deeper the longer you hold. 100 magicules a second',
  'Seismic Fury': 'slam the ground: everything within 10 takes your attack damage x0.5 (x1 mastered), more for every combo hit you stacked',
  'Growing Rampage': 'hold 3s: you Rampage for a minute and it grows a level every 30s (up to 30, 40 mastered)',
  'Crimson Rupture': 'needs Rampage 10+. hold 5s and it bursts out of you: 20 damage a level, 5 blocks + 0.5 a level. spends your Rampage',
 },
 'envy': {
  'Absorb': 'take the EP gap from someone stronger than you within 5 blocks, a quarter of it stays yours for good',
  'Strength Sap': 'hold: everything within 15 that is weaker than you slows down, weakens and bleeds EP. it can drive you a little insane. 1000 magicules a second',
  'Envious Field': 'a field 10 blocks wide (15 mastered) for 30s: drains aura and magicules from everything inside, anti skill and weakness every 5s',
  'Resentful Barrier': '20s (30s mastered): hits come out of your magicules instead of your health',
  'Disordered Desire': 'hold 3s on someone within 10: a quarter of their EP becomes yours',
 },
 'lust': {
  'Drain': 'pull 200 EP out of someone within 5. with nobody there your hits drain for 10s instead',
  'Invigorate': 'heal everything you are missing (80 magicules a heart, 40 mastered) and shake off bad effects. sneak: do it for who you look at',
  'Rebirth': 'you and your allies within 15 are held back from death once for 30s: the killing hit leaves them at full health',
  'Embracing Drain': 'lock someone in reach for 5s. they cannot look away or move, and it drains them every second',
  'Death Blessing': 'hold 5s: a field where you look. half your EP or less dies, 3/4 loses half its health, up to yours loses a quarter. 7500 a second',
  'Spiritual Manipulation': 'hold on something within 30 until its will breaks (90s mobs, 60s players, faster up close). sneak: let it go',
  'Transmission': 'hold 2s: give a fifth of your health to who you look at. sneak: take it from them',
  'Siphoning': 'hold on someone within 10 (20 mastered): their life flows into you',
  'Implode': 'hold 3s on someone within 10: a tenth of your magicules lands on them as damage',
 },
 'greed': {
  'Spiritual Domination': 'hold on something within 30 until its will breaks (90s mobs, 60s players, faster up close, faster the more it trades). sneak: let it go',
  'Greed Flare': 'burn your own power into attack (5% of your health, 10% mastered), or give an ally half. each hit costs magicules. press again to put it out',
  'Death Wish': 'hold 10s on something in reach. it can barely move, and when the wish is done it dies',
  'Energy Seize': '30s: everything within 15 has its power drained into you while it stays near (level 1, 3 mastered)',
  'Endless Avarice': 'grab at someone within 10: rip mastery out of a skill you both have, or a chance to take one you do not',
 },
 'pride': {
  'Copy': 'skills that hit you can become yours (20%, always mastered). press to try again at the last one that hit you',
  'Declare Supremacy': 'name something within 30 with half your EP or less as beneath you: it hits you for a quarter and folds when you hit it',
  'Supreme Order': 'everything within 30 with 3/4 your EP or less (all of it mastered) has its attacks and movement shut down for 15s',
  'Imperial Ascension': 'hold 5s: for 2 minutes your health and magicules double and nothing can knock you back',
 },
 'sloth': {
  'Deep Hypno': 'hold on something within 10: drowsier the longer you hold, then it falls asleep. breaks the spells of players you look at',
  'Fallen Hypno': 'hold: the same, on everything within 5 of you (10 mastered)',
  'Deprive': 'hold: everything within 5 (15 mastered) loses 1000 EP a second and some of its soul to you',
  'Rest': 'hold: lie still and recover 10 health and 10 magicules a second. sneak+press: heal your allies within 15',
  'Phantasmal Style': 'switch on: your hits strike the soul too (+10, +1% of their health mastered) and you move like a shadow',
  'Fallen Strike': 'step behind something within 8 and hit its soul: a quarter of its health plus 50, through armor',
  'Lethargy': 'everything within 15 is weighed down more every 10s for a minute. ten levels in, it sleeps',
  'Effort': 'why do it yourself: a copy of you fights for you (4 at once, 9 mastered). costs a tenth of your magicules',
  'Final Lullaby': 'hold 3s: you and your allies within 15 get +20% damage, +10% speed and 5% less damage for 2 minutes, then fall asleep',
 },
}
SKILL_DESC = {
 'wrath': 'The sin of wrath. Anger becomes power: a breeder reactor of magicules, a rampage that keeps growing, and fury that shakes the ground. Hits landed in rhythm build a combo.',
 'satanael': 'Wrath King. The ultimate form of Wrath: a bigger reactor, a deeper rampage and a rupture that levels everything around you.',
 'envy': 'The sin of envy. Take what others have: their EP, their strength, their energy. Lucky, and hits sometimes just miss you; weakness and slowness cannot hold you.',
 'leviathan': 'Envy Lord. The ultimate form of Envy, wider and hungrier, and harder to hit.',
 'lust': 'The sin of lust. Power over life and desire: drain, heal, embrace, bless with death, and bend wills.',
 'asmodeus': 'Lust King. The ultimate form of Lust: life and death in your hands.',
 'greed': 'The sin of greed. Want everything: wills, power, skills, even lives.',
 'mammon': 'Greed King. The ultimate form of Greed: wills break twice as fast and a death wish needs half the time.',
 'pride': 'The sin of pride. Everything beneath you is beneath you: skills that hit you become yours, the weak are crushed by your presence.',
 'lucifer': 'Pride King. The ultimate form of Pride: you can copy even ultimate skills.',
 'sloth': 'The sin of sloth. Sleep, rest and letting others do the work: hypnosis, drowsiness, soul strikes and copies of yourself.',
 'beelzebub': 'Beelzebuth, Lord of Gluttony. Everything Gluttony has at ultimate strength, plus soul devouring, a black hole of darkness and the power to steal souls outright.',
 'belphegor': 'Sloth King. The ultimate form of Sloth: deeper sleep over a wider area, and twice the copies.',
}

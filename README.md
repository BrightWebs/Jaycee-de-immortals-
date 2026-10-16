# Jaycee: Di Immortales

A clean-room Minecraft **1.21.1 / NeoForge / Java 21** mod project.

## First milestone: the interface

The original milestone was the interface. The current Phase 1 work also connects executable abilities, persistent bestowal and Essence-driven advancement; runtime acceptance remains required.

- Consume **three Jayce's Essences** to unlock the GUI (progress saved per player).
- Operators can supply the three legacy unlock items with `/jc debug essence`; these items are not the transferable Essence ledger.
- Render the **actual player's animated 3D model and skin**, not a screenshot of Steve.
- Five clickable tabs: Status, Abilities, Skills, Magic, Quests. Click the race name on Status to open species/evolution details; use Back to Status to return.
- A **persistent four-slot ability loadout with ten selectable pages** on every tab.
- Use the user's reference art as a guide for separately rendered UI pieces, interactive hitboxes, labels, tooltips, and responsive layouts, **not as a single background screenshot**.
- Only registered executable abilities work; encyclopedia entries are not automatically implemented powers.

## Development baseline

Official NeoForge 1.21.1 ModDevGradle starter:
https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Minecraft 1.21.1 / Java 21 / NeoForge 21.1.x.

## Gameplay HUD and loadouts

Sapphire follows the original `the realUI.png` composition: five tabs above the wide Status panel, a separate narrow Equip Page panel, portrait/race ribbon, animated cyan orbital sigil, Stats and stacked Titles/Trait/Elemental Affinity panels. Frames, text, icons and controls are rendered separately, never as a screenshot background. A shared design-to-screen transform fits the menu to the viewport and inversely transforms pointer input; tooltips and the 3D model use screen coordinates.

This restoration is stacked on **open, unmerged PR #3**, starting from `ac2a871e1fe6e77a17a4168b8aa1353cee5d9545`; it does not replace that PR's HUD/gameplay work or merge main's code. The reference was retrieved from main and visually inspected, with blob SHA `b606cc8e12a19936a4336775a73bfe0fc07b206b`. In-game comparison is still required before calling the restoration visually verified.

- Four always-visible vertical slots default to the middle-right. Bindings default to **Z/X/C/V** and follow Minecraft's Controls settings.
- **Alt + mouse wheel** changes the shared loadout page (ten pages); **Alt + ability key** cycles that slot's mode.
- In Abilities/Magic, select an art, then click an equipment slot to equip it. The selection remains available when returning to Status. Right-click a slot or use its **Remove** button to clear it; Alt-click to cycle its mode. Page arrows and all ten selectors operate on the same server-saved loadout as the HUD.
- The top-left HUD portrait and HP/SP/MP bars use live player data. The Status portrait's small **Face / 3D** toggle controls the same saved preference. Face View checks the current Hat flag; 3D uses the actual player renderer and its live skin-layer flags.
- Status level/grade/progress now derive from cumulative qualifying Essence, never Minecraft XP. HP, MP, SP, Attack, Defense (armor), Speed (movement attribute) and Luck are live values. Titles and affinities show explicitly saved records or honest empty states; race/faction and quests remain unavailable/previews. Skill mastery remains separate from divine level.
- HP reads Minecraft health/max-health, including attribute changes. The HUD interpolates bars, trails HP damage, and pulses on SP damage. Vanilla hearts return when the HUD is hidden/disabled.
- `config/jaycee-client.toml`, section `hud`: `enabled`, `showVanillaHearts`, resource/ability anchors and X/Y offsets, `scale`, and `iconDirectory`. Offsets are in scaled HUD units; positions clamp to the viewport. Other mods' overlay reservations are not discoverable universally; use offsets/anchors when needed.

Costs, cooldowns, modes, page selection and equipment are validated on the server. Unlock progress, portrait preference, SP and standalone mana survive death/respawn and reconnect; login, respawn and dimension changes resend the snapshot. The legacy essence counter migrates into the saved gameplay tag.

**Legacy ability scope:** the original six arts were UI previews, not implemented powers. Soul Restore costs 20 mana with a 10-second shared cooldown: Restore heals 30 SP; Fortify adds 30 spiritual barrier points. Neither charges if already full. The other five legacy arts remain previews and never charge resources. In particular, the old Aether Flight preview is not renamed or replaced by the separately granted Divine Flight intrinsic introduced below; reality manipulation and teleportation remain unimplemented.

### Ability engine — first stage

`AbilityEngine` separates executable definitions and ordered, mode-specific mana/cooldown rules from GUI catalog entries. Only explicitly registered handlers can cast; adding an `implemented` catalog flag alone can no longer accidentally run Soul Restore. Unknown abilities, invalid modes, restricted players, cooldowns and ineffective casts are rejected before payment. A successful cast consumes one resolved mana resource and records one ability-wide cooldown, shared across its modes, slots and pages.

`AbilityExecutors` binds the engine to the logical server and the existing player snapshot. In the first stage, Soul Restore was the only registered power, retaining its original ID, Restore/Fortify indices, 20 mana, 200 ticks and 30 SP/barrier. Its catalog costs/mode labels derive from the executable definition. That extraction kept the saved schema/network protocol unchanged, including re-reading NBT after mana consumption before committing effects/cooldowns. The Skills stage extends the same execution path and adds skill NBT without replacing existing fields. Executable handlers must validate eligibility before charging and apply their already-validated effect synchronously; unresolved designs are not executable definitions.

The [mod-des instruction file](https://github.com/BrightWebs/mod-des/blob/main/AGENTS.md) and all its referenced documents/data were read before this stage, including `docs/ASSET_PIPELINE.md`. The source package does **not** currently supply executable specifications for the complete requested system:

- Its 292 JSON catalog entries still have null mana cost, cooldown, range, area and cast time, and empty mode lists. `data/ability_modes.json` explicitly requires reconciliation; null is not zero/free.
- The newly uploaded **131-page Definitive Master Design Bible** restores the older patron/fusion appendix (22 patron paths, 220 named arts, 37 fusions). Its first seven pages supersede conflicting appendices. The earlier Markdown warning that this appendix is missing is now outdated. The encyclopedia and patron roster still need explicit reconciliation; no saved IDs or working modes were replaced.
- Mana, transferable Essence and permanent EP are separate. This stage adds the world ledger and consent transactions below, not natural earning/promotion recipes or a Tensura EP integration. The three-essence unlock item grants legacy Soul Restore once; later explicit revocation is respected across reconnects.
- `references/` is advertised but absent from the repository listing. Final PNG textures are explicitly not supplied. Some icon filenames/IDs have extraction punctuation or missing mappings, and briefs reuse motifs; they need reconciliation and original verified artwork, not blind imports or generic glyphs declared finished.

The first-stage engine build passed CI at `0dd8acc`, and the `/jayceetest` build passed at `6063b14`. Those artifacts do not contain the newer Skills or Divine Ascension work. An approval-required run with zero jobs is not a successful build.

## Skills, Magic and Quests scope

The Skills stage added inner Skills/Magic content and server support. Divine Ascension now separates ordinary navigation from the unique authority's approved five-tab Sapphire screen, retaining the background/frame, four-slot Equip Page, ten saved pages, existing bindings and Soul Restore's original resource effects.

- Skills exposes the registered, code-backed subset: Physical Training, Soul Restore mastery, Divine Speed, Divine Flight, Divine Perception, Regeneration and Divine Resistances. Empty requested categories are not padded with fictional skills or unlocks.
- The divine intrinsics require explicit server authorization or the operator's `/jc debug jayceetest` flag. Ordinary skill XP does not grant godhood or Jaycee's Eye. No Eye implementation or direct-patron acquisition system is claimed.
- Numeric tuning introduced for these mechanics is implementation balance, not recovered canon. Source costs, progression curves, patron grants, study rules and fusion/evolution recipes that remain undefined are not presented as official design values.
- Magic uses the existing ability catalog and execution path. Inspecting entries is not a cast; equipment, mana spending and cooldowns remain server-authoritative. Unsupported spells stay unavailable rather than becoming a second casting system.
- Mana is casting/upkeep fuel. The three essence items retain their legacy unlock meaning; they are not a blessing currency. The separate ledger below adds explicit Essence transactions and an EP award hook, not automatic earning, natural rank promotion or a Tensura EP bridge. External progression is not changed.
- Quests contains only the centered **QUEST SYSTEM / WORK IN PROGRESS** notice and the requested development message. There are no quest objectives, rewards or generators.

**Trying the intrinsic controls:** an operator can use `/jc edit <player> ability grant jaycee:divine_speed`, then the recipient presses **M** and opens Skills. Select the intrinsic, enable it, and adjust its output; flight also offers Hover/Cruise styles. Other implemented intrinsic IDs include `jaycee:divine_flight`, `jaycee:divine_perception`, `jaycee:regeneration` and `jaycee:divine_resistances`. `ability revoke <id>` removes an explicit grant; disable `/jc debug jayceetest` as well when checking genuine permission failures. This is administrative provisioning, not a completed natural patron-acquisition mechanic.

Server balance lives in the world's `serverconfig/jaycee-server.toml`. Defaults include 10 mastery levels with cumulative threshold `100 × level²` and a shared 20-XP/minute award budget. Effective movement/hostile combat, successful Soul Restore, distinct perception targets, restored hostile-inflicted injury and paid resistance use feed bounded, persisted award gates; merely opening the UI, toggling or changing modes does not award XP. These checks limit farming but are not a claim that every possible multiplayer farm has been tested.

Divine Speed adds a transient movement modifier without changing time. Flight manages its own survival flight lease and configurable speed. Perception reports line-of-sight nonplayer targets to its caster, not player statistics or global outlines. Regeneration restores missing **vanilla HP**, with proportional mana for actual healing; it does not replace Soul Restore's SP/barrier modes. Divine Resistances adds paid reduction without overwriting the saved base spiritual resistance. Upkeep uses one aggregate payment from the resolved mana backend; no external mana regeneration is added. Existing flight from creative/spectator or another provider is not intentionally taken over, but mods writing identical vanilla flight flags cannot be distinguished without a dedicated integration.

Nonzero output/style changes take effect at the next paid upkeep boundary, at most one second later. The current paid lease retains its original strength/cost so adjusting flight does not cause a forced fall or let a low-cost lease provide higher output for free. Turning a power off, setting output to zero, losing permission or failing payment stops its effect.

Original artwork for the new intrinsics and Physical Training, plus the shared Soul Restore healing icon, is supplied as text-free transparent PNGs with editable SVG sources under `src/main/art/ability`. Divine Speed depicts a running figure with motion trails, not time manipulation. Runtime textures use the existing canonical `assets/jaycee/textures/ability/<id>.png` path. The supplied 1024/64/32 exports support master/HUD/readability inspection; runtime menu textures are 256×256. Reproduce an export using `rsvg-convert -w 256 -h 256 <source.svg> -o <output.png>`. These assets do not imply the other five legacy preview arts or the 292 encyclopedia concepts have been implemented.

### Divine Ascension — interrupted stage continued

**M** opens ordinary players' **Status / Abilities / Skills / Magic / Divine Bonds / Quests** navigation. The five-tab navigation and sovereign interactions remain reserved for the single server-assigned Jaycee UUID. **Status itself now uses the same renderer and exact portrait/identity/EXP/Stats/Titles/Trait/Affinity geometry for everyone**, not a separate text profile. Ordinary Abilities/Skills/Magic browse only authorized entries; unresolved legacy-to-canon family mappings are labeled unresolved, not invented or imported as working patron trees. Both menus use the existing Equip Page and authoritative loadout operations. Quests remains only its development notice.

### Shared Status and patron presentation

The real player skin, persisted Face/3D choice, account username, synchronized divine rank/patron, divine level/grade/Essence progress and live HP/MP/SP/Attack/Defense/Speed/Luck use the original Status positions. Divine rank is not labeled a racial trait. **Trait** replaces the old heading for everyone. Traits and acquired affinities have separate clipped mouse-wheel/keyboard scroll regions; scrolling either card, including with Alt held, never changes the loadout page. The Equip Page retains all controls but has no resource footer.

Single retained NPC lineages select their own named palette and stationary central crest. Multiple retained lineages use the neutral navy/silver-blue/violet palette, with individual emblems on a 32-second client-only orbit; unblessed players receive a neutral emblem. The metadata is the ledger's current `Npc` set, never historical grant provenance: removal immediately removes the theme. Player-to-player patron bonds remain visible as affiliation, but do not invent a deity lineage. Operator `npc_add` metadata is for testing, not evidence of a completed offering or legitimate patron-acquisition mechanic.

All 22 named deity visual identities are mapped independently from gameplay. Colors/art never unlock domains. Trait entries derive from actual owned intrinsic grants (not merely diagnostic access); affinities require explicit server-stored entries and are not fabricated from Zeus/Poseidon/etc. Titles default to **No Title Equipped** because no natural title-management system exists. Empty trait/affinity lists are honest unavailable/unlocked states rather than six invented level-zero affinities. Server extension hooks in `StatusData` persist presentation records through the existing snapshot; no client packet grants title, trait or affinity progress.

For accessibility, enable `hud.reducedStatusMotion` in `config/jaycee-client.toml`; Minecraft's Hide Lightning Flashes setting also freezes the decorative movement. These settings affect neither game ticks nor powers. Original patron SVG sources and transparent PNG textures live under `src/main/art/patron` and `assets/jaycee/textures/patron`. Missing custom resources use a small development marker, not a falsely claimed completed symbol. The approved Zeus mockup was not present in the checkout or supplied as an attachment; exact comparison with it and in-game visual approval remain outstanding.

**N** opens the separate Divine Bonds/bestowal overlay for everyone, with server line-of-sight targeting or a nearby-recipient chooser. It includes Profile, Bestowal, Offers, Bonds, Dominion and History views. Opening or inspecting never spends resources. The Bestowal selector separates **Players** from **Mobs / Entities**. For players, choose Essence amount, an owned implemented power (or none), an optional Divine Name and an explicit subordinate invitation; request a server quote, inspect fees/total/balance, then send. The player recipient must explicitly accept; decline, disconnect, timeout and server restart cancel without payment. Acceptance revalidates ownership/mastery, eligibility, funds, caps, range/visibility, pending offer and absence of patron cycles, then commits once. A donation without the invitation does not silently create a bond.

World `SavedData` stores the unique authority, independent `long` EP/Essence balances, names, grants with provenance, patron graph and bounded audit history. Account UUID/original identity never changes. Private Jaycee fields and unauthorized grant records are filtered from personalized views before transmission, including offer display names. Release requests, denials, petitions, explicit Jaycee adjudication, privilege revocation and cap lifts use server-validated operations. Challenge eligibility is informational: combat victory and leadership reversal are **not** implemented.

### Mob bestowal

The mob view uses nearby recipients, a live preview/identity and power-selection panel, and a separate transaction-review panel. Selecting or inspecting a creature creates no divine relationship or resource balance. Naming uses Minecraft's custom-name rendering; the original entity type and UUID remain unchanged. Ordinary taming is separate from divine subordination, and another player's pet cannot be silently appropriated.

Mob transactions require a server quote followed by explicit confirmation, not player-offer acceptance. The server rechecks the actual loaded entity, dimension, visibility/range, ownership, compatibility, donor privileges/mastery and available funds. A nonce is single-use and expires; reconnects/restarts discard unconfirmed quotes. Existing player offers still require the player's consent. Jaycee uses the same server-assigned authority and transferable reserve/refill as before, not `/jayceetest`.

Mob EP, stored Essence and cumulative Essence received are separate fields. Transfers do not fabricate EP, mastery, elemental affinities, rank promotion or evolution. No creature-evolution recipes or lightning-casting AI are implemented by this change. Unsupported powers and entities must explain their missing compatibility rather than accepting a name-only power grant.

The initial functional mob adapters are deliberately bounded, passive implementations: **Divine Speed** adds 20% of base movement speed, capped at +0.10 attribute units; **Regeneration** heals at most 1 missing vanilla health point per 20 server ticks on compatible non-undead creatures. They retain existing AI and do not stack duplicate speed modifiers or consume/create EP or Essence. These mob-specific effects differ from the player's mana-paid configurable intrinsics and are implementation tuning, not recovered creature-progression lore. Other powers, boss/scripted AI and modded species need reviewed adapters before grants can function.

Mob records live beside player balances in the world ledger; entity persistent data is a mirror, not a second balance source. Dominion filters distinguish players and mobs, and show active/unloaded/deceased/removed mob records without force-loading chunks. Only an explicitly established divine subordinate gets deliberate despawn protection; mere inspection does not change persistence, taming or AI. An unbound gift does not guarantee that a naturally despawning creature survives indefinitely; bestowed custom names additionally follow vanilla name/persistence behavior.

Terminal records are retained for identity/death history; death does not refund gifts or move powers to a replacement entity. Vanilla conversions that replace an entity with a new UUID do not silently inherit a divine identity. Vanilla ownerless entities (including golems with no tame-owner UUID) are not assigned a guessed nearby-player owner. Scripted conversions, additional ownership systems and creature-specific evolution require explicit adapters.

**Compatibility limit:** wandering traders and trader llamas can be inspected, but divine binding is rejected until their special lifetime timers have a verified persistence adapter. Even Jaycee cannot bypass an unsupported adapter. Compatible unbound gifts remain subject to ordinary donor permissions and the creature's vanilla lifetime; no trades, ownership, leashes or AI are rewritten.

**Testing with cheats/operator level 2:**
- `/jc debug jayceeplayertest` previews the ordinary player's six-tab menu: run it, then press **M**, even while assigned as Jaycee. Append `off` to restore normal role-based navigation. This is a persisted **layout preview using your own live profile**, not impersonation or a permission sandbox. Use a second non-authority account for genuine ordinary-player permission tests.
- `/jc debug jayceeauthority claim` explicitly assigns **you as unique Jaycee**, and fails if authority is already assigned. The same restricted branch supports `assign <player>`, `reassign <player>` and `unassign`; reassignment cancels pending offers. Merely becoming an operator assigns nothing.
- `/jc debug jayceetest` remains reversible **test access only**, not authority or permission to bestow. Append `off` when checking genuine grant/revocation restrictions.
- `/jc edit <player> essence set <amount>` and `ep set <amount>` correct separate balances. `/jc debug divineadmin rank <player> MINOR_GOD` provisions an ordinary test donor; owned powers and mastery are still required for power bestowal.
- `/jc edit <player> ability grant|revoke <ability-id>` manages implemented grants. `patron grant|revoke <patron>` changes supported lineage metadata only, not offering powers.
- `/jc essence accept|decline|cancel <offer-uuid>` is the non-GUI consent path.

`serverconfig/jaycee-divinity-server.toml` contains **provisional**, not definitive canonical, balance policy: independent ordinary EP and Essence caps of 3,000,000; Jaycee Essence cap 2,000,000,000; online refill 1,000,000 Essence/second; ordinary establishment fee `max(100, ceil(recipient current Essence × 125%))`, plus 100 per granted power, burned in addition to the transferred amount. Jaycee bypasses fees but still pays the transfer. The exact field for the approximate 3-million design threshold, reserve basis/minimum/power fees and refill rate remain unresolved. These values are configurable implementation choices, not recovered canon; restart the world after changes. Revoking a cap lift does not delete historical balances; new credits stop at the current cap. Casting never spends EP or Essence.

**Not completed:** NPC cleric/totem offerings, full patron trees, betrayal/reconciliation, Kronos/Primordial unlocks, natural EP/Essence earning, rank recipes, all catalog arts/evolutions/fusions, Jaycee's Eye, actual Tensura integration and in-game multiplayer/visual acceptance. The unique UUID is operator authorization, not a claimed natural 3-billion-EP ascension. The definitive 3-billion EP reference and legacy 2-billion nominal display reference must not be confused with the separate Essence reservoir.

## Resources and optional magic integrations

HP, Spiritual Health (SP), Mana and Tensura EP are distinct. No EP data or evolution tiers are replaced; the planned **3,000,000,000 EP** Jaycee requirement remains a progression design constraint, not an enforced evolution implementation.

Standalone mana starts at 100 maximum and regenerates 2/second. SP starts at 100 maximum and recovers 0.5/second. Server integrations use `Gameplay.damageSoul`, `healSoul`, `addSoulBarrier`, and `setSoulResistance` (a 0–1 resisted fraction). Resistance applies before the spiritual barrier, then remaining damage reduces SP; these hooks never mirror or modify physical HP. At zero SP, casting is blocked and temporary weakness/slowness apply, never forced death.

`Gameplay.setMaxSoul(player, maximum)` and `configureStandaloneMana(player, maximum, regenPerSecond)` persist and sync modifier changes, clamping current values when maxima decrease without filling increased pools. Maxima are bounded to 1–1,000,000 and standalone regeneration to 0–10,000/second; nonfinite arguments are rejected. These hooks do not mutate an external backend.

`/jc get mana` reports the live selected resource. Restricted `/jc debug jayceemana` reports selected/effective/available backends; append `auto|standalone|<registered-id>` to save a validated choice. Default `auto` prefers a registered, available `irons_spellbooks` adapter, then `ars_nouveau`, then standalone. An unavailable saved backend falls back without deleting the preference. Operators can use `/jc debug jayceesp damage|heal <amount>` after unlocking to exercise the spiritual hooks.

**Testing identity:** `/jc debug jayceeauthority claim` explicitly assigns the unique Jaycee role; `/jc debug jayceetest` is only a persistent, reversible test unlock for existing intrinsic powers. Neither implements natural evolution, grants EP, resets cooldowns or replaces loadouts. Both require operator level 2.

`gameplay/ManaBackend.java` defines `getCurrent`, `getMax`, `getRegen` (per second) and atomic `consume`. `ManaBackends.register` is the guarded extension point: an optional adapter must reject insufficient funds without mutation, synchronize its owner's resource, and leave regeneration to that owner. Only one resolved backend is displayed and charged; Jaycee must never regenerate an external resource itself.

### Verified upstream APIs and remaining blockers

No external magic mod is a build/runtime requirement. **Direct Iron's/Ars adapters are not enabled in this checkout.** Installing an external mod alone does not share its mana with Jaycee; standalone remains functional until a matching adapter is registered and validated. Disable redundant external HUDs through their own settings when deploying an integrated pack.

| Mod / inspected source | Verified integration points | Outstanding validation |
| --- | --- | --- |
| [Iron's 1.21.1-3.16.3](https://github.com/iron431/irons-spells-n-spellbooks/tree/e4056af90302d37eb1739f5ff05020b020e6e252) (build target NeoForge 21.1.200) | `MagicData.getPlayerMagicData(player).getMana()`, `setMana(float)`, `AttributeRegistry.MAX_MANA` / `MANA_REGEN`, `SyncManaPacket` | No pinned dependency here; compile and test on the chosen pack/loader. `MANA_REGEN` is a multiplier, not MP/second. `setMana` fires a modifiable/cancellable event and does not itself synchronize; an adapter must handle consumption semantics correctly. Prefer Iron's when a validated adapter is available. |
| [Ars Nouveau 5.13.3](https://github.com/baileyholl/Ars-Nouveau/tree/fd8c9520722bb96a3e75febaf770d7078db5ac43) (MC 1.21.1, build target NeoForge 21.1.228) | Nullable `CapabilityRegistry.getMana(player)`, `ManaCap.getCurrentMana/getMaxMana/removeMana/syncToClient`, `ManaUtil.getMaxMana/getManaRegen` | Optional alternative, not a second simultaneous charge. `removeMana` alone does not reject insufficient funds or sync. No pinned dependency or combined-pack runtime test here. |
| [Tensura: Reincarnated 2.0.1.3](https://modrinth.com/mod/tensura-reincarnated/version/LttkLzXM) | NeoForge 1.21.1 distribution identified | Exact public progression API/source and minimum loader not verified. No speculative calls or duplicate EP store added. |

The upstream build targets above are **not** proof of incompatibility with this project's NeoForge **21.1.172**. They do require compilation and combined-pack testing before advertising support.

## Ability artwork mapping

Shared icon rendering is used by the ability list, equipment slots and gameplay HUD. A resource pack can supply:

| Ability ID | Resource-pack path |
| --- | --- |
| `jaycee:reality_severance` | `assets/jaycee/textures/ability/reality_severance.png` |
| `jaycee:celestial_guard` | `assets/jaycee/textures/ability/celestial_guard.png` |
| `jaycee:divine_pulse` | `assets/jaycee/textures/ability/divine_pulse.png` |
| `jaycee:temporal_step` | `assets/jaycee/textures/ability/temporal_step.png` |
| `jaycee:soul_restore` | `assets/jaycee/textures/ability/soul_restore.png` |
| `jaycee:aether_flight` | `assets/jaycee/textures/ability/aether_flight.png` |

Use square PNGs; resources reload with F3+T. Missing textures log once per resource reload and use the existing procedural glyphs, not unrelated spell art. `hud.iconDirectory` permits an alternate pack directory. Soul Restore now has an original healing illustration; the other five legacy previews retain their prior fallback/mapping. New intrinsic IDs use their own matching textures, shared by Skills, Magic, equipment and HUD. `BrightWebs/ModResources` previously returned 404; none of its artwork is claimed integrated.

## Phase 1: Essence progression, naming and commands

This continuation consulted `BrightWebs/mod-des` at `d40f9c84c66e665f7f26bf63bbbf96e7a398a3df`, including the **131-page Definitive Master Design Bible**, the illustrated **Master Ability Bible UI And Icons**, and its Markdown/JSON handoff. The latest owner's Essence-level formula, grade table, shared Status geometry and optional naming decisions supersede conflicting older wording. The PDFs contain concrete power/mode rules and proposed balance examples; empty JSON fields do not mean the PDF has no specifications. Existing original ability icons retain their canonical list/detail/equipment/HUD mappings. This change does not import or implement the entire encyclopedia.

### Distinct resources and progression

- **Transferable Essence** is the current spendable balance. **Total Earned Essence** is a separate, permanent progression record. **EP** remains permanent divine power; **Mana** remains casting/upkeep fuel; **Divine Rank** remains an independently authorized hierarchy.
- Divine level starts at **1**, with **0** earned Essence. Advancing from level L costs `100 × (L + 1)²` by default: the first step is 400, followed by 900. Cumulative thresholds use overflow-safe arithmetic, not repeated per-level loops or vanilla XP. The coefficient and nine grade minimum levels are configurable in `jaycee-divinity-server.toml`.
- Default minimum levels for **F/E/D/C/B/A/S/SS/SSS** are **1/11/21/36/51/66/81/96/111**. SSS additionally requires an explicit cap lift or unique authority; otherwise high-level players remain SS. These thresholds are the latest owner's proposed implementation balance, not recovered PDF lore. Jaycee's **Supreme Existence** authority remains above grade and separate from operator permission.
- Legitimate server credits and qualifying accepted blessings advance earned Essence once. Gifts do not reduce the giver's earned total. Resource provenance prevents passing the same gift around to generate endless progression: newly earned or regenerated transferable units can qualify for one recipient award; received units can be spent/gifted but do not award progression again. Burned fees never award progression. Administrative balance corrections do not mint progression-eligible units.
- Schema 4 stores earned totals and eligibility alongside player/mob records in the authoritative world ledger. Legacy saves begin the new progression record at zero; neither old spendable balances, mob received totals nor Minecraft XP are guessed to be legitimate earned progression. Existing resources, names, grants, authority and bonds remain intact. Explicit administrative level corrections are available.
- A qualifying hostile kill awards **10 Essence** by default, at most **100 per 1,200 overworld ticks** per player. Actual player/projectile kills, once-per-victim marking, persisted player budgets, and artificial-spawn exclusions guard awards. Spawners, spawn eggs, commands and other non-natural spawn reasons are excluded when observed by the spawn hook. Existing pre-update entities have no recoverable spawn provenance. These configurable limits are not a claim that every modded farm is impossible.
- `jaycee-progression-server.toml` configures combat rewards and appraisal. **Sneak-right-click an enchanting table** by default to appraise your own actual level, grade, rank, earned/transferable Essence and EP; this consumes no vanilla XP. The existing block ID and cooldown are configurable. Ordinary appraisal never discloses another player's hidden grade; only the server-assigned Jaycee role can use privileged grade inspection.

### Bestowal and permanent commands

**M** retains the divine menu, with contextual access to bestowal; **N** opens bestowal directly. Both use the same server targeting, quote, consent and persistence paths. Selecting a recipient prefills the editable visible name (or localized entity type), but preserving that suggestion sends **no rename**. An optional explicit rename remains separate from immutable account UUID/username. Editable Essence and +/- controls, available balance, reviewed cost and Bless/Cancel actions use the existing ornamental geometry. Player Bless creates an offer requiring acceptance; mob Bless confirms a validated single-use quote. A gift is not a subordinate bond and cannot be reclaimed by its original giver.

`/jc` and `/jaycee` share one Brigadier tree:

- `help`, `get`, `ability`, `patron`, `essence`, `grade`, `subordinate`, `edit`, `config`; autocomplete uses actual registered IDs and permitted abilities.
- Normal players inspect only their own data: `/jc get status`, `essence`, `ep`, `mana`, `level`, `grade`, `patron`, `ability list`, `ability mastery <ability>` and `trait`; `/jc subordinate list` lists actual followers.
- `/jc ability list|info|mode|toggle|cooldown` uses existing ownership, unlocked modes and runtime rules, not an alternate casting or payment engine.
- `/jc essence offers|accept|decline|cancel` controls real pending offers. Administrative edits require operator permission level 2: `/jc edit <player> essence add|set <amount>`, `ep set`, `level set`, `grade recalculate`, `patron grant|revoke`, `ability grant|revoke|mastery`, and `trait grant|revoke`. Traits are limited to existing code-backed intrinsic IDs, not arbitrary invented abilities.
- Diagnostics are restricted under `/jc debug`; they do not assign canonical Jaycee automatically. Explicit authority assignment is a separate restricted operation. `/jc config` reports configuration rather than pretending to write/reload settings.

### Unresolved species progression

Mob gifts persist real earned/received Essence separately from balance and EP. **No universal player-level tree or invented Essence-to-EP conversion is applied to mobs.** Species grades/evolution remain explicitly unavailable pending an approved numerical species policy; this is a known Phase 1 gap, not a completed growth mechanic. Existing functional compatible Speed/Regeneration adapters remain available. Boss/scripted/modded powers, new evolution recipes and lightning-casting AI are not supplied by this change.

### Phase 1 validation

Executed isolated Java 21 suites: **128** progression assertions, **316** existing player-ledger assertions, **95** mob-ledger assertions, **21** reward-budget checks, **4,410** layout checks, **52** browser checks and **343** bestowal UI checks (**5,365 total**). Source parsing is not type checking. The Java 21 / Gradle 8.10.2 build was attempted but stopped in `createMinecraftArtifacts`: `maven.neoforged.net` could not resolve `net.neoforged:neoform-runtime:1.0.40`. Full compilation, Minecraft-bound command/NBT suites, actual block interactions, multiplayer effects and rendered acceptance have **not** been verified locally. The original Status reference was visually inspected; that is not an in-game screenshot comparison.

## Build and validation status

Use **Java 21**, **Gradle 8.10.2**, and `gradle build --no-daemon` (the existing CI command). `check` includes dependency-free engine, skill-rules, browser/layout and `divineTest` Java executables. `gradle engineTest skillTest divineTest --no-daemon` targets execution/resource/ledger rules. No new testing libraries are required.

Isolated Java 21 HUD-math checks passed for anchor bounds, clamping, frame-rate-independent smoothing, delayed damage and pulses. These do not validate Minecraft rendering or multiplayer behavior.

The pre-Phase-1 baseline `d8c0ab7` passed [CI run 38873538883](https://github.com/BrightWebs/Jaycee-de-immortals-/actions/runs/38873538883), including the prior player/mob bestowal work. That artifact does **not** contain this Essence-level/command continuation. Full current build results must be reported separately; isolated arithmetic/model/UI checks do not execute real effects, external mana adapters or multiplayer.

The seven source SVGs and 28 PNG exports passed format/size checks; a separate image review confirmed distinct, readable silhouettes on navy at 32 pixels. Physical Training's gauntlet is less obviously a clenched fist than the other subjects. This is not in-game art approval.

The continued Divine Ascension slice passed **858 direct Java 21 assertions/checks**: engine 48, skill rules 82, browser model 52, layout 359, divine ledger 317 (14 scenarios). These cover consent/replay/expiry, authority, reserve-based quotes, EP separation, caps/refill, graph/release rules, private views and in-memory persistence images. The compiler parsed all 12 changed Java files with no syntax errors; this is not full Minecraft type checking. Review caught and this continuation corrected repeated Restore regrant after revocation, hidden names in offers, N-key loop nesting and colliding default server-config filenames. Secret scanning was clean. Full automated validation is recorded in the task report, not implied by these isolated tests.

Earlier isolated UI checks covered screen/local transforms, viewport containment, exclusive slot edges, background-before-panels ordering and final screen-space tooltips. These are not Minecraft rendering tests. No clean client, dedicated server or combined modpack could be launched here; no verified JAR for this continuation or in-game comparison is available. Complete `docs/GUI_ACCEPTANCE.md` after dependency access and CI approval, including real intrinsic effects/drain, Soul Restore Restore/Fortify, consent transactions, multiplayer permission failures, duplicate slots/pages, Alt-mode cycling without payment, reconnect, world-save NBT round trips and respawn. Pure-model tests do not establish the correctness of Minecraft bindings or real rendering.

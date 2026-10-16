# Jaycee: Di Immortales

A clean-room Minecraft **1.21.1 / NeoForge / Java 21** mod project.

## First milestone: the interface

The interface is the first deliverable. We will not build combat powers until the interface is functional and tested.

- Consume **three Jayce's Essences** to unlock the GUI (progress saved per player).
- Give players a development/test command to receive three essences.
- Render the **actual player's animated 3D model and skin**, not a screenshot of Steve.
- Five clickable tabs: Status, Abilities, Skills, Magic, Quests. Click the race name on Status to open species/evolution details; use Back to Status to return.
- A **persistent four-slot ability loadout with ten selectable pages** on every tab.
- Use the user's reference art as a guide for separately rendered UI pieces, interactive hitboxes, labels, tooltips, and responsive layouts, **not as a single background screenshot**.
- Gameplay ability implementation comes later.

## Development baseline

Official NeoForge 1.21.1 ModDevGradle starter:
https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Minecraft 1.21.1 / Java 21 / NeoForge 21.1.x.

## Gameplay HUD and loadouts

Sapphire follows the original `the realUI.png` composition: five tabs above the wide Status panel, a separate narrow Equip Page panel, portrait/race ribbon, animated cyan orbital sigil, Stats and stacked Titles/Racial Trait/Elemental Affinity panels. Frames, text, icons and controls are rendered separately, never as a screenshot background. A shared design-to-screen transform fits the menu to the viewport and inversely transforms pointer input; tooltips and the 3D model use screen coordinates.

This restoration is stacked on **open, unmerged PR #3**, starting from `ac2a871e1fe6e77a17a4168b8aa1353cee5d9545`; it does not replace that PR's HUD/gameplay work or merge main's code. The reference was retrieved from main and visually inspected, with blob SHA `b606cc8e12a19936a4336775a73bfe0fc07b206b`. In-game comparison is still required before calling the restoration visually verified.

- Four always-visible vertical slots default to the middle-right. Bindings default to **Z/X/C/V** and follow Minecraft's Controls settings.
- **Alt + mouse wheel** changes the shared loadout page (ten pages); **Alt + ability key** cycles that slot's mode.
- In Abilities/Magic, select an art, then click an equipment slot to equip it. The selection remains available when returning to Status. Right-click a slot or use its **Remove** button to clear it; Alt-click to cycle its mode. Page arrows and all ten selectors operate on the same server-saved loadout as the HUD.
- The top-left HUD portrait and HP/SP/MP bars use live player data. The Status portrait's small **Face / 3D** toggle controls the same saved preference. Face View checks the current Hat flag; 3D uses the actual player renderer and its live skin-layer flags.
- Status level/EXP are vanilla Minecraft values, not Jaycee progression. HP, MP, SP, Attack, Defense (armor), Speed (movement attribute) and Luck are live values. Titles, race, faction, Status affinities and the quest system remain unavailable/previews. Skills and Magic have their own inner-panel implementation described below; this does not turn Status EXP into skill mastery.
- HP reads Minecraft health/max-health, including attribute changes. The HUD interpolates bars, trails HP damage, and pulses on SP damage. Vanilla hearts return when the HUD is hidden/disabled.
- `config/jaycee-client.toml`, section `hud`: `enabled`, `showVanillaHearts`, resource/ability anchors and X/Y offsets, `scale`, and `iconDirectory`. Offsets are in scaled HUD units; positions clamp to the viewport. Other mods' overlay reservations are not discoverable universally; use offsets/anchors when needed.

Costs, cooldowns, modes, page selection and equipment are validated on the server. Unlock progress, portrait preference, SP and standalone mana survive death/respawn and reconnect; login, respawn and dimension changes resend the snapshot. The legacy essence counter migrates into the saved gameplay tag.

**Legacy ability scope:** the original six arts were UI previews, not implemented powers. Soul Restore costs 20 mana with a 10-second shared cooldown: Restore heals 30 SP; Fortify adds 30 spiritual barrier points. Neither charges if already full. The other five legacy arts remain previews and never charge resources. In particular, the old Aether Flight preview is not renamed or replaced by the separately granted Divine Flight intrinsic introduced below; reality manipulation and teleportation remain unimplemented.

### Ability engine — first stage

`AbilityEngine` separates executable definitions and ordered, mode-specific mana/cooldown rules from GUI catalog entries. Only explicitly registered handlers can cast; adding an `implemented` catalog flag alone can no longer accidentally run Soul Restore. Unknown abilities, invalid modes, restricted players, cooldowns and ineffective casts are rejected before payment. A successful cast consumes one resolved mana resource and records one ability-wide cooldown, shared across its modes, slots and pages.

`AbilityExecutors` binds the engine to the logical server and the existing player snapshot. In the first stage, Soul Restore was the only registered power, retaining its original ID, Restore/Fortify indices, 20 mana, 200 ticks and 30 SP/barrier. Its catalog costs/mode labels derive from the executable definition. That extraction kept the saved schema/network protocol unchanged, including re-reading NBT after mana consumption before committing effects/cooldowns. The Skills stage extends the same execution path and adds skill NBT without replacing existing fields. Executable handlers must validate eligibility before charging and apply their already-validated effect synchronously; unresolved designs are not executable definitions.

The [mod-des instruction file](https://github.com/BrightWebs/mod-des/blob/main/AGENTS.md) and all its referenced documents/data were read before this stage, including `docs/ASSET_PIPELINE.md`. The source package does **not** currently supply executable specifications for the complete requested system:

- Its 292 catalog entries have null mana cost, cooldown, range, area and cast time, and empty mode lists. `data/ability_modes.json` explicitly requires reconciliation; null is not zero/free.
- None of the existing six art names matches a catalog name exactly. The package warns that the earlier patron/fusion bible is missing and its roster may conflict. No names, saved IDs, existing modes or working powers were replaced.
- Essence for blessings/followers and permanent-power EP remain distinct from ability mana. Gain/spend rules, learning/mastery curves, grant permissions, exact evolutions/fusions and Jaycee identity checks remain unspecified; no substitute economy or EP store was fabricated. The three-essence unlock item is unchanged.
- `references/` is advertised but absent from the repository listing. Final PNG textures are explicitly not supplied. Some icon filenames/IDs have extraction punctuation or missing mappings, and briefs reuse motifs; they need reconciliation and original verified artwork, not blind imports or generic glyphs declared finished.

The first-stage engine build subsequently passed CI at `0dd8acc`, and the `/jayceetest` build passed at `6063b14`. The newer Skills/Magic/Quests work does not import the unresolved 292 concepts as working powers or alter the approved Abilities layout.

## Skills, Magic and Quests scope

This stage changes only the inner Skills, Magic and Quests content and the server-side support necessary for those screens. It retains the existing Status/Abilities content, five-tab navigation, Sapphire background, ornamental frame, four-slot Equip Page, ten saved pages, default keybindings and original Soul Restore behavior.

- Skills exposes the registered, code-backed subset: Physical Training, Soul Restore mastery, Divine Speed, Divine Flight, Divine Perception, Regeneration and Divine Resistances. Empty requested categories are not padded with fictional skills or unlocks.
- The divine intrinsics require explicit server authorization or the operator's `/jayceetest` flag. Ordinary skill XP does not grant godhood or Jaycee's Eye. No Eye implementation, direct-patron transaction system or hidden-player-stat visibility is claimed.
- Numeric tuning introduced for these mechanics is implementation balance, not recovered canon. Source costs, progression curves, patron grants, study rules and fusion/evolution recipes that remain undefined are not presented as official design values.
- Magic uses the existing ability catalog and execution path. Inspecting entries is not a cast; equipment, mana spending and cooldowns remain server-authoritative. Unsupported spells stay unavailable rather than becoming a second casting system.
- Mana is casting/upkeep fuel. The three consumed essence items retain their original menu-unlock meaning; they are not a blessing currency. Essence transactions, an EP economy and a Tensura EP bridge are not implemented by this stage. No EP cap or existing external progression is changed.
- Quests contains only the centered **QUEST SYSTEM / WORK IN PROGRESS** notice and the requested development message. There are no quest objectives, rewards or generators.

**Trying the intrinsic controls:** use survival mode, enable cheats/operator permission, run `/jayceetest`, press **M**, and open Skills. Select an intrinsic, enable it, and adjust its output; flight also offers Hover/Cruise styles. Alternatively, `/jayceeskill grant jaycee:divine_speed` grants that specific intrinsic to yourself; replace the ID with `divine_flight`, `divine_perception`, `regeneration` or `divine_resistances`, retaining the `jaycee:` namespace. `/jayceeskill revoke <id>` removes the explicit grant; disable `/jayceetest` as well to test an ordinarily locked player. Explicit grants do not replace the normal three-essence Sapphire unlock.

Server balance lives in the world's `serverconfig/jaycee-server.toml`. Defaults include 10 mastery levels with cumulative threshold `100 × level²` and a shared 20-XP/minute award budget. Effective movement/hostile combat, successful Soul Restore, distinct perception targets, restored hostile-inflicted injury and paid resistance use feed bounded, persisted award gates; merely opening the UI, toggling or changing modes does not award XP. These checks limit farming but are not a claim that every possible multiplayer farm has been tested.

Divine Speed adds a transient movement modifier without changing time. Flight manages its own survival flight lease and configurable speed. Perception reports line-of-sight nonplayer targets to its caster, not player statistics or global outlines. Regeneration restores missing **vanilla HP**, with proportional mana for actual healing; it does not replace Soul Restore's SP/barrier modes. Divine Resistances adds paid reduction without overwriting the saved base spiritual resistance. Upkeep uses one aggregate payment from the resolved mana backend; no external mana regeneration is added. Existing flight from creative/spectator or another provider is not intentionally taken over, but mods writing identical vanilla flight flags cannot be distinguished without a dedicated integration.

Nonzero output/style changes take effect at the next paid upkeep boundary, at most one second later. The current paid lease retains its original strength/cost so adjusting flight does not cause a forced fall or let a low-cost lease provide higher output for free. Turning a power off, setting output to zero, losing permission or failing payment stops its effect.

Original artwork for the new intrinsics and Physical Training, plus the shared Soul Restore healing icon, is supplied as text-free transparent PNGs with editable SVG sources under `src/main/art/ability`. Divine Speed depicts a running figure with motion trails, not time manipulation. Runtime textures use the existing canonical `assets/jaycee/textures/ability/<id>.png` path. The supplied 1024/64/32 exports support master/HUD/readability inspection; runtime menu textures are 256×256. Reproduce an export using `rsvg-convert -w 256 -h 256 <source.svg> -o <output.png>`. These assets do not imply the other five legacy preview arts or the 292 encyclopedia concepts have been implemented.

### Subsequent Divine Ascension / patronage stage — not implemented here

The requested separate ordinary-player menu, unique server-owned Jaycee authority, Divine Bonds/Dominion, N-key targeted bestowal, consent-backed atomic Essence transfers, divine names and persistent subordinate graph are a subsequent stage behind this stage's full-build gate. The existing `/jayceetest` flag is **not** unique supreme authority and must not be reused as patronage authorization. There is currently no working villager offering/patron system, EP/cap-removal system, hierarchy challenge resolution or follower economy to claim preserved or complete.

The new patronage brief specifies configurable proposed targets of a 125% relevant-reserve minimum investment for ordinary subordinate establishment, a 2,000,000,000 Jaycee Essence maximum with regeneration, and an approximately 3,000,000 ordinary progression cap. These are **not enforced by this build**; permanent EP, spendable Essence and mana must remain distinct when that stage is implemented. No recipient binding, divine name change or transfer is performed by the current menu or test commands.

## Resources and optional magic integrations

HP, Spiritual Health (SP), Mana and Tensura EP are distinct. No EP data or evolution tiers are replaced; the planned **3,000,000,000 EP** Jaycee requirement remains a progression design constraint, not an enforced evolution implementation.

Standalone mana starts at 100 maximum and regenerates 2/second. SP starts at 100 maximum and recovers 0.5/second. Server integrations use `Gameplay.damageSoul`, `healSoul`, `addSoulBarrier`, and `setSoulResistance` (a 0–1 resisted fraction). Resistance applies before the spiritual barrier, then remaining damage reduces SP; these hooks never mirror or modify physical HP. At zero SP, casting is blocked and temporary weakness/slowness apply, never forced death.

`Gameplay.setMaxSoul(player, maximum)` and `configureStandaloneMana(player, maximum, regenPerSecond)` persist and sync modifier changes, clamping current values when maxima decrease without filling increased pools. Maxima are bounded to 1–1,000,000 and standalone regeneration to 0–10,000/second; nonfinite arguments are rejected. These hooks do not mutate an external backend.

`/jayceemana` reports selected/effective/available backends; `/jayceemana auto|standalone|<registered-id>` saves a validated choice. Default `auto` prefers a registered, available `irons_spellbooks` adapter, then `ars_nouveau`, then standalone. An unavailable saved backend falls back without deleting the preference. Operators can use `/jayceesp`, `/jayceesp damage <amount>`, and `/jayceesp heal <amount>` on themselves after unlocking to exercise the spiritual hooks.

**Become Jaycee for testing:** with cheats enabled in single-player, or operator permission level 2 on a server, run `/jayceetest`, then press **M**. This gives only the executing player a saved, server-synced **Jaycee test identity** and bypasses the three-essence GUI/ability unlock. `/jayceetest off` removes the test flag without changing earned essence progress. The flag survives reconnect/respawn; non-operators cannot grant or revoke it, and no target-player argument is exposed. It does not change your displayed Human preview, implement an evolution, grant EP, refill resources, reset cooldowns, equip/replace abilities, or enable unfinished powers. It is not a substitute for the still-unresolved canonical Jaycee identity/permission rules.

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

## Build and validation status

Use **Java 21**, **Gradle 8.10.2**, and `gradle build --no-daemon` (the existing CI command). `check` includes dependency-free engine, skill-rules and Skills/Magic browser/layout Java executables. `gradle engineTest skillTest --no-daemon` targets execution/resource rules. No new testing libraries are required.

Isolated Java 21 HUD-math checks passed for anchor bounds, clamping, frame-rate-independent smoothing, delayed damage and pulses. These do not validate Minecraft rendering or multiplayer behavior.

The last successfully built predecessor is `6063b14`, [run 38850663107](https://github.com/BrightWebs/Jaycee-de-immortals-/actions/runs/38850663107). That artifact contains `/jayceetest` but **not** this Skills/Magic stage. Current Java 21 / Gradle 8.10.2 build attempts stop before Java compilation because `maven.neoforged.net` cannot resolve `net.neoforged:neoform-runtime:1.0.40`. Direct Java 21 checks cover the pure execution engine, skill arithmetic/award gates and browser filtering/layout; they do not execute Minecraft NBT, real effects, external mana adapters or multiplayer.

The seven source SVGs and 28 PNG exports passed format/size checks; a separate image review confirmed distinct, readable silhouettes on navy at 32 pixels. Physical Training's gauntlet is less obviously a clenched fist than the other subjects. This is not in-game art approval.

The final direct Java 21 runtime/browser suite reported **504 passing checks**. A separate read-only correctness review reported no significant issues, and secret scanning was clean. The requested combined automated review/CodeQL invocation could not execute because its session validation time limit was exhausted; **no fresh CodeQL pass is claimed for this stage**.

Earlier isolated UI checks covered screen/local transforms, viewport containment, exclusive slot edges, background-before-panels ordering and final screen-space tooltips. These are not Minecraft rendering tests. No clean client, dedicated server or combined modpack could be launched here; no verified Skills/Magic-stage JAR or in-game comparison is available. Complete `docs/GUI_ACCEPTANCE.md` after dependency access and CI approval, including real intrinsic effects/drain, Soul Restore Restore/Fortify, failure paths, duplicate slots/pages, Alt-mode cycling without payment, reconnect and respawn. The subsequent patronage/Divine Ascension scenarios have not been executed because those systems are not implemented yet.

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
- Status level/EXP are vanilla Minecraft values, not Jaycee progression. HP, MP, SP, Attack, Defense (armor), Speed (movement attribute) and Luck are live values. Titles, race, faction, traits, affinities, skills, category filters and quests remain explicitly unavailable/previews; no progression values are fabricated.
- HP reads Minecraft health/max-health, including attribute changes. The HUD interpolates bars, trails HP damage, and pulses on SP damage. Vanilla hearts return when the HUD is hidden/disabled.
- `config/jaycee-client.toml`, section `hud`: `enabled`, `showVanillaHearts`, resource/ability anchors and X/Y offsets, `scale`, and `iconDirectory`. Offsets are in scaled HUD units; positions clamp to the viewport. Other mods' overlay reservations are not discoverable universally; use offsets/anchors when needed.

Costs, cooldowns, modes, page selection and equipment are validated on the server. Unlock progress, portrait preference, SP and standalone mana survive death/respawn and reconnect; login, respawn and dimension changes resend the snapshot. The legacy essence counter migrates into the saved gameplay tag.

**Ability scope:** the original six arts were UI previews, not implemented powers. Soul Restore costs 20 mana with a 10-second shared cooldown: Restore heals 30 SP; Fortify adds 30 spiritual barrier points. Neither charges if already full. Other arts remain explicitly labeled previews and never charge resources. This update does not claim to implement reality manipulation, flight or teleportation.

### Ability engine — first stage

`AbilityEngine` separates executable definitions and ordered, mode-specific mana/cooldown rules from GUI catalog entries. Only explicitly registered handlers can cast; adding an `implemented` catalog flag alone can no longer accidentally run Soul Restore. Unknown abilities, invalid modes, restricted players, cooldowns and ineffective casts are rejected before payment. A successful cast consumes one resolved mana resource and records one ability-wide cooldown, shared across its modes, slots and pages.

`AbilityExecutors` binds the engine to the logical server and the existing player snapshot. Soul Restore remains the only registered power, with its original ID, Restore/Fortify indices, 20 mana, 200 ticks and 30 SP/barrier. Its catalog costs/mode labels derive from the executable definition. Saved NBT and the network protocol are unchanged, including re-reading NBT after mana consumption before committing effects/cooldowns. Existing server validation, rate limiting, equip/remove/page/mode operations, icons and all approved GUI files remain unchanged. Executable handlers must validate eligibility before charging and apply their already-validated effect synchronously; unresolved designs are not executable definitions.

The [mod-des instruction file](https://github.com/BrightWebs/mod-des/blob/main/AGENTS.md) and all its referenced documents/data were read before this stage, including `docs/ASSET_PIPELINE.md`. The source package does **not** currently supply executable specifications for the complete requested system:

- Its 292 catalog entries have null mana cost, cooldown, range, area and cast time, and empty mode lists. `data/ability_modes.json` explicitly requires reconciliation; null is not zero/free.
- None of the existing six art names matches a catalog name exactly. The package warns that the earlier patron/fusion bible is missing and its roster may conflict. No names, saved IDs, existing modes or working powers were replaced.
- Essence for blessings/followers and permanent-power EP remain distinct from ability mana. Gain/spend rules, learning/mastery curves, grant permissions, exact evolutions/fusions and Jaycee identity checks remain unspecified; no substitute economy or EP store was fabricated. The three-essence unlock item is unchanged.
- `references/` is advertised but absent from the repository listing. Final PNG textures are explicitly not supplied. Some icon filenames/IDs have extraction punctuation or missing mappings, and briefs reuse motifs; they need reconciliation and original verified artwork, not blind imports or generic glyphs declared finished.

No 292-entry catalog import, new power, progression economy, evolution/fusion, detailed Abilities-tab redesign or final icon artwork is claimed by this first-stage extraction. Development stops at this stage's build gate until full compilation and the missing canon/assets are available.

## Resources and optional magic integrations

HP, Spiritual Health (SP), Mana and Tensura EP are distinct. No EP data or evolution tiers are replaced; the planned **3,000,000,000 EP** Jaycee requirement remains a progression design constraint, not an enforced evolution implementation in this interface-only baseline.

Standalone mana starts at 100 maximum and regenerates 2/second. SP starts at 100 maximum and recovers 0.5/second. Server integrations use `Gameplay.damageSoul`, `healSoul`, `addSoulBarrier`, and `setSoulResistance` (a 0–1 resisted fraction). Resistance applies before the spiritual barrier, then remaining damage reduces SP; these hooks never mirror or modify physical HP. At zero SP, casting is blocked and temporary weakness/slowness apply, never forced death.

`Gameplay.setMaxSoul(player, maximum)` and `configureStandaloneMana(player, maximum, regenPerSecond)` persist and sync modifier changes, clamping current values when maxima decrease without filling increased pools. Maxima are bounded to 1–1,000,000 and standalone regeneration to 0–10,000/second; nonfinite arguments are rejected. These hooks do not mutate an external backend.

`/jayceemana` reports selected/effective/available backends; `/jayceemana auto|standalone|<registered-id>` saves a validated choice. Default `auto` prefers a registered, available `irons_spellbooks` adapter, then `ars_nouveau`, then standalone. An unavailable saved backend falls back without deleting the preference. Operators can use `/jayceesp`, `/jayceesp damage <amount>`, and `/jayceesp heal <amount>` on themselves after unlocking to exercise the spiritual hooks.

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

Use square PNGs; resources reload with F3+T. Missing textures log once per resource reload and use the existing procedural glyphs, not unrelated spell art. `hud.iconDirectory` permits an alternate pack directory. **Artwork TODO:** `BrightWebs/ModResources` returned 404 during repository-content access; provide access or upload the icon ZIP to replace all six placeholders. No supplied artwork was available to copy.

## Build and validation status

Use **Java 21**, **Gradle 8.10.2**, and `gradle build --no-daemon` (the existing CI command). `check` now includes the dependency-free `engineTest` Java executable; run `gradle engineTest --no-daemon` for its targeted checks. It verifies execution ordering, rejected casts without resource/effect mutation, shared cooldowns, exact expiry, mode order, bounded test effects, invalid definitions and payment rejection. No new testing libraries are required.

Isolated Java 21 HUD-math checks passed for anchor bounds, clamping, frame-rate-independent smoothing, delayed damage and pulses. These do not validate Minecraft rendering or multiplayer behavior.

The preceding UI/text build at `1620a6a` succeeded in [run 38833388652](https://github.com/BrightWebs/Jaycee-de-immortals-/actions/runs/38833388652), but that does **not** validate this new engine stage. Its `gradle engineTest build --no-daemon` attempt with Java 21/Gradle 8.10.2 stopped before mod compilation: `maven.neoforged.net` could not resolve while downloading `net.neoforged:neoform-runtime:1.0.40`. Directly compiling/running the production `AbilityEngine.java` and `AbilityEngineTest.java` with Java 21 passed **48 assertions**. Those isolated tests use a fake context, not Minecraft NBT, an actual mana adapter, multiplayer or the real Soul Restore binding.

Earlier isolated UI checks covered screen/local transforms, viewport containment, exclusive slot edges, background-before-panels ordering and final screen-space tooltips. These are not Minecraft rendering tests. No clean client, dedicated server or combined modpack could be launched here; no new engine-stage JAR or in-game comparison is available. Complete `docs/GUI_ACCEPTANCE.md` after dependency access and CI approval are restored, including real Soul Restore Restore/Fortify, failed/full-resource casts, duplicate slots/pages, Alt-mode cycling without payment, reconnect and respawn.

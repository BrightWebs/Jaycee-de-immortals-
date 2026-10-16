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

Use **Java 21**, **Gradle 8.10.2**, and `gradle build --no-daemon` (the existing CI command). Pull requests now run that same build workflow. There is no existing automated test suite.

Isolated Java 21 HUD-math checks passed for anchor bounds, clamping, frame-rate-independent smoothing, delayed damage and pulses. These do not validate Minecraft rendering or multiplayer behavior.

**This restoration has not produced a verified playable JAR.** Both baseline `compileJava` and restored `build --no-daemon` were attempted with Java 21/Gradle 8.10.2 but stopped before Java compilation: `maven.neoforged.net` could not resolve while downloading `net.neoforged:neoform-runtime:1.0.40`. Run [38825414382](https://github.com/BrightWebs/Jaycee-de-immortals-/actions/runs/38825414382) was independently checked: `completed / action_required`, head `ac2a871`, zero jobs—not a successful build. The restoration's initial PR run [38828813758](https://github.com/BrightWebs/Jaycee-de-immortals-/actions/runs/38828813758) also requires approval. A prior successful workflow for main does not validate these changes.

Isolated Java 21 checks passed 112 screen/local transform round trips at seven viewport sizes (1920×1080 through 240×135), viewport containment and exclusive slot edges. Source checks verified background-before-panels ordering, final screen-space tooltips, no Species tab and the valid narration lambda. These are not Minecraft compilation or rendering tests. No clean client, dedicated server or combined modpack could be launched; no in-game comparison screenshot or new artifact download is available. Complete `docs/GUI_ACCEPTANCE.md` after dependency access and CI approval are restored.

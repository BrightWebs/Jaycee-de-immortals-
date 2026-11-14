# Jaycee: Di Immortales

A Minecraft **26.3 / NeoForge 26.3 / Java 25** mod built on Greek mythology and **Ichor**, the golden blood of the gods.

This repo holds two mods:

| Mod | Folder | What it is |
|---|---|---|
| **Bright Core** (`brightcore`) | `brightcore/` | The framework: ability registry, per-player ability data, syncing, ability keys, the ability bar, `/ability` commands. Jaycee requires it. |
| **Jaycee: Di Immortales** (`jaycee`) | `jaycee/` | The content: the M menu, the ability bar's sapphire look, and (step by step) Ichor, the realms and the bestiary. |

## Playing it

Put **both** jars in your `mods` folder. GitHub builds them on every push: open the latest run under **Actions**, then download **jaycee-and-brightcore-26.3**.

| Key | Does |
|---|---|
| **M** | Open the Jaycee menu |
| **R, G, V, B, N** | Use ability slot 1 to 5 |
| **Y** | Next ability page (10 pages) |

All keys are rebindable in Controls.

Quick test: `/ability learnall @s`, then press **R**. In the menu's **Abilities** tab, pick a loadout slot on the right, then click an ability to place it there. Right-click a slot to clear it, and Alt + scroll to change pages.

## What's in so far

- **Ability bar** (bottom right, like Palladium): 5 slots per page, 10 pages, key labels, cooldown shade with seconds, red tint when you can't afford it, mana bar, page number.
- **Menu**: six tabs, live 3D player model, real health and mana, and the 5-slot loadout shared with the ability bar.
- **Five test abilities**: Thunderbolt, Hermes' Dash, Apollo's Mending, Shadow Step, Aegis. Placeholders until we design the real ones.

## For development

- `gradle build` builds both mods. `gradle :jaycee:runClient` runs a dev client with both loaded.
- The **API dump** workflow prints exact 26.3 class signatures for the classes listed in `tools/api-classes.txt`, which helps when Minecraft renames things.
- Design: the *Ichor: Foundation* doc.

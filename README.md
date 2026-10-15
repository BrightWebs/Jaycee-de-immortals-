# Jaycee: Di Immortales

Minecraft 1.21.1, NeoForge 21.1.172, Java 21.

## UI beta 0.2.0

- Seven interactive tabs: Status, Abilities, Skills, Magic, Species, Quests and Sovereign Nexus.
- Original reference artwork split into reusable panel borders and ability icons; all text and controls are live.
- Status displays the local player's actual skin and equipment, health, armor and Minecraft experience.
- Searchable ability library with category filtering, selection and descriptions.
- Forty saved loadout slots organized into ten pages of four slots. Select a slot and press Equip; Clear slot removes its entry.
- Persistent equipment panel on all tabs, direct page buttons, arrows and Alt + scroll.
- Matching four-slot gameplay HUD. Alt + scroll changes its page without scrolling the vanilla hotbar.
- UI scales to fit the window; render and mouse input use the same coordinate transform.
- Server-owned essence progress and loadout data synchronize on login, consumption, respawn and editing. State is copied on player clone.

## Try it

Install the beta JAR in a Minecraft 1.21.1 NeoForge instance, removing the previous Jaycee JAR first. Multiplayer servers need the same version.

With operator permission, run `/jayceeessence`. Use the three essences, then press **M**. Choose **Abilities**, select an entry and a right-hand equipment slot, then click **Equip to selected slot**. Switch tabs, pages and reconnect to check persistence.

Abilities, mastery, magic and evolution are interface previews. Equipping does not cast a combat power. Unimplemented values and chapters are labeled accordingly. The Awakening quest reflects the three-essence menu unlock.

## Build

Use Java 21 and Gradle 8.14.3. Run `gradle build`. Run `gradle runClient` for a development client. The production JAR is written to `build/libs/`.

See `docs/GUI_ACCEPTANCE.md` for the visual and interactive acceptance requirements. A successful compile does not establish exact reference fidelity or replace in-game testing.

## First-consumer advancement (Jaycee)

The **first player in a world** to consume one **Jayce's Essence** earns the exclusive **Jaycee** advancement. The claim is stored in the world save and is not reset when the first player logs out. All players must still absorb **three essences** to unlock the M interface. Claiming the advancement does not bypass the three-essence gate.

## Getting a test JAR

Open **Actions** on GitHub, select the latest successful **Build UI beta** run on `codex/finish-sapphire-ui`, and download the `jaycee-ui-beta` artifact ZIP. Extract the mod JAR from the ZIP. The workflow runs on branch pushes. If that run failed or has no artifact, the changed source is not ready to install; don't install an earlier artifact thinking it has these changes.

Use a separate test world if you want to verify the first-consumer advancement: that claim is intended to be awarded only once per world.

# GUI fidelity and acceptance requirements

**Priority:** Reproduce the user's original Jaycee fantasy RPG GUI reference art faithfully; don't interpret it as permission for generic blue rectangles.

## Artwork fidelity
- Use each supplied GUI reference to build separate decorative frame pieces, icons, tab art, edge ornaments, parchment/glass surfaces and highlighted states.
- Do not simply render an annotated reference image over the UI. Crop and redraw actual usable design elements.
- Maintain consistent proportions and crisp scaling at common Minecraft GUI scales and window sizes.
- Preserve readability, clipping, click bounds, hover/selected/focused states, keyboard accessibility and sound feedback.
- Preserve artwork when adding gameplay abilities. Compare in-game screenshots with each reference image before declaring visual fidelity.

## Screen anatomy
- Tabs: Status / Abilities / Skills / Magic / Quests, each clickable and visually distinct. Species is opened by the race field within Status.
- Player preview: render the local player's skin in the reference's compact portrait; offer a 3D toggle for the animated entity with the player's skin and equipment, never a fixed Steve screenshot.
- Status layout: left profile, level/EXP bar and stats; central luminous seal; right titles, racial trait and six elemental affinity rows.
- Shared Equip Page: permanently visible sidecar with four slots in a 2 × 2 grid and ten saved pages, also visible within Species. Preserve original content coordinates while fitting the wider shell.
- Ability/skill nodes: real widget hitboxes, selectable states and live descriptions, not painted-on buttons. Unimplemented gameplay content must show honest placeholder text.
- Shared visual treatment: illustrated ability rows, glowing diamond skill nodes, inset detail cards and central seals use the same silver-blue fantasy language as Status.
- Magic, Species and Quests: selectable preview collections must update the featured emblem and details, retain selection across tabs, and remain explicitly disconnected from gameplay progression.
- Gameplay HUD: four slots, remappable Z/X/C/V, Alt modes and Alt+wheel pages, using the same synchronized loadouts. Check cooldown, active, disabled and empty appearances without covering vanilla bars.

## Unlock and testing
- With default beta access enabled, press **M** from gameplay without consuming essences; all four HUD slots must appear, but abilities retain their unlock requirements.
- The existing operator-only essence command remains available but is not required to test the interface.
- Verify client/server behavior, resolution handling, model render, tab persistence, mouse and keyboard input, and full resource load using Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- Verify loadouts/modes/page across dimension changes, death, reconnect and server restart; verify portrait preference across client restart. With beta access disabled, enforce the centralized progression gate.
- A Gradle build passing is not proof of in-game visual fidelity. Do not label a JAR final until both build and in-game GUI checks pass.

## Source of truth
Actual supplied artwork is required to establish exact geometry and colors. If those images are missing, don't invent a supposedly exact recreation.

The Status attachment is now visible and guides the redesign. Its external callout boxes and arrows are presentation annotations, not part of the game screen. The two other attachments were not viewable; Abilities/Skills fidelity remains unverified. Procedural ornament and the vanilla font remain approximations pending final art and in-game screenshot comparison.

# GUI fidelity and acceptance requirements

**Priority:** Reproduce the user's original Jaycee fantasy RPG GUI reference art faithfully; don't interpret it as permission for generic blue rectangles.

## Artwork fidelity
- Use each supplied GUI reference to build separate decorative frame pieces, icons, tab art, edge ornaments, parchment/glass surfaces and highlighted states.
- Do not simply render an annotated reference image over the UI. Crop and redraw actual usable design elements.
- Maintain consistent proportions and crisp scaling at common Minecraft GUI scales and window sizes.
- Preserve readability, clipping, click bounds, hover/selected/focused states, keyboard accessibility and sound feedback.
- Match artwork before adding gameplay abilities. Compare in-game screenshots with each reference image before declaring completion.

## Screen anatomy
- Tabs: Status / Abilities / Skills / Magic / Species / Quests, each clickable and visually distinct.
- Player preview: render actual local player's animated 3D entity with the player's own skin and equipment, not a static portrait or default Steve.
- Shared ability loadout frame: four slots always visible across six tabs; ten page selectors consistently accessible. Swapping tabs must not reset equipped abilities or page.
- Ability/skill nodes: real widget hitboxes, selectable states and live descriptions, not painted-on buttons. Unimplemented gameplay content must show honest placeholder text.
- Follow-up: an in-game HUD can reuse the menu's four-slot loadout visual language; it is not part of this UI scaffold.

## Unlock and testing
- Press **M** from normal gameplay to open the menu immediately, without consuming essences.
- The existing operator-only essence command remains available but is not required to test the interface.
- Verify client/server behavior, resolution handling, model render, tab persistence, mouse and keyboard input, and full resource load using Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- A Gradle build passing is not proof of in-game visual fidelity. Do not label a JAR final until both build and in-game GUI checks pass.

## Source of truth
Actual supplied artwork is required to establish exact geometry and colors. If those images are missing, don't invent a supposedly exact recreation.

# Jaycee: Di Immortales

A clean-room Minecraft **1.21.1 / NeoForge / Java 21** mod project.

## First milestone: the interface

The interface is the first deliverable. We will not build combat powers until the interface is functional and tested.

- Press **M** during normal gameplay to open the interface; no unlock item is required.
- The existing essence item and operator command `/jayceeessence` remain available for experimentation, but do not gate the UI.
- Render the **actual player's skin** in the compact portrait, with a **3D** toggle for the live animated player model.
- Six clickable tabs: Status, Abilities, Skills, Magic, Species, Quests.
- A **four-slot preview loadout with ten selectable pages**, accessible from every tab.
- Reference-inspired sapphire glass, blue-silver ornamental borders, glowing icon tabs and a central magical seal. The visible Status reference guides the proportions; pixel-perfect parity is not claimed.
- Gameplay ability implementation comes later.

## Development baseline

Official NeoForge 1.21.1 ModDevGradle starter:
https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Minecraft 1.21.1 / Java 21 / NeoForge 21.1.x.

## Run in development

Install **JDK 21** and set `JAVA_HOME` to it. From the repository root:

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

On Windows use `gradlew.bat`. The first build downloads Gradle and the Minecraft/NeoForge development dependencies. The JAR is written under `build/libs/`. The development server requires accepting Mojang's EULA in its generated run directory before it starts a world.

Join a world and press **M** (rebind under Options → Controls → Jaycee). Escape closes the non-pausing screen. Tabs and skill nodes support mouse clicks and normal keyboard focus/activation.

## UI preview and customization

- **Status:** compact live skin portrait and player name, race/title/faction labels, level/EXP bar and core stats on the left; a glowing magical seal in the center; titles, racial trait and six elemental affinities on the right. The **3D / Face** toggle switches the portrait to the animated player model. Health, vanilla level and experience progress are live; RPG stats, traits and affinities are explicitly sample data.
- **Abilities:** illustrated category and ability rows, six visible rows with wheel/arrow scrolling, a glowing selected-art emblem, inset description card and **Equip preview** button.
- **Skills:** four mastery columns with connected diamond-shaped symbol nodes, four selectable tiers, a mastery seal, and inset requirement/effect cards. Hover/focus tooltips identify each node's branch and tier.
- **Magic:** six selectable elemental entries, a grimoire seal, spell notes and sample affinity/mana information.
- **Species:** selectable lineage stages, an ascension emblem and preview racial traits/requirements.
- **Quests:** a selectable journal, featured chronicle emblem, objective and sample reward cards. These are not accepted or tracked quests.
- **Loadout** opens a collapsible tray with four selectable slots and ten pages (`<` / `>` or Alt + scroll). **Equip preview** assigns to the selected slot/page and opens the tray so the result is visible. Close the tray to resume panel interactions. It survives tab switches and resizing, but resets when the screen closes. It is not synchronized, saved, or connected to gameplay powers.

Source organization (under `src/main/java/com/brightwebs/jaycee/`):

- `client/JayceeClient.java`: client-only registration and gameplay key handling. The common mod entry point never references client classes.
- `client/JayceeScreen.java`: icon navigation, collapsible loadout state and a 640 × 500 logical layout; rendering and pointer input share the same scale.
- `client/view/`: independent tab panels implementing `TabView`.
- `client/widget/OrnateButton.java`: shared hover, selected, focus, tooltip and disabled visuals.
- `client/view/ViewArt.java`: shared featured emblems, inset notes and preview meters.
- `client/view/JournalView.java` and `client/model/JournalData.java`: reusable illustrated collection layout and immutable sample entries for Magic, Species and Quests. Each tab keeps its own selection. Collections contain one to six entries, matching the visible list capacity.
- `client/widget/StatusSymbols.java`: compact heart/star/sword/shield/boot/clover and elemental silhouettes for the Status rows.
- `client/model/PreviewData.java`: replaceable UI-only records and sample values.
- `client/DivineFrame.java` and `client/DivineGlyph.java`: centralized procedural panels, ornamental borders, magical seal and recognizable navigation icons. No external UI textures are required; the portrait uses the player's loaded Minecraft skin.

For final art, replace drawing inside the frame/glyph helpers with resource-location blits and place textures under `src/main/resources/assets/jaycee/textures/gui/`. Preserve helper dimensions and control hitboxes; assets can then be overridden by resource packs. Adjust panel geometry in the individual views and the shared design size in the shell together. Follow-up polish includes hand-painted borders and icon textures, localization of preview labels, synchronized progression data and a gameplay HUD.

### Reference scope

The supplied Status mockup (`df5d9c15-68d6-4f26-a806-bcb9b23ba011`) guides the separate tab strip and main frame, left profile/stat stack, central luminous emblem and right inset cards. Its black presentation background, explanatory callout boxes and arrows are annotations, not in-game controls, and are intentionally omitted. Names and values use the current player or marked preview data rather than copying fixed screenshot text.

The other attachment URLs were supplied but their images were not visible in this session. All six tabs now extrapolate the visible Status artwork's dark glass, blue-silver ornament, glowing symbols and left/center/right hierarchy. Abilities and Skills retain their working interactions; Magic, Species and Quests use illustrated selectable preview collections rather than empty panels. These are not claimed to reproduce unseen layouts. Procedural art and Minecraft's default font are an approximation, not a pixel-identical recreation.

## Manual acceptance checks

Build validation does not establish in-game fidelity. In a dev client, check M in a new world without essences, all six icon tabs, portrait Face/3D modes, every category, list scrolling and endpoints, skill node details/tooltips, every Magic/Species/Quests entry, selection persistence across tab changes, opening/closing the loadout, equip/page persistence, long labels, Escape, and resize at multiple GUI scales (including a window smaller than the design size). Use Tab and Enter/Space to exercise illustrated rows and diamond nodes. While the tray is open, covered view controls must not respond. Check that M does not replace chat/inventory screens. Start a dedicated dev server to verify client-only isolation. Compare Status screenshots to the supplied reference; obtain the other images before claiming their layouts match.

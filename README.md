# Jaycee: Di Immortales

A clean-room Minecraft **1.21.1 / NeoForge / Java 21** mod project.

## First milestone: the interface

The interface is the first deliverable. We will not build combat powers until the interface is functional and tested.

- Press **M** during normal gameplay to open the interface; no unlock item is required.
- The existing essence item and operator command `/jayceeessence` remain available for experimentation, but do not gate the UI.
- Render the **actual player's animated 3D model and skin**, not a screenshot of Steve.
- Six clickable tabs: Status, Abilities, Skills, Magic, Species, Quests.
- A **persistent four-slot ability loadout with ten selectable pages** on every tab.
- Dark sapphire panels, glowing accents, separate interactive controls, and a layout that scales to the window. Reference images were not attached to this task, so exact visual fidelity is not claimed.
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

- **Status:** live player name, model, health, vanilla level and experience progress; MP, Attack, Defense, Speed and Luck are sample values.
- **Abilities:** category filtering, six visible list rows, wheel scrolling over the list or arrow buttons, selected details and an **Equip preview** button.
- **Skills:** four mastery columns, four selectable tiers, and a details panel.
- **Magic / Species / Quests:** distinct placeholder panels.
- The bottom loadout has four selectable slots and ten pages (`<` / `>` or Alt + scroll). Equip assigns to the selected slot/page. It survives tab switches and resizing, but resets when the screen closes. It is not synchronized, saved, or connected to gameplay powers.

Source organization (under `src/main/java/com/brightwebs/jaycee/`):

- `client/JayceeClient.java`: client-only registration and gameplay key handling. The common mod entry point never references client classes.
- `client/JayceeScreen.java`: navigation, loadout state and a 640 × 360 logical layout; rendering and pointer input share the same scale.
- `client/view/`: independent tab panels implementing `TabView`.
- `client/widget/OrnateButton.java`: shared hover, selected, focus, tooltip and disabled visuals.
- `client/model/PreviewData.java`: replaceable UI-only records and sample values.
- `client/DivineFrame.java` and `client/DivineGlyph.java`: centralized vector-like placeholder panels, portraits, borders and icons. No external UI textures are required.

For final art, replace drawing inside the frame/glyph helpers with resource-location blits and place textures under `src/main/resources/assets/jaycee/textures/gui/`. Preserve helper dimensions and control hitboxes; assets can then be overridden by resource packs. Adjust panel geometry in the individual views and the shared design size in the shell together. Follow-up polish includes supplied artwork, localization of preview labels, synchronized progression data and a gameplay HUD.

## Manual acceptance checks

Build validation does not establish in-game fidelity. In a dev client, check M in a new world without essences, all six selected tabs, every category, list scrolling and endpoints, skill node details, equip/page persistence, long labels, Escape, and resize at multiple GUI scales (including a window smaller than the design size). Check that M does not replace chat/inventory screens. Start a dedicated dev server to verify client-only isolation. Compare screenshots to the reference images once supplied.

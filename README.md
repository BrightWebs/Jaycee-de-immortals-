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

## Download and play the UI test build

1. Open this repository's **Actions** tab and select **Build NeoForge 1.21.1**. Choose a **successful run for the UI pull request's latest commit**, not an older run from another branch. PR builds run automatically; **Run workflow** can also build a selected branch manually.
   - If GitHub shows **Action required** or **Workflows awaiting approval**, a repository maintainer must use **Approve and run** on the pull request before the build can start. There is no downloadable artifact until that approved build succeeds.
2. Under **Artifacts**, download `jaycee-neoforge-1.21.1` (GitHub sign-in required). Extract the ZIP to get `jaycee-0.1.3.jar` (animated Status energy core, including the earlier size/header/blur fixes). The ZIP and GitHub's source-code downloads are not installable mods.
3. Install **NeoForge 21.1.172** for **Minecraft 1.21.1**, launch that profile once, then close the game. Use Java 21; Forge and Fabric are not compatible loaders for this JAR.
4. Put the extracted JAR into the **mods** folder of that profile's game directory. Remove older Jaycee JARs so only one version is installed.
5. Launch the NeoForge profile, confirm **Jaycee Di Immortales** appears in **Mods**, and enter a test world. Press **M** to open the UI immediately—no essence is needed. If M conflicts with another mod, rebind it under **Options → Controls → Key Binds → Jaycee**.

Test all six tabs, select skills and journal entries, scroll the ability list, and use **Equip preview** and **Loadout**. Escape closes the screen. Powers, progression, quests and equipment remain UI previews; this is an interface test build, not a completed RPG gameplay mod. Back up existing worlds before testing.

If no successful run/artifact exists for the latest commit, **there is no verified download yet**. A failed build does not publish a JAR. The same source can be built locally using the commands below; an internet connection to the NeoForge and Minecraft dependency hosts is required.

The build also checks that the output JAR contains NeoForge metadata and the mod entry point, client key handler and screen classes before uploading it. This checks packaging, not in-game behavior.

### UI blur test (0.1.1)

This screen now uses a plain dim overlay instead of Minecraft's post-processing menu blur. The backdrop is drawn once before the panels; inherited background calls do not apply a blur pass over the interface. This change is local to the Immortals screen and does not alter your global menu blur setting.

Remove older Jaycee JARs before installing the replacement. With menu background blur enabled in Minecraft, open the UI with M and compare Status text, borders, all other tabs, the loadout and Face/3D portraits. Close and reopen the screen and resize the window. The world should be dimmed, not blurred, while the interface stays unblurred. Layout scaling on very small windows can still reduce text legibility; that is separate from the removed full-screen blur effect.

### Compact layout and heading test (0.1.2)

The interface is 14% smaller than 0.1.1 at the same window size and GUI scale, leaving more room around the centered window. The shared panel header has a solid navy backing, 20-pixel horizontal text inset, and a divider below the text so ornamental corners cannot run through headings such as **Stats**, **Titles**, or **Racial Trait**. All tabs use this heading treatment. Artwork and the logical 640 × 500 layout remain unchanged.

Compare the overall footprint, heading clearance, ability scrolling, skill-node selection, journal entries, loadout controls and Face/3D portraits at multiple GUI scales. Drawing, mouse input and portrait bounds all use the same reduced scale. [Tensura: Reincarnated](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated) is a general RPG-interface reference, not a source of copied assets or an exact sizing specification.

### Great Sage-inspired energy core (0.1.3)

Only the Status seal's old flame is replaced. The existing blue rings stay at `(357, 231)` with radius 82 in the 640 × 500 design space. `GreatSageEnergyCore` draws inside radius 43, wholly inside the inner ring, without changing the surrounding panels, buttons or layout. All curve centers are mathematically bounded to 92% of the drawable radius; thin-line halos fit inside the remaining margin, so containment does not depend on a scissor rectangle interacting with scaled GUI poses.

Hundreds of separate curves deform in independent planes, with layered translucent subpixel strands, depth-dependent brightness, traveling light pulses, breathing ellipses, occasional turbulence and a pale radiant center. Periodic spatial color waves close cleanly at each loop seam and simultaneously blend sapphire/ice blue, cyan, white, violet, purple, gold, emerald and soft crimson. Geometry and color use monotonic elapsed time, not frame counters or a rotating texture. Only the formation is multicolored; rings retain their original blue treatment.

**Performance:** Minecraft's **Video Settings → Particles** selects core quality: All = 224 strands × 40 segments, Decreased = 144 × 32, Minimal = 64 × 24. Standard batched translucent GUI quads provide bright thin lines plus soft halos without changing global blending or invoking the background blur shader. Reduce quality on slower GPUs.

**States and integration:** Idle is automatic; hovering inside the core smoothly enters Analyzing. An observed vanilla level increase while Status is open triggers a three-second golden Level Up pulse (for example, `/experience add @s 1 levels` with cheats enabled). `JayceeScreen.energyCore()` exposes the live renderer for client-thread event notifications. Its `trigger(EnergyCoreMotion.State)` supports LEARNING (gold convergence/re-expansion), LEVEL_UP, ULTIMATE (extra turbulence/complexity), and AWAKENING (five-second white-gold surge); `setAnalyzing(boolean)` controls continuous analysis. Events crossfade against live baseline curves and return to idle/analysis. Hover analysis and quality are managed by Status each frame.

Learning/ultimate/awakening are rendering hooks, **not newly implemented gameplay powers**, and are not fired by merely selecting a preview skill or consuming an unsynchronized client essence counter. The renderer can also be reused independently via `render(graphics, centerX, centerY, radius)` and `setQuality(Quality)`. Motion/color mathematics live in the Minecraft-independent `client/model/EnergyCoreMotion.java`.

**Test:** Keep Status open for at least a minute; check that individual loops change shape and different regions carry different moving colors. Hover/unhover the center, trigger a vanilla level increase, compare all particle qualities, resize/change GUI scale, toggle Face/3D, open the loadout and switch every tab. Rings, stats and elemental panels must not change color or be overlapped. The energy core uses no static image, eyes, faces or added symbolic imagery. In-game appearance and frame-time still require client testing; mathematical checks alone do not establish reference fidelity.

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

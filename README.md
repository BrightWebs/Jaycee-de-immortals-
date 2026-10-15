# Jaycee: Di Immortales

A clean-room Minecraft **1.21.1 / NeoForge / Java 21** mod project.

## First milestone: the interface

The interface is the first deliverable. We will not build combat powers until the interface is functional and tested.

- Consume **three Jayce's Essences** to unlock the GUI (progress saved per player).
- Give players a development/test command to receive three essences.
- Render the **actual player's animated 3D model and skin**, not a screenshot of Steve.
- Six clickable tabs: Status, Abilities, Skills, Magic, Species, Quests.
- A **persistent four-slot ability loadout with ten selectable pages** on every tab.
- Use the user's reference art as a guide for separately rendered UI pieces, interactive hitboxes, labels, tooltips, and responsive layouts, **not as a single background screenshot**.
- Gameplay ability implementation comes later.

## Development baseline

Official NeoForge 1.21.1 ModDevGradle starter:
https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Minecraft 1.21.1 / Java 21 / NeoForge 21.1.x.

## Status

The repository is newly initialized. **No playable JAR has been built or tested yet.** Next changes will add the Gradle build and Java sources.

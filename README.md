# Jaycee: Di Immortales

A Minecraft **1.21.1 / NeoForge / Java 21** mod built on Greek mythology and **Ichor**, the golden blood of the gods.

## Current state

Only the interface remains. All earlier gameplay (Jayce's Essence, the `/jc` commands, mana, spells, essence bestowal) was removed so the Ichor system can be built on a clean base. It's all still in git history.

What's kept:
- `client/JayceeScreen.java`: the main menu, opened with **M**. Six tabs (Status, Abilities, Skills, Magic, Species, Quests), the live 3D player model, and the four-slot, ten-page loadout.
- `client/OrnateButton.java`, `client/DivineFrame.java`, `client/DivineGlyph.java`: the hand-drawn sapphire frame, buttons and ability glyphs.
- `client/JayceeClient.java`: the M keybind.
- `the realUI.png` and `docs/GUI_ACCEPTANCE.md`: the reference art and the GUI spec.

The menu currently opens for everyone; it isn't tied to any unlock yet.

## Design

The game design lives in the *Ichor: Foundation* doc: five realms (Olympus, Gaia, the Underworld, Tartarus, Chaos), Aether and Ichor, the Mist, Grades of Divinity from Mortal (about 5 Ichor) to Primordial (1 billion, capped at 2 billion), and a Greek bestiary of hidden beings and demi-creatures.

## Building

Official NeoForge 1.21.1 ModDevGradle starter: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

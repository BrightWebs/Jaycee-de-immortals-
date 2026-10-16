# AGENTS.md — Jaycee: Di Immortales authoritative development rules

## Source of truth

This project is the user's Minecraft Java 1.21.1 / NeoForge 21.1.x / Java 21 divine RPG mod. Implement from the actual 131-page `Jaycee_Di_Immortales_Definitive_Master_Design_Bible.pdf` and the original icon/image references, combined with the owner's **later** conversation revisions. Earlier conflicting GUI/layout requirements are superseded by newer explicit corrections. Never mistake a concept illustration or icon prompt for a compiled game asset.

### Later corrections with highest priority

- Every player's **Status screen** has the **same exact composition as Jaycee's original Status screen**: actual live character skin/model, level/progression bar, bottom-left HP/MP/SP/attack/defense/speed/luck, top-right Titles, middle-right **Trait** (not Racial Trait), bottom-right Elemental Affinity, central divine insignia, same geometry and ornate panels.
- Single patron recolors decorative **accents** and shows one stationary god emblem centrally. Zeus yellow lightning, Poseidon sea-blue trident, Hades dark violet underworld icon, Gaia emerald living-world glyph, and each remaining god own canonical emblem. Multiple retained patron lineages restore neutral palette and slowly orbit distinct emblems centrally. These changes never grant Jaycee authority.
- Ability slots remain four per equip page across ten pages. Remove bottom resource/HP/MP text from Equip Page; keep resource meters in Status and appropriate HUD. Ability lists/traits/magic/skills scroll. Same original ability artwork in library/details/slots/HUD.
- Quests = WORK IN PROGRESS, not a quest gameplay engine.
- Divine level advances from **cumulative qualifying essence earned/received**, not vanilla Minecraft XP. Spendable essence, cumulative earned essence, EP and mana are distinct. Grades F/E/D/C/B/A/S/SS/SSS appear beside level. Grade numbers from earlier proposals are configurable, not fixed canon; rank remains separate.
- M opens main role-aware menu, contextual bestowal may preselect living mob or player; N opens bestowal directly. Bestowal shows prefilled **editable** name (keep unchanged or choose new), amount beneath, and Bless/Cancel. When credited, transferred essence belongs to recipient, not giver. Player divine naming/fealty requires acceptance where appropriate, and gifting alone does not bind subordinate. Mob compatibility/persistence are required.
- A dedicated Subordinates control belongs directly above the right Equip Page panel. Patron/subordinate system must validate permissions, consent, transactional accounting, persistence and non-circularity.
- Standard player patron determines unlocked ability universe, patron-centric skills and magic. Ordinary players cannot browse or cast unrelated lineages. Jaycee is unique server-assigned privileged authority—not merely a matching username or an OP—who may give Universal Essence, override progression caps and adjudicate.
- Unified `/jc` or `/jaycee` command tree: player self-inspection and restricted `edit` branches for server operator actions, without confusing operators with Jaycee's in-game role.
- Use official names and ability meanings in encyclopedia for the 292 entries; real casting, cooldown, modes (Alt + ability slot cycles unlocked modes without casting), skill mastery and appropriate effects are necessary. No fake 'completed' entries, fake tests or fake art.

## Development constraints

1. Inspect repo before edits and preserve working GUIs/world saves. Work in feature branches and small compile-tested increments.
2. No client-trusted privilege, currency, targeting or damage packets. Server validates and applies transactions once.
3. Use metadata-driven ability definitions and original image assets; do not create several competing registries.
4. Distinct mana (casting), transferable essence (gift), cumulative progression essence (level), EP (permanent combat scale), and Jaycee-only universal essence.
5. Copy a user's PDF or other source references into repo only when permitted. Record provenance, and do not describe as runtime textures.
6. Build with Java 21 and NeoForge 21.1.x; test the client and server in-game before claiming a playable release.
7. Provide honest implemented vs planned coverage. No claims that 292 abilities work based only on catalog or JSON.

## Current repository state

The starting `main` branch is an old UI-only prototype. This feature branch adds a preliminary `PhaseOneGameplay` class and command-cast vertical slice, **not a verified working JAR**. The complete mod remains under development and the user's full specifications are not yet implemented.

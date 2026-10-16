# Phase 1 gameplay vertical slice (in progress)

This branch begins actual server-side gameplay beyond the existing GUI scaffold.

## Added in source code

- `/jc status`, `/jc essence`, and `/jc grade`: inspect tracked essence progression and grades (F through SSS).
- `/jc cast lightning`: block-targeted vanilla lightning, 30 mana, 60-tick cooldown.
- `/jc cast heal`: heal 4 hearts, 20 mana, 80-tick cooldown.
- `/jc cast dash`: launch along look direction, 12 mana, 30-tick cooldown.
- `/jc cast blink`: short-range block-targeted teleport, 22 mana, 50-tick cooldown.
- `/jc cast ward`: absorption effect, 18 mana, 30-tick cooldown.
- `/jc edit <player> essence add <amount>`: permission-level-2 admin command to credit tracked essence.
- `/jaycee` alias for `/jc`.
- Every absorbed Jaycee Essence item credits 500 tracked essence, while preserving the existing three-item GUI unlock.
- Mana regenerates at 3 per second up to 100.

These are candidate implementations: **build and in-world tests are not yet verified**. The current repository has no Gradle wrapper; use a Java 21 + compatible Gradle/NeoForge environment before calling this a working JAR.

## Explicitly not implemented yet

- An icon-backed ability registry, ability equip-to-cast keybindings, patron-restricted skill trees, actual skill mastery, divine-grade appraisal block, and the approved redesigned Status UI.
- M/N targeted essence transfers and editable divine names; mob bestowal; subordinate bonding and follower management.
- Full combat balancing, Essence/EP integration, godhood evolutions, the complete ability encyclopedia, and approved art assets.
- Proper respawn cloning of player progression and offline player transfers.

Please do not treat this slice as a completed first playable beta until it compiles, runs, and is tested in-game.

## Integration guidance

- Preserve approved GUI artwork and behavior.
- Connect the spell engine to the existing four-slot/tens-page equip UI; replace test-like `/jc cast` workflows with keybind casting while keeping optional commands.
- Replace the former three-item access gate with approved divine rank/patron permissions once that subsystem exists.
- Implement persistent recipient-owned essence and transaction validation before exposing bestowal.
- Add actual original icons from the artwork resources; text-only art prompts are not image files.

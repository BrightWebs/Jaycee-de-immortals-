# GUI fidelity and acceptance requirements

**Priority:** Reproduce the user's original Jaycee fantasy RPG GUI reference art faithfully; don't interpret it as permission for generic blue rectangles.

## Artwork fidelity
- Use each supplied GUI reference to build separate decorative frame pieces, icons, tab art, edge ornaments, parchment/glass surfaces and highlighted states.
- Do not simply render an annotated reference image over the UI. Crop and redraw actual usable design elements.
- Maintain consistent proportions and crisp scaling at common Minecraft GUI scales and window sizes.
- Preserve readability, clipping, click bounds, hover/selected/focused states, keyboard accessibility and sound feedback.
- Match artwork before adding gameplay abilities. Compare in-game screenshots with each reference image before declaring completion.
- Compare specifically against main's `the realUI.png` (blob `b606cc8e12a19936a4336775a73bfe0fc07b206b`): top bar over Status only, separate Equip Page panel, upper-left portrait/Face toggle and race ribbon, lower-left Stats/EXP, central animated orbital sigil and three stacked right-side detail panels. Capture the Status view at GUI scales 1–4 and a small window; check ornament proportions, text and slot hitboxes. Reference inspection is complete; this in-game comparison is not.

## Screen anatomy
- Tabs: Status / Abilities / Skills / Magic / Quests, each clickable and visually distinct. No Species tab: click the race name on Status for evolution details, then Back to Status. The baseline race remains the existing Human preview until progression integration is implemented.
- Confirm panel text, bars, borders and player portrait stay sharp with Minecraft Menu Background Blurriness above zero. The world background may blur, but must be processed only before Sapphire content, never between panels and widgets.
- Player preview: render actual local player's animated 3D entity with the player's own skin and equipment, not a static portrait or default Steve.
- Shared ability loadout frame: four slots always visible across all tabs and race details; ten page selectors consistently accessible. Swapping tabs must not reset equipped abilities or page.
- Ability/skill nodes: real widget hitboxes, selectable states and live descriptions, not painted-on buttons. Unimplemented gameplay content must show honest placeholder text.
- In-game HUD: four-slot loadout rendered with the same visual design language as the menu, not plain square placeholders.

## Unlock and testing
- With cheats/operator level 2, run `/jayceetest` and press M without consuming essences; verify Soul Restore remains usable through the existing equip system. Verify a non-operator and the server console cannot self-grant the identity. Reconnect/respawn and confirm the test flag persists. Run `/jayceetest off`: players with fewer than three consumed essences must be locked again; players who earned the three-essence unlock must stay unlocked. Check that command use never changes HP/SP/mana, EP, cooldowns, loadouts, portrait preference or the approved GUI.
- Three consumed **Jayce's Essences** unlock the menu per player; persist consumption count and unlock state across relogs.
- Add an operator-only test command for receiving three essences; test the lock before and after consumption.
- Verify client/server behavior, resolution handling, model render, tab persistence, mouse and keyboard input, and full resource load using Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- A Gradle build passing is not proof of in-game visual fidelity. Do not label a JAR final until both build and in-game GUI checks pass.

## Source of truth
Actual supplied artwork is required to establish exact geometry and colors. If those images are missing, don't invent a supposedly exact recreation.

## HUD/resource regression checklist (not yet runtime-verified)
- Text-spacing pass: verify all five navigation labels share the same pixel font size and baseline, centered below their icons. Check focused/hovered tabs too.
- Inspect Status headings, player names, largest resource/attribute values, racial-trait text, affinity rows, species descriptions and Back to Status. Confirm no text touches corner filigree or dividers; read long descriptions as wrapped text rather than a squeezed font.
- On Abilities/Magic, inspect every category and art row with resource-pack icons. On Skills, ensure all four tier numbers remain visible. Check every Quest heading and footer.
- Check equipment in empty, implemented and preview states on all ten pages, including selector **10**. Numbers 1–4 and Empty/Equipped/Preview sit above their frames; names sit below icons with measured ellipsis, and full names/modes/bindings remain in tooltips/narration.
- Rebind HUD slots to left/right Shift, Control, Alt and mouse buttons. Check compact hotkey labels remain distinguishable, modes/costs stay inside slots, and large mana-regeneration/resource values do not collide. Full binding names are available in Sapphire slot tooltips/narration.
- Repeat text checks at window sizes 1920×1080, 1438×874, 1280×720, 854×480 and 640×360 with GUI scales 1–4 where Minecraft allows them. Open tooltips near each edge; confirm wrapping stays on-screen. Isolated coordinate checks cover those dimensions, but do not establish rendered readability at very small scales.
- Start a clean Java 21 / NeoForge 21.1.172 client and a separate dedicated server. Confirm the server never loads client rendering classes.
- Consume three essences on the server; confirm M opens immediately, then confirm unlock survives death and reconnect.
- Check all four empty slots at middle-right. Rebind Z/X/C/V and confirm labels and inputs follow the new bindings.
- Select Soul Restore through Abilities, then click each equipment slot (including after returning to Status); switch tabs, close/reopen M, and check the HUD shows the same equipment/page. Verify all four Remove buttons and right-click clearing affect only the selected page. Confirm other arts remain non-activating previews.
- Click each of the ten page selectors and both arrows; confirm the highlighted selector follows the server snapshot, including changes made with Alt-scroll. Check empty slots, names/icons, mana and instructions remain inside Equip Page.
- Tab/Shift-Tab through controls and activate with the keyboard, including the narrated race ribbon and Back to Status. Resize with the screen open; check focus highlights, pointer hover/click/drag alignment, tooltips and 3D-model clipping at each GUI scale.
- Alt-scroll through all ten pages in both menu and gameplay, including rapid scrolling and wraparound. Confirm vanilla hotbar scrolling is not also triggered.
- Alt-key/Alt-click a slot to cycle modes; reconnect and confirm the mode persists. Verify duplicate copies/pages cannot bypass an ability's cooldown.
- Check Face View with a multiplayer skin and toggle Hat in Skin Customization without relogging. Check 3D View with hat, jacket, sleeves and pants toggled; both portraits must use the shared Status preference.
- Damage/heal the player and change `minecraft:generic.max_health` via `/attribute`. Check actual HP values, smooth bar motion, delayed HP damage, and no overflow.
- Use the explicit server spiritual hooks to damage/heal SP, apply resistance/barriers, and change its maximum. Confirm ordinary physical damage/healing does not mirror into SP.
- At zero SP, verify nonfatal penalties and activation refusal. At partial SP, verify Soul Restore consumes mana exactly once, updates SP/mana immediately, and enforces its cooldown server-side.
- Try locked, dead and spectator activation, invalid page/slot/mode/backend requests, and insufficient resources. None may activate or charge resources.
- Die, respawn, change dimension, disconnect/reconnect and restart the server. Verify resources, page, equipment, modes, backend choice and portrait preference; no stale cooldown pulse from another session.
- Toggle HUD/vanilla-heart compatibility, F1, F3, spectator mode, screens and pause. Hearts must not disappear when the custom health bar is unavailable.
- Test common GUI scales, small windows and configured anchors/offsets/scales; check hotbar, chat, bossbars, effects and scoreboard. Manually offset around third-party overlays as needed.
- Install a pack containing the six mapped icons, reload with F3+T, and check list/equipment/HUD consistency. Remove an icon and confirm a single missing-art warning and safe fallback.
- Once real optional adapters are supplied: test standalone, Iron's-only, Ars-only, both installed, capability unavailability and removal of an optional mod between sessions. Confirm exactly one backend is charged, no extra regeneration, and owner's resource synchronization works. Current guarded stubs are not evidence of external compatibility.

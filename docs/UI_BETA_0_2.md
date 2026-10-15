# UI beta 0.2.0 validation notes

This update replaces the previous fixed 520x310 procedural prototype with a scaled 740x340 interactive layout. Decorative borders come from the supplied sapphire menu collage, and ten ability icons come from the Reality Severance reference; labels, counts, selection states, descriptions, and equipment content are rendered independently. Status retains the actual player entity.

## Behavior changed

- Category buttons filter the library; the search box filters names without rebuilding itself while typing.
- Seven visible list rows can scroll. Empty searches disable equipping.
- Selecting an ability and clicking Equip updates the selected slot on the current page. Clear removes it.
- A fixed-size 40-slot payload and bounds-checked edit payload synchronize through the server. Clients cannot grant essence progress through the edit payload.
- Login, respawn, dimension change and essence consumption send current state. Cloning preserves essence count and slots.
- UI navigation and the HUD share the player's active page.
- Skill nodes support mouse selection and keyboard focus through normal Button widgets.
- The Magic, Species and Quests tabs have separate layouts and disclose unfinished gameplay.

## Still requires a Minecraft client acceptance run

1. Start locked, use three essences, press M. Confirm each unlock counter message and menu availability.
2. Equip different entries on pages 1 and 10; switch all six tabs, reopen the menu, reconnect, die and change dimension. Verify slot contents and page.
3. Filter by category and search for an absent name. Verify no stale entry can be equipped.
4. Use Tab and Enter to select controls, including skill nodes. Type into search and use Escape to close.
5. Check 854x480, 1280x720 and 1920x1080 at multiple GUI scales. Confirm entity clipping, tooltip placement and matching click bounds.
6. Test two players on a dedicated server. Verify one player's slot changes do not affect the other.
7. Compare screenshots with the original Status and Ability references. This implementation is not certified as an exact visual match until that comparison passes.

Gameplay powers, mastery progression, magic casting and the evolution system remain outside this UI beta.

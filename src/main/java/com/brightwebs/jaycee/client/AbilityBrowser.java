package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Native, local-coordinate controls; the owning screen transforms rendering and pointer input together. */
final class AbilityBrowser {
    static final int FILTER_ROWS = 11, LIST_ROWS = 7, DETAIL_ROWS = 18;
    private static final int INK = 0xFFB6CCDD, MUTED = 0xFF788DAB;
    private final AbilityBrowserModel model = AbilityDesignData.load();
    private final Button[] filters = new Button[FILTER_ROWS], rows = new Button[LIST_ROWS];
    private final IntConsumer selectedArt;
    private EditBox search;
    private Button details, modeButton;
    private int previewMode, syncedMode = -1;
    private String modeContext = "";
    private final int[] observedModes = new int[PlayerState.SLOTS];
    private int followedSlot = -1;
    private List<FormattedCharSequence> detailLines = List.of();

    AbilityBrowser(IntConsumer selectedArt) { this.selectedArt = selectedArt; }

    void mount(Consumer<AbstractWidget> add) {
        var font = Minecraft.getInstance().font;
        search = new EditBox(font, 140, 109, 157, 18, Component.literal("Search abilities within selected family"));
        search.setMaxLength(AbilityBrowserModel.MAX_SEARCH);
        search.setHint(Component.literal("Search name / concept"));
        search.setValue(model.search());
        search.setResponder(value -> { model.search(value); refresh(); });
        add.accept(search);
        for (int i = 0; i < FILTER_ROWS; i++) {
            final int row = i;
            filters[i] = control(14, 112 + i * 23, 116, 22, () -> {
                int index = model.filterOffset() + row;
                if (index < model.families().size()) { model.filter(model.families().get(index)); refresh(); }
            }, (g, button) -> {
                String family = model.families().get(model.filterOffset() + row);
                frame(g, button, family.equals(model.filter()));
                text(g, family, button.getX() + 5, button.getY() + 4, 88, .72f, INK);
                text(g, "" + model.count(family), button.getX() + 94, button.getY() + 4, 19, .7f, INK);
            });
            add.accept(filters[i]);
        }
        for (int i = 0; i < LIST_ROWS; i++) {
            final int row = i;
            rows[i] = control(137, 144 + i * 32, 163, 31, () -> {
                var found = model.results();
                int index = model.listOffset() + row;
                if (index < found.size()) { model.select(found.get(index)); selectionChanged(); }
            }, (g, button) -> {
                var entry = model.results().get(model.listOffset() + row);
                frame(g, button, entry.equals(model.selected()));
                int x = button.getX() + 5;
                if (entry.legacy()) {
                    AbilityIcons.render(g, AbilityCatalog.ALL.get(entry.legacyIndex()), x, button.getY() + 7, 17,
                            entry.equals(model.selected()));
                    x += 22;
                }
                text(g, entry.name(), x, button.getY() + 5, 295 - x, .8f, INK);
                text(g, entry.legacy() ? "Legacy / Lv unresolved" : "Concept / Lv unresolved",
                        x, button.getY() + 18, 295 - x, .65f, MUTED);
            });
            add.accept(rows[i]);
        }
        details = control(308, 176, 216, 190, () -> {}, (g, button) -> {
            if (button.isFocused()) g.fill(308, 174, 524, 175, 0xFFB08BDD);
            for (int i = 0; i < DETAIL_ROWS && model.detailOffset() + i < detailLines.size(); i++) {
                g.pose().pushPose();
                g.pose().translate(312, 179 + i * 10, 0);
                g.pose().scale(.8f, .8f, 1);
                g.drawString(Minecraft.getInstance().font, detailLines.get(model.detailOffset() + i), 0, 0, INK, false);
                g.pose().popPose();
            }
        });
        add.accept(details);
        modeButton = control(308, 375, 216, 20, () -> {
            var entry = model.selected();
            if (entry != null && entry.legacy()) {
                var art = AbilityCatalog.ALL.get(entry.legacyIndex());
                previewMode = (previewMode + 1) % Math.max(1, art.modes().size());
            }
        }, (g, button) -> {
            frame(g, button, false);
            text(g, button.getMessage().getString(), 314, 381, 204, .75f, INK);
        });
        add.accept(modeButton);
        refresh();
    }

    private interface Painter { void draw(GuiGraphics g, Button button); }
    private Button control(int x, int y, int width, int height, Runnable click, Painter paint) {
        return new Button(x, y, width, height, Component.empty(), button -> click.run(), Button.DEFAULT_NARRATION) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float delta) { paint.draw(g, this); }
        };
    }
    private static void text(GuiGraphics g, String value, int x, int y, int width, float scale, int color) {
        UiText.line(g, value, x, y, width, scale, color, false);
    }
    private static void frame(GuiGraphics g, Button button, boolean selected) {
        DivineFrame.sapphire(g, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                selected || button.isHoveredOrFocused());
    }
    private void selectionChanged() {
        modeContext = ""; syncedMode = -1; previewMode = 0;
        refresh();
    }
    private void refresh() {
        selectedArt.accept(model.selected() == null ? -1 : model.selected().legacyIndex());
        for (int i = 0; i < filters.length; i++) {
            int index = model.filterOffset() + i;
            filters[i].visible = index < model.families().size();
            if (filters[i].visible) {
                String family = model.families().get(index);
                filters[i].setMessage(Component.literal(family + ": " + model.count(family) + " entries. Arrow keys or wheel scroll families."));
            }
        }
        var found = model.results();
        for (int i = 0; i < rows.length; i++) {
            int index = model.listOffset() + i;
            rows[i].visible = index < found.size();
            if (rows[i].visible) rows[i].setMessage(Component.literal(found.get(index).name()
                    + ". Inspect only. Rank and level unresolved. Arrow keys browse, Page Up/Down scroll."));
        }
    }
    void render(GuiGraphics g, PlayerState state, int selectedSlot) {
        text(g, "Mana " + number(state.mana()) + "   Essence " + state.essence()
                + "   EP " + state.ep(), 18, 77, 506, .8f, INK);
        text(g, "Ability Categories", 16, 96, 114, .8f, INK);
        text(g, "Abilities: " + model.filter(), 139, 96, 160, .8f, INK);
        text(g, model.results().size() + " / " + model.count(model.filter()) + " matching", 140, 133, 157, .75f, MUTED);
        g.fill(133, 96, 134, 395, 0xFF35485E);
        g.fill(303, 96, 304, 395, 0xFF35485E);
        text(g, "Wheel / arrows: families", 16, 375, 114, .65f, MUTED);
        text(g, (model.filterOffset() + 1) + "-" + Math.min(model.filterOffset() + FILTER_ROWS, model.families().size())
                + " / " + model.families().size(), 16, 386, 114, .7f, MUTED);
        text(g, "Wheel / arrows / PgUp / PgDn", 140, 375, 158, .65f, MUTED);
        text(g, "Inspect only; use Equip Page", 140, 386, 158, .65f, MUTED);
        text(g, "289 canonical names (292 variants) + 6 legacy = 295 entries", 18, 402, 506, .72f, MUTED);
        var entry = model.selected();
        var blocks = new ArrayList<String>();
        if (entry == null) {
            text(g, "Select an ability", 312, 106, 208, 1f, INK);
            text(g, "Artwork unavailable", 312, 143, 208, .8f, MUTED);
            blocks.add("Selection only inspects; it does not cast or equip.");
            blocks.add("Canonical concepts are unimplemented. Legacy abilities are unmapped, not assigned guessed families.");
        } else {
            text(g, entry.name(), 312, 106, 208, 1f, INK);
            if (entry.legacy()) {
                var art = AbilityCatalog.ALL.get(entry.legacyIndex());
                syncMode(state, art, selectedSlot);
                AbilityIcons.render(g, art, 312, 126, 40, true);
                boolean available = Minecraft.getInstance().getResourceManager().getResource(
                        com.brightwebs.jaycee.config.HudConfig.ICON_DIRECTORY.get().equals("jaycee:textures/ability")
                                ? art.icon() : AbilityIcons.texture(art.id())).isPresent();
                text(g, available ? "Runtime icon" : "Fallback glyph", 358, 132, 162, .8f, MUTED);
                text(g, available ? "No illustration" : "Artwork unavailable", 358, 146, 162, .75f, MUTED);
                legacyDetails(blocks, state, art);
            } else {
                text(g, "Artwork unavailable", 312, 140, 208, .9f, MUTED);
                blocks.add("Rank / type / level: unresolved");
                blocks.add("Mana Cost: unresolved");
                blocks.add("Cooldown: unresolved");
                blocks.add("Range: unresolved");
                blocks.add("Area: unresolved");
                blocks.add("Cast Time: unresolved");
                blocks.add("Concept only — unimplemented. Not equippable.");
                blocks.add("Modes / progression / requirements / evolution / fusion: unresolved; no relationships inferred.");
                blocks.add(entry.variants().size() + " source variant(s), all retained:");
                for (var variant : entry.variants()) {
                    blocks.add("Variant #" + variant.index() + " | " + variant.family());
                    blocks.add("Original source ID: " + variant.sourceId());
                    blocks.add("Description / concept: " + variant.concept());
                    blocks.add("Effects: design intent only, not runtime mechanics.");
                    variant.fields().forEach((key, value) -> {
                        if (!List.of("name", "index", "id", "family", "gameplay_concept").contains(key))
                            blocks.add(key + ": " + value);
                    });
                }
            }
        }
        detailLines = blocks.stream().flatMap(block -> Minecraft.getInstance().font
                .split(Component.literal(block), 255).stream()).toList();
        model.scrollDetails(0, detailLines.size(), DETAIL_ROWS);
        details.setMessage(Component.literal((entry == null ? "Ability details" : entry.name())
                + ". Focus here and use arrow keys, Page Up/Down, Home/End or wheel to read all details. "
                + String.join(" ", blocks)));
        text(g, "Details " + (model.detailOffset() + 1) + "-" + Math.min(detailLines.size(), model.detailOffset() + DETAIL_ROWS)
                + "/" + detailLines.size() + " (scroll)", 312, 365, 208, .65f, MUTED);
        modeButton.active = entry != null && entry.legacy() && AbilityCatalog.ALL.get(entry.legacyIndex()).implemented();
        modeButton.setMessage(Component.literal(modeButton.active
                ? "Preview mode: " + AbilityCatalog.ALL.get(entry.legacyIndex()).modes().get(previewMode) + " (click)"
                : "Inspection only - use existing slots"));
    }
    private void syncMode(PlayerState state, AbilityCatalog.Art art, int selectedSlot) {
        String context = art.id() + ":" + state.page();
        boolean changed = !context.equals(modeContext);
        boolean snapshotChanged = changed;
        if (changed) {
            followedSlot = -1;
            for (int i = 0; i < PlayerState.SLOTS; i++)
                if (state.ability(state.page(), i).equals(art.id())) { followedSlot = i; break; }
            if (state.ability(state.page(), selectedSlot).equals(art.id())) followedSlot = selectedSlot;
        }
        for (int i = 0; i < PlayerState.SLOTS; i++) {
            int mode = state.ability(state.page(), i).equals(art.id()) ? state.mode(state.page(), i) : -1;
            if (!changed && observedModes[i] != mode) {
                snapshotChanged = true;
                if (mode >= 0) followedSlot = i;
            }
            observedModes[i] = mode;
        }
        if (followedSlot >= 0 && observedModes[followedSlot] < 0) {
            followedSlot = -1;
            for (int i = 0; i < PlayerState.SLOTS; i++) if (observedModes[i] >= 0) { followedSlot = i; break; }
        }
        int actual = followedSlot < 0 ? 0 : observedModes[followedSlot];
        actual = Math.clamp(actual, 0, Math.max(0, art.modes().size() - 1));
        if (snapshotChanged || actual != syncedMode) {
            previewMode = actual; syncedMode = actual; modeContext = context;
        }
    }
    private void legacyDetails(List<String> blocks, PlayerState state, AbilityCatalog.Art art) {
        var mode = AbilityCatalog.modeStats(art.id(), previewMode);
        blocks.add("Legacy (unmapped) | Rank / level: unresolved");
        blocks.add("Type: " + (art.implemented() ? "active, self" : "unresolved"));
        blocks.add("Mode: " + art.modes().get(previewMode) + " (inspection preview only)");
        blocks.add("Mana Cost: " + (mode == null ? "unresolved" : number(mode.manaCost())));
        blocks.add("Cooldown: " + (mode == null ? "unresolved"
                : String.format(Locale.ROOT, "%.1f s", mode.cooldownTicks() / 20.0)));
        blocks.add("Range: " + (art.implemented() ? "self" : "unresolved"));
        blocks.add("Area: " + (art.implemented() ? "N/A (self effect)" : "unresolved"));
        blocks.add("Cast Time: " + (art.implemented() ? "instant (synchronous)" : "unresolved"));
        if (art.implemented()) {
            blocks.add("Description / effects: " + (previewMode == 1
                    ? "Fortify adds up to 30 spiritual barrier, capped by maximum barrier. Requires missing barrier."
                    : "Restore adds up to 30 SP, capped by maximum SP. Requires missing SP."));
            blocks.add("Requirements: Sapphire unlock, alive, non-spectator, SP above zero, sufficient mana, cooldown ready.");
            blocks.add("Unlock: " + (state.unlocked() ? "unlocked" : "locked") + ". Equip via the existing Equip Page slot.");
            blocks.add("Successful uses: " + state.successfulUses(art.id()) + " (raw count, NOT mastery level)");
        } else blocks.add("Preview only — not implemented. No gameplay effects or resolved numeric stats.");
        blocks.add("Progression / evolution / fusion: unresolved. No canonical family mapping.");
        blocks.add("Runtime ID (unchanged): " + art.id());
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.0f", value); }

    boolean scroll(double x, double y, double amount) {
        if (y < 96 || y >= 396 || amount == 0) return false;
        int delta = amount > 0 ? -1 : 1;
        if (x >= 14 && x < 131) model.scrollFilters(delta, FILTER_ROWS);
        else if (x >= 137 && x < 301) model.scrollList(delta, LIST_ROWS);
        else if (x >= 308 && x < 525) model.scrollDetails(delta * 3, detailLines.size(), DETAIL_ROWS);
        else return false;
        refresh(); return true;
    }
    boolean key(int key, AbstractWidget focused) {
        if (focused == search) return false;
        int delta = switch (key) { case 265 -> -1; case 264 -> 1; case 266 -> -7; case 267 -> 7;
            case 268 -> Integer.MIN_VALUE; case 269 -> Integer.MAX_VALUE; default -> 0; };
        if (delta == 0) return false;
        if (focused == details) model.scrollDetails(delta, detailLines.size(), DETAIL_ROWS);
        else if (List.of(filters).contains(focused)) {
            model.scrollFilters(delta, FILTER_ROWS);
        } else if (List.of(rows).contains(focused)) {
            if (key == 268) { model.select(null); model.moveSelection(1, LIST_ROWS); }
            else if (key == 269) { model.select(null); model.moveSelection(-1, LIST_ROWS); }
            else model.moveSelection(delta, LIST_ROWS);
            modeContext = ""; syncedMode = -1;
        } else return false;
        refresh(); return true;
    }
    List<Component> tooltip(int x, int y) {
        for (var button : filters) if (button.visible && button.isMouseOver(x, y)) return List.of(button.getMessage());
        for (var button : rows) if (button.visible && button.isMouseOver(x, y)) return List.of(button.getMessage());
        if (x >= 308 && x < 525 && y >= 100 && y < 175 && model.selected() != null)
            return List.of(Component.literal(model.selected().name()), Component.literal("Artwork availability is labelled below the title."));
        return List.of();
    }
}

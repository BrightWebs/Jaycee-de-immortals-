package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.brightwebs.jaycee.gameplay.SkillDefinitions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Skills and Magic share browsing only, never a second gameplay or equipment system. */
final class SkillMagicBrowser {
    private static final int INK = 0xFFB6CCDD, MUTED = 0xFF788DAB, VIOLET = 0xFFBD9EE4;
    private static final String[] SKILLS = {"All Skills", "Physical Mastery", "Magical Mastery",
        "Divine Mastery", "Elemental Affinities", "Resistances", "Perception", "Movement", "Patron Disciplines"};
    private static final String[] MAGIC = {"All Magic", "Elemental", "Divine", "Healing", "Defensive",
        "Spatial", "Summoning", "Enchantment", "Patron", "Forbidden Magic"};
    private final boolean magic;
    private final boolean abilities;
    private final java.util.function.IntSupplier selectedSlot;
    private final SkillMagicBrowserModel model = new SkillMagicBrowserModel();
    private final List<Hint> hints = new ArrayList<>();
    private final List<Button> categories = new ArrayList<>(), rows = new ArrayList<>();
    private Button filter, toggle, minus, plus, mode, style, detailsFocus;
    private EditBox search;
    private List<FormattedCharSequence> detailLines = List.of();
    private String query = "";
    private record Hint(AbstractWidget widget, java.util.function.Supplier<String> text) {}

    SkillMagicBrowser(boolean magic, java.util.function.IntSupplier selectedSlot) {
        this(magic, false, selectedSlot);
    }
    SkillMagicBrowser(boolean magic, boolean abilities, java.util.function.IntSupplier selectedSlot) {
        this.magic = magic;
        this.abilities = abilities;
        this.selectedSlot = selectedSlot;
    }
    private PlayerState state() { return PlayerState.get(Minecraft.getInstance().player); }
    private String[] categoryNames() { return abilities ? new String[]{"All Abilities", "Canon mapping unresolved"} : magic ? MAGIC : SKILLS; }
    void init(Consumer<AbstractWidget> add) {
        hints.clear(); categories.clear(); rows.clear();
        refresh();
        var font = Minecraft.getInstance().font;
        search = new EditBox(font, 136, 106, 196, 16, Component.literal("Search " + (abilities ? "abilities" : magic ? "magic" : "skills")));
        search.setMaxLength(96);
        search.setHint(Component.literal("Search by name"));
        search.setValue(query);
        search.setResponder(value -> { query = value; model.query(value); });
        add.accept(search);
        String[] names = categoryNames();
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            categories.add(button(add, 20, 111 + i * 25, 100, 23, () -> names[index],
                () -> model.category(index == 0 ? "" : names[index]),
                () -> names[index] + ": " + model.count(index == 0 ? "" : names[index]) + " registered entries"));
        }
        filter = button(add, 136, 126, 196, 19, () -> "Show: " + model.filter().name(),
            model::cycleFilter, () -> "Filter registered entries: all, unlocked, locked, mastered");
        for (int i = 0; i < SkillMagicLayout.VISIBLE_ROWS; i++) {
            final int row = i;
            rows.add(button(add, 134, 149 + i * 36, 200, 34, () -> rowName(row),
                () -> { var entry = row(row); if (entry != null) model.select(entry.id()); },
                () -> { var entry = row(row); return entry == null ? "" : entry.name() + ". " + status(entry.id(), state()); }));
        }
        var text = SkillMagicLayout.TEXT;
        detailsFocus = new Button(text.x(), text.y(), text.width(), text.height(),
            Component.literal("Selected details"), b -> {}, narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {}
        };
        add.accept(detailsFocus);
        toggle = button(add, 350, 332, 166, 19, () -> "Toggle", () -> {
            var id = model.selected(); if (id != null) GameplayActions.skillToggle(id, !state().skillEnabled(id));
        }, () -> {
            var d = SkillDefinitions.find(model.selected());
            var s = state();
            return d == null ? "No intrinsic selected" : d.name() + ". " +
                (s.skillEnabled(d.id()) ? "Enabled" : "Disabled") + ". Output: " + s.skillOutput(d.id()) +
                "%. Activation: " + number(d.activationMana(s)) +
                " mana; disabling stops upkeep. Changes require server confirmation.";
        });
        minus = button(add, 350, 354, 39, 19, () -> "-10%", () -> output(-10),
            () -> outputHint() + ". Decrease output by ten percentage points.");
        plus = button(add, 477, 354, 39, 19, () -> "+10%", () -> output(10),
            () -> outputHint() + ". Increase output by ten percentage points.");
        mode = button(add, 350, 376, 81, 18, () -> "Mode", this::cycleMode, () -> {
            var d = SkillDefinitions.find(model.selected());
            return "Configured mode: " + (d == null || d.modes().isEmpty() ? "N/A" : d.modes().get(state().skillMode(d.id()))) +
                ". Cycle unlocked modes without casting. Changes take effect at the next paid upkeep.";
        });
        style = button(add, 435, 376, 81, 18, () -> "Style", this::cycleStyle, () ->
            "Configured flight style: " + (model.selected() == null ? "N/A" : state().skillStyle(model.selected())) +
                ". Cycle supported styles without casting. Changes take effect at the next paid upkeep.");
    }
    private Button button(Consumer<AbstractWidget> add, int x, int y, int w, int h,
                          java.util.function.Supplier<String> text, Runnable action,
                          java.util.function.Supplier<String> hint) {
        var button = new Button(x, y, w, h, Component.literal(text.get()), b -> action.run(),
            narration -> narration.get().copy().append(". " + hint.get())) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                boolean selected = categories.contains(this) && categories.indexOf(this) ==
                    java.util.Arrays.asList(categoryNames()).indexOf(model.category().isEmpty() ? categoryNames()[0] : model.category());
                DivineFrame.sapphire(g, getX(), getY(), width, height, selected || isHoveredOrFocused());
                if (!rows.contains(this)) UiText.line(g, getMessage().getString(), getX() + 5,
                    getY() + (height - 7) / 2, width - 10, .72f, active ? INK : MUTED, true);
            }
        };
        add.accept(button);
        hints.add(new Hint(button, hint));
        return button;
    }
    private void refresh() {
        var s = state();
        List<SkillMagicBrowserModel.Entry> entries = new ArrayList<>();
        if (magic) {
            for (var art : AbilityCatalog.registered(s)) {
                if (!s.supreme() && !s.abilityOwned(art.id())) continue;
                var d = SkillDefinitions.find(art.id());
                entries.add(new SkillMagicBrowserModel.Entry(art.id(), art.name(),
                    abilities ? List.of("Canon mapping unresolved") : art.id().equals(SkillDefinitions.RESTORE) ? List.of("Healing") :
                        d != null && d.intrinsic() ? List.of("Divine") : List.of(),
                    art.implemented() && granted(art.id(), s), mastered(art.id(), s)));
            }
        } else for (var d : SkillDefinitions.ALL) {
            if (!s.supreme() && !s.skillGranted(d.id())) continue;
            entries.add(new SkillMagicBrowserModel.Entry(d.id(), d.name(), d.categories(),
                s.unlocked() && s.skillGranted(d.id()), mastered(d.id(), s)));
        }
        model.entries(entries);
    }
    private boolean granted(String id, PlayerState s) {
        return s.unlocked() && (SkillDefinitions.find(id) == null || s.skillGranted(id));
    }
    private boolean mastered(String id, PlayerState s) {
        return SkillDefinitions.find(id) != null && s.skillLevel(id) >= s.skillMaxLevel();
    }
    String equippableSelection() {
        var art = AbilityCatalog.find(model.selected());
        return art != null && art.implemented() && granted(art.id(), state()) ? art.id() : null;
    }
    private SkillMagicBrowserModel.Entry row(int index) {
        var entries = model.visible();
        int actual = model.offset() + index;
        return actual < entries.size() ? entries.get(actual) : null;
    }
    private String rowName(int index) { var e = row(index); return e == null ? "" : e.name(); }
    private static String status(String id, PlayerState s) {
        var art = AbilityCatalog.find(id);
        if (art != null && !art.implemented()) return "Locked preview - mapping unresolved";
        if (!s.unlocked() || !s.skillGranted(id)) return "Locked - requires server grant";
        return s.skillLevel(id) >= s.skillMaxLevel() ? "Mastered" : "Unlocked";
    }
    void render(GuiGraphics g) {
        refresh();
        var s = state();
        for (var r : List.of(SkillMagicLayout.CATEGORIES, SkillMagicLayout.LIST, SkillMagicLayout.DETAILS))
            DivineFrame.sapphire(g, r.x(), r.y(), r.width(), r.height(), false);
        line(g, "Categories", 22, 94, 96, INK);
        line(g, abilities ? "Abilities" : magic ? "Magic" : "Skills", 136, 94, 90, INK);
        line(g, model.visible().size() + " / " + model.count(""), 244, 94, 88, MUTED);
        for (int i = 0; i < categories.size(); i++) {
            String name = categoryNames()[i], key = i == 0 ? "" : name;
            categories.get(i).setMessage(Component.literal(name + " (" + model.count(key) + ")"));
        }
        filter.setMessage(Component.literal("Show: " + model.filter().name().toLowerCase(Locale.ROOT)));
        for (int i = 0; i < rows.size(); i++) {
            var e = row(i); var b = rows.get(i);
            b.visible = e != null; b.active = e != null;
            if (e != null) b.setMessage(Component.literal(e.name() + ". " + status(e.id(), s) +
                (SkillDefinitions.find(e.id()) == null ? "" : ". Level " + s.skillLevel(e.id()) + ". XP " + s.skillXp(e.id()))));
        }
        if (model.visible().isEmpty()) UiText.paragraph(g, "No registered entries match.", 143, 170, 180, 50, .8f, MUTED);
        line(g, model.visible().isEmpty() ? "0 / 0" : (model.offset() + 1) + "-" +
            Math.min(model.offset() + 6, model.visible().size()) + " / " + model.visible().size(), 136, 369, 196, MUTED);
        line(g, "Mana " + number(s.mana()) + " / " + number(s.maxMana()), 136, 380, 196, INK);
        line(g, "Regen " + number(s.manaRegen()) + "/s | drain " + number(s.activeDrain()) + "/s", 136, 390, 196, MUTED);
        detail(g, s);
        updateControls(s);
    }
    /** Row content is drawn after screen widgets, so no custom widget can obscure it. */
    void renderRows(GuiGraphics g) {
        var s = state();
        for (int i = 0; i < rows.size(); i++) {
            var e = row(i); if (e == null) continue;
            int y = 149 + i * 36;
            if (e.id().equals(model.selected())) {
                g.fill(136, y + 2, 332, y + 32, 0x663D285F);
                g.fill(135, y + 2, 137, y + 32, VIOLET);
            }
            icon(g, e.id(), 141, y + 6, 22);
            line(g, e.name(), 169, y + 5, 157, INK);
            line(g, status(e.id(), s), 169, y + 16, 157, e.unlocked() ? VIOLET : MUTED);
            if (SkillDefinitions.find(e.id()) != null) {
                line(g, "Lv " + s.skillLevel(e.id()) + " | XP " + s.skillXp(e.id()), 169, y + 25, 157, MUTED);
                int width = (int) Math.round(progress(e.id(), s) * 188);
                g.fill(141, y + 32, 329, y + 33, 0xFF26384E);
                g.fill(141, y + 32, 141 + width, y + 33, VIOLET);
            }
        }
    }
    private double progress(String id, PlayerState s) {
        int level = s.skillLevel(id);
        if (level >= s.skillMaxLevel()) return 1;
        long current = (long) level * level * s.skillXpPerLevel();
        long next = (long) (level + 1) * (level + 1) * s.skillXpPerLevel();
        return Math.clamp((s.skillXp(id) - current) / (double) (next - current), 0, 1);
    }
    private void icon(GuiGraphics g, String id, int x, int y, int size) {
        var art = AbilityCatalog.find(id);
        var skill = SkillDefinitions.find(id);
        // Training is not an equippable spell; this view only supplies its registered icon.
        if (art == null && skill != null) art = new AbilityCatalog.Art(skill.id(), skill.name(),
            skill.icon(), skill.activationMana(state()), 0, skill.modes(), false);
        if (art != null) AbilityIcons.render(g, art, x, y, size, id.equals(model.selected()));
        else AbilityIcons.render(g, id, x, y, size, id.equals(model.selected()));
    }
    private void detail(GuiGraphics g, PlayerState s) {
        String id = model.selected();
        detailsFocus.active = id != null;
        if (id == null) {
            detailLines = List.of();
            detailsFocus.setMessage(Component.literal("No selected details"));
            return;
        }
        var d = SkillDefinitions.find(id); var art = AbilityCatalog.find(id, s);
        String name = magic && art != null ? art.name() : d == null ? id : d.name();
        icon(g, id, 350, 95, 42);
        UiText.paragraph(g, name, 400, 96, 116, 30, .85f, INK);
        line(g, status(id, s), 350, 141, 166, VIOLET);
        var lines = new ArrayList<String>();
        if (d != null) {
            lines.add(d.description());
            lines.add("Category: " + (magic ? id.equals(SkillDefinitions.RESTORE) ? "Healing" : "Divine" :
                String.join(", ", d.categories())));
            lines.add("Level: " + s.skillLevel(id) + " / " + s.skillMaxLevel());
            long next = (long) Math.min(s.skillMaxLevel(), s.skillLevel(id) + 1);
            lines.add("XP: " + s.skillXp(id) + " / " + next * next * s.skillXpPerLevel() +
                (mastered(id, s) ? " (mastered)" : " (next level)"));
            lines.add("Effects: " + d.effectSummary(s));
            if (d.intrinsic()) {
                lines.add("Activation mana: " + number(d.activationMana(s)));
                lines.add("Upkeep: up to " + number(d.upkeepMana(s)) + "/s at 100% effective output.");
                lines.add("Configured output: " + s.skillOutput(id) + "%. " + (s.skillEnabled(id) ? "Enabled" : "Disabled"));
                lines.add(SkillMagicBrowserModel.SETTING_TIMING);
                lines.add("At selected output: up to " + number(d.upkeepMana(s) * s.skillOutput(id) / 100.0) +
                    " mana/s while effective; the footer reports actual total drain.");
                lines.add("Prerequisite: explicit server grant or /jayceetest authorization. Canonical race / evolution rules unresolved.");
                String origin=s.divinity().getCompound("Grants").getString(id);
                lines.add("Origin: " + (s.supreme() ? "Unique Jaycee authority." : !origin.isEmpty() ? origin :
                    s.jayceeTest() ? "Test authorization, not a learned patron grant." : "No intrinsic grant."));
            } else {
                lines.add("Prerequisite: Sapphire unlock; use-based progression.");
                lines.add("Patron origin: none registered.");
            }
            lines.add("Mastery: effective server-validated use; server-configured XP curve. No learned patron powers awarded.");
            for (int i = 0; i < d.modes().size(); i++)
                lines.add("Mode: " + d.modes().get(i) + (!granted(id, s) ? " (requires authorization)" :
                    !d.intrinsic() || s.skillLevel(id) >= d.minimumModeLevel(s, i) ? " (available)" :
                        " (requires level " + d.minimumModeLevel(s, i) + ")"));
            if (!d.styles().isEmpty()) lines.add("Flight styles: " + String.join(", ", d.styles()) + ". Current: " + s.skillStyle(id));
        } else {
            lines.add("Legacy art preview only. Canonical discipline mapping unresolved; not learned or executable.");
            lines.add("Mastery / prerequisites / patron origin: unresolved.");
        }
        if (magic && art != null) {
            if (art.implemented()) {
                lines.add("Mana: " + number(d == null ? art.manaCost() : d.activationMana(s)) + " on activation");
                lines.add("Cooldown: " + number(art.cooldownTicks() / 20.0) + "s");
                lines.add("Range: " + (id.equals(SkillDefinitions.PERCEPTION) ? "See detection effect above" : "Self"));
                lines.add("Area: " + (id.equals(SkillDefinitions.PERCEPTION) ? "Visible nonplayer targets only" : "Self only"));
                lines.add("Cast time: immediate (synchronous server handler)");
                lines.add("Modes: " + String.join(", ", art.modes()));
                int slot = selectedSlot.getAsInt();
                if (id.equals(s.ability(s.page(), slot))) {
                    lines.add("Selected slot configured mode: " + art.modes().get(s.mode(s.page(), slot)));
                } else lines.add("Selected slot: this spell is not equipped here.");
                long now = Minecraft.getInstance().level == null ? s.serverTick() : Minecraft.getInstance().level.getGameTime();
                lines.add("Shared cooldown remaining: " + number(s.cooldownRemaining(id, now) / 20.0) + "s");
                lines.add("Select, then click an existing Equip Page slot. Alt + slot cycles unlocked modes, without casting.");
            } else {
                lines.add("Mana / cooldown / range / area / cast time / effects / modes: unresolved; preview cannot activate.");
            }
        }
        lines.add("Evolution / fusion requirements: not implemented.");
        lines.add("Essence: transferable bestowal reserve. EP: permanent power, not casting fuel. Inspect Divine Bonds (N). Natural earning rules remain unresolved.");
        lines.add("Scroll details or focus and use Page Up / Down.");
        detailsFocus.setMessage(Component.literal(SkillMagicBrowserModel.detailNarration(name, status(id, s), lines)));
        var wrapped = new ArrayList<FormattedCharSequence>();
        for (String line : lines) wrapped.addAll(Minecraft.getInstance().font.split(Component.literal(line), 207));
        detailLines = wrapped;
        model.detailScroll(0, detailLines.size());
        var r = SkillMagicLayout.TEXT;
        g.enableScissor(r.x(), r.y(), r.x() + r.width(), r.y() + r.height());
        g.pose().pushPose(); g.pose().translate(r.x(), r.y(), 0); g.pose().scale(.8f, .8f, 1);
        for (int i = 0; i < SkillMagicLayout.DETAIL_LINES && i + model.detailOffset() < detailLines.size(); i++)
            g.drawString(Minecraft.getInstance().font, detailLines.get(i + model.detailOffset()), 0, i * 13, INK, false);
        g.pose().popPose(); g.disableScissor();
        line(g, (model.detailOffset() + 1) + "-" + Math.min(detailLines.size(), model.detailOffset() + SkillMagicLayout.DETAIL_LINES) +
            " / " + detailLines.size(), 350, 317, 166, MUTED);
    }
    private boolean runtimeUsable(PlayerState s) {
        var p = Minecraft.getInstance().player;
        return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && s.unlocked() && s.soul() > 0;
    }
    private void updateControls(PlayerState s) {
        var d = SkillDefinitions.find(model.selected());
        boolean intrinsic = d != null && d.intrinsic();
        for (var b : List.of(toggle, minus, plus, mode, style)) { b.visible = intrinsic; b.active = false; }
        if (!intrinsic) return;
        String id = d.id();
        boolean allowed = runtimeUsable(s) && s.skillGranted(id);
        long now = Minecraft.getInstance().level == null ? s.serverTick() : Minecraft.getInstance().level.getGameTime();
        toggle.active = s.skillGranted(id) && (s.skillEnabled(id) || allowed &&
            s.skillOutput(id) > 0 && (!id.equals(SkillDefinitions.FLIGHT) ||
                !Minecraft.getInstance().player.getAbilities().mayfly) &&
            s.mana() >= d.activationMana(s) && s.cooldownRemaining(id, now) == 0);
        toggle.setMessage(Component.literal((s.skillEnabled(id) ? "Disable" : "Enable") + " intrinsic"));
        minus.active = allowed && s.skillOutput(id) > 0; plus.active = allowed && s.skillOutput(id) < 100;
        mode.active = allowed && d.modes().size() > 1 && s.skillLevel(id) >= d.minimumModeLevel(s, 1);
        style.active = allowed && d.styles().size() > 1;
        mode.visible = d.modes().size() > 1;
        style.visible = d.styles().size() > 1;
        mode.setMessage(Component.literal("Mode: " + (d.modes().isEmpty() ? "N/A" : d.modes().get(s.skillMode(id)))));
        style.setMessage(Component.literal("Style: " + (d.styles().isEmpty() ? "N/A" : s.skillStyle(id))));
    }
    void renderOutput(GuiGraphics g) {
        var d = SkillDefinitions.find(model.selected());
        if (d != null && d.intrinsic()) line(g, state().skillOutput(d.id()) + "%", 393, 360, 80, INK);
    }
    private void output(int delta) {
        String id = model.selected(); if (id != null) GameplayActions.skillOutput(id, Math.clamp(state().skillOutput(id) + delta, 0, 100));
    }
    private String outputHint() {
        var d = SkillDefinitions.find(model.selected());
        return d == null ? "No intrinsic selected" : d.name() + ". Server-confirmed configured output: " +
            state().skillOutput(d.id()) + "%. " + SkillMagicBrowserModel.SETTING_TIMING;
    }
    private void cycleMode() {
        var d = SkillDefinitions.find(model.selected()); if (d == null || d.modes().isEmpty()) return;
        var s = state(); int next = (s.skillMode(d.id()) + 1) % d.modes().size();
        if (s.skillLevel(d.id()) < d.minimumModeLevel(s, next)) next = 0;
        GameplayActions.skillMode(d.id(), next);
    }
    private void cycleStyle() {
        var d = SkillDefinitions.find(model.selected()); if (d == null || d.styles().isEmpty()) return;
        GameplayActions.skillStyle(d.id(), (d.styles().indexOf(state().skillStyle(d.id())) + 1) % d.styles().size());
    }
    boolean scroll(double x, double y, double delta) {
        int step = delta < 0 ? 1 : -1;
        if (delta == 0) return false;
        if (SkillMagicLayout.ROWS.contains(x, y)) { model.scroll(step); return true; }
        if (SkillMagicLayout.DETAILS.contains(x, y)) { model.detailScroll(step * 3, detailLines.size()); return true; }
        return false;
    }
    boolean keyPressed(int key) {
        if (search != null && search.isFocused()) return false;
        if (key == 266 || key == 267) {
            model.detailScroll((key == 266 ? -1 : 1) * SkillMagicLayout.DETAIL_LINES, detailLines.size());
            return true;
        }
        if (detailsFocus.isFocused() && (key == 264 || key == 265)) {
            model.detailScroll(key == 264 ? 1 : -1, detailLines.size());
            return true;
        }
        if (key == 264 || key == 265) {
            var list = model.visible(); if (list.isEmpty()) return true;
            int index = 0;
            for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(model.selected())) index = i;
            index = Math.clamp(index + (key == 264 ? 1 : -1), 0, list.size() - 1);
            model.select(list.get(index).id());
            if (index < model.offset()) model.scroll(index - model.offset());
            else if (index >= model.offset() + 6) model.scroll(index - model.offset() - 5);
            return true;
        }
        return false;
    }
    String tooltip(double x, double y, boolean focused) {
        for (var h : hints) {
            var b = h.widget();
            if (b.visible && (focused ? b.isFocused() : x >= b.getX() && x < b.getX() + b.getWidth() &&
                y >= b.getY() && y < b.getY() + b.getHeight())) return h.text().get();
        }
        if (!focused && SkillMagicLayout.DETAILS.contains(x, y) && y < 154) {
            var d = SkillDefinitions.find(model.selected()); var a = AbilityCatalog.find(model.selected());
            return a != null && magic ? a.name() : d != null ? d.name() : null;
        }
        return null;
    }
    private static void line(GuiGraphics g, String value, int x, int y, int width, int color) {
        UiText.line(g, value, x, y, width, .72f, color, false);
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
}

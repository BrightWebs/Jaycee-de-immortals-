package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.network.BrightNetwork;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Existence System menu (M). Everything is laid out on a 1536x1024 design canvas (the
 * size of the artwork) and scaled to fit the window. The artwork is baked into textures; this
 * class draws the live parts on top: your skin, name, race, title, rank, EP, the bars, presets
 * and equipped skills.
 */
public class JayceeScreen extends Screen {
    private static final Identifier STATUS_BG = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/gui/status_bg.png");
    private static final Identifier BLANK_BG = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/gui/blank_bg.png");
    private static final int W = 1536, H = 1024;

    private static final int STATUS = 0, STATS = 1, SKILLS = 2, MAGIC = 3, EVOLUTION = 4;
    private static final int[][] TABS = {{118, 133, 452, 194}, {470, 133, 778, 194}, {796, 133, 1116, 194}, {1136, 133, 1450, 194}};
    private static final int[] CARD_X = {851, 1006, 1156, 1306};
    private static final int CARD_W = 136;
    private static final String[] FILTERS = {"ALL", "UNIQUE", "EXTRA", "COMMON", "INTRINSIC", "RESIST"};

    private static int tab = STATUS;
    private static Identifier picked;
    private static int filter = 0, scroll = 0;

    private float k;
    private int left, top;

    public JayceeScreen() {
        super(Component.translatable("jaycee.menu.title"));
    }

    @Override
    protected void init() {
        k = Math.min(width * 0.96f / W, height * 0.96f / H);
        left = Math.round((width - W * k) / 2);
        top = Math.round((height - H * k) / 2);
    }

    // ---- helpers (all coordinates in design pixels) -----------------------------------

    private static LocalPlayer player() { return Minecraft.getInstance().player; }
    private static void send(CustomPacketPayload p) { ClientPacketDistributor.sendToServer(p); }

    private void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int u, int v, int w, int h, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, w, h, tw, th);
    }

    /** Text whose capital letters are about {@code size} design pixels tall, centred on y. align: 0 left, 1 centre, 2 right. */
    private void text(GuiGraphicsExtractor g, Component c, float x, float y, float size, int color, int align) {
        float s = size / 7f;
        int w = font.width(c);
        g.pose().pushMatrix();
        g.pose().translate(x, y - 3.5f * s);
        g.pose().scale(s, s);
        g.text(font, c, align == 0 ? 0 : align == 1 ? -w / 2 : -w, 0, color, false);
        g.pose().popMatrix();
    }

    private void text(GuiGraphicsExtractor g, String s, float x, float y, float size, int color, int align) {
        text(g, Component.literal(s).withStyle(ChatFormatting.BOLD), x, y, size, color, align);
    }

    private void plain(GuiGraphicsExtractor g, String s, float x, float y, float size, int color, int align) {
        text(g, Component.literal(s), x, y, size, color, align);
    }

    /** Width a string takes at a given size, in design pixels. */
    private float width(String s, float size, boolean bold) {
        Component c = bold ? Component.literal(s).withStyle(ChatFormatting.BOLD) : Component.literal(s);
        return font.width(c) * size / 7f;
    }

    private String fit(String s, float size, float max) {
        if (width(s, size, true) <= max) return s;
        while (s.length() > 1 && width(s + "…", size, true) > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void panel(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, String title) {
        g.fill(x1, y1, x2, y2, 0xE6081626);
        g.fill(x1, y1, x2, y1 + 2, 0xFF3FC8F0);
        g.fill(x1, y2 - 2, x2, y2, 0xFF2F98C8);
        g.fill(x1, y1, x1 + 2, y2, 0xFF2F98C8);
        g.fill(x2 - 2, y1, x2, y2, 0xFF2F98C8);
        if (title != null) {
            g.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 44, 0xFF0C2A44);
            g.fill(x1 + 2, y1 + 44, x2 - 2, y1 + 46, 0xFF2F98C8);
            g.fill(x1 + 14, y1 + 14, x1 + 20, y1 + 34, 0xFF4FE3FF);
            text(g, title, x1 + 32, y1 + 24, 18, 0xFFCFEFFF, 0);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double ratio, int color) {
        g.fill(x, y, x + w, y + h, 0xFF081626);
        g.fill(x, y, x + w, y + 1, 0xFF1F5070);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF1F5070);
        int f = (int) Math.round(w * Math.max(0, Math.min(1, ratio)));
        if (f > 0) g.fill(x, y + 1, x + f, y + h - 1, color);
    }

    private static boolean in(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    private static String num(double v) {
        return String.format(Locale.US, "%,d", (long) Math.floor(v));
    }

    private static String pct(double v, double max) {
        return String.format(Locale.US, "(%.1f%%)", max <= 0 ? 0 : 100 * v / max).replace(".0%", "%");
    }

    private static double ratio(double v, double max) { return max <= 0 ? 0 : v / max; }

    // ---- frame --------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        LocalPlayer p = player();
        if (p == null) return;
        // The blurred world background is already drawn by 26.3 before this; don't draw it again.
        g.pose().pushMatrix();
        g.pose().translate(left, top);
        g.pose().scale(k, k);
        blit(g, tab == STATUS ? STATUS_BG : BLANK_BG, 0, 0, 0, 0, W, H, W, H);
        int active = tab == EVOLUTION ? STATUS : tab;
        blit(g, SkillIcons.PARTS, TABS[active][0] - 12, TABS[active][1] - 10, 0, 0, 358, 81, 1024, 512);
        g.pose().popMatrix();

        if (tab == STATUS) {
            // Your own skin, turning to follow the cursor.
            InventoryScreen.extractEntityInInventoryFollowsMouse(g,
                    sx(140), sy(262), sx(420), sy(778), Math.max(10, Math.round(245 * k)), 0.0625f, mx, my, p);
        }

        g.pose().pushMatrix();
        g.pose().translate(left, top);
        g.pose().scale(k, k);
        double ux = (mx - left) / k, uy = (my - top) / k;
        switch (tab) {
            case STATUS -> status(g, p);
            case STATS -> stats(g, p);
            case SKILLS -> skills(g, p, ux, uy);
            case MAGIC -> magic(g);
            default -> evolution(g, p, ux, uy);
        }
        g.pose().popMatrix();
        super.extractRenderState(g, mx, my, dt);
    }

    private int sx(int x) { return left + Math.round(x * k); }
    private int sy(int y) { return top + Math.round(y * k); }

    // ---- STATUS -------------------------------------------------------------------------

    private void status(GuiGraphicsExtractor g, LocalPlayer p) {
        EnergyData e = EnergyAPI.get(p);
        text(g, fit(p.getName().getString(), 26, 280), 510, 357, 26, 0xFFFFFFFF, 0);
        String race = RaceAPI.race(p).map(r -> r.name().getString()).orElse("No race");
        text(g, fit(race, 24, 180), 578, 455, 24, 0xFFFFFFFF, 0);
        String title = StatusData.title(p);
        text(g, fit(title.isEmpty() ? "None" : title, 24, 205), 578, 551, 24, title.isEmpty() ? 0xFF9A6AB8 : 0xFFF070FF, 0);
        String rank = StatusData.rank(e.ep());
        text(g, rank.substring(rank.length() - 1), 537, 648, 20, 0xFFFFD040, 1);
        text(g, rank, 578, 647, 26, 0xFFFFD040, 0);
        text(g, fit(num(e.ep()), 44, 230), 568, 772, 44, 0xFF8FF4FF, 0);

        float hp = p.getHealth(), maxHp = p.getMaxHealth();
        double[][] rows = {{hp, maxHp}, {e.spirit(), e.maxSpirit()}, {e.mana(), e.maxMana()}, {e.aura(), e.maxAura()}};
        int[] cy = {284, 341, 397, 453}, barY = {300, 356, 412, 468};
        for (int i = 0; i < 4; i++) {
            double v = rows[i][0], max = rows[i][1];
            String pc = pct(v, max);
            plain(g, pc, 1416, cy[i], 15, 0xFFB8D8E8, 2);
            text(g, num(Math.ceil(v)) + " / " + num(max), 1404 - width(pc, 15, false), cy[i], 18, 0xFFFFFFFF, 2);
            int f = (int) Math.round(478 * Math.max(0, Math.min(1, ratio(v, max))));
            if (f > 0) blit(g, SkillIcons.PARTS, 937, barY[i], 0, 100 + i * 30, f, 20, 1024, 512);
        }
        text(g, num(StatusData.souls(p)), 1414, 517, 30, 0xFFE070FF, 2);

        AbilityData d = AbilityAPI.get(p);
        int page = d.page();
        blit(g, SkillIcons.PARTS, Math.round(855 + page * 63.5f) - 12, 602, 358, 0, 88, 62, 1024, 512);
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) card(g, p, d, i, CARD_X[i], 700, CARD_W);
    }

    /** One equipped-skill card: icon, name (up to two lines) and rank badge. */
    private void card(GuiGraphicsExtractor g, LocalPlayer p, AbilityData d, int slot, int x, int y, int w) {
        Ability a = BrightCoreClient.abilityIn(d, slot);
        int cx = x + w / 2;
        if (a == null) {
            plain(g, "Empty", cx, y + 92, 18, 0xFF5A86A0, 1);
            return;
        }
        SkillIcons.icon(g, a, cx - 34, y + 34, 68);
        float cd = BrightCoreClient.cooldownFraction(a, d);
        if (cd > 0) {
            g.fill(cx - 34, y + 34, cx + 34, y + 34 + Math.round(68 * cd), 0xB0050A18);
            text(g, String.valueOf(BrightCoreClient.cooldownSeconds(a, d)), cx, y + 68, 22, 0xFFFFFFFF, 1);
        }
        if (a.isToggle() && d.isToggled(a.id())) g.fill(cx - 34, y + 104, cx + 34, y + 106, 0xFF5FE07A);
        String name = a.name().getString();
        List<String> lines = wrap(name, 19, w - 14);
        if (lines.size() == 1) text(g, lines.get(0), cx, y + 118, 19, 0xFFFFFFFF, 1);
        else {
            text(g, lines.get(0), cx, y + 108, 18, 0xFFFFFFFF, 1);
            text(g, fit(lines.get(1), 18, w - 14), cx, y + 128, 18, 0xFFFFFFFF, 1);
        }
        SkillIcons.badge(g, a.type(), cx - 54, y + 143, 108, 27);
    }

    private List<String> wrap(String s, float size, float max) {
        if (width(s, size, true) <= max) return List.of(s);
        int sp = s.lastIndexOf(' ');
        while (sp > 0 && width(s.substring(0, sp), size, true) > max) sp = s.lastIndexOf(' ', sp - 1);
        if (sp <= 0) return List.of(fit(s, size, max));
        return List.of(s.substring(0, sp), s.substring(sp + 1));
    }

    // ---- STATS --------------------------------------------------------------------------

    private void stats(GuiGraphicsExtractor g, LocalPlayer p) {
        panel(g, 110, 222, 760, 880, "BODY");
        var race = RaceAPI.race(p);
        EnergyData e = EnergyAPI.get(p);
        String[][] rows = {
                {"Race", race.map(r -> r.name().getString()).orElse("None")},
                {"Tier", race.map(r -> String.valueOf(r.tier())).orElse("-")},
                {"Alignment", race.map(r -> r.alignment().label()).orElse("-")},
                {"Rank", StatusData.rank(e.ep())},
                {"Max Health", num(p.getMaxHealth())},
                {"Attack", race.map(r -> String.format(Locale.US, "%.1f", r.attack())).orElse("-")},
                {"Attack Speed", race.map(r -> String.format(Locale.US, "%.1f", r.attackSpeed())).orElse("-")},
                {"Speed", race.map(r -> String.format(Locale.US, "%.3f", r.speed())).orElse("-")},
                {"Knockback Resistance", race.map(r -> String.format(Locale.US, "%.0f%%", r.knockbackResistance() * 100)).orElse("-")},
                {"Size", race.map(r -> String.format(Locale.US, "%.2f", r.size())).orElse("-")},
                {"Max Spirit", num(e.maxSpirit())},
                {"Experience Level", String.valueOf(p.experienceLevel)},
        };
        for (int i = 0; i < rows.length; i++) {
            int y = 300 + i * 48;
            if (i > 0) g.fill(130, y - 24, 740, y - 23, 0xFF123A50);
            plain(g, rows[i][0], 134, y, 18, 0xFF7FC8E0, 0);
            text(g, rows[i][1], 736, y, 19, 0xFFFFFFFF, 2);
        }

        panel(g, 780, 222, 1440, 880, "RESISTANCES");
        List<Ability> res = AbilityAPI.owned(p).stream()
                .filter(a -> a.type() == SkillType.RESISTANCE || a.type() == SkillType.NULLIFICATION).toList();
        if (res.isEmpty()) {
            plain(g, "None yet.", 1110, 420, 20, 0xFF7FB8CC, 1);
            plain(g, "Endure a kind of damage long enough", 1110, 470, 16, 0xFF5A86A0, 1);
            plain(g, "and your body learns to resist it.", 1110, 500, 16, 0xFF5A86A0, 1);
        }
        for (int i = 0; i < Math.min(9, res.size()); i++) {
            Ability a = res.get(i);
            int y = 290 + i * 64;
            SkillIcons.icon(g, a, 800, y - 22, 44);
            text(g, fit(a.name().getString(), 18, 380), 856, y - 8, 18, 0xFFFFFFFF, 0);
            bar(g, 856, y + 10, 400, 10, AbilityAPI.masteryFraction(p, a), a.type() == SkillType.NULLIFICATION ? 0xFFFFFFFF : 0xFF9FB8D0);
            plain(g, AbilityAPI.mastery(p, a) + "/" + a.maxMastery(), 1420, y + 15, 14, 0xFF7FB8CC, 2);
            SkillIcons.badge(g, a.type(), 1300, y - 22, 120, 30);
        }
    }

    // ---- SKILLS -------------------------------------------------------------------------

    private List<Ability> listed(LocalPlayer p) {
        List<Ability> out = new ArrayList<>();
        for (Ability a : AbilityAPI.owned(p)) {
            boolean ok = switch (filter) {
                case 1 -> a.type() == SkillType.UNIQUE || a.type() == SkillType.ULTIMATE;
                case 2 -> a.type() == SkillType.EXTRA;
                case 3 -> a.type() == SkillType.COMMON;
                case 4 -> a.type() == SkillType.INTRINSIC;
                case 5 -> a.type() == SkillType.RESISTANCE || a.type() == SkillType.NULLIFICATION;
                default -> true;
            };
            if (ok) out.add(a);
        }
        out.sort((x, y) -> x.type() != y.type() ? Integer.compare(order(x.type()), order(y.type()))
                : x.name().getString().compareTo(y.name().getString()));
        return out;
    }

    private static int order(SkillType t) {
        return switch (t) {
            case ULTIMATE -> 0; case UNIQUE -> 1; case EXTRA -> 2; case COMMON -> 3;
            case INTRINSIC -> 4; case RESISTANCE -> 5; case NULLIFICATION -> 6;
        };
    }

    private static final int ROWS = 8, ROW_H = 62, LIST_Y = 330;

    private void skills(GuiGraphicsExtractor g, LocalPlayer p, double ux, double uy) {
        panel(g, 110, 222, 640, 880, "SKILL LIST");
        for (int i = 0; i < FILTERS.length; i++) {
            int x = 126 + i * 84;
            boolean on = i == filter;
            g.fill(x, 278, x + 78, 312, on ? 0xFF1A5070 : 0xFF0A2236);
            g.fill(x, 278, x + 78, 280, on ? 0xFF7FF6FF : 0xFF2F98C8);
            plain(g, FILTERS[i], x + 39, 296, 11, on ? 0xFFFFFFFF : 0xFF9FD8EA, 1);
        }
        List<Ability> list = listed(p);
        scroll = Math.max(0, Math.min(scroll, list.size() - ROWS));
        if (list.isEmpty()) plain(g, "No skills here.", 375, 420, 18, 0xFF7FB8CC, 1);
        for (int i = 0; i < Math.min(ROWS, list.size() - scroll); i++) {
            Ability a = list.get(i + scroll);
            int y = LIST_Y + i * ROW_H;
            boolean sel = a.id().equals(picked), hover = in(ux, uy, 124, y, 626, y + ROW_H - 6);
            g.fill(124, y, 626, y + ROW_H - 6, sel ? 0xFF2A2410 : hover ? 0xFF0E2E46 : 0xFF0A1C2E);
            g.fill(124, y, 128, y + ROW_H - 6, sel ? 0xFFFFC24A : 0xFF2F98C8);
            SkillIcons.icon(g, a, 138, y + 5, 46);
            text(g, fit(a.name().getString(), 18, 290), 196, y + 28, 18, sel ? 0xFFFFE2A0 : 0xFFFFFFFF, 0);
            SkillIcons.badge(g, a.type(), 512, y + 14, 104, 26);
        }
        if (list.size() > ROWS) {
            int track = ROWS * ROW_H - 6, th = Math.max(30, track * ROWS / list.size());
            int ty = LIST_Y + (track - th) * scroll / Math.max(1, list.size() - ROWS);
            g.fill(630, LIST_Y, 634, LIST_Y + track, 0xFF0A2236);
            g.fill(630, ty, 634, ty + th, 0xFF3FC8F0);
        }

        panel(g, 660, 222, 1440, 880, "SKILL DETAIL");
        Ability a = picked == null ? null : AbilityAPI.owned(p).stream().filter(x -> x.id().equals(picked)).findFirst().orElse(null);
        if (a == null) {
            plain(g, "Select a skill on the left.", 1050, 450, 20, 0xFF7FB8CC, 1);
        } else {
            g.fill(684, 288, 812, 416, 0xFF0A1C2E);
            SkillIcons.icon(g, a, 690, 294, 116);
            text(g, fit(a.name().getString().toUpperCase(Locale.ROOT), 32, 590), 836, 318, 32, 0xFFFFE2A0, 0);
            String kind = a.type().label() + " Skill  ·  " + a.category().label()
                    + (a.isPassive() ? "  ·  Passive" : a.isToggle() ? "  ·  Toggle" : "")
                    + (a.modes().size() > 1 ? "  ·  Mode " + (AbilityAPI.modeOf(p, a) + 1) + "/" + a.modes().size() : "");
            plain(g, kind, 836, 364, 16, 0xFFFFC24A, 0);
            SkillIcons.badge(g, a.type(), 836, 384, 120, 30);
            float ds = 17;
            List<FormattedCharSequence> lines = font.split(a.description(), (int) (700 / (ds / 7f)));
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                float s = ds / 7f;
                g.pose().pushMatrix();
                g.pose().translate(688, 446 + i * 32 - 3.5f * s);
                g.pose().scale(s, s);
                g.text(font, lines.get(i), 0, 0, 0xFFCFEAF5, false);
                g.pose().popMatrix();
            }
            plain(g, "MASTERY", 688, 588, 14, 0xFF7FB8CC, 0);
            plain(g, AbilityAPI.mastery(p, a) + " / " + a.maxMastery(), 1416, 588, 14, 0xFF7FB8CC, 2);
            bar(g, 688, 602, 728, 14, AbilityAPI.masteryFraction(p, a), 0xFFFFC24A);
            int n = a.modes().size(), cur = AbilityAPI.modeOf(p, a);
            if (!a.isPassive()) {
                int mw = (728 - (n - 1) * 12) / n;
                for (int i = 0; i < n; i++) {
                    int x = 688 + i * (mw + 12);
                    var m = a.mode(i);
                    g.fill(x, 634, x + mw, 694, 0xFF0A1C2E);
                    g.fill(x, 634, x + mw, 636, i == cur ? 0xFFFFC24A : 0xFF2F98C8);
                    text(g, fit(a.modeName(i).getString().toUpperCase(Locale.ROOT), 15, mw - 20), x + 12, 654, 15, 0xFFFFFFFF, 0);
                    StringBuilder cost = new StringBuilder();
                    if (m.manaCost() > 0) cost.append(m.manaCost()).append(" Mana  ");
                    if (m.auraCost() > 0) cost.append(m.auraCost()).append(" Aura  ");
                    if (m.cooldownTicks() > 0) cost.append(m.cooldownTicks() / 20f).append("s");
                    plain(g, cost.length() == 0 ? "Free" : cost.toString().trim(), x + 12, 678, 13, 0xFF9FD8EA, 0);
                }
            }
        }
        // Equip row: the four slots of the current preset.
        AbilityData d = AbilityAPI.get(p);
        plain(g, a == null || a.isPassive() ? "PRESET " + (d.page() + 1) + "  ·  right-click a slot to clear it"
                : "Click a slot to equip " + a.name().getString() + "  ·  preset " + (d.page() + 1), 688, 722, 14, 0xFF7FB8CC, 0);
        String[] keys = {"Z", "X", "C", "V"};
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            int x = 688 + i * 186, y = 740;
            boolean hover = in(ux, uy, x, y, x + 174, y + 124);
            g.fill(x, y, x + 174, y + 124, hover ? 0xFF0E2E46 : 0xFF0A1C2E);
            g.fill(x, y, x + 174, y + 2, 0xFF3FC8F0);
            g.fill(x + 8, y + 10, x + 38, y + 38, 0xFF050D18);
            text(g, keys[i], x + 23, y + 24, 15, 0xFFFFFFFF, 1);
            Ability s = BrightCoreClient.abilityIn(d, i);
            if (s == null) plain(g, "Empty", x + 87, y + 66, 16, 0xFF5A86A0, 1);
            else {
                SkillIcons.icon(g, s, x + 62, y + 12, 50);
                text(g, fit(s.name().getString(), 15, 160), x + 87, y + 80, 15, 0xFFFFFFFF, 1);
                SkillIcons.badge(g, s.type(), x + 37, y + 92, 100, 25);
            }
        }
    }

    // ---- MAGIC --------------------------------------------------------------------------

    private void magic(GuiGraphicsExtractor g) {
        panel(g, 110, 222, 1440, 880, "MAGIC");
        text(g, "NO MAGIC LEARNED", 775, 500, 30, 0xFF8FF4FF, 1);
        plain(g, "Spells arrive in a later update.", 775, 556, 18, 0xFF7FB8CC, 1);
    }

    // ---- EVOLUTION (opened from the Race field) -------------------------------------------

    private void evolution(GuiGraphicsExtractor g, LocalPlayer p, double ux, double uy) {
        panel(g, 110, 222, 620, 880, "CURRENT FORM");
        var race = RaceAPI.race(p);
        if (race.isEmpty()) {
            plain(g, "No race chosen.", 365, 420, 20, 0xFF7FB8CC, 1);
            return;
        }
        Race r = race.get();
        text(g, fit(r.name().getString().toUpperCase(Locale.ROOT), 30, 460), 365, 320, 30, 0xFFFFE2A0, 1);
        plain(g, "Tier " + r.tier() + "  ·  " + r.alignment().label() + (r.isDivine() ? "  ·  Divine" : ""), 365, 366, 17, 0xFF9FD8EA, 1);
        List<FormattedCharSequence> lines = font.split(r.description(), (int) (440 / (16 / 7f)));
        for (int i = 0; i < Math.min(6, lines.size()); i++) {
            float s = 16 / 7f;
            g.pose().pushMatrix();
            g.pose().translate(140, 420 + i * 30 - 3.5f * s);
            g.pose().scale(s, s);
            g.text(font, lines.get(i), 0, 0, 0xFFCFEAF5, false);
            g.pose().popMatrix();
        }
        plain(g, "← Back to Status", 140, 850, 16, 0xFF7FC8E0, 0);

        panel(g, 640, 222, 1440, 880, "EVOLUTION");
        var next = r.evolutions();
        if (next.isEmpty()) {
            text(g, r.isDivine() ? "THE PEAK: DIVINITY" : "NO FURTHER EVOLUTION", 1040, 480, 26, 0xFFFFD040, 1);
            return;
        }
        for (int i = 0; i < Math.min(3, next.size()); i++) {
            var evo = next.get(i);
            Race t = RaceRegistry.get(evo.target());
            int y = 290 + i * 190;
            g.fill(660, y, 1420, y + 172, 0xFF0A1C2E);
            g.fill(660, y, 1420, y + 2, 0xFF3FC8F0);
            text(g, t == null ? "?" : t.name().getString(), 684, y + 32, 24, 0xFFFFFFFF, 0);
            if (t != null) plain(g, "Tier " + t.tier() + "  ·  " + t.alignment().label(), 684, y + 64, 15, 0xFF9FD8EA, 0);
            double ep = EnergyAPI.get(p).ep();
            boolean epOk = ep >= evo.epNeeded(), xpOk = p.isCreative() || p.experienceLevel >= evo.xpLevels();
            plain(g, (epOk ? "✔ " : "✘ ") + "Reach " + num(evo.epNeeded()) + " EP  (" + num(ep) + ")", 684, y + 102, 16, epOk ? 0xFF5FE07A : 0xFFFF7A8A, 0);
            plain(g, (xpOk ? "✔ " : "✘ ") + "Reach XP level " + evo.xpLevels() + "  (" + p.experienceLevel + ")", 684, y + 134, 16, xpOk ? 0xFF5FE07A : 0xFFFF7A8A, 0);
            boolean ready = RaceAPI.blocker(p, evo).isEmpty();
            boolean hover = in(ux, uy, 1210, y + 100, 1400, y + 150);
            g.fill(1210, y + 100, 1400, y + 150, ready ? (hover ? 0xFF2A7090 : 0xFF1A5070) : 0xFF0E1C28);
            g.fill(1210, y + 100, 1400, y + 102, ready ? 0xFF7FF6FF : 0xFF2A4A60);
            text(g, "EVOLVE", 1305, y + 126, 20, ready ? 0xFFFFFFFF : 0xFF4A6A80, 1);
        }
    }

    // ---- input --------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        LocalPlayer p = player();
        if (p == null) return super.mouseClicked(event, doubleClick);
        double x = (event.x() - left) / k, y = (event.y() - top) / k;
        int button = event.button();
        if (button == 0) {
            for (int i = 0; i < TABS.length; i++) {
                if (in(x, y, TABS[i][0], TABS[i][1], TABS[i][2], TABS[i][3])) {
                    tab = i;
                    return true;
                }
            }
        }
        AbilityData d = AbilityAPI.get(p);
        switch (tab) {
            case STATUS -> {
                if (button == 0 && in(x, y, 494, 430, 798, 480)) { tab = EVOLUTION; return true; }
                for (int i = 0; i < AbilityData.PAGES; i++) {
                    int px = Math.round(855 + i * 63.5f);
                    if (button == 0 && in(x, y, px, 613, px + 64, 655)) { send(new BrightNetwork.SetPage(i)); return true; }
                }
                for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
                    if (in(x, y, CARD_X[i], 700, CARD_X[i] + CARD_W, 878)) {
                        if (button == 1) send(new BrightNetwork.AssignSlot(d.page() * AbilityData.SLOTS_PER_PAGE + i, ""));
                        else tab = SKILLS;
                        return true;
                    }
                }
            }
            case SKILLS -> {
                for (int i = 0; i < FILTERS.length; i++) {
                    int fx = 126 + i * 84;
                    if (button == 0 && in(x, y, fx, 278, fx + 78, 312)) { filter = i; scroll = 0; return true; }
                }
                List<Ability> list = listed(p);
                for (int i = 0; i < Math.min(ROWS, list.size() - scroll); i++) {
                    int ry = LIST_Y + i * ROW_H;
                    if (button == 0 && in(x, y, 124, ry, 626, ry + ROW_H - 6)) { picked = list.get(i + scroll).id(); return true; }
                }
                for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
                    int sx = 688 + i * 186;
                    if (in(x, y, sx, 740, sx + 174, 864)) {
                        int index = d.page() * AbilityData.SLOTS_PER_PAGE + i;
                        if (button == 1) send(new BrightNetwork.AssignSlot(index, ""));
                        else if (picked != null) send(new BrightNetwork.AssignSlot(index, picked.toString()));
                        return true;
                    }
                }
            }
            case EVOLUTION -> {
                if (button == 0 && in(x, y, 130, 830, 420, 870)) { tab = STATUS; return true; }
                var next = RaceAPI.next(p);
                for (int i = 0; i < Math.min(3, next.size()); i++) {
                    int ey = 290 + i * 190;
                    if (button == 0 && in(x, y, 1210, ey + 100, 1400, ey + 150) && RaceAPI.blocker(p, next.get(i)).isEmpty()) {
                        send(new BrightNetwork.Evolve(next.get(i).target().toString()));
                        return true;
                    }
                }
            }
            default -> {}
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (tab == SKILLS && dy != 0) {
            scroll += dy < 0 ? 1 : -1;
            return true;
        }
        if (dy != 0 && Minecraft.getInstance().hasAltDown()) {
            LocalPlayer p = player();
            if (p != null) send(new BrightNetwork.SetPage(AbilityAPI.get(p).page() + (dy < 0 ? 1 : AbilityData.PAGES - 1)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

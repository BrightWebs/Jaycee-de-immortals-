package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.network.BrightNetwork;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.race.JayceeRaces;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/** Reincarnation: pick one of the 24 starting races. Shown on first join (and if your race is gone). */
public class RaceSelectScreen extends Screen {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final int W = 1586, H = 992;
    private static final String[] FILTERS = {"all", "humans", "demihumans", "beastfolk", "monsters", "undead", "spiritual"};
    private static final int[][] COLS = {{335, 497}, {513, 675}, {691, 853}, {869, 1031}};
    private static final int[][] ROWS = {{245, 392}, {410, 558}, {577, 725}, {742, 892}};
    private static final int CHIP_W = 96;

    private String filter = "all", selected = "human";
    private int scroll;
    private float k;
    private int left, top;

    public RaceSelectScreen() { super(Component.translatable("jaycee.menu.title")); }

    @Override
    protected void init() {
        k = Math.min(width * 0.96f / W, height * 0.92f / H);
        left = Math.round((width - W * k) / 2);
        top = Math.round((height - H * k) / 2);
    }

    private List<String> races() {
        return JayceeRaces.STARTING.stream().filter(id -> filter.equals("all") || JayceeRaces.INFO.get(id).group().equals(filter)).toList();
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        double x = (mx - left) / k, y = (my - top) / k;
        g.pose().pushMatrix();
        g.pose().translate(left, top);
        g.pose().scale(k, k);
        UI.tex(g, "bg_race_select", 0, 0, W, H);
        // filters
        for (int i = 0; i < FILTERS.length; i++) {
            float cx = 328 + i * (CHIP_W + 6);
            boolean on = FILTERS[i].equals(filter), hov = !on && Page.in(x, y, cx + 6, 162, cx + 6 + CHIP_W, 206);
            UI.tex(g, on ? "chip_1" : hov ? "chip_h" : "chip_0", cx, 156);
            Component label = Component.translatable("jaycee.ui.filter." + FILTERS[i]);
            UI.semi(g, label, cx + 54, 185, UI.fitSize(label.getString(), 17, 12, 84, false), on ? 0xFFFFFFFF : hov ? 0xFFFFFFFF : 0xFFE0ECF4, 1);
        }
        // tiles
        List<String> list = races();
        int rows = (list.size() + 3) / 4;
        scroll = Math.max(0, Math.min(scroll, rows - 4));
        for (int i = 0; i < 16; i++) {
            int idx = scroll * 4 + i;
            if (idx >= list.size()) break;
            String id = list.get(idx);
            int[] c = COLS[i % 4], r = ROWS[i / 4];
            boolean on = id.equals(selected), hov = !on && Page.in(x, y, c[0], r[0], c[1], r[1]);
            UI.tex(g, on ? "rtile_sel" : hov ? "rtile_h" : "rtile", c[0] - 3, r[0] - 3);
            UI.tex(g, "../race/face/" + id, c[0] + 3, r[0] + 3, c[1] - c[0] - 6, r[1] - r[0] - 40);
            UI.fill(g, c[0] + 3, r[1] - 37, c[1] - 3, r[1] - 3, 0xF0060C18);
            String name = Component.translatable("race.jaycee." + id).getString();
            UI.text(g, name, (c[0] + c[1]) / 2f, r[1] - 20, UI.fitSize(name, 23, 14, c[1] - c[0] - 14, true), 0xFFFFFFFF, 1);
            pill(g, "pill_", JayceeRaces.INFO.get(id).difficulty(), c[0] + 4, r[0] + 4, false);
        }
        if (rows > 4) {
            float th = 640f * 4 / rows, ty = 250 + (640 - th) * scroll / (rows - 4);
            UI.fill(g, 1048, 250, 1053, 890, 0xFF0A2232);
            UI.fill(g, 1048, ty, 1053, ty + th, 0xFF3FE0FF);
        }
        // the chosen race, full body
        int[] bs = UI.size("../race/body/" + selected);
        float bh = 600, bw = bh * bs[0] / bs[1];
        if (bw > 300) { bw = 300; bh = bw * bs[1] / bs[0]; }
        UI.tex(g, "../race/body/" + selected, 165 - bw / 2, 790 - bh, bw, bh);
        details(g);
        labels(g);
        confirm(g, x, y);
        g.pose().popMatrix();
        super.extractRenderState(g, mx, my, dt);
    }

    /** Difficulty pill: the blank pill art with its word drawn live. */
    private static void pill(GuiGraphicsExtractor g, String prefix, String diff, float x, float y, boolean full) {
        String tex = prefix + diff;
        int[] s = UI.size(tex);
        UI.tex(g, tex, x, y);
        Component word = Component.translatable("jaycee.ui.difficulty." + diff);
        String text = (full ? Component.translatable("jaycee.ui.difficulty").getString() + ": " : "") + word.getString().toUpperCase(java.util.Locale.ROOT);
        UI.text(g, text, x + s[0] / 2f, y + s[1] / 2f, full ? 13 : 12, 0xFF0A1A10, 1);
    }

    /** The fixed headings, drawn as text so they stay sharp at any size. */
    private static void labels(GuiGraphicsExtractor g) {
        UI.semi(g, Component.translatable("jaycee.ui.rs.subtitle"), 788, 110, 26, 0xFFFFFFFF, 1);
        UI.text(g, Component.translatable("jaycee.ui.rs.lifeform"), 1112, 208, 20, 0xFF3FEFFF, 0);
        String[] heads = {"strengths", "limitations", "potential", "intrinsic"};
        float[] hy = {344, 455.5f, 565.5f, 752};
        float[] hw = {104, 134, 158, 230}; // inside of each header chip
        for (int i = 0; i < 4; i++) {
            Component h = Component.translatable("jaycee.ui.rs." + heads[i]);
            UI.text(g, h, 1136, hy[i], UI.fitSize(h.getString(), 23, 14, hw[i], true), 0xFFFFFFFF, 0);
        }
        String[] pots = {"strength", "speed", "magic", "aura"};
        float[] py = {601.5f, 634.5f, 667.5f, 700};
        for (int i = 0; i < 4; i++) {
            Component t = Component.translatable("jaycee.ui.rs.pot." + pots[i]);
            UI.semi(g, t, 1147, py[i], UI.fitSize(t.getString(), 20, 13, 126, false), 0xFFFFFFFF, 0);
        }
    }

    private boolean pressing;

    private static boolean overConfirm(double x, double y) { return x >= 1115 && x < 1530 && y >= 838 && y < 908; }

    /** Confirm Race: its own button, brighter under the mouse and pressed in while held. */
    private void confirm(GuiGraphicsExtractor g, double x, double y) {
        boolean hov = overConfirm(x, y), down = hov && pressing;
        float off = down ? 2 : 0;
        UI.tex(g, down ? "btn_confirm_2" : hov ? "btn_confirm_1" : "btn_confirm_0", 1104, 828 + off);
        UI.text(g, Component.translatable("jaycee.ui.rs.confirm"), 1347.5f, 872 + off, 34, down ? 0xFFD8CCF0 : 0xFFFFFFFF, 1);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean was = pressing;
        pressing = false;
        double x = (event.x() - left) / k, y = (event.y() - top) / k;
        if (was && UI.button(event) == 0 && overConfirm(x, y)) {
            LOG.info("[Jaycee] confirm race {}", selected);
            ClientPacketDistributor.sendToServer(new BrightNetwork.ChooseRace(JayceeMod.ID + ":" + selected));
            MenuScreen.click();
            onClose();
            return true;
        }
        return super.mouseReleased(event);
    }

    private void details(GuiGraphicsExtractor g) {
        JayceeRaces.Info info = JayceeRaces.INFO.get(selected);
        String key = "race.jaycee." + selected;
        UI.text(g, Component.translatable(key), 1130, 162, 36, 0xFFFFFFFF, 0);
        int[] ds = UI.size("pilld_" + info.difficulty());
        pill(g, "pilld_", info.difficulty(), 1550 - ds[0], 194 - ds[1], true);
        UI.semi(g, Component.translatable(key + ".lifeform"), 1218, 209, 20, 0xFFFFFFFF, 0);
        List<String> desc = UI.wrap(Component.translatable(key + ".desc").getString(), 17, 432, false);
        for (int i = 0; i < Math.min(4, desc.size()); i++) UI.semi(g, desc.get(i), 1112, 240 + i * 21.5f, 17, 0xFFF0F0FA, 0);
        String[] st = Component.translatable(key + ".strengths").getString().split("\\n");
        for (int i = 0; i < Math.min(3, st.length); i++) {
            UI.text(g, "◆", 1117, 374 + i * 22, 11, 0xFF5AE6FF, 1);
            UI.semi(g, UI.fit(st[i], 15.5f, 420, false), 1128, 374 + i * 22, 15.5f, 0xFFF0F0FA, 0);
        }
        String[] li = Component.translatable(key + ".limitations").getString().split("\\n");
        for (int i = 0; i < Math.min(3, li.length); i++) {
            UI.text(g, "◆", 1117, 485 + i * 22, 11, 0xFFC878FF, 1);
            UI.semi(g, UI.fit(li[i], 15.5f, 420, false), 1128, 485 + i * 22, 15.5f, 0xFFF0F0FA, 0);
        }
        int[] pot = {info.strength(), info.speed(), info.magic(), info.aura()};
        int[] py = {600, 633, 666, 699};
        for (int i = 0; i < 4; i++) {
            for (int s = 0; s < 8; s++) {
                float x = 1282 + s * 20.5f;
                UI.fill(g, x, py[i] - 7, x + 17, py[i] + 7, s < pot[i] ? 0xFF28D8F0 : 0xFF1A2A3A);
            }
            String word = pot[i] <= 2 ? "low" : pot[i] <= 4 ? "moderate" : pot[i] <= 6 ? "high" : "exceptional";
            UI.semi(g, Component.translatable("jaycee.ui.potential." + word), 1458, py[i], 17, 0xFFFFFFFF, 0);
        }
        // starting intrinsic skills
        var race = com.brightwebs.brightcore.api.race.RaceRegistry.get(Identifier.fromNamespaceAndPath(JayceeMod.ID, selected));
        String intr = race == null || race.intrinsics().isEmpty() ? Component.translatable("jaycee.ui.none").getString()
                : String.join(", ", race.intrinsics().stream().map(id -> {
                    var a = AbilityRegistry.get(id);
                    return a == null ? id.getPath() : a.name().getString();
                }).toList());
        UI.text(g, "◆", 1117, 784, 11, 0xFF5AE6FF, 1);
        UI.semi(g, intr, 1128, 784, 17, 0xFFF0F0FA, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = (event.x() - left) / k, y = (event.y() - top) / k;
        if (UI.button(event) != 0) return false;
        for (int i = 0; i < FILTERS.length; i++) {
            float cx = 328 + i * (CHIP_W + 6);
            if (x >= cx + 6 && x < cx + 6 + CHIP_W && y >= 162 && y < 206) { filter = FILTERS[i]; scroll = 0; MenuScreen.click(); return true; }
        }
        List<String> list = races();
        for (int i = 0; i < 16; i++) {
            int idx = scroll * 4 + i;
            if (idx >= list.size()) break;
            int[] c = COLS[i % 4], r = ROWS[i / 4];
            if (x >= c[0] && x < c[1] && y >= r[0] && y < r[1]) { selected = list.get(idx); MenuScreen.click(); return true; }
        }
        if (overConfirm(x, y)) { pressing = true; return true; }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll += dy < 0 ? 1 : -1;
        return true;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}

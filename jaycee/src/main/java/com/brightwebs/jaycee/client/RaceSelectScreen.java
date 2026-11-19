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
            UI.tex(g, "chip_" + FILTERS[i] + "_" + (FILTERS[i].equals(filter) ? 1 : 0), 328 + i * (CHIP_W + 6), 156);
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
            boolean on = id.equals(selected);
            UI.tex(g, on ? "rtile_sel" : "rtile", c[0] - 3, r[0] - 3);
            UI.tex(g, "../race/face/" + id, c[0] + 3, r[0] + 3, c[1] - c[0] - 6, r[1] - r[0] - 40);
            UI.fill(g, c[0] + 3, r[1] - 37, c[1] - 3, r[1] - 3, 0xF0060C18);
            String name = Component.translatable("race.jaycee." + id).getString();
            UI.text(g, name, (c[0] + c[1]) / 2f, r[1] - 20, UI.fitSize(name, 23, 14, c[1] - c[0] - 14, true), 0xFFFFFFFF, 1);
            UI.tex(g, "diff_" + JayceeRaces.INFO.get(id).difficulty(), c[0] + 4, r[0] + 4);
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
        g.pose().popMatrix();
        super.extractRenderState(g, mx, my, dt);
    }

    private void details(GuiGraphicsExtractor g) {
        JayceeRaces.Info info = JayceeRaces.INFO.get(selected);
        String key = "race.jaycee." + selected;
        UI.text(g, Component.translatable(key), 1130, 162, 36, 0xFFFFFFFF, 0);
        String d = "diffd_" + info.difficulty();
        int[] ds = UI.size(d);
        UI.tex(g, d, 1550 - ds[0], 194 - ds[1]);
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
        if (x >= 1115 && x < 1530 && y >= 838 && y < 908) {
            LOG.info("[Jaycee] confirm race {}", selected);
            ClientPacketDistributor.sendToServer(new BrightNetwork.ChooseRace(JayceeMod.ID + ":" + selected));
            MenuScreen.click();
            onClose();
            return true;
        }
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

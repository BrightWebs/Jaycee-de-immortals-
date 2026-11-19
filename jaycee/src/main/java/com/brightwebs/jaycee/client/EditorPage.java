package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.appearance.Appearance;
import com.brightwebs.jaycee.appearance.Features;
import com.brightwebs.jaycee.network.JayceeNetwork;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Character Editor (side button 2), laid out to the approved design: a live 3D preview you can
 * drag to turn and scroll to zoom, the categories, a thumbnail gallery, the colour panel, quick
 * presets, and Reset / Randomize / Apply. Changes show at once in the preview; Apply sends them to
 * the server, which checks them against your race before everyone sees them.
 */
final class EditorPage extends Page {
    // ---- layout (design pixels of the 1536 x 1024 frame) ----
    private static final float PV_X1 = 100, PV_Y1 = 286, PV_X2 = 430, PV_Y2 = 878;
    private static final float CAT_X = 446, CAT_Y = 292, CAT_W = 170, CAT_H = 52, CAT_STEP = 62;
    private static final float GAL_X = 630, GAL_Y = 286, TILE_W = 82, TILE_H = 84, TILE_X0 = 648, TILE_Y0 = 372, TILE_SX = 88, TILE_SY = 92;
    private static final int COLS = 5, ROWS = 3, PER_PAGE = COLS * ROWS;
    private static final float CP_X = 1124, SQ_X2 = 1374, HUE_X1 = 1386, HUE_X2 = 1406;
    private static final float SQ_Y1 = 346, SQ_Y2 = 432, ST2_Y1 = 528, ST2_Y2 = 552, ST3_Y1 = 596, ST3_Y2 = 620;
    private static final String[] PALETTE = {"1A1A22", "D8DCE8", "F0C850", "C02A2A", "8A4AD8", "3A7BFF", "F08AB8"};
    private static final String[] SWATCHES = {"F4F4F8", "1A1A22", "8A5A2E", "E8C4A8", "52D6FF", "B46CFF", "5FE07A", "E05A6A"};

    private Map<String, String> edit = new LinkedHashMap<>(), applied = Map.of();
    private final Deque<Map<String, String>> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private int cat = 1, tab = 0, pageNo = 0, row = 1;
    private boolean previewMode, showSaved, editingHex, spinning = true, walking;
    private String hex = "";
    private float yaw = 25, pitch = 0, zoom = 1;
    private boolean dragging;
    private final float[] hue = {0, 0.6f, 0.55f, 0.75f};
    private final List<String> recent = new ArrayList<>();
    private List<Map<String, String>> saved;
    private long lastNanos;
    private Object openedFor;

    EditorPage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_editor"; }
    @Override boolean showsPlayer() { return false; }

    // ---- state helpers ---------------------------------------------------------------
    private String race() { return player() == null ? "human" : Appearance.raceId(player()); }

    private void syncFromPlayer() {
        LocalPlayer p = player();
        if (p == null) return;
        Map<String, String> now = Appearance.of(p);
        if (openedFor != p || !now.equals(applied)) {
            boolean unchanged = edit.equals(applied);
            applied = now;
            if (openedFor != p || unchanged) edit = new LinkedHashMap<>(now);
            openedFor = p;
        }
    }

    private void change(Runnable r) {
        undo.push(new LinkedHashMap<>(edit));
        while (undo.size() > 50) undo.removeLast();
        redo.clear();
        r.run();
        edit = new LinkedHashMap<>(Appearance.sanitize(edit, race()));
    }

    private List<String> slots() {
        Features.Category c = Features.CATEGORIES.get(cat);
        List<String> out = new ArrayList<>();
        for (String s : c.slots()) if (s.equals("style") || s.equals("look") || Features.hasChoices(s, race())) out.add(s);
        return out;
    }

    private String slot() {
        List<String> s = slots();
        if (s.isEmpty()) return null;
        return s.get(Math.min(tab, s.size() - 1));
    }

    private List<Features.Option> options() {
        String s = slot();
        return s == null ? List.of() : Features.available(s, race());
    }

    private String group() {
        String s = slot();
        return Features.colorGroup(s == null ? "hair" : s);
    }

    private boolean selected(Features.Option o) {
        if (o.slot().equals("style") || o.slot().equals("look")) {
            Map<String, String> c = Features.combo(o.slot(), o.id());
            if (o.slot().equals("look")) {
                for (String w : Appearance.WORN) {
                    if (w.equals("eyes") && !c.containsKey(w)) continue;
                    String want = c.getOrDefault(w, "none"), have = edit.getOrDefault(w, "none");
                    if (Features.required(race(), w) == null && !want.equals(have)) return false;
                }
                return true;
            }
            for (Map.Entry<String, String> e : c.entrySet()) if (!e.getValue().equals(edit.getOrDefault(e.getKey(), "none"))) return false;
            return true;
        }
        return o.id().equals(edit.getOrDefault(o.slot(), "none"));
    }

    private void choose(Features.Option o) {
        change(() -> {
            if (o.slot().equals("look")) {
                for (String w : Appearance.WORN) edit.remove(w);
                edit.putAll(Features.combo("look", o.id()));
            } else if (o.slot().equals("style")) {
                edit.putAll(Features.combo("style", o.id()));
            } else {
                edit.put(o.slot(), o.id());
            }
        });
    }

    private String colorKey(int r) { return group() + ".c" + r; }

    private int colorOf(int r) {
        String v = edit.get(colorKey(r));
        if (v == null) {
            Features.Option o = currentOption();
            String d = o == null ? null : r == 1 ? o.c1() : o.c2();
            v = d != null ? d.replace("#", "") : Appearance.defaultFor(group(), r);
        }
        try { return 0xFF000000 | Integer.parseInt(v, 16); } catch (NumberFormatException e) { return 0xFFFFFFFF; }
    }

    private Features.Option currentOption() {
        String s = slot();
        if (s == null) return null;
        String real = s.equals("style") || s.equals("look") ? "bangs" : s;
        return Features.get(real, edit.getOrDefault(real, "none"));
    }

    private void setColor(int r, int rgb) {
        String v = String.format("%06X", rgb & 0xFFFFFF);
        if (v.equals(edit.get(colorKey(r)))) return;
        change(() -> edit.put(colorKey(r), v));
    }

    private void remember(int rgb) {
        String v = String.format("%06X", rgb & 0xFFFFFF);
        recent.remove(v);
        recent.add(0, v);
        while (recent.size() > 7) recent.remove(recent.size() - 1);
    }

    // ---- drawing -------------------------------------------------------------------------

    /** The live preview is drawn in screen space, before the scaled page content. */
    @Override
    void drawScreen(GuiGraphicsExtractor g, int mx, int my) {
        syncFromPlayer();
        LocalPlayer p = player();
        if (p == null) return;
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        if (spinning && !dragging) yaw += dt * 25;
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState st = dispatcher.getRenderer(p).createRenderState(p, 1.0f);
        st.shadowPieces.clear();
        st.outlineColor = 0;
        if (st instanceof AvatarRenderState as) {
            as.getModelPartVisibility().clear();
            FeatureRender.apply(as, previewMode ? applied : edit);
        }
        float h = 1.8f;
        if (st instanceof LivingEntityRenderState l) {
            l.bodyRot = 180 + yaw;
            l.yRot = 0;
            l.xRot = 0;
            l.boundingBoxWidth = l.boundingBoxWidth / l.scale;
            l.boundingBoxHeight = l.boundingBoxHeight / l.scale;
            l.scale = 1;
            h = l.boundingBoxHeight;
            if (walking) { l.walkAnimationPos = (System.currentTimeMillis() % 100000) / 90f; l.walkAnimationSpeed = 1; }
        }
        Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(pitch));
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        int size = Math.max(8, Math.round(menu.k() * 230 * zoom));
        g.entity(st, size, new Vector3f(0, h / 2 + 0.05f, 0), rot, tilt,
                menu.sx(PV_X1 + 14), menu.sy(PV_Y1 + 40), menu.sx(PV_X2 - 14), menu.sy(PV_Y2 - 56));
    }

    @Override
    void draw(GuiGraphicsExtractor g, double mx, double my) {
        syncFromPlayer();
        header(g, mx, my);
        previewChrome(g, mx, my);
        categories(g, mx, my);
        if (showSaved) savedGallery(g, mx, my); else gallery(g, mx, my);
        colorPanel(g, mx, my);
        quickPresets(g, mx, my);
        bottomButtons(g, mx, my);
    }

    private static void piece(GuiGraphicsExtractor g, String tex, float x, float y, float w, float h) {
        UI.tex(g, tex, x - 10, y - 10, w + 20, h + 20);
    }

    private static void icon(GuiGraphicsExtractor g, String name, float cx, float cy, float size, int argb) {
        UI.tex(g, "edi_" + name, cx - size / 2, cy - size / 2, size, size, argb);
    }

    private void button(GuiGraphicsExtractor g, double mx, double my, float x, float y, float w, float h, boolean on, String iconName, Component label) {
        boolean hover = in(mx, my, x, y, x + w, y + h);
        piece(g, on || hover ? "ed_btn_on" : "ed_btn", x, y, w, h);
        float tx = x + w / 2;
        if (iconName != null && label != null) {
            float lw = UI.width(label, 15, true);
            icon(g, iconName, x + w / 2 - lw / 2 - 6, y + h / 2, 20, 0xFFFFFFFF);
            tx += 10;
        } else if (iconName != null) {
            icon(g, iconName, x + w / 2, y + h / 2, 22, 0xFFFFFFFF);
        }
        if (label != null) UI.text(g, label, tx, y + h / 2, 15, 0xFFFFFFFF, 1);
    }

    private void header(GuiGraphicsExtractor g, double mx, double my) {
        icon(g, "pencil", 136, 246, 32, 0xFFFFFFFF);
        UI.text(g, Component.translatable("jaycee.editor.title"), 162, 246, 28, 0xFFFFFFFF, 0);
        UI.semi(g, Component.translatable("jaycee.editor.mode"), 640, 246, 16, 0xFFCFE8F5, 1);
        button(g, mx, my, 676, 228, 96, 36, !previewMode, null, Component.translatable("jaycee.editor.edit"));
        button(g, mx, my, 780, 228, 104, 36, previewMode, null, Component.translatable("jaycee.editor.preview"));
        button(g, mx, my, 904, 228, 44, 36, false, "undo", null);
        button(g, mx, my, 954, 228, 44, 36, false, "redo", null);
        button(g, mx, my, 1018, 228, 196, 36, false, "save", Component.translatable("jaycee.editor.save_preset"));
        button(g, mx, my, 1224, 228, 204, 36, showSaved, "load", Component.translatable("jaycee.editor.load_preset"));
    }

    private void previewChrome(GuiGraphicsExtractor g, double mx, double my) {
        icon(g, "mouse", 128, 314, 22, 0xFFCFE8F5);
        UI.semi(g, Component.translatable("jaycee.editor.drag"), 144, 314, 15, 0xFFCFE8F5, 0);
        // zoom
        for (int i = 0; i < 2; i++) {
            float cy = 690 + i * 44;
            boolean hover = in(mx, my, 376, cy - 18, 412, cy + 18);
            UI.fill(g, 378, cy - 16, 410, cy + 16, hover ? 0xFF143450 : 0xC0081424);
            UI.rect(g, 378, cy - 16, 410, cy + 16, 2, 0xFF4FE3FF);
            UI.text(g, i == 0 ? "+" : "−", 394, cy, 22, 0xFFFFFFFF, 1);
        }
        // play / pause / walk
        String[] ic = {"play", "pause", "run"};
        for (int i = 0; i < 3; i++) {
            float x = 186 + i * 60;
            boolean on = i == 0 ? spinning : i == 1 ? !spinning : walking;
            button(g, mx, my, x, 834, 46, 34, on, ic[i], null);
        }
        if (previewMode) UI.semi(g, Component.translatable("jaycee.editor.showing_applied"), 265, 342, 14, 0xFFFFD27A, 1);
    }

    private void categories(GuiGraphicsExtractor g, double mx, double my) {
        for (int i = 0; i < Features.CATEGORIES.size(); i++) {
            Features.Category c = Features.CATEGORIES.get(i);
            float y = CAT_Y + i * CAT_STEP;
            boolean sel = i == cat, hover = in(mx, my, CAT_X, y, CAT_X + CAT_W, y + CAT_H);
            boolean empty = c.id().equals("race") && c.slots().stream().noneMatch(s -> Features.hasChoices(s, race()));
            piece(g, sel || hover ? "ed_cat_sel" : "ed_cat", CAT_X, y, CAT_W, CAT_H);
            int col = empty ? 0xFF6A7A8A : 0xFFFFFFFF;
            icon(g, c.id(), CAT_X + 30, y + CAT_H / 2, 28, col);
            Component name = Component.translatable("jaycee.editor.cat." + c.id());
            UI.text(g, name, CAT_X + 52, y + CAT_H / 2, UI.fitSize(name.getString(), 17, 11, 112, true), col, 0);
        }
    }

    private void gallery(GuiGraphicsExtractor g, double mx, double my) {
        Features.Category c = Features.CATEGORIES.get(cat);
        UI.text(g, Component.translatable("jaycee.editor.cat." + c.id()), GAL_X + 22, GAL_Y + 22, 21, 0xFFFFFFFF, 0);
        List<String> slots = slots();
        if (slots.isEmpty()) {
            UI.semi(g, Component.translatable("jaycee.editor.no_features"), 864, 500, 16, 0xFFB8D8E8, 1);
            UI.semi(g, Component.translatable("jaycee.editor.no_features.hint"), 864, 526, 14, 0xFF7FB8CC, 1);
            return;
        }
        tab = Math.min(tab, slots.size() - 1);
        for (int i = 0; i < slots.size(); i++) {
            float x = 646 + i * 112, y = 330;
            boolean sel = i == tab, hover = in(mx, my, x, y, x + 104, y + 32);
            piece(g, sel || hover ? "ed_tab_sel" : "ed_tab", x, y, 104, 32);
            Component t = Component.translatable("jaycee.editor.slot." + slots.get(i));
            UI.text(g, t, x + 52, y + 16, UI.fitSize(t.getString(), 15, 10, 92, true), 0xFFFFFFFF, 1);
        }
        List<Features.Option> opts = options();
        int pages = Math.max(1, (opts.size() + PER_PAGE - 1) / PER_PAGE);
        pageNo = Math.min(pageNo, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = pageNo * PER_PAGE + i;
            float x = TILE_X0 + (i % COLS) * TILE_SX, y = TILE_Y0 + (i / COLS) * TILE_SY;
            if (idx >= opts.size()) { piece(g, "ed_tile", x, y, TILE_W, TILE_H); continue; }
            Features.Option o = opts.get(idx);
            boolean sel = selected(o), hover = in(mx, my, x, y, x + TILE_W, y + TILE_H);
            piece(g, sel ? "ed_tile_sel" : "ed_tile", x, y, TILE_W, TILE_H);
            if (hover && !sel) UI.fill(g, x + 2, y + 2, x + TILE_W - 2, y + TILE_H - 2, 0x2252D6FF);
            if (o.id().equals("none") || o.id().equals("keep_skin")) {
                Thumb.draw(g, List.of(), b -> 0, o.view(), x + 6, y + 4, TILE_W - 12, TILE_H - 22);
                UI.text(g, "∅", x + TILE_W - 14, y + 14, 16, 0xFF9FB8C8, 1);
            } else {
                Thumb.draw(g, o.boxes(), b -> Appearance.color(edit, o, b.tint()), o.view(), x + 4, y + 2, TILE_W - 8, TILE_H - 18);
            }
            Component n = Component.translatable("jaycee.editor.opt." + o.id());
            UI.semi(g, n, x + TILE_W / 2, y + TILE_H - 9, UI.fitSize(n.getString(), 11, 7, TILE_W - 6, false), 0xFFE6F4FF, 1);
        }
        // scrollbar
        UI.fill(g, 1086, 372, 1090, 640, 0x60346080);
        float barH = 268f / pages;
        UI.fill(g, 1085, 372 + pageNo * barH, 1091, 372 + (pageNo + 1) * barH, 0xFF52D6FF);
        // pager
        button(g, mx, my, 790, 652, 44, 32, false, null, Component.literal("‹"));
        button(g, mx, my, 894, 652, 44, 32, false, null, Component.literal("›"));
        UI.text(g, (pageNo + 1) + " / " + pages, 864, 668, 17, 0xFFFFFFFF, 1);
    }

    private void savedGallery(GuiGraphicsExtractor g, double mx, double my) {
        UI.text(g, Component.translatable("jaycee.editor.saved"), GAL_X + 22, GAL_Y + 22, 21, 0xFFFFFFFF, 0);
        List<Map<String, String>> list = saved();
        if (list.isEmpty()) {
            UI.semi(g, Component.translatable("jaycee.editor.saved.none"), 864, 500, 16, 0xFFB8D8E8, 1);
            return;
        }
        for (int i = 0; i < Math.min(PER_PAGE, list.size()); i++) {
            float x = TILE_X0 + (i % COLS) * TILE_SX, y = TILE_Y0 - 30 + (i / COLS) * TILE_SY;
            Map<String, String> look = list.get(i);
            boolean hover = in(mx, my, x, y, x + TILE_W, y + TILE_H);
            piece(g, hover ? "ed_tile_sel" : "ed_tile", x, y, TILE_W, TILE_H);
            List<Features.Box> all = new ArrayList<>();
            Map<Features.Box, Integer> cols = new HashMap<>();
            for (String w : Appearance.WORN) {
                Features.Option o = Features.get(w, look.getOrDefault(w, "none"));
                if (o == null) continue;
                for (Features.Box b : o.boxes()) { all.add(b); cols.put(b, Appearance.color(look, o, b.tint())); }
            }
            Thumb.draw(g, all, b -> cols.getOrDefault(b, 0xFFFFFFFF), Features.View.BODY, x + 4, y + 2, TILE_W - 8, TILE_H - 18);
            UI.semi(g, Component.translatable("jaycee.editor.preset_n", i + 1), x + TILE_W / 2, y + TILE_H - 9, 11, 0xFFE6F4FF, 1);
        }
    }

    private void colorPanel(GuiGraphicsExtractor g, double mx, double my) {
        String grp = group();
        boolean hair = grp.equals("hair");
        UI.text(g, Component.translatable("jaycee.editor.color"), 1132, 308, 21, 0xFFFFFFFF, 0);
        UI.semi(g, Component.translatable("jaycee.editor.color_of", Component.translatable("jaycee.editor.group." + grp)), CP_X, 334, 15, 0xFFE6F4FF, 0);
        // primary: saturation/value square + hue bar
        int c1 = colorOf(1);
        float[] hsv = hsv(c1);
        if (hsv[1] > 0.05f && hsv[2] > 0.05f) hue[1] = hsv[0];
        float colW = (SQ_X2 - CP_X) / 48f;
        for (int i = 0; i < 48; i++) {
            float s = i / 47f;
            g.fillGradient(Math.round(CP_X + i * colW), Math.round(SQ_Y1), Math.round(CP_X + (i + 1) * colW) + 1, Math.round(SQ_Y2),
                    0xFF000000 | hsvToRgb(hue[1], s, 1), 0xFF000000);
        }
        UI.rect(g, CP_X - 1, SQ_Y1 - 1, SQ_X2 + 1, SQ_Y2 + 1, 1, row == 1 ? 0xFF9AF4FF : 0xFF3A6A8A);
        float kx = CP_X + hsv[1] * (SQ_X2 - CP_X), ky = SQ_Y1 + (1 - hsv[2]) * (SQ_Y2 - SQ_Y1);
        UI.rect(g, kx - 5, ky - 5, kx + 5, ky + 5, 2, 0xFFFFFFFF);
        hueBar(g, SQ_Y1, SQ_Y2, hue[1]);
        // recent colours
        UI.semi(g, Component.translatable("jaycee.editor.recent"), CP_X, 452, 14, 0xFFCFE8F5, 0);
        for (int i = 0; i < 7; i++) {
            float x = CP_X + i * 30;
            if (i < recent.size()) UI.fill(g, x, 464, x + 26, 486, 0xFF000000 | Integer.parseInt(recent.get(i), 16));
            else UI.fill(g, x, 464, x + 26, 486, 0x40243A50);
            UI.rect(g, x, 464, x + 26, 486, 1, 0xFF3A6A8A);
        }
        boolean plusHover = in(mx, my, CP_X + 214, 464, CP_X + 240, 486);
        UI.rect(g, CP_X + 214, 464, CP_X + 240, 486, 2, plusHover ? 0xFF9AF4FF : 0xFF4FE3FF);
        UI.text(g, "+", CP_X + 227, 475, 18, 0xFFFFFFFF, 1);
        // row 2: highlights (hair) or secondary
        UI.semi(g, Component.translatable(hair ? "jaycee.editor.highlights" : "jaycee.editor.secondary"), CP_X, 510, 15, 0xFFE6F4FF, 0);
        if (hair) toggle(g, 1372, 500, "1".equals(edit.get("hair.hl")));
        strip(g, 2, ST2_Y1, ST2_Y2);
        if (hair) {
            UI.semi(g, Component.translatable("jaycee.editor.tips"), CP_X, 578, 15, 0xFFE6F4FF, 0);
            toggle(g, 1372, 568, "1".equals(edit.get("hair.tip")));
            strip(g, 3, ST3_Y1, ST3_Y2);
        }
        // swatch presets
        for (int i = 0; i < SWATCHES.length; i++) {
            float x = CP_X + i * 32;
            UI.fill(g, x, 636, x + 28, 656, 0xFF000000 | Integer.parseInt(SWATCHES[i], 16));
            UI.rect(g, x, 636, x + 28, 656, 1, 0xFF3A6A8A);
        }
        // HEX / RGB
        int c = colorOf(row);
        boolean hexHover = in(mx, my, CP_X, 668, CP_X + 150, 700);
        UI.fill(g, CP_X, 668, CP_X + 150, 700, editingHex ? 0xFF102A40 : 0xC0081424);
        UI.rect(g, CP_X, 668, CP_X + 150, 700, editingHex || hexHover ? 2 : 1, editingHex ? 0xFF9AF4FF : 0xFF3A6A8A);
        String shown = editingHex ? "#" + hex + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "") : String.format("#%06X", c & 0xFFFFFF);
        UI.text(g, shown, CP_X + 10, 684, 16, 0xFFFFFFFF, 0);
        UI.semi(g, ((c >> 16) & 255) + ", " + ((c >> 8) & 255) + ", " + (c & 255), CP_X + 166, 684, 13, 0xFF9FB8C8, 0);
    }

    private void toggle(GuiGraphicsExtractor g, float x, float y, boolean on) {
        UI.fill(g, x, y, x + 34, y + 18, on ? 0xFF1E8AA0 : 0xFF243040);
        UI.rect(g, x, y, x + 34, y + 18, 1, on ? 0xFF9AF4FF : 0xFF5A6A7A);
        float kx = on ? x + 20 : x + 2;
        UI.fill(g, kx, y + 2, kx + 12, y + 16, 0xFFFFFFFF);
    }

    private void hueBar(GuiGraphicsExtractor g, float y1, float y2, float h) {
        float segH = (y2 - y1) / 6f;
        for (int i = 0; i < 6; i++) {
            g.fillGradient(Math.round(HUE_X1), Math.round(y1 + i * segH), Math.round(HUE_X2), Math.round(y1 + (i + 1) * segH),
                    0xFF000000 | hsvToRgb(i / 6f, 1, 1), 0xFF000000 | hsvToRgb((i + 1) / 6f, 1, 1));
        }
        float ky = y1 + h * (y2 - y1);
        UI.rect(g, HUE_X1 - 2, ky - 2, HUE_X2 + 2, ky + 2, 1, 0xFFFFFFFF);
    }

    /** A colour strip: shades of its hue from deep to pale, with a hue bar beside it. */
    private void strip(GuiGraphicsExtractor g, int r, float y1, float y2) {
        int c = colorOf(r);
        float[] hsv = hsv(c);
        if (hsv[1] > 0.05f && hsv[2] > 0.05f) hue[r] = hsv[0];
        float colW = (SQ_X2 - CP_X) / 40f;
        for (int i = 0; i < 40; i++) {
            float t = i / 39f;
            UI.fill(g, CP_X + i * colW, y1, CP_X + (i + 1) * colW + 1, y2, 0xFF000000 | shadeAt(hue[r], t));
        }
        UI.rect(g, CP_X - 1, y1 - 1, SQ_X2 + 1, y2 + 1, 1, row == r ? 0xFF9AF4FF : 0xFF3A6A8A);
        float t = stripPos(hsv);
        float kx = CP_X + t * (SQ_X2 - CP_X);
        UI.rect(g, kx - 5, (y1 + y2) / 2 - 6, kx + 5, (y1 + y2) / 2 + 6, 2, 0xFFFFFFFF);
        hueBar(g, y1, y2, hue[r]);
    }

    /** t 0..0.5: dark -> full colour; 0.5..1: full colour -> pale. */
    private static int shadeAt(float h, float t) {
        return t < 0.5f ? hsvToRgb(h, 0.9f, 0.15f + t * 1.7f) : hsvToRgb(h, 0.9f - (t - 0.5f) * 1.6f, 1);
    }

    private static float stripPos(float[] hsv) {
        return hsv[2] < 0.99f ? Math.max(0, (hsv[2] - 0.15f) / 1.7f) : 0.5f + Math.max(0, (0.9f - hsv[1]) / 1.6f);
    }

    private void quickPresets(GuiGraphicsExtractor g, double mx, double my) {
        String grp = group();
        UI.text(g, Component.translatable("jaycee.editor.quick", Component.translatable("jaycee.editor.group." + grp)), 650, 748, 19, 0xFFFFFFFF, 0);
        Features.Option shown = currentOption();
        String s = slot();
        if (s != null && !s.equals("style") && !s.equals("look")) shown = Features.get(s, edit.getOrDefault(s, "none"));
        if (shown == null || shown.boxes().isEmpty()) {
            Features.Option st = Features.get("style", "long_straight");
            shown = grp.equals("hair") ? st : shown;
        }
        for (int i = 0; i < PALETTE.length; i++) {
            float x = 700 + i * 96, y = 770;
            boolean hover = in(mx, my, x, y, x + 84, y + 84);
            piece(g, hover ? "ed_tile_sel" : "ed_tile", x, y, 84, 84);
            int col = 0xFF000000 | Integer.parseInt(PALETTE[i], 16);
            if (shown != null && !shown.boxes().isEmpty()) {
                Features.Option o = shown;
                Thumb.draw(g, o.boxes(), b -> b.tint() <= Features.TIP || b.tint() == Features.GLOW ? col : Appearance.color(edit, o, b.tint()),
                        o.view(), x + 4, y + 4, 76, 76);
            } else {
                UI.fill(g, x + 16, y + 16, x + 68, y + 68, col);
            }
        }
        button(g, mx, my, 646, 794, 40, 40, false, null, Component.literal("‹"));
        button(g, mx, my, 1384, 794, 40, 40, false, null, Component.literal("›"));
    }

    private void bottomButtons(GuiGraphicsExtractor g, double mx, double my) {
        boolean dirty = !edit.equals(applied);
        piece(g, "ed_btn_wide", 440, 886, 210, 40);
        icon(g, "reset", 486, 906, 24, 0xFFFFFFFF);
        UI.text(g, Component.translatable("jaycee.editor.reset"), 568, 906, 17, 0xFFFFFFFF, 1);
        piece(g, "ed_btn_wide", 710, 886, 250, 40);
        icon(g, "dice", 770, 906, 26, 0xFFFFFFFF);
        UI.text(g, Component.translatable("jaycee.editor.randomize"), 860, 906, 17, 0xFFFFFFFF, 1);
        piece(g, "ed_btn_apply", 1040, 886, 300, 40);
        if (in(mx, my, 1040, 886, 1340, 926)) UI.fill(g, 1048, 890, 1332, 922, 0x2250FFB0);
        icon(g, "check", 1110, 906, 26, dirty ? 0xFF7CFFB0 : 0xFFB8D8C8);
        UI.text(g, Component.translatable("jaycee.editor.apply"), 1206, 906, 18, dirty ? 0xFFFFFFFF : 0xFFB8D8C8, 1);
    }

    // ---- input -----------------------------------------------------------------------

    @Override
    boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        editingHex = false;
        // header
        if (in(mx, my, 676, 228, 772, 264)) { previewMode = false; return true; }
        if (in(mx, my, 780, 228, 884, 264)) { previewMode = true; return true; }
        if (in(mx, my, 904, 228, 948, 264)) {
            if (!undo.isEmpty()) { redo.push(new LinkedHashMap<>(edit)); edit = undo.pop(); }
            return true;
        }
        if (in(mx, my, 954, 228, 998, 264)) {
            if (!redo.isEmpty()) { undo.push(new LinkedHashMap<>(edit)); edit = redo.pop(); }
            return true;
        }
        if (in(mx, my, 1018, 228, 1214, 264)) { savePreset(); return true; }
        if (in(mx, my, 1224, 228, 1428, 264)) { showSaved = !showSaved; return true; }
        // preview controls
        if (in(mx, my, 376, 672, 412, 708)) { zoom = Math.min(2.2f, zoom + 0.15f); return true; }
        if (in(mx, my, 376, 716, 412, 752)) { zoom = Math.max(0.5f, zoom - 0.15f); return true; }
        for (int i = 0; i < 3; i++) {
            float x = 186 + i * 60;
            if (in(mx, my, x, 834, x + 46, 868)) {
                if (i == 0) spinning = true; else if (i == 1) spinning = false; else walking = !walking;
                return true;
            }
        }
        if (in(mx, my, PV_X1, PV_Y1, PV_X2, PV_Y2)) { dragging = true; return true; }
        // categories
        for (int i = 0; i < Features.CATEGORIES.size(); i++) {
            float y = CAT_Y + i * CAT_STEP;
            if (in(mx, my, CAT_X, y, CAT_X + CAT_W, y + CAT_H)) { cat = i; tab = 0; pageNo = 0; row = 1; showSaved = false; return true; }
        }
        if (showSaved) {
            List<Map<String, String>> list = saved();
            for (int i = 0; i < Math.min(PER_PAGE, list.size()); i++) {
                float x = TILE_X0 + (i % COLS) * TILE_SX, y = TILE_Y0 - 30 + (i / COLS) * TILE_SY;
                if (in(mx, my, x, y, x + TILE_W, y + TILE_H)) {
                    Map<String, String> look = list.get(i);
                    change(() -> { edit.clear(); edit.putAll(look); });
                    showSaved = false;
                    return true;
                }
            }
        } else {
            List<String> slots = slots();
            for (int i = 0; i < slots.size(); i++) {
                float x = 646 + i * 112;
                if (in(mx, my, x, 330, x + 104, 362)) { tab = i; pageNo = 0; return true; }
            }
            List<Features.Option> opts = options();
            for (int i = 0; i < PER_PAGE; i++) {
                int idx = pageNo * PER_PAGE + i;
                float x = TILE_X0 + (i % COLS) * TILE_SX, y = TILE_Y0 + (i / COLS) * TILE_SY;
                if (idx < opts.size() && in(mx, my, x, y, x + TILE_W, y + TILE_H)) { choose(opts.get(idx)); return true; }
            }
            int pages = Math.max(1, (opts.size() + PER_PAGE - 1) / PER_PAGE);
            if (in(mx, my, 790, 652, 834, 684)) { pageNo = (pageNo + pages - 1) % pages; return true; }
            if (in(mx, my, 894, 652, 938, 684)) { pageNo = (pageNo + 1) % pages; return true; }
        }
        // colour panel
        if (in(mx, my, CP_X, SQ_Y1, SQ_X2, SQ_Y2) || in(mx, my, HUE_X1 - 2, SQ_Y1, HUE_X2 + 2, SQ_Y2)
                || in(mx, my, CP_X, ST2_Y1, HUE_X2 + 2, ST2_Y2) || in(mx, my, CP_X, ST3_Y1, HUE_X2 + 2, ST3_Y2)) {
            pickColor(mx, my, true);
            return true;
        }
        boolean hair = group().equals("hair");
        if (hair && in(mx, my, 1368, 496, 1410, 522)) { change(() -> edit.put("hair.hl", "1".equals(edit.get("hair.hl")) ? "0" : "1")); return true; }
        if (hair && in(mx, my, 1368, 564, 1410, 590)) { change(() -> edit.put("hair.tip", "1".equals(edit.get("hair.tip")) ? "0" : "1")); return true; }
        for (int i = 0; i < recent.size(); i++) {
            float x = CP_X + i * 30;
            if (in(mx, my, x, 464, x + 26, 486)) { setColor(row, Integer.parseInt(recent.get(i), 16)); return true; }
        }
        if (in(mx, my, CP_X + 214, 464, CP_X + 240, 486)) { remember(colorOf(row)); return true; }
        for (int i = 0; i < SWATCHES.length; i++) {
            float x = CP_X + i * 32;
            if (in(mx, my, x, 636, x + 28, 656)) { setColor(row, Integer.parseInt(SWATCHES[i], 16)); remember(colorOf(row)); return true; }
        }
        if (in(mx, my, CP_X, 668, CP_X + 150, 700)) { editingHex = true; hex = ""; return true; }
        // quick presets
        for (int i = 0; i < PALETTE.length; i++) {
            float x = 700 + i * 96;
            if (in(mx, my, x, 770, x + 84, 854)) { row = 1; setColor(1, Integer.parseInt(PALETTE[i], 16)); remember(colorOf(1)); return true; }
        }
        // bottom
        if (in(mx, my, 440, 886, 650, 926)) { change(() -> { edit.clear(); edit.putAll(applied); }); return true; }
        if (in(mx, my, 710, 886, 960, 926)) { randomize(); return true; }
        if (in(mx, my, 1040, 886, 1340, 926)) {
            ClientPacketDistributor.sendToServer(new JayceeNetwork.ApplyLook(new HashMap<>(edit)));
            if (player() != null) player().sendOverlayMessage(Component.translatable("jaycee.editor.applied"));
            return true;
        }
        return false;
    }

    private void pickColor(double mx, double my, boolean press) {
        if (in(mx, my, CP_X, SQ_Y1 - 4, SQ_X2, SQ_Y2 + 4)) {
            row = 1;
            float s = clamp((float) ((mx - CP_X) / (SQ_X2 - CP_X))), v = clamp(1 - (float) ((my - SQ_Y1) / (SQ_Y2 - SQ_Y1)));
            setColor(1, hsvToRgb(hue[1], s, v));
            return;
        }
        int r = my < SQ_Y2 + 8 ? 1 : my < ST2_Y2 + 8 ? 2 : 3;
        float y1 = r == 1 ? SQ_Y1 : r == 2 ? ST2_Y1 : ST3_Y1, y2 = r == 1 ? SQ_Y2 : r == 2 ? ST2_Y2 : ST3_Y2;
        if (r == 3 && !group().equals("hair")) return;
        row = r;
        if (mx >= HUE_X1 - 4) {
            hue[r] = clamp((float) ((my - y1) / (y2 - y1)));
            float[] cur = hsv(colorOf(r));
            setColor(r, hsvToRgb(hue[r], Math.max(0.35f, cur[1]), Math.max(0.35f, cur[2])));
        } else if (r > 1) {
            setColor(r, shadeAt(hue[r], clamp((float) ((mx - CP_X) / (SQ_X2 - CP_X)))));
        }
    }

    @Override
    boolean drag(double mx, double my, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 0.8f;
            pitch = Math.max(-35, Math.min(35, pitch + (float) dy * 0.4f));
            spinning = false;
            return true;
        }
        if (in(mx, my, CP_X, SQ_Y1 - 4, HUE_X2 + 4, ST3_Y2 + 4)) { pickColor(mx, my, false); return true; }
        return false;
    }

    @Override
    boolean release(double mx, double my) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    boolean scroll(double mx, double my, double dy) {
        if (in(mx, my, PV_X1, PV_Y1, PV_X2, PV_Y2)) { zoom = Math.max(0.5f, Math.min(2.2f, zoom + (float) dy * 0.1f)); return true; }
        if (in(mx, my, GAL_X, GAL_Y, GAL_X + 468, GAL_Y + 430) && !showSaved) {
            int pages = Math.max(1, (options().size() + PER_PAGE - 1) / PER_PAGE);
            pageNo = Math.max(0, Math.min(pages - 1, pageNo + (dy < 0 ? 1 : -1)));
            return true;
        }
        return false;
    }

    @Override
    boolean typed(int cp) {
        if (!editingHex) return false;
        char c = Character.toUpperCase((char) cp);
        if (hex.length() < 6 && ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) hex += c;
        if (hex.length() == 6) { setColor(row, Integer.parseInt(hex, 16)); remember(colorOf(row)); editingHex = false; }
        return true;
    }

    @Override
    boolean key(int key) {
        if (!editingHex) return false;
        if (key == 259 && !hex.isEmpty()) hex = hex.substring(0, hex.length() - 1);
        else if (key == 256) editingHex = false;
        return true;
    }

    private void randomize() {
        Random r = new Random();
        change(() -> {
            for (String w : Appearance.WORN) {
                List<Features.Option> opts = Features.available(w, race());
                if (opts.isEmpty()) continue;
                boolean optional = w.startsWith("acc_") || w.equals("cape") || w.equals("waist") || w.equals("markings") || w.equals("hair_extra");
                if (optional && r.nextFloat() < 0.6f) { edit.put(w, "none"); continue; }
                edit.put(w, opts.get(r.nextInt(opts.size())).id());
            }
            edit.put("hair.c1", PALETTE[r.nextInt(PALETTE.length)]);
            for (String grp : new String[]{"eyes", "ears", "tail", "horns", "wings", "cape"}) edit.put(grp + ".c1", String.format("%06X", hsvToRgb(r.nextFloat(), 0.5f + r.nextFloat() * 0.4f, 0.6f + r.nextFloat() * 0.4f)));
        });
    }

    // ---- saved presets (appearance only, on this computer) ----
    private Path presetFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("jaycee_looks.json"); }

    private List<Map<String, String>> saved() {
        if (saved == null) {
            saved = new ArrayList<>();
            try {
                Path f = presetFile();
                if (Files.exists(f)) {
                    List<Map<String, String>> l = new Gson().fromJson(Files.readString(f), new TypeToken<List<Map<String, String>>>() {}.getType());
                    if (l != null) saved.addAll(l);
                }
            } catch (Exception ignored) {}
        }
        return saved;
    }

    private void savePreset() {
        List<Map<String, String>> l = saved();
        l.add(0, new LinkedHashMap<>(edit));
        while (l.size() > PER_PAGE) l.remove(l.size() - 1);
        try { Files.writeString(presetFile(), new Gson().toJson(l)); } catch (Exception ignored) {}
        if (player() != null) player().sendOverlayMessage(Component.translatable("jaycee.editor.saved_msg"));
    }

    // ---- colour maths ----
    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }

    static int hsvToRgb(float h, float s, float v) {
        s = clamp(s); v = clamp(v);
        float hh = (h - (float) Math.floor(h)) * 6f;
        int i = (int) Math.floor(hh) % 6;
        float f = hh - (float) Math.floor(hh), p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    static float[] hsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6; else if (max == g) h = (b - r) / d + 2; else h = (r - g) / d + 4;
            h /= 6; if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}

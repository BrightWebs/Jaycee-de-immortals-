package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drawing helpers for the Existence System menus. All coordinates are in "design pixels" (the
 * pixels of the approved artwork); the caller sets up the pose so they map onto the screen.
 * Text uses the bundled Inter font so it reads the same as the designs at any GUI scale.
 */
public final class UI {
    public static final FontDescription BOLD = new FontDescription.Resource(Identifier.fromNamespaceAndPath(JayceeMod.ID, "inter"));
    public static final FontDescription SEMI = new FontDescription.Resource(Identifier.fromNamespaceAndPath(JayceeMod.ID, "inter_semibold"));
    /** The TTF font's nominal size in GUI pixels (font/inter.json). */
    private static final float FONT_PX = 11f;
    /** Where the middle of capital letters sits below the draw y, in font pixels. Tuned for Inter. */
    public static float capMiddle = 4.0f;

    private UI() {}

    static Font font() { return Minecraft.getInstance().font; }

    /**
     * 26.3 reads the mouse through SDL, which numbers buttons 1 = left, 2 = middle, 3 = right.
     * Menus use 0 = left, 1 = right, 2 = middle.
     */
    public static int button(net.minecraft.client.input.MouseButtonEvent event) {
        return switch (event.button()) {
            case 1 -> 0;
            case 3 -> 1;
            case 2 -> 2;
            default -> -1;
        };
    }

    public static Identifier gui(String name) {
        // Resource paths may not contain "..", so "../race/x" means textures/race/x.
        String path = name.startsWith("../") ? "textures/" + name.substring(3) : "textures/gui/" + name;
        return Identifier.fromNamespaceAndPath(JayceeMod.ID, path + ".png");
    }

    static int[] size(String name) {
        int[] s = TexSizes.S.get(name);
        return s == null ? new int[]{16, 16} : s;
    }

    /** Draws a GUI texture (path under textures/gui/) at its own size. */
    public static void tex(GuiGraphicsExtractor g, String name, float x, float y) {
        int[] s = size(name);
        tex(g, name, x, y, s[0], s[1]);
    }

    /** Draws a whole texture stretched to w x h. */
    public static void tex(GuiGraphicsExtractor g, String name, float x, float y, float w, float h) {
        int[] s = size(name);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / s[0], h / s[1]);
        g.blit(RenderPipelines.GUI_TEXTURED, gui(name), 0, 0, 0, 0, s[0], s[1], s[0], s[1]);
        g.pose().popMatrix();
    }

    /** Draws a texture tinted/faded by an ARGB color. */
    public static void tex(GuiGraphicsExtractor g, String name, float x, float y, float w, float h, int argb) {
        int[] s = size(name);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / s[0], h / s[1]);
        g.blit(RenderPipelines.GUI_TEXTURED, gui(name), 0, 0, 0, 0, s[0], s[1], s[0], s[1], argb);
        g.pose().popMatrix();
    }

    /** Draws the left part of a texture (fraction 0..1 of its width), e.g. a bar fill. */
    public static void texPart(GuiGraphicsExtractor g, String name, float x, float y, float w, float h, double frac) {
        int[] s = size(name);
        int pw = (int) Math.round(s[0] * Math.max(0, Math.min(1, frac)));
        if (pw <= 0) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / s[0], h / s[1]);
        g.blit(RenderPipelines.GUI_TEXTURED, gui(name), 0, 0, 0, 0, pw, s[1], s[0], s[1]);
        g.pose().popMatrix();
    }

    /** Any texture by full resource location, stretched to w x h. */
    public static void image(GuiGraphicsExtractor g, Identifier id, int texW, int texH, float x, float y, float w, float h, int argb) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / texW, h / texH);
        g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, texW, texH, texW, texH, argb);
        g.pose().popMatrix();
    }

    // ---- icons -----------------------------------------------------------------------

    public static void abilityIcon(GuiGraphicsExtractor g, Ability a, float cx, float cy, float size) {
        String p = "../ability/" + a.id().getPath();
        if (!TexSizes.S.containsKey(p)) return;
        tex(g, p, cx - size / 2, cy - size / 2, size, size);
    }

    public static void modeIcon(GuiGraphicsExtractor g, Ability a, int mode, float cx, float cy, float size) {
        String p = "../ability/mode/" + a.id().getPath() + "_" + a.mode(mode).key();
        if (!TexSizes.S.containsKey(p)) { abilityIcon(g, a, cx, cy, size); return; }
        tex(g, p, cx - size / 2, cy - size / 2, size, size);
    }

    // ---- fills ---------------------------------------------------------------------

    public static void fill(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int argb) {
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    public static void rect(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float t, int argb) {
        fill(g, x1, y1, x2, y1 + t, argb); fill(g, x1, y2 - t, x2, y2, argb);
        fill(g, x1, y1, x1 + t, y2, argb); fill(g, x2 - t, y1, x2, y2, argb);
    }

    /** A rounded progress bar: dark track and a two-color gradient fill. */
    public static void bar(GuiGraphicsExtractor g, float x, float y, float w, float h, double frac, int c1, int c2) {
        fill(g, x, y, x + w, y + h, 0xFF081422);
        rect(g, x, y, x + w, y + h, 1, 0xFF2A5068);
        float fw = (float) (w * Math.max(0, Math.min(1, frac)));
        if (fw < 1) return;
        int steps = Math.max(1, (int) (fw / 6));
        for (int i = 0; i < steps; i++) {
            float a = x + 1 + (fw - 2) * i / steps, b = x + 1 + (fw - 2) * (i + 1) / steps;
            fill(g, a, y + 1, b, y + h - 1, lerp(c1, c2, (float) i / Math.max(1, steps - 1)));
        }
        fill(g, x + 1, y + 1, x + fw - 1, y + 1 + (h - 2) * 0.35f, 0x30FFFFFF);
    }

    static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | bb;
    }

    // ---- text ------------------------------------------------------------------------

    public static MutableComponent styled(Component c, boolean bold) {
        return c.copy().withStyle(st -> st.withFont(bold ? BOLD : SEMI));
    }

    public static MutableComponent styled(String s, boolean bold) {
        return styled(Component.literal(s), bold);
    }

    /** Width in design pixels of text at a given size. */
    public static float width(Component c, float size, boolean bold) {
        return font().width(styled(c, bold)) * size / FONT_PX;
    }

    public static float width(String s, float size, boolean bold) { return width(Component.literal(s), size, bold); }

    /** Text with its capital letters vertically centred on y. align: 0 left, 1 centre, 2 right. */
    public static void text(GuiGraphicsExtractor g, Component c, float x, float y, float size, int color, int align, boolean bold, boolean shadow) {
        MutableComponent s = styled(c, bold);
        float k = size / FONT_PX;
        int w = font().width(s);
        g.pose().pushMatrix();
        g.pose().translate(x, y - capMiddle * k);
        g.pose().scale(k, k);
        int dx = align == 0 ? 0 : align == 1 ? -w / 2 : -w;
        if (shadow) g.text(font(), s, dx + 1, 1, 0x99000000, false);
        g.text(font(), s, dx, 0, color, false);
        g.pose().popMatrix();
    }

    public static void text(GuiGraphicsExtractor g, String s, float x, float y, float size, int color, int align) {
        text(g, Component.literal(s), x, y, size, color, align, true, true);
    }

    public static void semi(GuiGraphicsExtractor g, String s, float x, float y, float size, int color, int align) {
        text(g, Component.literal(s), x, y, size, color, align, false, true);
    }

    public static void text(GuiGraphicsExtractor g, Component c, float x, float y, float size, int color, int align) {
        text(g, c, x, y, size, color, align, true, true);
    }

    public static void semi(GuiGraphicsExtractor g, Component c, float x, float y, float size, int color, int align) {
        text(g, c, x, y, size, color, align, false, true);
    }

    /** Cuts text to fit, adding an ellipsis. */
    public static String fit(String s, float size, float max, boolean bold) {
        if (width(s, size, bold) <= max) return s;
        while (s.length() > 1 && width(s + "…", size, bold) > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Wraps text into lines no wider than max (design pixels). */
    public static List<String> wrap(String s, float size, float max, boolean bold) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (width(next, size, bold) > max && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(word); }
                else line = new StringBuilder(next);
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Text auto-sized down (from size to min) so it fits max width. */
    public static float fitSize(String s, float size, float min, float max, boolean bold) {
        while (size > min && width(s, size, bold) > max) size -= 0.5f;
        return size;
    }

    public static String num(double v) { return String.format(Locale.US, "%,d", (long) Math.floor(v)); }

    /** Short form for big numbers: 2.15 B, 47.3 T. */
    public static String shortNum(double v) {
        String[] suf = {"", "K", "M", "B", "T", "Q"};
        int i = 0;
        while (v >= 1000 && i < suf.length - 1) { v /= 1000; i++; }
        String n = v < 10 ? String.format(Locale.US, "%.2f", v) : v < 100 ? String.format(Locale.US, "%.1f", v) : String.format(Locale.US, "%.0f", v);
        return n + (i == 0 ? "" : " " + suf[i]);
    }

    public static FormattedCharSequence seq(String s, boolean bold) { return styled(s, bold).getVisualOrderText(); }
}

package com.brightwebs.jaycee.appearance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.brightwebs.jaycee.appearance.Features.*;

/** Voxel hairstyles: bangs, back and extra pieces, and finished styles built from them. */
final class Hair {
    private Hair() {}

    private static final Map<String, List<Box>> BANGS = new LinkedHashMap<>(), BACKS = new LinkedHashMap<>(), EXTRAS = new LinkedHashMap<>();

    static void register() {
        // ---- bangs (front) ----
        bang("none", b -> {});
        bang("straight", b -> fringe(b, -4.75f, f(1.6f, 1.5f, 1.6f, 1.6f, 1.5f, 1.6f), f(2.4f, 2.6f, 2.5f, 2.6f, 2.4f, 2.6f)));
        bang("swept", b -> fringe(b, -4.75f, f(1.6f, 1.6f, 1.5f, 1.5f, 1.5f, 1.7f), f(3.6f, 3.2f, 2.8f, 2.2f, 1.6f, 1.2f)));
        bang("parted", b -> fringe(b, -4.75f, f(1.6f, 1.6f, 1.5f, 0.8f, 1.5f, 2.4f), f(3.4f, 2.6f, 1.6f, 0.4f, 1.6f, 3.2f)));
        bang("choppy", b -> fringe(b, -4.75f, f(1.2f, 1.6f, 1.0f, 1.8f, 1.2f, 1.6f, 1.0f), f(3.0f, 2.0f, 3.4f, 1.6f, 3.0f, 2.2f, 3.2f)));
        bang("curtain", b -> fringe(b, -4.75f, f(2.0f, 1.6f, 1.0f, 1.0f, 1.6f, 2.0f), f(4.4f, 3.0f, 1.0f, 1.0f, 3.0f, 4.4f)));
        bang("short", b -> fringe(b, -4.75f, f(9.2f), f(1.4f)));
        bang("spiky", b -> {
            for (int i = 0; i < 5; i++) {
                float x = -4.2f + i * 1.9f;
                b.rot(HEAD, x + 0.8f, -7.6f, -4.7f, 0, 0, (i - 2) * 9, -0.8f, 0, -0.1f, 1.6f, 3.0f, 0.8f, i % 2 == 0 ? PRIMARY : HIGHLIGHT);
                b.rot(HEAD, x + 0.8f, -7.6f, -4.7f, 0, 0, (i - 2) * 9, -0.5f, 3.0f, -0.1f, 1.0f, 0.9f, 0.8f, TIP);
            }
        });
        bang("hime", b -> {
            fringe(b, -4.75f, f(9.2f), f(2.6f));
            b.box(HEAD, -4.8f, -6.0f, -4.8f, 1.6f, 6.6f, 1.2f, PRIMARY).box(HEAD, 3.2f, -6.0f, -4.8f, 1.6f, 6.6f, 1.2f, PRIMARY);
            b.box(HEAD, -4.7f, 0.6f, -4.8f, 1.4f, 1.0f, 1.2f, TIP).box(HEAD, 3.3f, 0.6f, -4.8f, 1.4f, 1.0f, 1.2f, TIP);
        });
        bang("locks", b -> {
            fringe(b, -4.75f, f(1.6f, 1.5f, 3.0f, 1.5f, 1.6f), f(3.0f, 2.2f, 1.2f, 2.2f, 3.0f));
            b.box(HEAD, -4.9f, -6.0f, -4.4f, 1.2f, 8.5f, 1.6f, HIGHLIGHT).box(HEAD, 3.7f, -6.0f, -4.4f, 1.2f, 8.5f, 1.6f, HIGHLIGHT);
            b.box(HEAD, -4.8f, 2.5f, -4.3f, 1.0f, 1.2f, 1.4f, TIP).box(HEAD, 3.8f, 2.5f, -4.3f, 1.0f, 1.2f, 1.4f, TIP);
        });
        bang("messy", b -> {
            fringe(b, -4.75f, f(1.4f, 1.8f, 1.2f, 1.6f, 1.4f, 1.8f), f(2.6f, 1.6f, 3.2f, 2.0f, 2.8f, 1.8f));
            b.rot(HEAD, -2f, -8.6f, -4.5f, -25, 0, -15, -0.8f, -1.5f, 0, 1.6f, 1.6f, 1.0f, PRIMARY);
            b.rot(HEAD, 2f, -8.6f, -4.5f, -25, 0, 15, -0.8f, -1.5f, 0, 1.6f, 1.6f, 1.0f, HIGHLIGHT);
        });

        // ---- back ----
        back("none", b -> {});
        back("short", b -> backPlate(b, 4.2f, false));
        back("medium", b -> backPlate(b, 8.4f, false));
        back("layered", b -> backPlate(b, 9.6f, true));
        back("long", b -> backPlate(b, 15f, false));
        back("very_long", b -> backPlate(b, 21f, true));
        back("ponytail", b -> {
            backPlate(b, 5.6f, false);
            b.box(HEAD, -1.2f, -7.0f, 4.7f, 2.4f, 1.4f, 1.6f, GOLD);
            b.rot(HEAD, 0, -6.4f, 5.6f, 18, 0, 0, -1.3f, 0, -1.2f, 2.6f, 9.0f, 2.4f, PRIMARY);
            b.rot(HEAD, 0, -6.4f, 5.6f, 18, 0, 0, -1.0f, 9.0f, -1.0f, 2.0f, 2.0f, 2.0f, TIP);
        });
        back("high_ponytail", b -> {
            backPlate(b, 4.6f, false);
            b.rot(HEAD, 0, -8.2f, 4.2f, 40, 0, 0, -1.4f, -0.6f, -1.2f, 2.8f, 11.0f, 2.4f, PRIMARY);
            b.rot(HEAD, 0, -8.2f, 4.2f, 40, 0, 0, -1.0f, 10.4f, -1.0f, 2.0f, 2.2f, 2.0f, TIP);
        });
        back("twin_tails", b -> {
            backPlate(b, 6f, false);
            for (int s = -1; s <= 1; s += 2) {
                b.rot(HEAD, s * 4.6f, -7.0f, 2.0f, 10, 0, s * -12, -1.2f, 0, -1.2f, 2.4f, 12f, 2.4f, PRIMARY);
                b.rot(HEAD, s * 4.6f, -7.0f, 2.0f, 10, 0, s * -12, -0.9f, 12f, -0.9f, 1.8f, 2.4f, 1.8f, TIP);
                b.box(HEAD, s * 4.6f - 0.9f, -7.6f, 1.2f, 1.8f, 1.2f, 1.8f, GOLD);
            }
        });
        back("bun", b -> {
            backPlate(b, 4.4f, false);
            b.box(HEAD, -2.0f, -9.6f, 3.0f, 4.0f, 3.6f, 3.4f, PRIMARY);
            b.box(HEAD, -1.4f, -10.0f, 3.4f, 2.8f, 0.6f, 2.6f, HIGHLIGHT);
        });
        back("twin_buns", b -> {
            backPlate(b, 4.4f, false);
            b.box(HEAD, -5.0f, -10.2f, 0.4f, 3.2f, 3.2f, 3.2f, PRIMARY).box(HEAD, 1.8f, -10.2f, 0.4f, 3.2f, 3.2f, 3.2f, PRIMARY);
        });
        back("braid", b -> {
            backPlate(b, 6f, false);
            for (int i = 0; i < 7; i++) b.box(HEAD, i % 2 == 0 ? -1.4f : -1.1f, -1.6f + i * 2.0f, 4.4f, i % 2 == 0 ? 2.8f : 2.2f, 2.0f, 1.8f, i % 2 == 0 ? PRIMARY : HIGHLIGHT);
            b.box(HEAD, -0.9f, 12.4f, 4.6f, 1.8f, 2.2f, 1.4f, TIP);
        });
        back("spiky", b -> {
            backPlate(b, 5f, false);
            for (int i = 0; i < 5; i++) {
                float a = -40 + i * 20;
                b.rot(HEAD, 0, -6f, 4.6f, -30, 0, a, -1.0f, -6.5f, -0.6f, 2.0f, 6.0f, 1.6f, i % 2 == 0 ? PRIMARY : HIGHLIGHT);
            }
        });
        back("wavy", b -> {
            backPlate(b, 12f, false);
            for (int i = 0; i < 4; i++) b.box(HEAD, i % 2 == 0 ? -4.9f : 3.9f, -1 + i * 2.6f, 3.0f, 1.0f, 2.8f, 1.9f, HIGHLIGHT);
        });

        // ---- extras ----
        extra("none", b -> {});
        extra("ahoge", b -> {
            b.rot(HEAD, 0, -8.8f, -1.0f, 0, 0, 20, -0.5f, -3.4f, -0.4f, 1.0f, 3.4f, 0.8f, PRIMARY);
            b.rot(HEAD, 0, -8.8f, -1.0f, 0, 0, 20, -0.5f, -4.2f, -0.4f, 2.4f, 1.0f, 0.8f, TIP);
        });
        extra("cowlick", b -> {
            for (int i = 0; i < 3; i++) b.rot(HEAD, -2 + i * 2, -8.8f, 1f, -35, 0, (i - 1) * 25, -0.7f, -2.6f, -0.5f, 1.4f, 2.6f, 1.0f, i == 1 ? HIGHLIGHT : PRIMARY);
        });
        extra("side_braid", b -> {
            for (int i = 0; i < 6; i++) b.box(HEAD, i % 2 == 0 ? 3.8f : 4.0f, -4.6f + i * 1.8f, -2.8f, 1.6f, 1.8f, 1.6f, i % 2 == 0 ? PRIMARY : HIGHLIGHT);
            b.box(HEAD, 4.0f, 6.2f, -2.6f, 1.2f, 1.4f, 1.2f, TIP);
        });
        extra("sidelocks", b -> {
            b.box(HEAD, -5.0f, -6f, -3.8f, 1.2f, 9f, 1.6f, PRIMARY).box(HEAD, 3.8f, -6f, -3.8f, 1.2f, 9f, 1.6f, PRIMARY);
            b.box(HEAD, -4.9f, 3f, -3.7f, 1.0f, 1.4f, 1.4f, TIP).box(HEAD, 3.9f, 3f, -3.7f, 1.0f, 1.4f, 1.4f, TIP);
        });
        extra("antenna", b -> {
            b.rot(HEAD, -1, -8.8f, -2f, -20, 0, -20, -0.4f, -4f, -0.4f, 0.8f, 4f, 0.8f, PRIMARY);
            b.rot(HEAD, 1, -8.8f, -2f, -20, 0, 20, -0.4f, -4f, -0.4f, 0.8f, 4f, 0.8f, PRIMARY);
        });

        for (Map.Entry<String, List<Box>> e : BANGS.entrySet()) add("bangs", e.getKey(), View.FACE, ANY, null, null, withCap(e.getValue(), !e.getKey().equals("none")));
        for (Map.Entry<String, List<Box>> e : BACKS.entrySet()) add("back", e.getKey(), View.BACK, ANY, null, null, withCap(e.getValue(), !e.getKey().equals("none")));
        for (Map.Entry<String, List<Box>> e : EXTRAS.entrySet()) add("hair_extra", e.getKey(), View.HEAD, ANY, null, null, e.getValue());

        // ---- finished styles (Style tab) ----
        style("none", "none", "none", "none");
        style("classic", "straight", "short", "none");
        style("swept_short", "swept", "short", "none");
        style("hero", "spiky", "spiky", "none");
        style("shoulder", "parted", "medium", "none");
        style("layered_bob", "choppy", "layered", "none");
        style("long_straight", "straight", "long", "none");
        style("long_curtain", "curtain", "very_long", "none");
        style("ponytail", "swept", "ponytail", "none");
        style("high_ponytail", "parted", "high_ponytail", "sidelocks");
        style("twin_tails", "straight", "twin_tails", "none");
        style("bun", "curtain", "bun", "sidelocks");
        style("twin_buns", "choppy", "twin_buns", "none");
        style("braid", "parted", "braid", "none");
        style("side_braid", "swept", "medium", "side_braid");
        style("hime", "hime", "very_long", "none");
        style("messy", "messy", "medium", "cowlick");
        style("anime_lead", "locks", "long", "ahoge");
        style("wavy", "curtain", "wavy", "none");
        style("short_spikes", "spiky", "short", "cowlick");
        style("crop", "short", "short", "none");
        style("wild", "messy", "spiky", "ahoge");
        style("princess", "hime", "twin_tails", "none");
        style("scholar", "parted", "bun", "none");
        style("ranger", "choppy", "ponytail", "sidelocks");
        style("drifter", "swept", "layered", "ahoge");
        style("royal", "locks", "very_long", "side_braid");
        style("tidy", "straight", "medium", "none");
        style("windswept", "swept", "wavy", "ahoge");
        style("insect", "short", "short", "antenna");
    }

    private interface Shape { void make(B b); }

    private static float[] f(float... v) { return v; }

    private static void bang(String id, Shape s) { B b = b(); s.make(b); BANGS.put(id, b.done()); }
    private static void back(String id, Shape s) { B b = b(); s.make(b); BACKS.put(id, b.done()); }
    private static void extra(String id, Shape s) { B b = b(); s.make(b); EXTRAS.put(id, b.done()); }

    private static List<Box> withCap(List<Box> boxes, boolean cap) {
        if (!cap) return boxes;
        B b = b();
        cap(b);
        b.list.addAll(boxes);
        return b.done();
    }

    /** The cap is drawn once by the renderer for whichever piece is worn; the style's thumbnail shows all three. */
    private static void style(String id, String bangs, String back, String extra) {
        List<Box> all = new ArrayList<>();
        boolean any = !bangs.equals("none") || !back.equals("none");
        if (any) { B c = b(); cap(c); all.addAll(c.done()); }
        all.addAll(BANGS.get(bangs));
        all.addAll(BACKS.get(back));
        all.addAll(EXTRAS.get(extra));
        add("style", id, View.HEAD, ANY, null, null, all);
        combo("style", id, Map.of("bangs", bangs, "back", back, "hair_extra", extra));
    }
}

package com.brightwebs.jaycee.appearance;

import com.brightwebs.jaycee.race.JayceeRaces;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Every Character Editor option: voxel parts attached to the player's model (hair, ears, tails,
 * horns, wings...) plus whole-look presets. Pure data, shared by server (validation) and client
 * (rendering and thumbnails). Add options with {@link #add}; nothing else needs to change.
 *
 * Geometry is in model pixels, using the player model's own spaces: head pivot at the neck
 * (head cube -4..4, -8..0, -4..4, face toward -z), body pivot at the neck (-4..4, 0..12, -2..2).
 */
public final class Features {
    private Features() {}

    // ---- parts and colours -------------------------------------------------------------
    public static final int HEAD = 0, BODY = 1, RIGHT_ARM = 2, LEFT_ARM = 3, RIGHT_LEG = 4, LEFT_LEG = 5;
    /** Tints: the slot's colours, or a fixed colour. */
    public static final int PRIMARY = 0, HIGHLIGHT = 1, TIP = 2, SECONDARY = 3, WHITE = 4, DARK = 5, GOLD = 6, GLOW = 7;

    /** One cube. Coordinates are relative to its own pivot (px,py,pz), rotated by rx,ry,rz degrees. */
    public record Box(int part, float px, float py, float pz, float rx, float ry, float rz,
                      float x, float y, float z, float w, float h, float d, int tint) {}

    /** How a thumbnail frames the option. */
    public enum View { HEAD, FACE, BACK, BODY }

    public record Option(String slot, String id, List<Box> boxes, Predicate<String> race, View view, String c1, String c2) {
        public String key() { return slot + ":" + id; }
    }

    /** Editor categories, each with its tabs (slots). */
    public record Category(String id, List<String> slots) {}

    public static final List<Category> CATEGORIES = List.of(
            new Category("general", List.of("look")),
            new Category("hair", List.of("style", "bangs", "back", "hair_extra")),
            new Category("eyes", List.of("eyes")),
            new Category("skin", List.of("markings")),
            new Category("race", List.of("ears", "tail", "horns", "wings", "race_extra")),
            new Category("accessories", List.of("acc_head", "acc_face", "acc_neck")),
            new Category("outfit", List.of("cape", "waist")));

    /** Which colour set a slot uses. All hair pieces share the hair colours. */
    public static String colorGroup(String slot) {
        return switch (slot) {
            case "style", "bangs", "back", "hair_extra", "look" -> "hair";
            default -> slot;
        };
    }

    private static final Map<String, Option> ALL = new LinkedHashMap<>();
    private static final Map<String, List<Option>> BY_SLOT = new LinkedHashMap<>();
    /** Hair styles and whole looks: which pieces they set. */
    private static final Map<String, Map<String, String>> COMBOS = new LinkedHashMap<>();
    /** Slots a race must keep (natural form): race line -> slot -> option id. */
    private static final Map<String, Map<String, String>> REQUIRED = new LinkedHashMap<>();

    public static Option get(String slot, String id) { return ALL.get(slot + ":" + id); }
    public static List<Option> of(String slot) { return BY_SLOT.getOrDefault(slot, List.of()); }
    public static Map<String, String> combo(String slot, String id) { return COMBOS.getOrDefault(slot + ":" + id, Map.of()); }

    public static List<Option> available(String slot, String raceId) {
        List<Option> out = new ArrayList<>();
        for (Option o : of(slot)) if (o.race().test(raceId)) out.add(o);
        return out;
    }

    /** Whether the race has any choice in this slot (tabs and categories hide when it doesn't). */
    public static boolean hasChoices(String slot, String raceId) {
        for (Option o : of(slot)) if (!o.id().equals("none") && o.race().test(raceId)) return true;
        return false;
    }

    /** The option this race must wear in the slot, or null. */
    public static String required(String raceId, String slot) {
        Map<String, String> m = REQUIRED.get(raceId);
        if (m == null) m = REQUIRED.get(JayceeRaces.lineOf(raceId) + "*");
        return m == null ? null : m.get(slot);
    }

    public static Map<String, String> requiredAll(String raceId) {
        Map<String, String> m = REQUIRED.get(raceId);
        if (m == null) m = REQUIRED.get(JayceeRaces.lineOf(raceId) + "*");
        return m == null ? Map.of() : m;
    }

    static Option add(String slot, String id, View view, Predicate<String> race, String c1, String c2, List<Box> boxes) {
        Option o = new Option(slot, id, List.copyOf(boxes), race, view, c1, c2);
        ALL.put(o.key(), o);
        BY_SLOT.computeIfAbsent(slot, k -> new ArrayList<>()).add(o);
        return o;
    }

    // ---- race filters ------------------------------------------------------------------
    static final Predicate<String> ANY = r -> true;

    static Predicate<String> line(String... lines) {
        Set<String> s = Set.of(lines);
        return r -> s.contains(JayceeRaces.lineOf(r));
    }

    static Predicate<String> races(String... ids) {
        Set<String> s = Set.of(ids);
        return s::contains;
    }

    // ---- geometry helpers ------------------------------------------------------------
    static final class B {
        final List<Box> list = new ArrayList<>();
        B box(int part, float x, float y, float z, float w, float h, float d, int tint) {
            list.add(new Box(part, 0, 0, 0, 0, 0, 0, x, y, z, w, h, d, tint));
            return this;
        }
        B rot(int part, float px, float py, float pz, float rx, float ry, float rz, float x, float y, float z, float w, float h, float d, int tint) {
            list.add(new Box(part, px, py, pz, rx, ry, rz, x, y, z, w, h, d, tint));
            return this;
        }
        /**
         * A jointed limb (tails, horns, curls): each segment {length, width, pitch, yaw, tint} starts
         * where the last ended. Pitch tilts it up (+) or down (-) from pointing straight back (+z).
         */
        B chain(int part, float px, float py, float pz, float[][] segs) {
            for (float[] s : segs) {
                float len = s[0], w = s[1], pitch = s[2], yaw = s[3];
                list.add(new Box(part, px, py, pz, pitch, yaw, 0, -w / 2, -w / 2, 0, w, w, len, (int) s[4]));
                double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
                px += (float) (len * Math.cos(p) * Math.sin(y));
                py += (float) (-len * Math.sin(p));
                pz += (float) (len * Math.cos(p) * Math.cos(y));
            }
            return this;
        }
        /** Mirror every box added so far across x = 0 (pairs: ears, horns, wings). */
        B mirror() {
            List<Box> copy = new ArrayList<>(list);
            for (Box b : copy) {
                int part = b.part() == RIGHT_ARM ? LEFT_ARM : b.part() == LEFT_ARM ? RIGHT_ARM
                        : b.part() == RIGHT_LEG ? LEFT_LEG : b.part() == LEFT_LEG ? RIGHT_LEG : b.part();
                list.add(new Box(part, -b.px(), b.py(), b.pz(), b.rx(), -b.ry(), -b.rz(), -b.x() - b.w(), b.y(), b.z(), b.w(), b.h(), b.d(), b.tint()));
            }
            return this;
        }
        List<Box> done() { return list; }
    }

    static B b() { return new B(); }

    /** A row of hanging strands across the forehead: widths and lengths in pixels, every other one highlighted. */
    static void fringe(B b, float z, float[] widths, float[] lengths) {
        float x = -4.6f;
        for (int i = 0; i < widths.length; i++) {
            float len = lengths[i];
            float main = len > 2.2f ? len - 0.9f : len;
            b.box(HEAD, x, -7.6f, z, widths[i], main, 0.8f, i % 2 == 1 ? HIGHLIGHT : PRIMARY);
            if (len > 2.2f) b.box(HEAD, x + 0.15f, -7.6f + main, z, widths[i] - 0.3f, 0.9f, 0.8f, TIP);
            x += widths[i];
        }
    }

    /** The crown every hairstyle sits on: top and sides, open at the face. */
    static void cap(B b) {
        b.box(HEAD, -4.7f, -8.8f, -4.7f, 9.4f, 1.6f, 9.4f, PRIMARY);
        b.box(HEAD, 3.9f, -7.4f, -4.7f, 0.8f, 3.6f, 9.4f, PRIMARY);
        b.box(HEAD, -4.7f, -7.4f, -4.7f, 0.8f, 3.6f, 9.4f, PRIMARY);
    }

    /** Hair down the back of the head (and past it): length below the crown. */
    static void backPlate(B b, float length, boolean layered) {
        float main = Math.max(1, length - 1.6f);
        b.box(HEAD, -4.7f, -7.4f, 3.9f, 9.4f, main, 0.9f, PRIMARY);
        b.box(HEAD, -4.5f, -7.4f + main, 3.9f, 9.0f, 1.6f, 0.9f, TIP);
        if (layered) {
            b.box(HEAD, -4.2f, -7.4f, 4.7f, 8.4f, main * 0.6f, 0.6f, HIGHLIGHT);
        }
        if (length > 4) {
            float side = Math.min(length, 8.4f) - 3.6f;
            b.box(HEAD, 3.9f, -3.8f, -1f, 0.8f, side, 5.7f, PRIMARY);
            b.box(HEAD, -4.7f, -3.8f, -1f, 0.8f, side, 5.7f, PRIMARY);
        }
    }

    // ---- the catalogue -----------------------------------------------------------------
    static {
        Hair.register();
        RaceParts.register();
        Extras.register();
    }

    /** Called once from mod setup so the catalogue loads early. */
    public static void bootstrap() {}

    static void combo(String slot, String id, Map<String, String> pieces) {
        COMBOS.put(slot + ":" + id, pieces);
    }

    static void require(String raceOrLine, String slot, String id) {
        REQUIRED.computeIfAbsent(raceOrLine, k -> new LinkedHashMap<>()).put(slot, id);
    }
}

package com.brightwebs.jaycee.appearance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.brightwebs.jaycee.appearance.Features.*;

/** Eyes, skin markings, accessories, outfit pieces and the General tab's whole looks. */
final class Extras {
    private Extras() {}

    /** The face is at z = -4; eyes sit on row y -4..-3 like a default skin's. */
    private static final float FZ = -4.12f;

    static void register() {
        // ---- eyes ----
        add("eyes", "none", View.FACE, ANY, null, null, b().done());
        add("eyes", "round", View.FACE, ANY, "#3A7BFF", "#FFFFFF", eyes(b(), 2, 1, false).done());
        add("eyes", "anime", View.FACE, ANY, "#3A7BFF", "#FFFFFF", b()
                .box(HEAD, -3.2f, -4.6f, FZ, 2.2f, 2.0f, 0.12f, SECONDARY).box(HEAD, 1.0f, -4.6f, FZ, 2.2f, 2.0f, 0.12f, SECONDARY)
                .box(HEAD, -2.6f, -4.4f, FZ - 0.02f, 1.4f, 1.8f, 0.12f, PRIMARY).box(HEAD, 1.2f, -4.4f, FZ - 0.02f, 1.4f, 1.8f, 0.12f, PRIMARY)
                .box(HEAD, -2.4f, -4.3f, FZ - 0.04f, 0.5f, 0.5f, 0.12f, WHITE).box(HEAD, 1.4f, -4.3f, FZ - 0.04f, 0.5f, 0.5f, 0.12f, WHITE)
                .box(HEAD, -3.3f, -4.8f, FZ - 0.02f, 2.4f, 0.3f, 0.12f, DARK).box(HEAD, 0.9f, -4.8f, FZ - 0.02f, 2.4f, 0.3f, 0.12f, DARK).done());
        add("eyes", "sharp", View.FACE, ANY, "#C0303A", "#FFFFFF", eyes(b(), 2, 1, false)
                .box(HEAD, -3.2f, -4.4f, FZ - 0.04f, 2.4f, 0.4f, 0.12f, DARK).box(HEAD, 0.8f, -4.4f, FZ - 0.04f, 2.4f, 0.4f, 0.12f, DARK).done());
        add("eyes", "slit", View.FACE, races("vampire", "vampire_overcomer", "vampire_lord", "divine_vampire", "lizardman", "dragonewt",
                "true_dragonewt", "divine_dragon", "catkin", "lesser_daemon", "greater_daemon", "arch_daemon", "daemon_lord", "devil_lord"),
                "#E0B020", "#FFF0A0", b()
                .box(HEAD, -3, -4, FZ, 2, 1, 0.12f, PRIMARY).box(HEAD, 1, -4, FZ, 2, 1, 0.12f, PRIMARY)
                .box(HEAD, -2.15f, -4.1f, FZ - 0.02f, 0.3f, 1.2f, 0.12f, DARK).box(HEAD, 1.85f, -4.1f, FZ - 0.02f, 0.3f, 1.2f, 0.12f, DARK).done());
        add("eyes", "glowing", View.FACE, ANY, "#52D6FF", "#FFFFFF", b()
                .box(HEAD, -3, -4, FZ, 2, 1, 0.12f, GLOW).box(HEAD, 1, -4, FZ, 2, 1, 0.12f, GLOW).done());
        add("eyes", "heterochromia", View.FACE, ANY, "#3A7BFF", "#E0B020", b()
                .box(HEAD, -3, -4, FZ, 2, 1, 0.12f, WHITE).box(HEAD, 1, -4, FZ, 2, 1, 0.12f, WHITE)
                .box(HEAD, -2, -4, FZ - 0.02f, 1, 1, 0.12f, PRIMARY).box(HEAD, 1, -4, FZ - 0.02f, 1, 1, 0.12f, SECONDARY).done());
        add("eyes", "sleepy", View.FACE, ANY, "#6A4A8A", "#FFFFFF", b()
                .box(HEAD, -3, -3.6f, FZ, 2, 0.6f, 0.12f, PRIMARY).box(HEAD, 1, -3.6f, FZ, 2, 0.6f, 0.12f, PRIMARY)
                .box(HEAD, -3.1f, -4.0f, FZ - 0.02f, 2.2f, 0.4f, 0.12f, DARK).box(HEAD, 0.9f, -4.0f, FZ - 0.02f, 2.2f, 0.4f, 0.12f, DARK).done());
        add("eyes", "closed", View.FACE, ANY, "#2A1E1A", "#2A1E1A", b()
                .box(HEAD, -3, -3.6f, FZ, 2, 0.4f, 0.12f, PRIMARY).box(HEAD, 1, -3.6f, FZ, 2, 0.4f, 0.12f, PRIMARY).done());
        add("eyes", "starry", View.FACE, ANY, "#B46CFF", "#FFE27A", eyes(b(), 2, 1, false)
                .box(HEAD, -2.3f, -3.8f, FZ - 0.04f, 0.6f, 0.6f, 0.12f, SECONDARY).box(HEAD, 1.7f, -3.8f, FZ - 0.04f, 0.6f, 0.6f, 0.12f, SECONDARY).done());

        // ---- skin markings ----
        add("markings", "none", View.FACE, ANY, null, null, b().done());
        add("markings", "cheek_marks", View.FACE, ANY, "#C0303A", "#C0303A", b()
                .box(HEAD, -3.6f, -2.6f, FZ, 1.6f, 0.3f, 0.12f, PRIMARY).box(HEAD, -3.6f, -2.0f, FZ, 1.6f, 0.3f, 0.12f, PRIMARY)
                .box(HEAD, 2.0f, -2.6f, FZ, 1.6f, 0.3f, 0.12f, PRIMARY).box(HEAD, 2.0f, -2.0f, FZ, 1.6f, 0.3f, 0.12f, PRIMARY).done());
        add("markings", "forehead_mark", View.FACE, ANY, "#B46CFF", "#52D6FF", b()
                .box(HEAD, -0.5f, -6.6f, FZ, 1, 1.4f, 0.12f, PRIMARY).box(HEAD, -1.2f, -6.0f, FZ, 2.4f, 0.4f, 0.12f, SECONDARY).done());
        add("markings", "under_eye", View.FACE, ANY, "#2A1A2E", "#B0203A", b()
                .box(HEAD, -3, -2.9f, FZ, 2, 0.3f, 0.12f, PRIMARY).box(HEAD, 1, -2.9f, FZ, 2, 0.3f, 0.12f, PRIMARY)
                .box(HEAD, -2.4f, -2.6f, FZ, 0.4f, 1.2f, 0.12f, PRIMARY).box(HEAD, 2.0f, -2.6f, FZ, 0.4f, 1.2f, 0.12f, PRIMARY).done());
        add("markings", "freckles", View.FACE, ANY, "#A0603A", "#A0603A", b()
                .box(HEAD, -3.2f, -2.8f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY).box(HEAD, -2.4f, -2.4f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY)
                .box(HEAD, -1.6f, -2.9f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY).box(HEAD, 1.2f, -2.9f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY)
                .box(HEAD, 2.0f, -2.4f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY).box(HEAD, 2.8f, -2.8f, FZ, 0.4f, 0.4f, 0.12f, PRIMARY).done());
        add("markings", "war_paint", View.FACE, ANY, "#2A6AE6", "#F8F6F0", b()
                .box(HEAD, -4.05f, -4.4f, FZ, 8.1f, 0.6f, 0.12f, PRIMARY).box(HEAD, -0.3f, -3.8f, FZ, 0.6f, 2.2f, 0.12f, SECONDARY).done());
        add("markings", "arm_bands", View.BODY, ANY, "#1A1A2A", "#B46CFF", b()
                .box(RIGHT_ARM, -3.15f, 3.0f, -2.15f, 4.3f, 0.6f, 4.3f, PRIMARY).box(RIGHT_ARM, -3.15f, 4.0f, -2.15f, 4.3f, 0.6f, 4.3f, SECONDARY)
                .box(LEFT_ARM, -1.15f, 3.0f, -2.15f, 4.3f, 0.6f, 4.3f, PRIMARY).box(LEFT_ARM, -1.15f, 4.0f, -2.15f, 4.3f, 0.6f, 4.3f, SECONDARY).done());
        add("markings", "daemon_sigils", View.BODY, line("lesser_daemon"), "#B0203A", "#2A1A2E", b()
                .box(HEAD, -0.6f, -6.8f, FZ, 1.2f, 1.6f, 0.12f, PRIMARY)
                .box(BODY, -2.0f, 2.0f, -2.15f, 4.0f, 0.4f, 0.12f, PRIMARY).box(BODY, -0.2f, 2.0f, -2.15f, 0.4f, 4.0f, 0.12f, PRIMARY).done());

        // ---- accessories ----
        add("acc_head", "none", View.HEAD, ANY, null, null, b().done());
        add("acc_head", "crown", View.HEAD, ANY, "#F0C040", "#C0303A", b()
                .box(HEAD, -4.8f, -9.6f, -4.8f, 9.6f, 1.2f, 0.6f, PRIMARY).box(HEAD, -4.8f, -9.6f, 4.2f, 9.6f, 1.2f, 0.6f, PRIMARY)
                .box(HEAD, -4.8f, -9.6f, -4.8f, 0.6f, 1.2f, 9.6f, PRIMARY).box(HEAD, 4.2f, -9.6f, -4.8f, 0.6f, 1.2f, 9.6f, PRIMARY)
                .box(HEAD, -4.8f, -10.8f, -4.8f, 1.0f, 1.2f, 0.6f, PRIMARY).box(HEAD, -0.5f, -11.2f, -4.8f, 1.0f, 1.6f, 0.6f, PRIMARY).box(HEAD, 3.8f, -10.8f, -4.8f, 1.0f, 1.2f, 0.6f, PRIMARY)
                .box(HEAD, -0.4f, -9.4f, -5.0f, 0.8f, 0.8f, 0.3f, SECONDARY).done());
        add("acc_head", "tiara", View.FACE, ANY, "#E8E8F0", "#3A7BFF", b()
                .box(HEAD, -4.7f, -8.0f, -4.75f, 9.4f, 0.5f, 0.4f, PRIMARY).box(HEAD, -1.0f, -9.2f, -4.8f, 2.0f, 1.2f, 0.4f, PRIMARY)
                .box(HEAD, -0.5f, -8.9f, -5.0f, 1.0f, 1.0f, 0.3f, SECONDARY).done());
        add("acc_head", "headband", View.FACE, ANY, "#C0303A", "#F8F6F0", b()
                .box(HEAD, -4.75f, -7.2f, -4.75f, 9.5f, 1.0f, 9.5f, PRIMARY).box(HEAD, -1.0f, -7.0f, 4.6f, 2.0f, 4.0f, 0.4f, PRIMARY).done());
        add("acc_head", "flower_pin", View.FACE, ANY, "#E87AA8", "#FFE27A", b()
                .box(HEAD, 3.6f, -7.4f, -3.0f, 1.8f, 1.8f, 1.8f, PRIMARY).box(HEAD, 4.0f, -7.0f, -3.15f, 1.0f, 1.0f, 0.3f, SECONDARY).done());
        add("acc_head", "witch_hat", View.HEAD, ANY, "#2A1E3A", "#B46CFF", b()
                .box(HEAD, -7.0f, -9.2f, -7.0f, 14, 0.8f, 14, PRIMARY).box(HEAD, -4.4f, -12.2f, -4.4f, 8.8f, 3, 8.8f, PRIMARY)
                .box(HEAD, -4.5f, -10.2f, -4.5f, 9.0f, 0.8f, 9.0f, SECONDARY)
                .rot(HEAD, 0, -12.2f, 0, -15, 0, 0, -2.8f, -4.0f, -2.8f, 5.6f, 4, 5.6f, PRIMARY)
                .rot(HEAD, 0, -12.2f, 0, -15, 0, 0, -1.4f, -6.6f, -0.2f, 2.8f, 2.8f, 2.8f, PRIMARY).done());
        add("acc_face", "none", View.FACE, ANY, null, null, b().done());
        add("acc_face", "glasses", View.FACE, ANY, "#2A2A34", "#9FE8FF", b()
                .box(HEAD, -3.6f, -4.6f, -4.6f, 3.0f, 0.3f, 0.3f, PRIMARY).box(HEAD, -3.6f, -2.9f, -4.6f, 3.0f, 0.3f, 0.3f, PRIMARY)
                .box(HEAD, -3.6f, -4.6f, -4.6f, 0.3f, 2.0f, 0.3f, PRIMARY).box(HEAD, -0.9f, -4.6f, -4.6f, 0.3f, 2.0f, 0.3f, PRIMARY)
                .box(HEAD, 0.6f, -4.6f, -4.6f, 3.0f, 0.3f, 0.3f, PRIMARY).box(HEAD, 0.6f, -2.9f, -4.6f, 3.0f, 0.3f, 0.3f, PRIMARY)
                .box(HEAD, 0.6f, -4.6f, -4.6f, 0.3f, 2.0f, 0.3f, PRIMARY).box(HEAD, 3.3f, -4.6f, -4.6f, 0.3f, 2.0f, 0.3f, PRIMARY)
                .box(HEAD, -0.6f, -4.2f, -4.6f, 1.2f, 0.3f, 0.3f, PRIMARY).done());
        add("acc_face", "eyepatch", View.FACE, ANY, "#1A1A22", "#1A1A22", b()
                .box(HEAD, -3.4f, -4.6f, -4.45f, 2.8f, 2.4f, 0.3f, PRIMARY).box(HEAD, -4.6f, -5.6f, -4.5f, 9.2f, 0.4f, 9.0f, PRIMARY).done());
        add("acc_face", "mask", View.FACE, ANY, "#1A1A22", "#B0203A", b()
                .box(HEAD, -4.3f, -2.8f, -4.45f, 8.6f, 2.8f, 0.4f, PRIMARY).box(HEAD, -4.3f, -1.6f, -4.5f, 8.6f, 0.4f, 0.4f, SECONDARY).done());
        add("acc_face", "oni_mask", View.FACE, ANY, "#F8F6F0", "#C0303A", b()
                .box(HEAD, 4.1f, -7.4f, -3.4f, 0.5f, 4.6f, 4.4f, PRIMARY)
                .box(HEAD, 4.5f, -6.4f, -2.8f, 0.2f, 0.5f, 1.2f, SECONDARY).box(HEAD, 4.5f, -6.4f, -0.8f, 0.2f, 0.5f, 1.2f, SECONDARY)
                .box(HEAD, 4.5f, -4.4f, -2.0f, 0.2f, 0.4f, 1.8f, SECONDARY).box(HEAD, 4.2f, -8.4f, -3.0f, 0.4f, 1.2f, 0.8f, SECONDARY).done());
        add("acc_neck", "none", View.BODY, ANY, null, null, b().done());
        add("acc_neck", "scarf", View.BODY, ANY, "#C0303A", "#F0C040", b()
                .box(BODY, -4.4f, -0.4f, -2.4f, 8.8f, 2.0f, 4.8f, PRIMARY).box(BODY, 1.6f, 1.4f, -2.6f, 2.0f, 5.0f, 0.6f, PRIMARY)
                .box(BODY, 1.6f, 6.2f, -2.6f, 2.0f, 0.6f, 0.6f, SECONDARY).done());
        add("acc_neck", "necklace", View.BODY, ANY, "#F0C040", "#52D6FF", b()
                .box(BODY, -2.6f, 0.4f, -2.15f, 5.2f, 0.4f, 0.2f, PRIMARY).box(BODY, -0.6f, 0.8f, -2.2f, 1.2f, 1.4f, 0.3f, SECONDARY).done());
        add("acc_neck", "choker", View.BODY, ANY, "#1A1A22", "#B46CFF", b()
                .box(HEAD, -4.15f, -0.9f, -4.15f, 8.3f, 0.9f, 8.3f, PRIMARY).box(HEAD, -0.4f, -0.6f, -4.3f, 0.8f, 0.8f, 0.2f, SECONDARY).done());
        add("acc_neck", "pauldrons", View.BODY, ANY, "#8A8EA0", "#F0C040", b()
                .box(RIGHT_ARM, -3.6f, -2.6f, -2.6f, 5.0f, 2.6f, 5.2f, PRIMARY).box(RIGHT_ARM, -3.7f, -0.4f, -2.7f, 5.2f, 0.6f, 5.4f, SECONDARY)
                .box(LEFT_ARM, -1.4f, -2.6f, -2.6f, 5.0f, 2.6f, 5.2f, PRIMARY).box(LEFT_ARM, -1.5f, -0.4f, -2.7f, 5.2f, 0.6f, 5.4f, SECONDARY).done());

        // ---- outfit ----
        add("cape", "none", View.BACK, ANY, null, null, b().done());
        add("cape", "short", View.BACK, ANY, "#2A2E5A", "#F0C040", b()
                .rot(BODY, 0, 0, 2.2f, 8, 0, 0, -4.6f, 0, 0, 9.2f, 9.0f, 0.4f, PRIMARY).rot(BODY, 0, 0, 2.2f, 8, 0, 0, -4.6f, 8.6f, 0.05f, 9.2f, 0.6f, 0.4f, SECONDARY).done());
        add("cape", "long", View.BACK, ANY, "#6A1E2E", "#F0C040", b()
                .rot(BODY, 0, 0, 2.2f, 6, 0, 0, -5.0f, 0, 0, 10, 18, 0.4f, PRIMARY).rot(BODY, 0, 0, 2.2f, 6, 0, 0, -5.0f, 17.4f, 0.05f, 10, 0.6f, 0.4f, SECONDARY)
                .box(BODY, -4.6f, -0.4f, -2.4f, 1.4f, 1.4f, 4.8f, SECONDARY).box(BODY, 3.2f, -0.4f, -2.4f, 1.4f, 1.4f, 4.8f, SECONDARY).done());
        add("cape", "royal", View.BACK, ANY, "#4A1E6A", "#F8F6F0", b()
                .rot(BODY, 0, 0, 2.2f, 6, 0, 0, -5.4f, 0, 0, 10.8f, 19, 0.4f, PRIMARY)
                .box(BODY, -5.0f, -0.6f, -2.6f, 10, 1.8f, 5.2f, SECONDARY).box(BODY, -5.2f, -0.6f, -2.6f, 0.8f, 6, 0.6f, SECONDARY).box(BODY, 4.4f, -0.6f, -2.6f, 0.8f, 6, 0.6f, SECONDARY).done());
        add("cape", "tattered", View.BACK, ANY, "#2A2A30", "#5A5A66", b()
                .rot(BODY, 0, 0, 2.2f, 8, 0, 0, -4.6f, 0, 0, 9.2f, 12, 0.4f, PRIMARY)
                .rot(BODY, 0, 0, 2.2f, 8, 0, 0, -4.6f, 12, 0, 2, 2, 0.4f, PRIMARY).rot(BODY, 0, 0, 2.2f, 8, 0, 0, -1.2f, 12, 0, 2.4f, 3, 0.4f, SECONDARY)
                .rot(BODY, 0, 0, 2.2f, 8, 0, 0, 2.4f, 12, 0, 2.2f, 1.6f, 0.4f, PRIMARY).done());
        add("waist", "none", View.BODY, ANY, null, null, b().done());
        add("waist", "belt", View.BODY, ANY, "#4A3022", "#F0C040", b()
                .box(BODY, -4.3f, 9.6f, -2.3f, 8.6f, 1.2f, 4.6f, PRIMARY).box(BODY, -0.8f, 9.4f, -2.5f, 1.6f, 1.6f, 0.4f, SECONDARY).done());
        add("waist", "sash", View.BODY, ANY, "#C0303A", "#F0C040", b()
                .box(BODY, -4.3f, 9.4f, -2.3f, 8.6f, 1.6f, 4.6f, PRIMARY).box(BODY, 2.4f, 10.8f, -2.5f, 1.4f, 4.6f, 0.4f, PRIMARY)
                .box(BODY, 2.4f, 15.2f, -2.5f, 1.4f, 0.6f, 0.4f, SECONDARY).done());
        add("waist", "tassets", View.BODY, ANY, "#8A8EA0", "#F0C040", b()
                .box(BODY, -4.3f, 9.6f, -2.3f, 8.6f, 1.0f, 4.6f, SECONDARY)
                .box(RIGHT_LEG, -2.3f, -1.4f, -2.4f, 4.4f, 4.4f, 0.4f, PRIMARY).box(LEFT_LEG, -2.1f, -1.4f, -2.4f, 4.4f, 4.4f, 0.4f, PRIMARY).done());

        // ---- General: whole looks ----
        look("keep_skin", Map.of());
        look("adventurer", Map.of("bangs", "swept", "back", "short", "acc_neck", "scarf", "waist", "belt", "cape", "short"));
        look("sage", Map.of("bangs", "curtain", "back", "very_long", "acc_head", "tiara", "eyes", "anime", "cape", "long"));
        look("noble", Map.of("bangs", "locks", "back", "ponytail", "acc_neck", "necklace", "cape", "royal", "waist", "sash"));
        look("rogue", Map.of("bangs", "messy", "back", "short", "acc_face", "mask", "cape", "tattered", "waist", "belt"));
        look("knight", Map.of("bangs", "short", "back", "short", "acc_neck", "pauldrons", "waist", "tassets", "cape", "long"));
        look("mystic", Map.of("bangs", "hime", "back", "long", "acc_head", "witch_hat", "eyes", "glowing", "markings", "forehead_mark"));
        look("shrine", Map.of("bangs", "straight", "back", "high_ponytail", "acc_head", "flower_pin", "acc_face", "oni_mask", "waist", "sash"));
        look("scholar", Map.of("bangs", "parted", "back", "bun", "acc_face", "glasses", "acc_neck", "necklace"));
        look("warrior", Map.of("bangs", "spiky", "back", "spiky", "markings", "war_paint", "acc_head", "headband", "waist", "belt"));
        look("royalty", Map.of("bangs", "locks", "back", "very_long", "acc_head", "crown", "cape", "royal", "acc_neck", "necklace"));
    }

    private static B eyes(B b, float w, float h, boolean tall) {
        return b.box(HEAD, -3, -4, FZ, w, h, 0.12f, SECONDARY).box(HEAD, 1, -4, FZ, w, h, 0.12f, SECONDARY)
                .box(HEAD, -2, -4, FZ - 0.02f, 1, h, 0.12f, PRIMARY).box(HEAD, 1, -4, FZ - 0.02f, 1, h, 0.12f, PRIMARY);
    }

    private static void look(String id, Map<String, String> pieces) {
        List<Box> all = new ArrayList<>();
        for (Map.Entry<String, String> e : pieces.entrySet()) {
            Option o = get(e.getKey(), e.getValue());
            if (o != null) all.addAll(o.boxes());
        }
        add("look", id, View.BODY, ANY, null, null, all);
        combo("look", id, pieces);
    }
}

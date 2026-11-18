package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.jaycee.training.Attr;
import com.brightwebs.jaycee.training.Potential;
import com.brightwebs.jaycee.training.Training;
import com.brightwebs.jaycee.training.TrainingData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Stats: trainable attributes, their details and racial potential, and the live training feed. */
final class StatsPage extends Page {
    private static final T TOP = new T(474f, 222.417f, 0.86996f, 385, 166);
    private static final T BOT_L = new T(474f, 755.70f, 0.67361f, 60, 782);
    private static final int[] ROWS = {253, 292, 329, 365, 402, 439, 474, 510, 547, 583, 617, 651, 687, 724};
    private static final int[] FEED_Y = {847, 888, 930};

    private record B(String label, String fmt, double perLevel) {}
    private static final EnumMap<Attr, B[]> BON = new EnumMap<>(Attr.class);
    static {
        BON.put(Attr.PHYSICAL_STRENGTH, new B[]{new B("Melee damage","+%.1f",0.06),new B("Mining speed","+%d%%",1)});
        BON.put(Attr.SPEED, new B[]{new B("Move speed","+%.1f%%",0.6)});
        BON.put(Attr.AGILITY, new B[]{new B("Jump power","+%.2f",0.003),new B("Safe fall","+%.1f",0.06)});
        BON.put(Attr.ENDURANCE, new B[]{new B("Toughness","+%.1f",0.08),new B("Knockback res.","+%.1f%%",0.4)});
        BON.put(Attr.VITALITY, new B[]{new B("Max HP","+%.1f",0.4)});
        BON.put(Attr.LUNG_CAPACITY, new B[]{new B("Breath","+%.1f",0.1)});
        BON.put(Attr.SWIMMING_SPEED, new B[]{new B("Swim speed","+%d%%",1)});
        BON.put(Attr.PERCEPTION, new B[]{new B("Reach","+%.2f",0.02)});
        BON.put(Attr.TARGETING, new B[]{new B("Ranged damage","+%.1f%%",0.4)});
        BON.put(Attr.ARCHERY, new B[]{new B("Ranged damage","+%.1f%%",0.8)});
        BON.put(Attr.COMBAT_MASTERY, new B[]{new B("Attack speed","+%.2f",0.012)});
        BON.put(Attr.MAGIC_CONTROL, new B[]{new B("Mana cost","-%.1f%%",0.5)});
        BON.put(Attr.AURA_CONTROL, new B[]{new B("Aura efficiency","+%.1f%%",0.5)});
        BON.put(Attr.MENTAL_FORTITUDE, new B[]{new B("Magic damage","-%.1f%%",0.5)});
        BON.put(Attr.REGENERATION, new B[]{new B("Extra healing","+%.2f/5s",0.05)});

    }

    private static Attr selected = Attr.PHYSICAL_STRENGTH;
    private int scroll = 0, feedScroll = 0;

    StatsPage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_stats"; }

    private static int gradeColor(String g) {
        return switch (g) {
            case "SSS" -> 0xFFFF78E6; case "SS" -> 0xFFFF96DC; case "S" -> 0xFFFFC85A; case "A" -> 0xFFFFD65A;
            case "B" -> 0xFF78DCFF; case "C" -> 0xFF78E696; case "D" -> 0xFFBEC8D2; default -> 0xFFA0AAB4;
        };
    }

    @Override
    void draw(GuiGraphicsExtractor g, double mx, double my) {
        LocalPlayer p = player();
        TrainingData d = Training.get(p);
        String line = Potential.line(p);
        Attr[] all = Attr.values();
        scroll = Math.max(0, Math.min(scroll, all.length - ROWS.length));

        TOP.push(g);
        for (int r = 0; r < ROWS.length; r++) {
            int i = r + scroll;
            if (i >= all.length) break;
            Attr a = all[i];
            int y = ROWS[r], lvl = d.level(a), cap = Potential.limit(p, a);
            String grade = Potential.grade(Potential.talent(line, a));
            if (a == selected) {
                UI.fill(g, 400, y - 17, 996, y + 17, 0x40288CC8);
                UI.rect(g, 400, y - 17, 996, y + 17, 2, 0xFF78F0FF);
            }
            int[] is = UI.size("attr/" + a.key());
            UI.tex(g, "attr/" + a.key(), 427 - is[0] * 0.44f, y - is[1] * 0.44f, is[0] * 0.88f, is[1] * 0.88f);
            UI.semi(g, Component.translatable("attr.jaycee." + a.key()), 455, y, 19, 0xFFFFFFFF, 0);
            UI.semi(g, "Lv.", 617, y, 19, 0xFF5AE6FF, 0);
            UI.text(g, String.valueOf(lvl), 646, y, 21, 0xFFFFFFFF, 0);
            if (lvl >= cap) {
                UI.text(g, Component.translatable("jaycee.ui.max"), 887, y - 9, 15, 0xFFFFD65A, 2);
                UI.bar(g, 693, y + 4, 194, 11, 1, 0xFFE6A028, 0xFFFFDC5A);
            } else {
                long need = Potential.need(lvl);
                UI.semi(g, UI.num(d.exp(a)) + " / " + UI.num(need), 887, y - 9, 15, 0xFFFFFFFF, 2);
                UI.bar(g, 693, y + 4, 194, 11, d.exp(a) / need, 0xFF9646E6, 0xFFDC82FF);
            }
            UI.text(g, grade, 931, y, 22, gradeColor(grade), 1);
        }
        // scrollbar
        UI.fill(g, 998, 238, 1004, 742, 0xFF0E2838);
        float th = 504f * ROWS.length / all.length, ty = 238 + (504 - th) * scroll / Math.max(1, all.length - ROWS.length);
        UI.fill(g, 998, ty, 1004, ty + th, 0xFF5AE6FF);

        // details
        Attr a = selected;
        int lvl = d.level(a), cap = Potential.limit(p, a), talent = Potential.talent(line, a);
        int[] is = UI.size("attr/" + a.key());
        UI.tex(g, "attr/" + a.key(), 1090 - is[0] / 2f, 227 - is[1] / 2f);
        UI.text(g, UI.fit(Component.translatable("attr.jaycee." + a.key()).getString(), 23, 250, true), 1132, 227, 23, 0xFFFFFFFF, 0);
        UI.semi(g, "Lv. " + lvl, 1425, 229, 18, 0xFFC8FAFF, 1);
        List<String> desc = UI.wrap(Component.translatable("attr.jaycee." + a.key() + ".desc").getString(), 17, 400, false);
        for (int k = 0; k < Math.min(2, desc.size()); k++) UI.semi(g, desc.get(k), 1062, 262 + k * 23, 17, 0xFFEBF0F5, 0);
        String[] src = Component.translatable("attr.jaycee." + a.key() + ".sources").getString().split("\n");
        for (int k = 0; k < Math.min(3, src.length); k++) {
            UI.text(g, "•", 1088, 349 + k * 22, 16, 0xFFC8F0FF, 1);
            UI.semi(g, UI.fit(src[k], 15, 360, false), 1102, 349 + k * 22, 15, 0xFFEBF0F5, 0);
        }
        Race race = RaceAPI.race(p).orElse(null);
        UI.semi(g, "Race :", 1130, 486, 17, 0xFFE1EBF5, 0);
        UI.text(g, UI.fit(race == null ? "-" : race.name().getString(), 19, 260, true), 1188, 486, 19, 0xFFFFFFFF, 0);
        UI.semi(g, "Evolution :", 1130, 513, 17, 0xFFE1EBF5, 0);
        String stage = race == null ? "-" : Component.translatable("jaycee.ui.tier", race.tier()).getString()
                + (race.isDivine() ? " · " + Component.translatable("jaycee.ui.divine").getString() : "");
        UI.text(g, stage, 1222, 513, 19, 0xFFF060F0, 0);
        UI.semi(g, UI.fit("Attribute Potential (" + Component.translatable("attr.jaycee." + a.key()).getString() + ")", 14, 285, false), 1068, 550, 14, 0xFFEBF0F5, 0);
        UI.text(g, "Lv. " + lvl, 1336, 578, 17, 0xFF6EE6FF, 2);
        UI.text(g, "Lv. " + Potential.baseLimit(talent), 1336, 605, 17, 0xFFFFFFFF, 2);
        UI.text(g, "Lv. " + cap, 1336, 632, 17, 0xFFFFD63C, 2);
        String grade = Potential.grade(talent);
        UI.text(g, grade, 1421, 606, grade.length() == 1 ? 48 : 34, 0xFFFF6EF0, 1);
        if (lvl >= cap) UI.semi(g, Component.translatable("jaycee.ui.evolve_hint"), 1068, 660, 13, 0xFFFFD65A, 0);
        // current bonus
        UI.text(g, "CURRENT BONUS", 1112, 698, 20, 0xFFE1F0FF, 0);
        float x = 1072;
        for (B b : BON.getOrDefault(a, new B[0])) {
            UI.semi(g, b.label(), x, 740, 17, 0xFFEBF0F5, 0);
            x += UI.width(b.label(), 17, false) + 10;
            String val = String.format(Locale.US, b.fmt(), b.fmt().contains("%d") ? (Object) (int) Math.round(b.perLevel() * lvl) : (Object) (b.perLevel() * lvl));
            UI.text(g, val, x, 740, 23, 0xFF5AF0FF, 0);
            x += UI.width(val, 23, true) + 26;
        }
        TOP.pop(g);

        // training feed
        BOT_L.push(g);
        long now = p.level().getGameTime();
        List<TrainingData.Recent> feed = d.recent().stream().filter(r -> now - r.lastTick() <= 100).toList();
        feedScroll = Math.max(0, Math.min(feedScroll, feed.size() - FEED_Y.length));
        for (int k = 0; k < FEED_Y.length && k + feedScroll < feed.size(); k++) {
            TrainingData.Recent r = feed.get(k + feedScroll);
            Attr ra = Attr.byKey(r.attr());
            if (ra == null) continue;
            long age = now - r.lastTick();
            int alpha = age < 80 ? 255 : (int) (255 * (100 - age) / 20.0);
            int fade = (Math.max(30, alpha) << 24);
            int y = FEED_Y[k];
            int[] fs = UI.size("attr/" + ra.key());
            UI.tex(g, "attr/" + ra.key(), 128 - fs[0] * 0.4f, y - fs[1] * 0.4f, fs[0] * 0.8f, fs[1] * 0.8f, fade | 0xFFFFFF);
            String label = Component.translatable("attr.jaycee." + ra.key()).getString() + " (" + Component.translatable("activity.jaycee." + r.activity()).getString() + ")";
            UI.semi(g, UI.fit(label, 17, 215, false), 165, y, 17, fade | 0xFFFFFF, 0);
            int lvl2 = d.level(ra);
            double frac = lvl2 >= Potential.limit(p, ra) ? 1 : d.exp(ra) / Potential.need(lvl2);
            UI.bar(g, 387, y - 7, 298, 14, frac, fade | 0x28C8E6, fade | 0x6EFAFF);
            UI.text(g, "+" + UI.num(r.gained()) + " EXP", 826, y, 18, fade | 0xF05AE6, 2);
            UI.semi(g, String.format(Locale.US, "%.1fs", Math.max(0, (100 - age) / 20.0)), 915, y, 17, fade | 0xDCEBF5, 2);
        }
        if (feed.size() > FEED_Y.length) {
            UI.fill(g, 968, 830, 973, 948, 0xFF0E2838);
            float fh = 118f * FEED_Y.length / feed.size(), fy = 830 + (118 - fh) * feedScroll / Math.max(1, feed.size() - FEED_Y.length);
            UI.fill(g, 968, fy, 973, fy + fh, 0xFF5AE6FF);
        }
        BOT_L.pop(g);
    }

    @Override
    boolean click(double fx, double fy, int b) {
        double x = TOP.x(fx), y = TOP.y(fy);
        for (int r = 0; r < ROWS.length; r++) {
            int i = r + scroll;
            if (i < Attr.values().length && in(x, y, 400, ROWS[r] - 17, 996, ROWS[r] + 17)) { selected = Attr.values()[i]; return true; }
        }
        return false;
    }

    @Override
    boolean scroll(double fx, double fy, double dy) {
        double bx = BOT_L.x(fx), by = BOT_L.y(fy);
        if (in(bx, by, 100, 825, 990, 955)) { feedScroll += dy < 0 ? 1 : -1; return true; }
        scroll += dy < 0 ? 1 : -1;
        return true;
    }
}

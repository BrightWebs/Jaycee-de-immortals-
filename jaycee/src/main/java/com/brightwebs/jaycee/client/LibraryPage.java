package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.client.BrightKeys;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.network.BrightNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Skills and Magic: categories, a searchable library, the selected entry's details and the
 * shared ability presets. Only what the player owns is listed. Equip: pick an entry, click a slot.
 */
final class LibraryPage extends Page {
    /** Everything that differs between the two layouts, in each reference's own pixels. */
    private record L(T t, String bg, String[] cats, int[] catY, int catX1, int catX2, String catSel, int catSelW, int catSelH,
                     int listX, int listTop, int listStep, int rows, String card, String cardSel, int cardX, int cardSelX,
                     int iconX, int nameX, int lvX, int barX, int barW, int pillX, int searchX1, int searchY1, int searchX2, int searchY2,
                     int detIconX, int detIconY, int detIcon, int nameDX, int nameDY, int nameMax, int pillDX, int pillDY,
                     int[] presetX, int presetY, String presetOn, int[][] slots, int slotKeyY, int slotIconY, int slotNameY,
                     int scrollX, int scrollY1, int scrollY2) {}

    private static final L SKILLS = new L(new T(474f, 235.598f, 0.82765f, 343, 145), "bg_skills",
            new String[]{"resistance", "intrinsic", "common", "extra", "unique", "ultimate"}, new int[]{260, 375, 490, 607, 722, 836},
            352, 604, "sk_cat_sel", 244, 108,
            618, 256, 117, 5, "card", "card_sel", 618, 616, 677, 730, 818, 732, 246, 880, 624, 210, 884, 244,
            1101, 253, 80, 1165, 217, 180, 1350, 205,
            new int[]{1081, 1137, 1186, 1236, 1286, 1334, 1383, 1432, 1480}, 687, "sk_preset_on",
            new int[][]{{1046, 1155}, {1168, 1268}, {1280, 1380}, {1392, 1492}}, 749, 800, 846, 999, 258, 846);
    private static final L MAGIC = new L(new T(474f, 242.440f, 0.79835f, 300, 140), "bg_magic",
            new String[]{"aspectual", "elemental", "holy", "summoning", "other"}, new int[]{255, 364, 474, 585, 695},
            306, 584, "mg_cat_sel", 272, 120,
            594, 256, 115, 5, "mg_card", "mg_card_sel", 594, 592, 650, 708, 802, 708, 254, 868, 600, 206, 880, 242,
            1078, 262, 90, 1155, 215, 175, 1340, 205,
            new int[]{1062, 1118, 1170, 1222, 1274, 1326, 1378, 1430, 1482}, 663, "mg_preset_on",
            new int[][]{{1040, 1148}, {1158, 1265}, {1276, 1383}, {1395, 1500}}, 724, 790, 832, 985, 254, 832);

    private final boolean magic;
    private final L l;
    private static String skillCat = "unique", magicCat = "aspectual";
    private static Identifier skillPick, magicPick;
    private int scroll;
    private String search = "";
    private boolean searching;

    LibraryPage(MenuScreen m, boolean magic) {
        super(m);
        this.magic = magic;
        this.l = magic ? MAGIC : SKILLS;
    }

    @Override String background() { return l.bg(); }

    private String cat() { return magic ? magicCat : skillCat; }
    private Identifier pick() { return magic ? magicPick : skillPick; }
    private void pick(Identifier id) { if (magic) magicPick = id; else skillPick = id; }

    private List<Ability> listed(LocalPlayer p) {
        List<Ability> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Ability a : AbilityAPI.owned(p)) {
            if (a.isMagic() != magic) continue;
            if (!q.isEmpty() ? !a.name().getString().toLowerCase(Locale.ROOT).contains(q) : !Skin.inCategory(a, cat())) continue;
            out.add(a);
        }
        out.sort((x, y) -> x.name().getString().compareTo(y.name().getString()));
        return out;
    }

    private Ability selected(LocalPlayer p) {
        Identifier id = pick();
        if (id != null) for (Ability a : AbilityAPI.owned(p)) if (a.id().equals(id)) return a;
        List<Ability> list = listed(p);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    void draw(GuiGraphicsExtractor g, double fmx, double fmy) {
        LocalPlayer p = player();
        l.t().push(g);
        // category highlight
        String[] cats = l.cats();
        for (int i = 0; i < cats.length; i++) {
            if (cats[i].equals(cat()) && search.isEmpty()) UI.tex(g, l.catSel(), (l.catX1() + l.catX2() - l.catSelW()) / 2f, l.catY()[i] - l.catSelH() / 2f);
        }
        // search
        if (searching || !search.isEmpty()) {
            UI.fill(g, l.searchX1() + 44, l.searchY1() + 5, l.searchX2() - 6, l.searchY2() - 5, 0xFF060C16);
            String shown = search + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            UI.semi(g, shown, l.searchX1() + 50, (l.searchY1() + l.searchY2()) / 2f, 16, 0xFFE6F6FF, 0);
        }
        // library
        List<Ability> list = listed(p);
        scroll = Math.max(0, Math.min(scroll, list.size() - l.rows()));
        Ability sel = selected(p);
        if (list.isEmpty()) {
            UI.semi(g, Component.translatable(magic ? "jaycee.ui.no_magic" : "jaycee.ui.no_skills"),
                    l.listX() + 190, l.listTop() + 140, 16, 0xFF7FB8CC, 1);
        }
        for (int r = 0; r < l.rows() && r + scroll < list.size(); r++) {
            Ability a = list.get(r + scroll);
            int top = l.listTop() + r * l.listStep();
            boolean on = sel != null && a.id().equals(sel.id());
            if (on) UI.tex(g, l.cardSel(), l.cardSelX(), top);
            else UI.tex(g, l.card(), l.cardX(), top + 4);
            UI.abilityIcon(g, a, l.iconX(), top + 59, 64);
            UI.text(g, UI.fit(a.name().getString(), 25, 145, true), l.nameX(), top + 31, 25, 0xFFFFFFFF, 0);
            UI.semi(g, Component.translatable("jaycee.ui.mastery"), l.nameX(), top + 68, 17, 0xFFE6EBF5, 0);
            String lv = Component.translatable("jaycee.ui.lv_of", AbilityAPI.level(p, a), 10).getString();
            if (a.isMagic()) lv += " · " + Component.translatable("magic.jaycee.school." + a.school()).getString();
            UI.semi(g, UI.fit(lv, 17, 170, false), l.lvX(), top + 68, 17, 0xFFFFFFFF, 0);
            UI.fill(g, l.barX(), top + 86, l.barX() + l.barW(), top + 99, 0xFF0A0E1E);
            UI.rect(g, l.barX(), top + 86, l.barX() + l.barW(), top + 99, 1, 0xFF6E50AA);
            UI.texPart(g, "mastery_fill", l.barX(), top + 86, l.barW(), 13, AbilityAPI.masteryFraction(p, a));
            UI.tex(g, "lp_" + Skin.pill(a), l.pillX(), top + 14);
        }
        // scrollbar
        UI.fill(g, l.scrollX(), l.scrollY1(), l.scrollX() + 7, l.scrollY2(), 0xFF071420);
        if (list.size() > l.rows()) {
            float h = (l.scrollY2() - l.scrollY1()) * (float) l.rows() / list.size();
            float y = l.scrollY1() + (l.scrollY2() - l.scrollY1() - h) * scroll / Math.max(1, list.size() - l.rows());
            UI.fill(g, l.scrollX(), y, l.scrollX() + 7, y + h, 0xFF3FE0FF);
        }
        if (sel != null) details(g, p, sel);
        presets(g, p, sel);
        l.t().pop(g);
    }

    private void details(GuiGraphicsExtractor g, LocalPlayer p, Ability a) {
        UI.abilityIcon(g, a, l.detIconX(), l.detIconY(), l.detIcon());
        float ns = UI.fitSize(a.name().getString(), magic ? 30 : 32, 18, l.nameMax(), true);
        UI.text(g, a.name().getString(), l.nameDX(), l.nameDY(), ns, 0xFFFFFFFF, 0);
        UI.tex(g, "dp_" + Skin.pill(a), l.pillDX(), l.pillDY());
        int lvl = AbilityAPI.level(p, a);
        if (!magic) {
            List<String> desc = UI.wrap(a.description().getString(), 15, 330, false);
            for (int k = 0; k < Math.min(3, desc.size()); k++) UI.semi(g, desc.get(k), 1166, 258 + k * 19, 15, 0xFFF0F0FA, 0);
            UI.text(g, Component.translatable("jaycee.ui.lv_of", lvl, 10), 1488, 333, 20, 0xFFF060F0, 2);
            UI.text(g, AbilityAPI.mastery(p, a) + " / " + a.maxMastery(), 1488, 365, 20, 0xFFF060F0, 2);
            String act = a.isPassive() ? Component.translatable("jaycee.ui.passive").getString()
                    : a.isToggle() ? Component.translatable("jaycee.ui.toggle").getString()
                    : Component.translatable("jaycee.ui.active").getString()
                    + (a.modes().size() > 1 ? " · " + Component.translatable("jaycee.ui.modes", a.modes().size()).getString() : "");
            UI.text(g, act, 1488, 397, 18, 0xFF50E6FF, 2);
            // main functions = modes
            int[][] fx = {{1058, 1194}, {1209, 1346}, {1361, 1494}};
            int cur = AbilityAPI.modeOf(p, a), n = Math.min(3, a.modes().size());
            for (int i = 0; i < n; i++) {
                float cx = (fx[i][0] + fx[i][1]) / 2f;
                if (n > 1 && i == cur) UI.rect(g, fx[i][0] + 2, 456, fx[i][1] - 2, 566, 2, 0xFFFFC24A);
                if (n == 1) {
                    UI.abilityIcon(g, a, cx, 482, 44);
                    UI.text(g, UI.fit(a.name().getString(), 17, 128, true), cx, 512, 17, 0xFFFFFFFF, 1);
                    UI.semi(g, Component.translatable(a.isPassive() ? "jaycee.ui.passive" : a.isToggle() ? "jaycee.ui.toggle" : "jaycee.ui.active"), cx, 540, 14, 0xFFEBEBF5, 1);
                    continue;
                }
                UI.modeIcon(g, a, i, cx, 482, 44);
                UI.text(g, UI.fit(a.modeName(i).getString(), 17, 128, true), cx, 512, 17, 0xFFFFFFFF, 1);
                String[] d = Component.translatable(a.descriptionId() + ".mode." + a.mode(i).key() + ".desc").getString().split("\\n");
                for (int k = 0; k < Math.min(2, d.length); k++) UI.semi(g, UI.fit(d[k], 13, 128, false), cx, 538 + k * 18, 13, 0xFFEBEBF5, 1);
            }
        } else {
            UI.tex(g, "dp_school", 1146, 251);
            UI.semi(g, Component.translatable("magic.jaycee.school." + a.school()), 1213, 269, 17, 0xFFFFFFFF, 1);
            List<String> desc = UI.wrap(a.description().getString(), 16, 345, false);
            for (int k = 0; k < Math.min(3, desc.size()); k++) UI.semi(g, desc.get(k), 1150, 300 + k * 22.5f, 16, 0xFFF0F0FA, 0);
            UI.text(g, Component.translatable("jaycee.ui.lv_of", lvl, 10), 1490, 370, 20, 0xFFF060F0, 2);
            UI.text(g, Component.translatable("jaycee.ui.mp", a.manaCost()), 1490, 408, 20, 0xFF50E6FF, 2);
            boolean instant = a.castTicks() <= 0;
            UI.text(g, instant ? Component.translatable("jaycee.ui.instant") : Component.translatable("jaycee.ui.seconds",
                    String.format(Locale.US, "%.1f", a.castTicks() / 20f)), 1490, 445, 20, 0xFF50E6FF, 2);
            UI.text(g, Component.translatable("jaycee.ui.blocks", a.range()), 1490, 483, 20, 0xFF50E6FF, 2);
            UI.text(g, Component.translatable(a.descriptionId() + ".effect"), 1490, 521, 18, 0xFFFFFFFF, 2);
        }
    }

    private void presets(GuiGraphicsExtractor g, LocalPlayer p, Ability sel) {
        AbilityData d = AbilityAPI.get(p);
        UI.tex(g, l.presetOn(), l.presetX()[d.page()] - 32, l.presetY() - 30);
        for (int i = 0; i < 9; i++) UI.semi(g, String.valueOf(i + 1), l.presetX()[i], l.presetY(), 24, 0xFFF0FAFF, 1);
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            int[] s = l.slots()[i];
            float cx = (s[0] + s[1]) / 2f;
            UI.text(g, BrightKeys.label(i), cx, l.slotKeyY(), 18, 0xFFFFFFFF, 1);
            Ability a = BrightCoreClient.abilityIn(d, i);
            if (a == null) {
                UI.semi(g, "+", cx, l.slotIconY(), 40, 0xFFA8B4C0, 1);
                UI.semi(g, Component.translatable("jaycee.ui.empty"), cx, l.slotNameY(), 15, 0xFFA8B4C0, 1);
                continue;
            }
            UI.abilityIcon(g, a, cx, l.slotIconY(), 54);
            UI.semi(g, UI.fit(a.name().getString(), 14, s[1] - s[0] - 12, false), cx, l.slotNameY(), 14, 0xFFFFFFFF, 1);
            int[] ps = UI.size("lp_" + Skin.pill(a));
            UI.tex(g, "lp_" + Skin.pill(a), cx - ps[0] * 0.42f, l.slotNameY() + 12, ps[0] * 0.84f, ps[1] * 0.84f);
        }
    }

    @Override
    boolean click(double fx, double fy, int b) {
        LocalPlayer p = player();
        double x = l.t().x(fx), y = l.t().y(fy);
        searching = b == 0 && in(x, y, l.searchX1(), l.searchY1(), l.searchX2(), l.searchY2());
        if (searching) return true;
        String[] cats = l.cats();
        for (int i = 0; i < cats.length; i++) {
            if (b == 0 && in(x, y, l.catX1(), l.catY()[i] - 52, l.catX2(), l.catY()[i] + 52)) {
                if (magic) magicCat = cats[i]; else skillCat = cats[i];
                search = ""; scroll = 0; pick(null);
                return true;
            }
        }
        List<Ability> list = listed(p);
        for (int r = 0; r < l.rows() && r + scroll < list.size(); r++) {
            int top = l.listTop() + r * l.listStep();
            if (b == 0 && in(x, y, l.cardX(), top, l.cardX() + 380, top + 112)) { pick(list.get(r + scroll).id()); return true; }
        }
        Ability sel = selected(p);
        if (!magic && sel != null && b == 0 && sel.modes().size() > 1) {
            int[][] mx = {{1058, 1194}, {1209, 1346}, {1361, 1494}};
            for (int i = 0; i < Math.min(3, sel.modes().size()); i++) {
                if (in(x, y, mx[i][0], 454, mx[i][1], 568)) {
                    ClientPacketDistributor.sendToServer(new BrightNetwork.SetMode(sel.id().toString(), i));
                    return true;
                }
            }
        }
        AbilityData d = AbilityAPI.get(p);
        for (int i = 0; i < 9; i++) {
            if (b == 0 && in(x, y, l.presetX()[i] - 25, l.presetY() - 22, l.presetX()[i] + 25, l.presetY() + 22)) {
                ClientPacketDistributor.sendToServer(new BrightNetwork.SetPage(i));
                return true;
            }
        }
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            int[] s = l.slots()[i];
            if (!in(x, y, s[0], l.slotKeyY() - 20, s[1], l.slotNameY() + 40)) continue;
            int index = d.page() * AbilityData.SLOTS_PER_PAGE + i;
            if (b == 1) ClientPacketDistributor.sendToServer(new BrightNetwork.AssignSlot(index, ""));
            else if (sel != null && !sel.isPassive()) ClientPacketDistributor.sendToServer(new BrightNetwork.AssignSlot(index, sel.id().toString()));
            return true;
        }
        return false;
    }

    @Override
    boolean scroll(double fx, double fy, double dy) {
        scroll += dy < 0 ? 1 : -1;
        return true;
    }

    @Override
    boolean typed(int cp) {
        if (!searching || cp < 32 || search.length() >= 24) return false;
        search += new String(Character.toChars(cp));
        scroll = 0;
        return true;
    }

    @Override
    boolean key(int key) {
        if (!searching) return false;
        if (key == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
        if (key == 257) { searching = false; return true; }
        return true; // swallow keys while typing so ability keys don't fire
    }
}

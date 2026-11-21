package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.brightcore.client.BrightKeys;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.network.BrightNetwork;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Locale;

// status: individual card, EP, parameters, souls, ability presets and the Z/X/C/V cards
final class StatusPage extends Page {
    static final int[] PRESET_X = {887, 960, 1022, 1086, 1150, 1212, 1276, 1339, 1401};
    static final int[] CARD_X = {849, 1003, 1153, 1302}, CARD_CX = {922, 1075, 1225, 1373};

    StatusPage(MenuScreen m) { super(m); }

    @Override String background() {
        return StatusData.hasGuildCard(player()) ? "bg_status" : "bg_status_norank";
    }

    @Override
    void draw(GuiGraphicsExtractor g, double mx, double my) {
        LocalPlayer p = player();
        EnergyData e = EnergyAPI.get(p);
        // individual
        UI.text(g, UI.fit(p.getName().getString(), 30, 282, true), 509, 357, 30, 0xFFFFFFFF, 0);
        Component race = RaceAPI.race(p).map(r -> r.name()).orElse(Component.translatable("jaycee.ui.no_race"));
        UI.text(g, UI.fit(race.getString(), 28, 186, true), 578, 454, 28, 0xFFFFFFFF, 0);
        String t = StatusData.title(p);
        if (t.isEmpty()) UI.semi(g, Component.translatable("jaycee.ui.no_title"), 578, 550, 24, 0xFF9A78B8, 0);
        else UI.text(g, UI.fit(Component.translatable("title.jaycee." + t).getString(), 25, 182, true), 578, 550, 25, 0xFFF060F0, 0);
        if (StatusData.hasGuildCard(p)) {
            String rank = StatusData.rank(e.ep());
            UI.text(g, rank.substring(rank.length() - 1), 537, 649, 24, 0xFF281800, 1);
            UI.text(g, rank, 580, 647, 30, 0xFFFFC81A, 0);
        }
        // EP gets smaller as the number grows, short form from a billion up
        double ep = e.ep();
        String eps = ep < 1_000_000_000 ? UI.num(ep) : UI.shortNum(ep);
        float size = UI.fitSize(eps, 40, 24, 222, true);
        UI.text(g, eps, 566, 773, size, 0xFF5FF4FF, 0);
        if (ep >= 1_000_000_000 && Page.in(mx, my, 560, 745, 800, 800)) tooltip(g, UI.num(ep) + " EP", mx, my);
        // parameters
        double[][] v = {{p.getHealth(), p.getMaxHealth()}, {e.spirit(), e.maxSpirit()}, {e.mana(), e.maxMana()}, {e.aura(), e.maxAura()}};
        String[] fills = {"fill_hp", "fill_spirit", "fill_mana", "fill_aura"};
        int[] cy = {284, 340, 396, 452}, by = {297, 353, 409, 465};
        for (int i = 0; i < 4; i++) {
            double cur = Math.max(0, v[i][0]), max = Math.max(1, v[i][1]);
            String pct = cur >= max ? "(100%)" : String.format(Locale.US, "(%.1f%%)", 100 * cur / max);
            UI.semi(g, pct, 1418, cy[i], 20, 0xFFEBF0F5, 2);
            UI.text(g, UI.num(Math.ceil(cur)) + " / " + UI.num(max), 1418 - UI.width(pct, 20, false) - 22, cy[i], 21, 0xFFFFFFFF, 2);
            float w = (float) (483 * Math.min(1, cur / max));
            if (w > 1) UI.tex(g, fills[i], 935, by[i], w, 23);
        }
        UI.text(g, UI.num(StatusData.souls(p)), 1414, 518, 36, 0xFFD860F0, 2);
        // ability presets
        AbilityData d = AbilityAPI.get(p);
        UI.tex(g, "st_preset_on", PRESET_X[d.page()] - 39, 603);
        for (int i = 0; i < 9; i++) UI.semi(g, String.valueOf(i + 1), PRESET_X[i], 633, 30, 0xFFF0FAFF, 1);
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) card(g, d, i);
    }

    private void card(GuiGraphicsExtractor g, AbilityData d, int i) {
        Ability a = BrightCoreClient.abilityIn(d, i);
        int x = CARD_X[i], cx = CARD_CX[i];
        String rank = a == null ? "extra" : Skin.rank(a);
        UI.tex(g, "stcard_" + rank, x, 697, 143, 183, a == null ? 0xFF9AA8B8 : 0xFFFFFFFF);
        UI.text(g, BrightKeys.label(i), cx, 715, 27, 0xFFFFFFFF, 1);
        if (a == null) {
            UI.semi(g, "+", cx, 768, 44, 0xFFB8C8D8, 1);
            UI.semi(g, Component.translatable("jaycee.ui.empty"), cx, 836, 18, 0xFFB8C8D8, 1);
            return;
        }
        UI.abilityIcon(g, a, cx, 762, 62);
        float cd = BrightCoreClient.cooldownFraction(a, d);
        if (cd > 0) {
            UI.fill(g, cx - 31, 731, cx + 31, 731 + 62 * cd, 0xB0050A18);
            UI.text(g, String.valueOf(BrightCoreClient.cooldownSeconds(a, d)), cx, 762, 22, 0xFFFFFFFF, 1);
        }
        if (a.isToggle() && d.isToggled(a.id())) UI.fill(g, cx - 31, 795, cx + 31, 797, 0xFF5FE07A);
        List<String> lines = UI.wrap(a.name().getString(), 22, 128, false);
        if (lines.size() == 1) UI.semi(g, lines.get(0), cx, 815, 23, 0xFFFFFFFF, 1);
        else {
            UI.semi(g, UI.fit(lines.get(0), 21, 130, false), cx, 803, 21, 0xFFFFFFFF, 1);
            UI.semi(g, UI.fit(lines.get(1), 21, 130, false), cx, 826, 21, 0xFFFFFFFF, 1);
        }
        int[] bs = UI.size("stbadge_" + rank);
        UI.tex(g, "stbadge_" + rank, cx - bs[0] / 2f, 839);
    }

    static void tooltip(GuiGraphicsExtractor g, String s, double x, double y) {
        float w = UI.width(s, 18, false) + 24;
        UI.fill(g, (float) x + 12, (float) y - 40, (float) x + 12 + w, (float) y - 6, 0xF00A1424);
        UI.rect(g, (float) x + 12, (float) y - 40, (float) x + 12 + w, (float) y - 6, 2, 0xFF78F0FF);
        UI.semi(g, s, (float) x + 24, (float) y - 23, 18, 0xFFE6FAFF, 0);
    }

    @Override
    boolean click(double x, double y, int b) {
        LocalPlayer p = player();
        if (b == 0 && in(x, y, 494, 430, 798, 480)) { menu.open(MenuScreen.Id.RACE); return true; }
        if (b == 0 && in(x, y, 494, 526, 798, 576)) { menu.open(MenuScreen.Id.TITLES); return true; }
        for (int i = 0; i < 9; i++) {
            if (b == 0 && in(x, y, PRESET_X[i] - 30, 612, PRESET_X[i] + 30, 655)) {
                ClientPacketDistributor.sendToServer(new BrightNetwork.SetPage(i));
                return true;
            }
        }
        AbilityData d = AbilityAPI.get(p);
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) {
            if (!in(x, y, CARD_X[i], 697, CARD_X[i] + 143, 880)) continue;
            if (b == 1) ClientPacketDistributor.sendToServer(new BrightNetwork.AssignSlot(d.page() * AbilityData.SLOTS_PER_PAGE + i, ""));
            else {
                Ability a = BrightCoreClient.abilityIn(d, i);
                menu.open(a != null && a.isMagic() ? MenuScreen.Id.MAGIC : MenuScreen.Id.SKILLS);
            }
            return true;
        }
        return false;
    }
}

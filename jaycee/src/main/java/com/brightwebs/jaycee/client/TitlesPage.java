package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.network.JayceeNetwork;
import com.brightwebs.jaycee.status.StatusData;
import com.brightwebs.jaycee.status.Titles;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

// titles: what you've earned, what they do, and which one you have on (opened from the title field)
final class TitlesPage extends Page {
    private static final T TF = new T(474f, 241.214f, 0.81513f, 332, 140);
    private static final int ROWS = 6, TOP = 270, STEP = 98;
    private String viewing;
    private int scroll;

    TitlesPage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_titles"; }

    private static String name(String id) { return Component.translatable("title.jaycee." + id).getString(); }

    @Override
    void draw(GuiGraphicsExtractor g, double fmx, double fmy) {
        LocalPlayer p = player();
        List<String> owned = StatusData.titles(p).stream().filter(Titles.ALL::containsKey).toList();
        String equipped = StatusData.title(p);
        if (viewing == null || !owned.contains(viewing)) viewing = owned.contains(equipped) ? equipped : owned.isEmpty() ? null : owned.get(0);
        scroll = Math.max(0, Math.min(scroll, owned.size() - ROWS));
        TF.push(g);
        UI.text(g, owned.size() + " / " + Titles.ALL.size(), 945, 173, 18, 0xFFF060F0, 0);
        if (owned.isEmpty()) UI.semi(g, Component.translatable("jaycee.ui.no_titles"), 684, 560, 17, 0xFF7FB8CC, 1);
        for (int r = 0; r < ROWS && r + scroll < owned.size(); r++) {
            String id = owned.get(r + scroll);
            Titles.Title t = Titles.get(id);
            int top = TOP + r * STEP;
            if (id.equals(viewing)) UI.tex(g, "trow_sel", 346, top - 8);
            else UI.tex(g, "trow", 355, top);
            int[] es = UI.size("temb" + t.emblem());
            UI.tex(g, "temb" + t.emblem(), 423 - es[0] * 0.45f, top + 48 - es[1] * 0.45f, es[0] * 0.9f, es[1] * 0.9f);
            UI.text(g, UI.fit("[ " + name(id) + " ]", 24, 360, true), 500, top + 26, 24, 0xFFFFFFFF, 0);
            List<String> desc = UI.wrap(Component.translatable("title.jaycee." + id + ".desc").getString(), 14, 360, false);
            for (int k = 0; k < Math.min(2, desc.size()); k++) UI.semi(g, desc.get(k), 494, top + 58 + k * 19, 14, 0xFFF0F0FA, 0);
            UI.tex(g, "tp_" + t.rank(), 876, top + 10);
            UI.tex(g, id.equals(equipped) ? "tp_selected" : "tp_equip", id.equals(equipped) ? 873 : 880, top + 56);
        }
        if (viewing != null) {
            Titles.Title t = Titles.get(viewing);
            int[] es = UI.size("temb" + t.emblem());
            UI.tex(g, "temb" + t.emblem(), 1140 - es[0] * 0.62f, 262 - es[1] * 0.62f, es[0] * 1.24f, es[1] * 1.24f);
            UI.text(g, UI.fit("[ " + name(viewing) + " ]", 24, 300, true), 1207, 224, 24, 0xFFFFFFFF, 0);
            UI.tex(g, "tp_" + t.rank(), 1240, 252);
            List<String> desc = UI.wrap(Component.translatable("title.jaycee." + viewing + ".desc").getString(), 16, 296, false);
            for (int k = 0; k < Math.min(2, desc.size()); k++) UI.semi(g, desc.get(k), 1205, 309 + k * 24, 16, 0xFFF0F0FA, 0);
            int[] ey = {417, 453, 488, 523};
            for (int i = 0; i < Math.min(4, t.effects().size()); i++) {
                Titles.Effect e = t.effects().get(i);
                UI.text(g, "◆", 1123, ey[i], 14, 0xFF5AE6FF, 1);
                UI.semi(g, Component.translatable("title.effect.jaycee." + e.label()), 1157, ey[i], 17, 0xFFF0F0FA, 0);
                UI.text(g, e.value(), 1480, ey[i], 18, 0xFFF060F0, 2);
            }
        }
        if (!StatusData.titlePublic(p)) {
            UI.fill(g, 1402, 655, 1484, 691, 0xFF0A1C2A);
            UI.rect(g, 1406, 659, 1480, 687, 2, 0xFF2F6A88);
            UI.fill(g, 1411, 663, 1435, 683, 0xFF8090A0);
        }
        String shown = Titles.get(equipped) != null && Titles.get(equipped).rank().equals("greater") ? "[ " + name(equipped) + " ]"
                : Component.translatable("jaycee.ui.none").getString();
        UI.text(g, shown, 1293, 785, 21, 0xFFFFFFFF, 1);
        TF.pop(g);
    }

    @Override
    boolean click(double fx, double fy, int b) {
        double x = TF.x(fx), y = TF.y(fy);
        LocalPlayer p = player();
        if (b != 0) return false;
        if (in(x, y, 1400, 650, 1488, 694)) {
            ClientPacketDistributor.sendToServer(new JayceeNetwork.TitlePublic(!StatusData.titlePublic(p)));
            return true;
        }
        List<String> owned = StatusData.titles(p).stream().filter(Titles.ALL::containsKey).toList();
        for (int r = 0; r < ROWS && r + scroll < owned.size(); r++) {
            String id = owned.get(r + scroll);
            int top = TOP + r * STEP;
            if (in(x, y, 872, top + 54, 996, top + 92)) {
                ClientPacketDistributor.sendToServer(new JayceeNetwork.EquipTitle(id.equals(StatusData.title(p)) ? "" : id));
                return true;
            }
            if (in(x, y, 355, top, 1008, top + 96)) { viewing = id; return true; }
        }
        return false;
    }

    @Override
    boolean scroll(double fx, double fy, double dy) { scroll += dy < 0 ? 1 : -1; return true; }
}

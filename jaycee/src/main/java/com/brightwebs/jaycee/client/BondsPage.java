package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// bonds: subordinates, allies and party. you get subordinates by naming stuff with N
final class BondsPage extends Page {
    BondsPage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_bonds"; }
    @Override boolean showsPlayer() { return false; }

    // the bonds layout sits inside the same frame as status
    private static final T TF = new T(96, 228, 0.911f, 24, 150);

    @Override
    void draw(GuiGraphicsExtractor g, double mx, double my) {
        TF.push(g);
        content(g);
        TF.pop(g);
    }

    private void content(GuiGraphicsExtractor g) {
        var subs = com.brightwebs.jaycee.bonds.Bonds.subs(player());
        UI.text(g, "Total: " + subs.size(), 990, 203, 22, 0xFFFFFFFF, 1);
        if (subs.isEmpty()) {
            UI.semi(g, Component.translatable("jaycee.ui.no_subordinates"), 695, 590, 22, 0xFFB8D8E8, 1);
            UI.semi(g, Component.translatable("jaycee.ui.no_subordinates.hint"), 695, 626, 16, 0xFF7FB8CC, 1);
            return;
        }
        for (int i = 0; i < Math.min(subs.size(), 9); i++) {
            var s = subs.get(i);
            float y = 362 + i * 56;
            UI.fill(g, 330, y, 1060, y + 48, 0xC00A1424);
            UI.rect(g, 330, y, 1060, y + 48, 1, 0xFF4FE3FF);
            UI.text(g, s.name(), 350, y + 24, 20, 0xFFFFFFFF, 0);
            String type = s.type().contains(":") ? s.type().substring(s.type().indexOf(':') + 1).replace('_', ' ') : s.type();
            UI.semi(g, type + "  \u2022  EP " + UI.shortNum(s.ep()) + "  \u2022  " + s.order(), 640, y + 24, 15, 0xFFB8D8E8, 0);
        }
    }
}

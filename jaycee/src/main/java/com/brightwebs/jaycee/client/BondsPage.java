package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Bonds: subordinates, allies and party. Naming and recruiting arrive in a later update, so the
 * list is empty for now; the page, its commands and the side bar are in place.
 */
final class BondsPage extends Page {
    BondsPage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_bonds"; }
    @Override boolean showsPlayer() { return false; }

    @Override
    void draw(GuiGraphicsExtractor g, double mx, double my) {
        UI.text(g, "Total: 0", 990, 203, 22, 0xFFFFFFFF, 1);
        UI.semi(g, Component.translatable("jaycee.ui.no_subordinates"), 695, 590, 22, 0xFFB8D8E8, 1);
        UI.semi(g, Component.translatable("jaycee.ui.no_subordinates.hint"), 695, 626, 16, 0xFF7FB8CC, 1);
    }
}

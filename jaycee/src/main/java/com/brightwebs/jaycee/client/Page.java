package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** One page of the Existence System. Coordinates passed in are frame design pixels. */
abstract class Page {
    final MenuScreen menu;

    Page(MenuScreen menu) { this.menu = menu; }

    LocalPlayer player() { return menu.player(); }

    /** Background texture (under textures/gui/). */
    abstract String background();

    /** Whether the live player model stands in the left portrait. */
    boolean showsPlayer() { return true; }

    abstract void draw(GuiGraphicsExtractor g, double mx, double my);

    boolean click(double mx, double my, int button) { return false; }

    boolean scroll(double mx, double my, double dy) { return false; }

    boolean typed(int codepoint) { return false; }

    boolean key(int key) { return false; }

    static boolean in(double x, double y, double x1, double y1, double x2, double y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }
}

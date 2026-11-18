package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Maps a region of a reference design (its own pixel coordinates) into the menu frame:
 * frame = offset + (ref - src) * scale. Pages draw in their reference's coordinates.
 */
record T(float ox, float oy, float s, float sx, float sy) {
    static final T ID = new T(0, 0, 1, 0, 0);

    void push(GuiGraphicsExtractor g) {
        g.pose().pushMatrix();
        g.pose().translate(ox, oy);
        g.pose().scale(s, s);
        g.pose().translate(-sx, -sy);
    }

    void pop(GuiGraphicsExtractor g) { g.pose().popMatrix(); }

    double x(double frameX) { return (frameX - ox) / s + sx; }
    double y(double frameY) { return (frameY - oy) / s + sy; }
}

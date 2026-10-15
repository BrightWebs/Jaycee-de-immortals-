package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import net.minecraft.client.gui.GuiGraphics;

/** Views occupy the left 640 x 500 region of the scaled shell; Equip Page is a shared sidecar. */
public interface TabView {
 void init(JayceeScreen screen);
 void render(JayceeScreen screen, GuiGraphics graphics, int mouseX, int mouseY);
 /** Entity renderers use screen-space clipping, outside the shell's pose transform. */
 default void renderUnscaled(JayceeScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {}
 default boolean scroll(JayceeScreen screen, double x, double y, double delta) { return false; }
}

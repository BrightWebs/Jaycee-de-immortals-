package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import net.minecraft.client.gui.GuiGraphics;

/** Coordinates are in the shell's scaled 640 x 360 design space. */
public interface TabView {
 void init(JayceeScreen screen);
 void render(JayceeScreen screen, GuiGraphics graphics, int mouseX, int mouseY);
 default boolean scroll(JayceeScreen screen, double x, double y, double delta) { return false; }
}

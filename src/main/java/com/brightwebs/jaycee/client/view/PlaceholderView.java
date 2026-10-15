package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import net.minecraft.client.gui.GuiGraphics;

public record PlaceholderView(String title,String preview) implements TabView {
 @Override public void init(JayceeScreen s) {}
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,102,138,365,"Immortals");
  s.panel(g,158,102,246,365,title);
  s.panel(g,412,102,216,365,"Details");
  s.wrapped(g,"The path of ascension",24,151,110);
  s.wrapped(g,preview,176,151,206);
  s.wrapped(g,"Placeholder panel. Gameplay and progression will be added later.",430,151,176);
 }
}

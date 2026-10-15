package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import net.minecraft.client.gui.GuiGraphics;

public record PlaceholderView(String title,String preview) implements TabView {
 @Override public void init(JayceeScreen s) {}
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,48,138,250,"IMMORTALS");
  s.panel(g,158,48,246,250,title.toUpperCase());
  s.panel(g,412,48,216,250,"DETAILS");
  s.wrapped(g,"The path of ascension",24,90,110);
  s.wrapped(g,preview,176,100,206);
  s.wrapped(g,"Placeholder panel. Gameplay and progression will be added later.",430,100,176);
 }
}

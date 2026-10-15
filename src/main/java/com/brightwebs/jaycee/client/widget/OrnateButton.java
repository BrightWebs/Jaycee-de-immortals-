package com.brightwebs.jaycee.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Interactive button with sapphire glass styling rather than vanilla grey widgets. */
public final class OrnateButton extends Button {
 private boolean selected;
 public OrnateButton selected(boolean selected) { this.selected=selected; return this; }
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable onPress) {
  super(x,y,w,h,Component.literal(caption),b->onPress.run(),DEFAULT_NARRATION);
  setTooltip(Tooltip.create(Component.literal(caption)));
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
  int x=getX(),y=getY(),w=width,h=height;
  boolean hover=isHoveredOrFocused() || selected;
  int edge=hover?0xFFBDEAFF:0xFF518DCF;
  g.fill(x+2,y+2,x+w-2,y+h-2,hover?0xED1E396B:0xDB0B1C38);
  g.fill(x+4,y,x+w-4,y+1,edge);
  g.fill(x+4,y+h-1,x+w-4,y+h,edge);
  g.fill(x,y+4,x+1,y+h-4,edge);
  g.fill(x+w-1,y+4,x+w,y+h-4,edge);
  g.fill(x+2,y+2,x+10,y+3,0xFF9FDFFF);
  g.fill(x+w-10,y+h-3,x+w-2,y+h-2,0xFF9FDFFF);
  var font=Minecraft.getInstance().font;
  String label=font.plainSubstrByWidth(getMessage().getString(),w-8);
  g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,!active?0xFF647A92:hover?0xFFFFFFFF:0xFFC3E6FF);
 }
}

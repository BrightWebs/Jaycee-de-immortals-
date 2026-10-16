package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Interactive button with sapphire glass styling rather than vanilla grey widgets. */
public final class OrnateButton extends Button {
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable onPress) {
  super(x,y,w,h,Component.literal(caption),b->onPress.run(),DEFAULT_NARRATION);
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
  int x=getX(),y=getY(),w=width,h=height;
  boolean hover=isHoveredOrFocused();
  int edge=hover?0xFFBDEAFF:0xFF518DCF;
  g.fill(x+2,y+2,x+w-2,y+h-2,hover?0xED1E396B:0xDB0B1C38);
  g.fill(x+4,y,x+w-4,y+1,edge);
  g.fill(x+4,y+h-1,x+w-4,y+h,edge);
  g.fill(x,y+4,x+1,y+h-4,edge);
  g.fill(x+w-1,y+4,x+w,y+h-4,edge);
  g.fill(x+2,y+2,x+10,y+3,0xFF9FDFFF);
  g.fill(x+w-10,y+h-3,x+w-2,y+h-2,0xFF9FDFFF);
  UiText.line(g,getMessage().getString(),x+12,y+(h-9)/2,w-24,1f,
   hover?0xFFFFFFFF:0xFFC3E6FF,true);
 }
}

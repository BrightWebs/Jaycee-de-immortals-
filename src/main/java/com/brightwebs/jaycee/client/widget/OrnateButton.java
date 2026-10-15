package com.brightwebs.jaycee.client.widget;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.DivineGlyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Interactive button with sapphire glass styling rather than vanilla grey widgets. */
public final class OrnateButton extends Button {
 private boolean selected;
 private int navigationIcon=-1;
 public OrnateButton selected(boolean selected) { this.selected=selected; return this; }
 public OrnateButton navigation(int icon) { navigationIcon=icon; return this; }
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable onPress) {
  super(x,y,w,h,Component.literal(caption),b->onPress.run(),DEFAULT_NARRATION);
  setTooltip(Tooltip.create(Component.literal(caption)));
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
  int x=getX(),y=getY(),w=width,h=height;
  boolean hover=isHoveredOrFocused() || selected;
  if(navigationIcon>=0){
   g.fillGradient(x+2,y+2,x+w-2,y+h-2,hover?0xFF173F6D:0xFF101B2A,0xFF0B1421);
   if(hover){
    DivineFrame.frame(g,x,y,w,h,true);
    g.fillGradient(x+4,y+3,x+w-4,y+h-4,0x773F93EF,0x083F93EF);
    g.fill(x+8,y+h-3,x+w-8,y+h-2,0xFFB4EFFF);
   }else g.fill(x+w-1,y+8,x+w,y+h-8,0xFF35465F);
   DivineGlyph.navigation(g,x+w/2,y+22,navigationIcon,hover);
  }else{
   DivineFrame.frame(g,x,y,w,h,hover);
  }
  var font=Minecraft.getInstance().font;
  String label=font.plainSubstrByWidth(getMessage().getString(),w-8);
  g.drawCenteredString(font,label,x+w/2,navigationIcon>=0?y+h-17:y+(h-8)/2,
   !active?0xFF647A92:hover?0xFFE1F5FF:0xFFABB9CD);
 }
}

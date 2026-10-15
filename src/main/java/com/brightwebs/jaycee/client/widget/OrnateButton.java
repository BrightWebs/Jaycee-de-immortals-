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
 private int rowIcon=-1;
 private boolean node;
 public OrnateButton selected(boolean selected) { this.selected=selected; return this; }
 public OrnateButton navigation(int icon) { navigationIcon=icon; return this; }
 public OrnateButton illustrated(int icon) { rowIcon=icon; return this; }
 public OrnateButton skillNode(int icon) { rowIcon=icon; node=true; return this; }
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable onPress) {
  super(x,y,w,h,Component.literal(caption),b->onPress.run(),DEFAULT_NARRATION);
  setTooltip(Tooltip.create(Component.literal(caption)));
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
  int x=getX(),y=getY(),w=width,h=height;
  boolean hover=isHoveredOrFocused() || selected;
  if(node){
   int cx=x+w/2,cy=y+h/2,r=Math.min(w,h)/2-2;
   for(int dy=-r;dy<=r;dy++){
    int span=r-Math.abs(dy);
    g.fill(cx-span,cy+dy,cx+span+1,cy+dy+1,hover?0xFF8BCFFF:0xFF47647E);
    if(span>1)g.fill(cx-span+1,cy+dy,cx+span,cy+dy+1,hover?0xFF123659:0xFF0B192B);
   }
   DivineGlyph.navigation(g,cx,cy,rowIcon,hover);
   if(isFocused())g.fill(x+4,y+h-1,x+w-4,y+h,0xFFE1F5FF);
   return;
  }
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
  int color=!active?0xFF647A92:hover?0xFFE1F5FF:0xFFABB9CD;
  if(rowIcon>=0){
   DivineGlyph.navigation(g,x+19,y+h/2,rowIcon,hover);
   g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),w-44),x+36,y+(h-8)/2,color,false);
  }else{
   String label=font.plainSubstrByWidth(getMessage().getString(),w-8);
   g.drawCenteredString(font,label,x+w/2,navigationIcon>=0?y+h-17:y+(h-8)/2,color);
  }
 }
}

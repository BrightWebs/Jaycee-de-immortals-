package com.brightwebs.jaycee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Live, clickable controls styled to match the sapphire panels. */
public final class OrnateButton extends Button {
 public boolean selected,compact;
 public int icon=-1,tabIcon=-1;
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable action){
  super(x,y,w,h,Component.literal(caption),b->action.run(),DEFAULT_NARRATION);
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
  int x=getX(),y=getY();
  var font=Minecraft.getInstance().font;
  boolean highlighted=selected||isHoveredOrFocused();
  if(compact&&height>=40)UiArt.slot(g,x,y,width,height,highlighted,icon>=0);
  else UiArt.frame(g,x,y,width,height,highlighted);
  if(isHoveredOrFocused()&&active){
   g.fill(x+8,y+6,x+width-8,y+7,0xAAE2F5FF);
  }
  int color=!active?0xFF6A7A90:selected?0xFFFFE5B2:isHoveredOrFocused()?0xFFFFFFFF:0xFFCEDDF3;
  if(tabIcon>=0){
   UiArt.tab(g,x+(width-14)/2,y+3,tabIcon);
   g.drawCenteredString(font,getMessage(),x+width/2,y+height-11,color);
   if(selected)g.fill(x+width/2-9,y+height-3,x+width/2+9,y+height-2,0xFFFFD68A);
  }else if(compact){
   int size=Math.min(26,Math.min(width-8,height-8));
   if(icon>=0)UiArt.icon(g,x+(width-size)/2,y+4,size,icon);
   if(height>40){
    String caption=getMessage().getString();
    g.drawCenteredString(font,font.plainSubstrByWidth(caption,width-6),x+width/2,y+height-15,color);
   }else if(icon<0){
    g.drawCenteredString(font,getMessage(),x+width/2,y+(height-8)/2,color);
   }
  }else if(icon>=0){
   UiArt.icon(g,x+5,y+3,19,icon);
   g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),width-30),x+28,y+(height-8)/2,color,false);
  }else{
   g.drawCenteredString(font,font.plainSubstrByWidth(getMessage().getString(),width-8),x+width/2,y+(height-8)/2,color);
  }
 }
}

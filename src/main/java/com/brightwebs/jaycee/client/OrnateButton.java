package com.brightwebs.jaycee.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class OrnateButton extends Button {
 public boolean selected,compact;
 public int icon=-1,tabIcon=-1;
 public OrnateButton(String caption,int x,int y,int w,int h,Runnable action){super(x,y,w,h,Component.literal(caption),b->action.run(),DEFAULT_NARRATION);}
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
  int x=getX(),y=getY();var font=Minecraft.getInstance().font;
  UiArt.frame(g,x,y,width,height,selected||isHoveredOrFocused());
  int color=!active?0xFF60718A:selected?0xFFE8D7FF:isHoveredOrFocused()?0xFFFFFFFF:0xFFBCD1EF;
  if(tabIcon>=0){UiArt.tab(g,x+(width-14)/2,y+3,tabIcon);g.drawCenteredString(font,getMessage(),x+width/2,y+height-11,color);}
  else if(compact){
   int size=Math.min(26,Math.min(width-8,height-8));
   if(icon>=0)UiArt.icon(g,x+(width-size)/2,y+4,size,icon);
   if(height>40)g.drawCenteredString(font,getMessage(),x+width/2,y+height-15,color);
  }else if(icon>=0){
   UiArt.icon(g,x+5,y+3,19,icon);g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),width-30),x+28,y+(height-8)/2,color,false);
  }else g.drawCenteredString(font,getMessage(),x+width/2,y+(height-8)/2,color);
 }
}

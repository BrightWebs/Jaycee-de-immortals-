package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.DivineGlyph;
import com.brightwebs.jaycee.client.JayceeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Shared visual motifs keep every tab in the Status artwork's glass-and-silver language. */
public final class ViewArt {
 private ViewArt() {}
 public static void emblem(GuiGraphics g,int cx,int cy,int radius,int icon){
  var player=Minecraft.getInstance().player;
  DivineFrame.sigil(g,cx,cy,radius,player==null?0:player.tickCount);
  g.pose().pushPose();
  g.pose().translate(cx,cy,0);
  g.pose().scale(1.5f,1.5f,1);
  DivineGlyph.navigation(g,0,0,icon,true);
  g.pose().popPose();
 }
 public static void note(JayceeScreen s,GuiGraphics g,String title,String body,int x,int y,int width,int height){
  s.panel(g,x,y,width,height,title);
  s.wrapped(g,body,x+14,y+39,width-28);
 }
 public static void meter(JayceeScreen s,GuiGraphics g,String label,int x,int y,int width){
  s.text(g,label,x,y,width);
  g.fill(x,y+16,x+width,y+22,0xFF08111D);
  g.fill(x,y+16,x+width,y+17,0xFF4A627E);
  s.coloredText(g,"0 / Preview",x,y+30,width,0xFF7D96B4);
 }
}

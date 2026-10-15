package com.brightwebs.jaycee.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

/** Reusable sapphire fantasy surfaces. Texture corners remain independent of content. */
final class UiArt {
 static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("jaycee","textures/gui/panel.png");
 private static final int ICE=0xFFB9EDFF, GOLD=0xFFE5C888, PURPLE=0xFFB79AFF;
 static void piece(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh){
  g.blit(PANEL,x,y,w,h,(float)u,(float)v,sw,sh,125,285);
 }
 static void frame(GuiGraphics g,int x,int y,int w,int h,boolean selected){
  if(w<14||h<14)return;
  // Deep inset with a dimensional, translucent sapphire/glass face.
  g.fillGradient(x+5,y+5,x+w-5,y+h-5,selected?0xEF233E67:0xED162C4C,0xF0061124);
  g.fill(x+7,y+7,x+w-7,y+8,selected?0x8886CFFF:0x553A77AD);
  g.fill(x+7,y+h-8,x+w-7,y+h-7,0x55315B9A);
  RenderSystem.enableBlend();
  int c=9;
  piece(g,x,y,c,c,0,0,c,c);piece(g,x+w-c,y,c,c,125-c,0,c,c);
  piece(g,x,y+h-c,c,c,0,285-c,c,c);piece(g,x+w-c,y+h-c,c,c,125-c,285-c,c,c);
  piece(g,x+c,y,w-2*c,5,30,0,60,5);piece(g,x+c,y+h-5,w-2*c,5,30,280,60,5);
  piece(g,x,y+c,5,h-2*c,0,45,5,180);piece(g,x+w-5,y+c,5,h-2*c,120,45,5,180);
  if(w>=37&&h>=24){
   // Gold filigree marks instead of anonymous blue rectangles.
   g.fill(x+8,y+5,x+18,y+6,GOLD);
   g.fill(x+w-18,y+5,x+w-8,y+6,GOLD);
   g.fill(x+8,y+h-6,x+15,y+h-5,0xFF739FCA);
   g.fill(x+w-15,y+h-6,x+w-8,y+h-5,0xFF739FCA);
   g.fill(x+w/2-1,y+3,x+w/2+1,y+5,selected?ICE:PURPLE);
  }
  if(selected){
   g.fill(x+9,y+6,x+w-9,y+7,0xFF9DD9FF);
   g.fill(x+9,y+h-7,x+w-9,y+h-6,0xFF83C4FF);
  }
  RenderSystem.disableBlend();
 }
 static void slot(GuiGraphics g,int x,int y,int w,int h,boolean selected,boolean occupied){
  frame(g,x,y,w,h,selected);
  int cx=x+w/2,cy=y+h/2-5;
  // Recessed diamond aperture, visible even when no ability is equipped.
  g.fill(cx-12,cy-12,cx+12,cy+12,0xAD050D20);
  for(int i=0;i<10;i++){
   int span=10-i;
   g.fill(cx-span,cy-10+i,cx+span+1,cy-9+i,occupied?0x443A8BB5:0x332C587A);
  }
  g.fill(cx-1,cy-10,cx+1,cy-7,selected?GOLD:0xFF5D85B5);
  g.fill(cx-1,cy+7,cx+1,cy+10,selected?GOLD:0xFF5D85B5);
 }
 static void tab(GuiGraphics g,int x,int y,int tab){
  RenderSystem.enableBlend();
  g.blit(ResourceLocation.fromNamespaceAndPath("jaycee","textures/gui/tab_"+tab+".png"),x,y,14,14,0f,0f,20,20,20,20);
  RenderSystem.disableBlend();
 }
 static void icon(GuiGraphics g,int x,int y,int size,int art){
  if(art<0)return;
  RenderSystem.enableBlend();
  g.blit(ResourceLocation.fromNamespaceAndPath("jaycee","textures/gui/art_"+(art%10)+".png"),x,y,size,size,0f,0f,22,22,22,22);
  RenderSystem.disableBlend();
 }
}

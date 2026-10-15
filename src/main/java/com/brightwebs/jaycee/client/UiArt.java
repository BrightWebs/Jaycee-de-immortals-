package com.brightwebs.jaycee.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

/** Independent border pieces sampled from the approved reference, with live content inside. */
final class UiArt {
 static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("jaycee","textures/gui/panel.png");
 static void piece(GuiGraphics g,int x,int y,int w,int h,int u,int v,int sw,int sh){
  g.blit(PANEL,x,y,w,h,(float)u,(float)v,sw,sh,125,285);
 }
 static void frame(GuiGraphics g,int x,int y,int w,int h,boolean selected){
  g.fillGradient(x+4,y+4,x+w-4,y+h-4,0xE0122039,0xE007101E);
  RenderSystem.enableBlend();
  int c=9;
  piece(g,x,y,c,c,0,0,c,c);piece(g,x+w-c,y,c,c,125-c,0,c,c);
  piece(g,x,y+h-c,c,c,0,285-c,c,c);piece(g,x+w-c,y+h-c,c,c,125-c,285-c,c,c);
  piece(g,x+c,y,w-2*c,5,30,0,60,5);piece(g,x+c,y+h-5,w-2*c,5,30,280,60,5);
  piece(g,x,y+c,5,h-2*c,0,45,5,180);piece(g,x+w-5,y+c,5,h-2*c,120,45,5,180);
  if(selected){g.fill(x+6,y+5,x+w-6,y+6,0xFF9DD9FF);g.fill(x+6,y+h-6,x+w-6,y+h-5,0xFF83C4FF);}
  RenderSystem.disableBlend();
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

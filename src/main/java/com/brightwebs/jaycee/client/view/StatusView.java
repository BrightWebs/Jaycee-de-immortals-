package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.DivineGlyph;
import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import com.brightwebs.jaycee.client.widget.StatusSymbols;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Reference layout: compact profile/stats, free-floating seal, and inset detail cards. */
public final class StatusView implements TabView {
 private boolean fullModel;
 @Override public void init(JayceeScreen s){
  s.button(fullModel?"Face":"3D",84,182,26,18,()->{fullModel=!fullModel;s.rebuild();});
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  var player=Minecraft.getInstance().player;
  DivineFrame.portrait(g,24,106,94,94);
  if(player!=null&&!fullModel){
   // Skin layers stay separate from the frame, so resource-pack frames remain replaceable.
   g.pose().pushPose();
   g.pose().translate(39,121,0);
   g.pose().scale(8,8,1);
   g.blit(player.getSkin().texture(),0,0,8f,8f,8,8,64,64);
   g.blit(player.getSkin().texture(),0,0,40f,8f,8,8,64,64);
   g.pose().popPose();
  }
  s.largeText(g,player==null?"PlayerName":player.getName().getString(),132,115,306,1.35f,0xFFE3E9F6);
  s.coloredText(g,"Race: Human",132,147,280,0xFF8EAFD1);
  s.coloredText(g,"Title: None",132,166,280,0xFF8EAFD1);
  s.coloredText(g,"Faction: None",132,185,280,0xFF8EAFD1);
  s.largeText(g,"Lv. "+(player==null?0:player.experienceLevel),28,220,230,1.9f,0xFF90C3FF);
  float progress=player==null?0:Math.clamp(player.experienceProgress,0f,1f);
  g.fill(28,253,256,265,0xFF030B15);
  g.fill(29,254,255,255,0xFF58718A);
  g.fill(29,255,29+(int)(225*progress),263,0xFF286BD0);
  g.fill(29,255,29+(int)(225*progress),257,0xFF97DFFF);
  s.rightText(g,(int)(progress*100)+" / 100 EXP",254,274,0xFFBAC8DC);
  s.panel(g,22,299,248,182,"Stats");
  s.coloredText(g,"HP",53,332,100,0xFFB5C7DF);
  StatusSymbols.stat(g,34,332,0);
  s.rightText(g,player==null?"- / -":(int)Math.ceil(player.getHealth())+" / "+(int)player.getMaxHealth(),
   255,332,0xFF9EC4EE);
  for(int i=0;i<PreviewData.STATS.size();i++){
   var stat=PreviewData.STATS.get(i);
   int y=356+i*24;
   g.fill(32,y-5,258,y-4,0xFF233347);
   StatusSymbols.stat(g,34,y,i+1);
   s.coloredText(g,stat.name(),53,y,120,0xFFB5C7DF);
   s.rightText(g,stat.value(),255,y,0xFF9EC4EE);
  }
  DivineFrame.sigil(g,357,231,82,player==null?0:player.tickCount);
  s.panel(g,458,102,160,59,"Titles");
  s.coloredText(g,"- No Title Equipped -",471,139,135,0xFF97A7BF);
  s.panel(g,458,170,160,105,"Racial Trait");
  DivineGlyph.navigation(g,480,216,4,false);
  s.text(g,"None",499,213,106);
  s.wrapped(g,"No racial traits unlocked.",471,245,134);
  s.panel(g,458,284,160,197,"Elemental Affinity");
  for(int i=0;i<PreviewData.AFFINITIES.size();i++){
   var affinity=PreviewData.AFFINITIES.get(i);
   int y=322+i*26;
   g.fill(470,y+15,605,y+16,0xFF27374E);
   StatusSymbols.affinity(g,470,y,i,affinity.color());
   s.coloredText(g,affinity.name(),485,y,77,0xFFC3CEE0);
   s.rightText(g,"Lv. 0",605,y,0xFF96A7C2);
  }
  s.coloredText(g,"Sample traits / stats",288,435,156,0xFF71839C);
  s.coloredText(g,"HP and EXP are live",288,449,156,0xFF71839C);
 }
 @Override public void renderUnscaled(JayceeScreen s,GuiGraphics g,int mx,int my){
  var player=Minecraft.getInstance().player;
  if(player!=null&&fullModel)InventoryScreen.renderEntityInInventoryFollowsMouse(g,
   s.screenX(32),s.screenY(114),s.screenX(110),s.screenY(180),
   Math.max(1,s.scaledSize(29)),0f,mx,my,player);
 }
}

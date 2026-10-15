package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import net.minecraft.client.gui.GuiGraphics;

public final class SkillsView implements TabView {
 private int branch,tier;
 @Override public void init(JayceeScreen s){
  for(int b=0;b<4;b++)for(int t=0;t<4;t++){
   final int selectedBranch=b,selectedTier=t;
   s.button(PreviewData.TRACKS.get(b)+" mastery / Tier "+(t+1),170+b*57,173+t*65,32,32,()->{
    branch=selectedBranch;tier=selectedTier;s.rebuild();
   }).selected(b==branch&&t==tier).skillNode(b);
  }
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,102,138,206,"Mastery");
  s.panel(g,158,102,246,365,"Skill Tree");
  s.panel(g,412,102,216,211,"Selected Skill");
  ViewArt.emblem(g,81,182,38,2);
  s.text(g,"Divine Initiate",24,242,114);
  ViewArt.meter(s,g,"Mastery points",24,267,114);
  ViewArt.note(s,g,"Ascension","Choose a glowing node to inspect a mastery tier. Connecting paths are visual guides, not unlocks.",12,318,138,149);
  for(int b=0;b<4;b++){
   s.text(g,PreviewData.TRACKS.get(b),170+b*57,147,52);
   int x=185+b*57;
   g.fill(x-1,189,x+2,385,0xFF162A43);
   g.fill(x,189,x+1,385,b==branch?0xFF83BDEC:0xFF425B78);
   for(int t=0;t<3;t++)DivineFrame.divider(g,x-4,221+t*65,9);
  }
  ViewArt.emblem(g,520,190,47,branch);
  s.largeText(g,PreviewData.TRACKS.get(branch)+" mastery",430,250,180,1.05f,0xFFCEE9FF);
  s.coloredText(g,"Tier "+(tier+1)+" / Not learned",430,280,180,0xFF8DB6DF);
  ViewArt.note(s,g,"Requirements","Sample tier "+(tier+1)+". Progression and learning costs are not connected to gameplay.",412,321,216,94);
  s.panel(g,412,423,216,44,"Preview only / no effects");
  s.coloredText(g,"Select a node to inspect",174,435,218,0xFF7D96B4);
 }
}

package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import net.minecraft.client.gui.GuiGraphics;

public final class SkillsView implements TabView {
 private int branch,tier;
 @Override public void init(JayceeScreen s){
  for(int b=0;b<4;b++)for(int t=0;t<4;t++){
   final int selectedBranch=b,selectedTier=t;
   s.button(""+(t+1),170+b*57,173+t*65,32,32,()->{
    branch=selectedBranch;tier=selectedTier;s.rebuild();
   }).selected(b==branch&&t==tier);
  }
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,102,138,365,"Mastery");
  s.panel(g,158,102,246,365,"Skill Tree");
  s.panel(g,412,102,216,365,"Selected Skill");
  s.wrapped(g,"Choose a node to inspect its mastery tier.",24,151,114);
  s.wrapped(g,"Points: 0 (sample)\nSkills do not grant effects yet.",24,288,114);
  for(int b=0;b<4;b++){
   s.text(g,PreviewData.TRACKS.get(b),170+b*57,147,52);
   g.fill(185+b*57,189,187+b*57,385,0xFF386EA9);
  }
  s.text(g,PreviewData.TRACKS.get(branch)+" mastery",430,151,180);
  s.text(g,"Tier "+(tier+1),430,186,180);
  s.wrapped(g,"A foundation for an immortal's journey. Replace this sample with skill requirements, costs and effects.",430,245,176);
  s.text(g,"Not learned / preview",430,435,180);
 }
}

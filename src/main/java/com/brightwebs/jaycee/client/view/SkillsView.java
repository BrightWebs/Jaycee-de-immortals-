package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import net.minecraft.client.gui.GuiGraphics;

public final class SkillsView implements TabView {
 private int branch,tier;
 @Override public void init(JayceeScreen s){
  for(int b=0;b<4;b++)for(int t=0;t<4;t++){
   final int selectedBranch=b,selectedTier=t;
   s.button(""+(t+1),170+b*57,112+t*40,32,25,()->{
    branch=selectedBranch;tier=selectedTier;s.rebuild();
   }).selected(b==branch&&t==tier);
  }
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,48,138,250,"MASTERY");
  s.panel(g,158,48,246,250,"SKILL TREE");
  s.panel(g,412,48,216,250,"SELECTED SKILL");
  s.wrapped(g,"Choose a node to inspect its mastery tier.",24,90,114);
  s.wrapped(g,"Points: 0 (sample)\nSkills do not grant effects yet.",24,175,114);
  for(int b=0;b<4;b++){
   s.text(g,PreviewData.TRACKS.get(b),170+b*57,85,52);
   g.fill(185+b*57,137,187+b*57,232,0xFF386EA9);
  }
  s.text(g,PreviewData.TRACKS.get(branch)+" mastery",430,90,180);
  s.text(g,"Tier "+(tier+1),430,119,180);
  s.wrapped(g,"A foundation for an immortal's journey. Replace this sample with skill requirements, costs and effects.",430,155,176);
  s.text(g,"Not learned / preview",430,259,180);
 }
}

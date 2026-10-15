package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import com.brightwebs.jaycee.client.model.PreviewData.Ability;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

public final class AbilitiesView implements TabView {
 private static final int VISIBLE_ROWS=6;
 private String category="All";
 private Ability selected=PreviewData.ABILITIES.getFirst();
 private int offset;
 private List<Ability> filtered(){
  return PreviewData.ABILITIES.stream().filter(a->category.equals("All")||a.category().equals(category)).toList();
 }
 @Override public void init(JayceeScreen s){
  for(int i=0;i<PreviewData.CATEGORIES.size();i++){
   String c=PreviewData.CATEGORIES.get(i);
   s.button(c,22,142+i*42,112,33,()->{
    category=c;offset=0;selected=filtered().getFirst();s.rebuild();
   }).selected(category.equals(c)).illustrated(i);
  }
  List<Ability> list=filtered();
  for(int i=offset;i<Math.min(list.size(),offset+VISIBLE_ROWS);i++){
   Ability ability=list.get(i);
   s.button(ability.name(),158,142+(i-offset)*42,234,33,()->{
    selected=ability;s.rebuild();
   }).selected(ability.equals(selected)).illustrated(PreviewData.ABILITIES.indexOf(ability)%6);
  }
  var up=s.button("^",338,435,24,22,()->shift(s,-1));up.active=offset>0;
  var down=s.button("v",368,435,24,22,()->shift(s,1));down.active=offset+VISIBLE_ROWS<list.size();
  s.button("Equip preview",430,429,180,28,()->s.equip(selected));
 }
 private void shift(JayceeScreen s,int delta){
  int next=Math.clamp(offset+delta,0,Math.max(0,filtered().size()-VISIBLE_ROWS));
  if(next!=offset){offset=next;s.rebuild();}
 }
 @Override public boolean scroll(JayceeScreen s,double x,double y,double delta){
  if(x>=146&&x<404&&y>=132&&y<467&&delta!=0){shift(s,delta<0?1:-1);return true;}
  return false;
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,102,130,365,"Categories");
  s.panel(g,146,102,258,365,"Divine Arts");
  s.panel(g,412,102,216,211,"Selected Art");
  ViewArt.emblem(g,520,190,47,PreviewData.ABILITIES.indexOf(selected)%6);
  s.largeText(g,selected.name(),430,250,180,1.05f,0xFFCEE9FF);
  s.coloredText(g,selected.category()+" / MP "+selected.mana(),430,280,184,0xFF8DB6DF);
  ViewArt.note(s,g,"Description",selected.description(),412,321,216,94);
  s.coloredText(g,"Preview only / no effects",158,408,234,0xFF7D96B4);
  s.coloredText(g,"Assigns to preview loadout",430,469,180,0xFF7D96B4);
  s.text(g,(offset+1)+"-"+Math.min(filtered().size(),offset+VISIBLE_ROWS)+" / "+filtered().size(),158,442,166);
 }
}

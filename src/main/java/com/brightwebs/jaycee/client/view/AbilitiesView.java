package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineGlyph;
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
   s.button(c,22,83+i*30,112,25,()->{
    category=c;offset=0;selected=filtered().getFirst();s.rebuild();
   }).selected(category.equals(c));
  }
  List<Ability> list=filtered();
  for(int i=offset;i<Math.min(list.size(),offset+VISIBLE_ROWS);i++){
   Ability ability=list.get(i);
   s.button(ability.name(),158,83+(i-offset)*29,234,25,()->{
    selected=ability;s.rebuild();
   }).selected(ability.equals(selected));
  }
  var up=s.button("^",338,265,24,22,()->shift(s,-1));up.active=offset>0;
  var down=s.button("v",368,265,24,22,()->shift(s,1));down.active=offset+VISIBLE_ROWS<list.size();
  s.button("Equip preview",430,261,180,25,()->s.equip(selected));
 }
 private void shift(JayceeScreen s,int delta){
  int next=Math.clamp(offset+delta,0,Math.max(0,filtered().size()-VISIBLE_ROWS));
  if(next!=offset){offset=next;s.rebuild();}
 }
 @Override public boolean scroll(JayceeScreen s,double x,double y,double delta){
  if(x>=146&&x<404&&y>=76&&y<294&&delta!=0){shift(s,delta<0?1:-1);return true;}
  return false;
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  s.panel(g,12,48,130,250,"CATEGORIES");
  s.panel(g,146,48,258,250,"DIVINE ARTS");
  s.panel(g,412,48,216,250,"ABILITY DETAILS");
  DivineGlyph.icon(g,430,82,30,PreviewData.ABILITIES.indexOf(selected),true);
  s.text(g,selected.name(),470,87,146);
  s.text(g,selected.category()+" / MP "+selected.mana(),430,125,184);
  s.wrapped(g,selected.description(),430,151,180);
  s.wrapped(g,"Sample only. Equip changes this screen's preview loadout, not gameplay.",430,207,180);
  s.text(g,(offset+1)+"-"+Math.min(filtered().size(),offset+VISIBLE_ROWS)+" / "+filtered().size(),158,271,166);
 }
}

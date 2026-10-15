package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.ability.AbilityCatalog;
import com.brightwebs.jaycee.ability.LoadoutClientState;
import com.brightwebs.jaycee.client.DivineGlyph;
import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;

public final class AbilitiesView implements TabView {
 private String category="All",query="";
 private int selected=2,offset;
 private boolean searchChanged;
 private List<AbilityCatalog.Ability> filtered(){
  return AbilityCatalog.ALL.stream().filter(a->category.equals("All")||a.category().equals(category))
   .filter(a->a.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
 }
 @Override public void init(JayceeScreen s){
  s.selectAbility(selected);
  for(int i=0;i<PreviewData.CATEGORIES.size();i++){
   String c=PreviewData.CATEGORIES.get(i);
   s.button(c,22,142+i*42,112,33,()->{category=c;offset=0;s.rebuild();}).selected(category.equals(c)).illustrated(i);
  }
  s.search(query,value->{query=value;offset=0;searchChanged=true;});
  addRows(s);
 }
 private void addRows(JayceeScreen s){
  var list=filtered();
  offset=Math.clamp(offset,0,Math.max(0,list.size()-6));
  for(int i=offset;i<Math.min(list.size(),offset+6);i++){
   var ability=list.get(i);
   s.button(ability.name(),158,168+(i-offset)*39,234,33,()->{
    selected=ability.id();s.selectAbility(selected);s.rebuild();
   }).selected(selected==ability.id()).illustrated(ability.id()%6);
  }
  s.button("^",338,435,24,22,()->shift(s,-1)).active=offset>0;
  s.button("v",368,435,24,22,()->shift(s,1)).active=offset+6<list.size();
 }
 /** Rebuild after text input dispatch, preserving focus and cursor for continued typing. */
 public void tick(JayceeScreen s){
  if(searchChanged){searchChanged=false;s.rebuildSearch();}
 }
 private void shift(JayceeScreen s,int delta){
  int next=Math.clamp(offset+delta,0,Math.max(0,filtered().size()-6));
  if(next!=offset){offset=next;s.rebuild();}
 }
 @Override public boolean scroll(JayceeScreen s,double x,double y,double delta){
  if(x>=146&&x<404&&y>=158&&y<467&&delta!=0){shift(s,delta<0?1:-1);return true;}
  return false;
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  var ability=AbilityCatalog.ALL.get(selected);
  boolean unlocked=LoadoutClientState.get().unlocked().get(selected);
  s.panel(g,12,102,130,365,"Categories");
  s.panel(g,146,102,258,365,"Divine Arts");
  s.panel(g,412,102,216,365,"Ability Details");
  ViewArt.emblem(g,520,181,36,selected%6);
  DivineGlyph.icon(g,430,225,16,selected,true);
  s.text(g,ability.name(),450,229,160);
  s.coloredText(g,unlocked?"Lv. 1 / Unlocked":"Locked / "+ability.requiredEssences()+" essences",430,249,180,
   unlocked?0xFF9EDDB7:0xFFDA8EAA);
  s.wrapped(g,ability.description(),430,270,180);
  s.wrapped(g,ability.effects(),430,316,180);
  s.text(g,"MP "+ability.mana()+" / CD "+ability.cooldownTicks()/20+"s",430,370,180);
  s.text(g,"Range "+ability.range()+" blocks",430,390,180);
  s.wrapped(g,"Modes: "+String.join(", ",ability.modes()),430,410,180);
  s.text(g,unlocked?"Click an Equip Page slot":"Cannot equip locked abilities",430,449,180);
  s.text(g,filtered().isEmpty()?"No matches":(offset+1)+" / "+filtered().size(),158,442,166);
 }
}

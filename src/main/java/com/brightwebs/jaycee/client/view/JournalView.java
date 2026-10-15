package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.JournalData.Collection;
import net.minecraft.client.gui.GuiGraphics;

/** Shared illustrated catalogue; each tab owns its selection and distinct presentation data. */
public final class JournalView implements TabView {
 private final Collection collection;
 private int selected;
 public JournalView(Collection collection){this.collection=collection;}
 @Override public void init(JayceeScreen s){
  for(int i=0;i<collection.entries().size();i++){
   final int index=i;
   var entry=collection.entries().get(i);
   s.button(entry.name(),22,148+i*40,176,34,()->{
    selected=index;s.rebuild();
   }).illustrated(entry.symbol()).selected(selected==i);
  }
 }
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my){
  var entry=collection.entries().get(selected);
  s.panel(g,12,102,196,365,collection.listTitle());
  s.panel(g,216,102,196,365,collection.title());
  ViewArt.emblem(g,314,230,77,entry.symbol());
  s.largeText(g,entry.name(),233,332,162,1.1f,0xFFD0E8FF);
  s.wrapped(g,entry.subtitle(),233,359,162);
  DivineFrame.divider(g,234,402,160);
  s.coloredText(g,"Illustrated preview",234,426,160,0xFF829BB8);
  ViewArt.note(s,g,collection.detailTitle(),entry.description(),420,102,208,163);
  ViewArt.note(s,g,"Requirements",entry.requirement(),420,275,208,89);
  ViewArt.note(s,g,"Potential",entry.reward(),420,374,208,93);
  s.coloredText(g,"Preview only / no gameplay effects",24,437,170,0xFF7D96B4);
 }
}

package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.AbilityCatalog;
import com.brightwebs.jaycee.JayceeNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Coordinates and hitboxes share one transform, including on small GUI scales. */
public class JayceeScreen extends Screen {
 private static final int W=740,H=340,INK=0xFFD5E5FF,MUTED=0xFF8AA9CD;
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private static final String[] TRACKS={"Body","Spirit","Divine","Utility"};
 private static final String[] ELEMENTS={"Fire","Water","Wind","Earth","Light","Dark"};
 private int tab,page,slot,art,category,offset,branch,tier,element,quest;
 private double originX,originY,scale=1;
 private String query="";
 private EditBox search;
 private final List<OrnateButton> rows=new ArrayList<>();
 private final List<Integer> filtered=new ArrayList<>();
 private OrnateButton equip;
 public JayceeScreen(){super(Component.literal("Jaycee: Di Immortales"));var p=Minecraft.getInstance().player;if(p!=null)page=JayceeNetwork.page(p);}
 private OrnateButton button(String label,int x,int y,int w,int h,Runnable action){
  var b=new OrnateButton(label,x,y,w,h,action);addRenderableWidget(b);return b;
 }
 @Override protected void init(){
  scale=Math.min(1.0,Math.min((width-12.0)/W,(height-12.0)/H));
  originX=(width-W*scale)/2;originY=(height-H*scale)/2;
  for(int i=0;i<TABS.length;i++){
   final int t=i;var b=button(TABS[i],58+i*80,7,78,31,()->{tab=t;rebuild();});b.selected=tab==i;b.tabIcon=i;
  }
  button("x",568,12,24,23,this::onClose);
  button("<",500,79,20,20,()->changePage(-1));button(">",574,79,20,20,()->changePage(1));
  for(int i=0;i<4;i++){
   final int s=i;var b=button("Slot "+(i+1),500+(i%2)*47,113+(i/2)*62,43,55,()->{slot=s;updateSelection();});
   b.icon=loadout()[page*4+i];b.compact=true;b.selected=slot==i;
  }
  for(int i=0;i<10;i++){final int p=i;var b=button(""+(i+1),500+(i%5)*19,248+(i/5)*20,18,18,()->setPage(p));b.selected=page==i;}
  button("Clear slot",501,296,91,23,()->setArt(-1));
  rows.clear();search=null;equip=null;
  if(tab==1){
   for(int i=0;i<AbilityCatalog.CATEGORIES.length;i++){
    final int c=i;var b=button(AbilityCatalog.CATEGORIES[i],9,82+i*31,111,27,()->{category=c;offset=0;rebuild();});b.selected=category==i;
   }
   search=new EditBox(font,137,81,144,18,Component.literal("Search abilities"));
   search.setMaxLength(60);search.setValue(query);search.setHint(Component.literal("Search abilities..."));
   search.setResponder(value->{query=value;offset=0;filter();});addRenderableWidget(search);
   for(int i=0;i<7;i++){final int row=i;rows.add(button("",133,108+i*27,152,25,()->{int n=offset+row;if(n<filtered.size()){art=filtered.get(n);filter();}}));}
   button("^",136,303,30,23,()->{offset=Math.max(0,offset-1);filter();});
   button("v",254,303,30,23,()->{offset=Math.min(Math.max(0,filtered.size()-7),offset+1);filter();});
   equip=button("Equip to selected slot",307,298,170,25,()->setArt(art));filter();
  }else if(tab==2){
   for(int b=0;b<4;b++)for(int t=0;t<4;t++){
    final int bb=b,tt=t;var n=button(""+(t+1),149+b*80,120+t*40,27,27,()->{branch=bb;tier=tt;rebuild();});n.icon=(b+t)%10;n.compact=true;n.selected=b==branch&&t==tier;
   }
  }else if(tab==3){
   for(int i=0;i<6;i++){final int e=i;var b=button(ELEMENTS[i],14,85+i*32,102,27,()->{element=e;rebuild();});b.selected=element==i;}
  }else if(tab==5){
   String[] names={"Awakening","Essence of Existence","First Ascension"};
   for(int i=0;i<3;i++){final int q=i;var b=button(names[i],15,88+i*40,166,33,()->{quest=q;rebuild();});b.selected=quest==i;}
  }
 }
 private void updateSelection(){rebuild();}
 private void rebuild(){clearWidgets();init();}
 private int[] loadout(){var p=Minecraft.getInstance().player;return p==null?new int[40]:JayceeNetwork.slots(p);}
 private void changePage(int n){setPage(Math.floorMod(page+n,10));}
 private void setPage(int p){page=p;send(-1,-1);rebuild();}
 private void setArt(int a){send(slot,a);rebuild();}
 private void send(int s,int a){
  var p=Minecraft.getInstance().player;if(p==null)return;
  p.getPersistentData().putInt("JayceePage",page);
  if(s>=0){var slots=loadout();slots[page*4+s]=a;p.getPersistentData().putIntArray("JayceeSlots",slots);}
  PacketDistributor.sendToServer(new JayceeNetwork.Edit(page,s,a));
 }
 private void filter(){
  filtered.clear();
  for(int i=0;i<AbilityCatalog.ARTS.length;i++){
   var a=AbilityCatalog.ARTS[i];
   if((category==0||a.category().equals(AbilityCatalog.CATEGORIES[category]))&&a.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))filtered.add(i);
  }
  if(!filtered.isEmpty()&&!filtered.contains(art))art=filtered.get(0);
  for(int i=0;i<rows.size();i++){
   var b=rows.get(i);int n=offset+i;b.visible=n<filtered.size();b.active=b.visible;
   if(b.visible){int id=filtered.get(n);b.setMessage(Component.literal(AbilityCatalog.ARTS[id].name()));b.icon=id;b.selected=id==art;}
  }
  if(equip!=null)equip.active=!filtered.isEmpty();
 }
 private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,x,y,color,false);}
 private void wrap(GuiGraphics g,String s,int x,int y,int width,int color){g.drawWordWrap(font,Component.literal(s),x,y,width,color);}
 private void line(GuiGraphics g,int x,int y,int w){DivineFrame.divider(g,x,y,w);}
 private void panel(GuiGraphics g,int x,int y,int w,int h){UiArt.frame(g,x,y,w,h,false);}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  renderBackground(g,mx,my,dt);
  int localX=(int)((mx-originX)/scale)-140,localY=(int)((my-originY)/scale);
  g.pose().pushPose();g.pose().translate((float)originX,(float)originY,0);g.pose().scale((float)scale,(float)scale,1);
  profile(g,localX+140,localY);g.pose().translate(140,0,0);
  panel(g,53,3,490,39);
  panel(g,493,48,106,284);
  text(g,"Equip Page",510,60,INK);text(g,(page+1)+" / 10",532,85,MUTED);
  text(g,"Selected: "+(slot+1),513,236,MUTED);
  text(g,"Alt + scroll",515,280,MUTED);
  if(tab==0)status(g,localX,localY);
  if(tab==1)abilities(g);
  if(tab==2)skills(g);
  if(tab==3)magic(g);
  if(tab==4)species(g);
  if(tab==5)quests(g);
  super.render(g,localX,localY,dt);
  for(int i=0;i<4;i++){
   int x=500+(i%2)*47,y=113+(i/2)*62,id=loadout()[page*4+i];
   if(id>=0&&id<AbilityCatalog.ARTS.length&&localX>=x&&localX<x+43&&localY>=y&&localY<y+55)
    g.renderTooltip(font,Component.literal(AbilityCatalog.ARTS[id].name()),Math.min(localX,420),localY);
  }
  g.pose().popPose();
 }
 private void profile(GuiGraphics g,int mx,int my){
  panel(g,1,48,135,284);var p=Minecraft.getInstance().player;if(p==null)return;
  panel(g,13,62,45,57);
  InventoryScreen.renderEntityInInventoryFollowsMouse(g,16,65,55,115,20,0f,mx,my,p);
  text(g,font.plainSubstrByWidth(p.getName().getString(),65),63,68,INK);
  text(g,"Human",63,84,MUTED);text(g,"Lv. "+p.experienceLevel,63,99,MUTED);
  line(g,15,131,106);text(g,"Player Profile",15,145,INK);
  text(g,"Health",15,169,0xFFFFA6BA);
  g.fill(15,184,121,190,0xFF26364B);
  g.fill(15,184,15+(int)(106*Math.clamp(p.getHealth()/Math.max(1,p.getMaxHealth()),0,1)),190,0xFFDD668C);
  text(g,"Armor   "+p.getArmorValue(),15,205,MUTED);
  text(g,"Mana    --",15,225,MUTED);text(g,"Soul HP --",15,245,MUTED);
  line(g,15,266,106);wrap(g,"Divine interface awakened",15,282,105,0xFF9BC9F3);
 }
 private void status(GuiGraphics g,int mx,int my){
  panel(g,2,48,486,284);var p=Minecraft.getInstance().player;if(p==null)return;
  text(g,font.plainSubstrByWidth(p.getName().getString(),180),24,64,INK);
  text(g,"Race: Human",24,80,MUTED);text(g,"Title: None equipped",24,94,MUTED);
  panel(g,18,112,138,157);
  InventoryScreen.renderEntityInInventoryFollowsMouse(g,24,119,150,259,49,0f,mx,my,p);
  text(g,"Lv. "+p.experienceLevel,31,281,0xFF98D3FF);
  g.fill(26,302,147,307,0xFF243853);g.fill(26,302,26+(int)(121*p.experienceProgress),307,0xFF66BFFF);
  DivineFrame.jewel(g,254,149,System.currentTimeMillis()/50,0xFF7FBFFF);
  text(g,"JAYCEE",232,202,INK);text(g,"Di Immortales",211,216,MUTED);
  panel(g,171,239,167,79);text(g,"Core Stats",183,249,INK);
  text(g,String.format(Locale.ROOT,"Health   %.0f / %.0f",p.getHealth(),p.getMaxHealth()),183,265,0xFFFF9AB4);
  text(g,"Armor    "+p.getArmorValue(),183,280,MUTED);
  text(g,"Mana     Not unlocked",183,295,MUTED);
  panel(g,350,62,125,65);text(g,"Titles",361,74,INK);wrap(g,"No title equipped",361,94,102,MUTED);
  panel(g,350,135,125,66);text(g,"Racial Traits",361,146,INK);wrap(g,"No racial traits unlocked.",361,165,101,MUTED);
  panel(g,350,208,125,109);text(g,"Elemental Affinity",360,219,INK);
  for(int i=0;i<6;i++)text(g,ELEMENTS[i]+"    --",365,236+i*12,MUTED);
 }
 private void abilities(GuiGraphics g){
  panel(g,2,48,123,284);panel(g,128,48,161,284);panel(g,292,48,196,284);
  text(g,"Categories",15,62,INK);text(g,"Abilities ("+filtered.size()+")",140,62,INK);
  text(g,(filtered.isEmpty()?0:offset+1)+" / "+filtered.size(),184,311,MUTED);
  if(filtered.isEmpty()){wrap(g,"No abilities match your search.",307,82,165,MUTED);return;}
  var a=AbilityCatalog.ARTS[art];UiArt.icon(g,305,65,30,a.icon());
  wrap(g,a.name(),342,67,132,INK);text(g,a.category(),342,93,0xFFBB99EB);line(g,307,111,165);
  panel(g,306,123,169,75);UiArt.icon(g,368,137,47,a.icon());
  text(g,"Description",306,210,INK);wrap(g,a.description(),306,226,166,MUTED);
  text(g,"Power preview",307,279,0xFFD6B576);
 }
 private void skills(GuiGraphics g){
  panel(g,2,48,123,284);panel(g,128,48,360,284);
  text(g,"Mastery",15,64,INK);wrap(g,TRACKS[branch]+" / Tier "+(tier+1),15,91,100,0xFFCBAAFF);
  wrap(g,"Select a node to inspect it. Mastery progression is not available yet.",15,126,100,MUTED);
  text(g,"Divine Skill Tree",147,65,INK);line(g,147,83,317);
  for(int i=0;i<4;i++){
   text(g,TRACKS[i],141+i*80,98,INK);
   g.fill(162+i*80,143,163+i*80,259,0xFF6979AB);
  }
  text(g,TRACKS[branch]+" mastery / Tier "+(tier+1),149,293,INK);
  text(g,"Not unlocked",149,310,MUTED);
 }
 private void magic(GuiGraphics g){
  panel(g,2,48,123,284);panel(g,128,48,360,284);
  text(g,"Elements",16,64,INK);text(g,ELEMENTS[element]+" Magic",146,65,INK);line(g,147,84,320);
  DivineFrame.jewel(g,310,162,System.currentTimeMillis()/50,new int[]{0xFFFF986A,0xFF60CAFF,0xFF8CE1C5,0xFFE7BD78,0xFFFFE4AC,0xFFB393F4}[element]);
  wrap(g,"Explore the "+ELEMENTS[element].toLowerCase(Locale.ROOT)+" school. Spells, mastery and casting will appear here when magic gameplay is implemented.",153,229,302,MUTED);
  text(g,"No spells unlocked",153,294,0xFFC9ACF1);
 }
 private void species(GuiGraphics g){
  panel(g,2,48,486,284);text(g,"Divine Evolution",20,65,INK);text(g,"Current species: Human",20,85,MUTED);
  String[] names={"Human","Demigod","Minor God","Major God","Olympian","Protogenos"};
  for(int i=0;i<6;i++){
   int x=22+(i%3)*155,y=123+(i/3)*75;panel(g,x,y,137,55);text(g,names[i],x+12,y+12,INK);text(g,i==0?"Current":"Locked",x+12,y+31,i==0?0xFF9EE9D5:MUTED);
  }
  wrap(g,"Evolution requirements and the final Jaycee branch will be connected when progression gameplay is implemented.",24,282,437,MUTED);
 }
 private void quests(GuiGraphics g){
  panel(g,2,48,188,284);panel(g,195,48,293,284);text(g,"Quest Journal",16,64,INK);
  String[] titles={"Awakening","Essence of Existence","First Ascension"};text(g,titles[quest],212,67,INK);line(g,212,87,255);
  if(quest==0){wrap(g,"Consume three Jayce's Essences to awaken the interface.",213,108,247,MUTED);text(g,"Complete: 3 / 3 essences",213,163,0xFF98E7CC);text(g,"Reward: Divine menu access",213,184,INK);}
  else {wrap(g,"This chapter is planned. Objectives and rewards will be added with gameplay progression.",213,109,247,MUTED);text(g,"Not available yet",213,182,0xFFD9B87F);}
 }
 private double x(double x){return (x-originX)/scale-140;}private double y(double y){return (y-originY)/scale;}
 @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x(x),y(y),b);}
 @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x(x),y(y),b);}
 @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(x(x),y(y),b,dx/scale,dy/scale);}
 @Override public void mouseMoved(double x,double y){super.mouseMoved(x(x),y(y));}
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
  if(dy!=0&&Screen.hasAltDown()){changePage(dy<0?1:-1);return true;}
  if(tab==1&&x(x)>=128&&x(x)<290){offset=Math.max(0,Math.min(Math.max(0,filtered.size()-7),offset+(dy<0?1:-1)));filter();return true;}
  return super.mouseScrolled(x(x),y(y),dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

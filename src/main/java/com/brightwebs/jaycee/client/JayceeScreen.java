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
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests","Sovereign"};
 private static final String[] TRACKS={"Body","Spirit","Divine","Utility"};
 private static final String[] ELEMENTS={"Fire","Water","Wind","Earth","Light","Dark"};
 private int tab,page,slot,art,category,offset,branch,tier,element,quest,speciesChoice,magicSchool,nexus,editorStep,root,delivery,effect,alignment;
 private boolean editorOpen=false;
 private final String[] sections={"Dashboard","Ability Library","Essence & Followers","Divine Seats","Spirits","Passives","Inhibitor","Realm Builder"};
 private final String[] roots={"Reality","Fire","Water","Storm","Void","Nature"};
 private final String[] deliveries={"Self","Ray","Touch","Projectile","Beam","Area"};
 private final String[] effects={"Damage","Healing","Teleport","Status","Summon","Soul"};
 private final String[] alignments={"Pure","Dark","Crimson"};
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
   final int t=i;var b=button(TABS[i],45+i*74,7,72,31,()->{tab=t;rebuild();});b.selected=tab==i;b.tabIcon=i;
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
  if(tab==6){
   if(editorOpen){
    for(int i=0;i<4;i++){final int step=i;var b=button(new String[]{"Source","Delivery","Effects","Identity"}[i],11+i*117,76,110,27,()->{editorStep=step;rebuild();});b.selected=editorStep==i;}
    String[] opts=editorStep==0?roots:editorStep==1?deliveries:editorStep==2?effects:alignments;
    for(int i=0;i<opts.length;i++){final int n=i;int row=i/2,col=i%2;var b=button(opts[i],23+col*139,143+row*39,128,31,()->{if(editorStep==0)root=n;else if(editorStep==1)delivery=n;else if(editorStep==2)effect=n;else alignment=n;rebuild();});b.selected=n==(editorStep==0?root:editorStep==1?delivery:editorStep==2?effect:alignment);}
    button("Back to Nexus",334,294,135,27,()->{editorOpen=false;rebuild();});
   }else{
    for(int i=0;i<sections.length;i++){final int n=i;var b=button(sections[i],12,83+i*30,148,27,()->{nexus=n;rebuild();});b.selected=nexus==i;}
    button("+ CREATE ABILITY",309,67,161,29,()->{editorOpen=true;editorStep=0;rebuild();});
   }
  }
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
   String[] schools={"Divine","Arcane","Elemental","Spatial","Temporal","Reality","Spirit","Healing","Summoning","Forbidden"};
   for(int i=0;i<schools.length;i++){final int n=i;var b=button(schools[i],128,87+i*23,133,21,()->{magicSchool=n;rebuild();});b.selected=magicSchool==i;}
  }else if(tab==4){
   String[] races={"Human","Elf","Demon","Dragon","Angel","Beastkin","Undead","Primordial","Custom"};
   for(int i=0;i<races.length;i++){final int chosen=i;var b=button(races[i],14,87+i*25,106,23,()->{speciesChoice=chosen;rebuild();});b.selected=speciesChoice==i;}
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
  if(tab==6)sovereign(g);
  super.render(g,localX,localY,dt);
  for(int i=0;i<4;i++){
   int x=500+(i%2)*47,y=113+(i/2)*62,id=loadout()[page*4+i];
   if(id>=0&&id<AbilityCatalog.ARTS.length&&localX>=x&&localX<x+43&&localY>=y&&localY<y+55)
    g.renderTooltip(font,Component.literal(AbilityCatalog.ARTS[id].name()),Math.min(localX,420),localY);
  }
  g.pose().popPose();
 }
 private void profile(GuiGraphics g,int mx,int my){
  panel(g,1,48,135,284);
  var p=Minecraft.getInstance().player;if(p==null)return;
  DivineFrame.portrait(g,11,57,55,65);
  InventoryScreen.renderEntityInInventoryFollowsMouse(g,15,62,62,118,22,0f,mx,my,p);
  text(g,font.plainSubstrByWidth(p.getName().getString(),64),70,64,INK);
  text(g,"The Immortales",70,81,0xFF91C5F3);
  text(g,"Lv. "+p.experienceLevel,70,100,0xFFDFCD9B);
  line(g,12,129,111);
  text(g,"Race",13,137,INK);text(g,"Human",79,137,0xFF9FCDF8);
  text(g,"HP",13,163,0xFFFFA5B7);
  g.fill(34,165,126,172,0xFF213C56);
  g.fill(34,165,34+(int)(92*Math.clamp(p.getHealth()/Math.max(1,p.getMaxHealth()),0,1)),172,0xFFFF6C8B);
  text(g,"Soul HP",13,180,0xFFCEA6F9);g.fill(63,183,126,190,0xFF282544);
  text(g,"--",108,181,MUTED);
  text(g,"Mana",13,198,0xFF89CEFF);g.fill(48,201,126,208,0xFF143A58);
  text(g,"--",109,199,MUTED);
  line(g,12,217,110);text(g,"Core Attributes",14,228,0xFF9FD7FF);
  String[] stats={"Strength","Endurance","Agility","Magic","Spiritual","Luck"};
  for(int i=0;i<stats.length;i++){
   text(g,stats[i],13,242+i*13,MUTED);
   text(g,"--",109,242+i*13,INK);
  }
 }
 private void status(GuiGraphics g,int mx,int my){
  panel(g,2,48,486,284);
  var p=Minecraft.getInstance().player;if(p==null)return;
  text(g,"Status Overview",195,59,0xFFCDEEFF);
  line(g,14,76,458);
  DivineFrame.portrait(g,13,84,213,230);
  // The model is the real local player, not a pasted character screenshot.
  InventoryScreen.renderEntityInInventoryFollowsMouse(g,30,95,215,294,59,0f,mx,my,p);
  panel(g,231,85,242,135);
  String[] labels={"Title","Rank","Race","Essence","Alignment","World","Playtime"};
  String[] values={"None equipped","Human","Human","Not available","Neutral",
    p.level().dimension().location().getPath(), "In progress"};
  for(int i=0;i<labels.length;i++){
   int y=96+i*17;
   text(g,labels[i],242,y,0xFF92BDD9);
   text(g,font.plainSubstrByWidth(values[i],134),326,y,INK);
  }
  panel(g,231,228,242,87);
  text(g,"Unique Traits",296,236,0xFFBFE6FF);
  line(g,245,251,214);
  String[] traits={"Flight and divine movement","Existence manipulation","Reality / creation / destruction"};
  for(int i=0;i<traits.length;i++){
   DivineFrame.gem(g,250,263+i*15,2,i==0?0xFFE6CD88:0xFF7FB8F4);
   text(g,traits[i],261,259+i*15,0xFFCBD9F5);
  }
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
  panel(g,2,48,486,284);
  text(g,"Skill Tree",224,60,INK);
  panel(g,13,84,462,195);
  int[] colors={0xFFFF827E,0xFFC18AFF,0xFFEAC27D,0xFF58E1DC};
  int[] xx={68,173,281,392};
  DivineFrame.jewel(g,235,111,System.currentTimeMillis()/50,0xFF9BDAFF);
  for(int col=0;col<4;col++){
   int cx=xx[col],color=colors[col];
   g.fill(235,120,cx+1,121,0xFF385D94);
   g.fill(cx,120,cx+1,151,color);
   DivineFrame.gem(g,cx,156,11,color);
   text(g,TRACKS[col],cx-18,178,color);
   for(int tierIndex=0;tierIndex<3;tierIndex++){
    int nx=cx-23+tierIndex*23;
    g.fill(cx,188,nx+1,189,0xAA4764A4);
    g.fill(nx,189,nx+1,214,0xAA4764A4);
    DivineFrame.gem(g,nx,219,7,color);
   }
   g.fill(cx,227,cx+1,245,0xFF56729F);
   DivineFrame.gem(g,cx,250,5,color);
  }
  text(g,"ALL      PASSIVE      COMBAT      MOVEMENT      DIVINE      SPIRITUAL",25,290,MUTED);
  text(g,"Selected: "+TRACKS[branch]+" / Tier "+(tier+1)+"   |   Mastery locked",24,308,INK);
 }
 private void magic(GuiGraphics g){
  panel(g,2,48,486,284);
  text(g,"Magic Library",212,60,INK);
  panel(g,11,78,102,241);
  panel(g,117,78,152,241);
  panel(g,274,78,202,241);
  String[] schools={"Divine Magic","Arcane Magic","Elemental Magic","Spatial Magic","Temporal Magic","Reality Magic","Spirit Magic","Healing Magic","Summoning Magic","Forbidden Magic"};
  for(int i=0;i<schools.length;i++){
   int y=87+i*23;
   UiArt.icon(g,125,y,16,i);
   text(g,schools[i],146,y+5,i==element?0xFFFFDBA2:INK);
  }
  DivineFrame.jewel(g,375,156,System.currentTimeMillis()/40,new int[]{0xFFFF986A,0xFF60CAFF,0xFF8CE1C5,0xFFE7BD78,0xFFFFE4AC,0xFFB393F4}[element]);
  text(g,font.plainSubstrByWidth(schools[magicSchool],115)+" Magic",311,214,0xFFFFDBAB);
  wrap(g,"Magic from higher essence. Develop magical affinities and spell mastery across your divine journey.",293,232,169,MUTED);
  text(g,"Mastery Level",299,280,INK);
  g.fill(291,297,458,304,0xFF193958);
  g.fill(291,297,299,304,0xFF83D7FF);
  text(g,"Unavailable",392,309,MUTED);
 }
 private void species(GuiGraphics g){
  panel(g,2,48,486,284);
  text(g,"Species / Race",211,60,INK);
  panel(g,8,79,119,248);
  panel(g,132,79,219,248);
  panel(g,355,79,125,248);
  String[] races={"Human","Elf","Demon","Dragon","Angel","Beastkin","Undead","Primordial Divinity","Custom"};
  DivineFrame.jewel(g,236,167,System.currentTimeMillis()/35,
    speciesChoice==7?0xFFE5D0FC:speciesChoice==3?0xFFFF9B73:0xFF7EC6FF);
  text(g,font.plainSubstrByWidth(races[speciesChoice],188),147,236,0xFFE9D6AA);
  wrap(g,speciesChoice==7?"An origin beyond creation, transcending ordinary divine limits.":"Race preview. Evolution paths and requirements depend on the server's progression rules.",145,255,194,MUTED);
  text(g,"Race Traits",373,92,0xFFBFE6FF);
  line(g,368,109,94);
  String[] traits=speciesChoice==7?
   new String[]{"Existence affinity","All elements","Immortality","Reality","Divine essence"}:
   new String[]{"Race identity","Affinity: unknown","Resistances: --","Growth: --","Awakening: --"};
  for(int i=0;i<traits.length;i++){
   DivineFrame.gem(g,370,125+i*26,2,i%2==0?0xFFE8CD8F:0xFF6EA2DA);
   wrap(g,traits[i],378,120+i*26,95,MUTED);
  }
  text(g,"Selected race preview",365,296,0xFFD9BD87);
  text(g,"Evolution is not enabled",365,309,MUTED);
 }
 private void quests(GuiGraphics g){
  panel(g,2,48,486,284);
  text(g,"Quest List",147,60,INK);
  panel(g,11,77,177,245);
  panel(g,193,77,171,245);
  panel(g,370,77,107,245);
  String[] names={"Awakening","Essence of Existence","First Ascension"};
  for(int i=0;i<names.length;i++){
   int y=95+i*44;
   UiArt.icon(g,199,y,26,i);
   wrap(g,names[i],231,y+3,126,i==quest?0xFFEED8A2:INK);
   text(g,i==0?"Complete":"Locked",232,y+26,i==0?0xFF9EE2CF:MUTED);
  }
  String name=names[quest];
  text(g,font.plainSubstrByWidth(name,92),378,92,INK);
  line(g,378,109,92);
  text(g,quest==0?"MAIN QUEST":"COMING SOON",379,118,0xFFE3C792);
  wrap(g,quest==0?"Consume three Jayce's Essences to awaken your divine interface.":"This chapter unlocks when its progression system becomes available.",378,145,91,MUTED);
  text(g,"Objectives",378,214,INK);
  wrap(g,quest==0?"Essences 3 / 3":"Not available",378,229,90,0xFFA5CFF0);
  text(g,"Rewards",378,265,INK);
  DivineFrame.gem(g,390,289,8,0xFF7BC5FF);
  text(g,quest==0?"Menu":"TBD",407,286,MUTED);
  text(g,"Quest details",24,307,0xFF96C6EF);
 }
 private void sovereign(GuiGraphics g){
  panel(g,2,48,486,284);
  if(editorOpen){
   text(g,"DIVINE ART FORGE",18,60,0xFFEBD6A0);
   text(g,"An original ability, assembled by its creator",180,61,MUTED);
   text(g,"01  ROOT       02  DELIVERY       03  EFFECT       04  ALIGNMENT",27,114,0xFF9BBDDB);
   panel(g,19,126,451,101);
   text(g,"ART CONFIGURATION",29,237,0xFFE6CA90);
   text(g,"Source: "+roots[root]+"  /  Target: "+deliveries[delivery],30,288,INK);
   text(g,"Effect: "+effects[effect]+"  /  Essence: "+alignments[alignment],30,305,MUTED);
   
   return;
  }
  text(g,"JAYCEE  /  SOVEREIGN NEXUS",17,60,0xFFEFDCA6);
  panel(g,7,75,160,248);
  panel(g,175,106,298,215);
  String headline=sections[nexus];
  text(g,headline.toUpperCase(Locale.ROOT),187,120,0xFFE9D1A0);
  line(g,187,137,273);
  if(nexus==0){
   text(g,"CURRENT",191,153,MUTED);text(g,"Jaycee  |  Source",289,153,INK);
   text(g,"ESSENCE",191,177,MUTED);text(g,"Unbounded  |  Pure / Dark / Crimson",263,177,INK);
   text(g,"SOUL",191,201,MUTED);text(g,"Damage control: preview",263,201,INK);
   text(g,"ABILITIES",191,225,MUTED);text(g,"Personal + server arts",263,225,INK);
   text(g,"REALITY",191,249,MUTED);text(g,"Existence / Creation / Destruction",263,249,INK);
   wrap(g,"Sovereign administration features require server-authoritative gameplay.",191,281,260,MUTED);
  }else if(nexus==1){
   wrap(g,"Your personal and server-created arts will appear here. Open Create Ability to configure a new art's source, targeting, effect and essence alignment.",190,153,262,INK);
  }else if(nexus==2){
   text(g,"PURE   /   DARK   /   CRIMSON",191,157,0xFFCEB9F4);
   wrap(g,"Follower selection, essence gifting and reclaiming, Soul HP and inherited abilities will be managed here.",190,182,266,MUTED);
  }else if(nexus==3){
   wrap(g,"Olympian and lesser divine seats, succession challenges and approval records. A seat cannot have duplicate owners.",190,155,267,MUTED);
  }else if(nexus==4){
   wrap(g,"Spirit creation, ownership, presence and commands. Jaycee has no fixed spirit limit in the design.",190,154,264,MUTED);
  }else if(nexus==5){
   String[] names={"Flight and speed","Aquatic breathing","Underwater movement","Night vision","Regeneration","Universal Shapeshift"};
   for(int i=0;i<names.length;i++){int y=149+i*24;UiArt.frame(g,189,y,269,22,false);text(g,names[i],198,y+7,INK);text(g,"CONFIG",407,y+7,0xFFE6C98D);}
  }else if(nexus==6){
   wrap(g,"The divine inhibitor limits granted strength, authority and abilities. Changing restrictions must be validated by the server.",190,156,265,MUTED);
  }else{
   wrap(g,"Realm Builder: choose Divine or Dark blocks, assemble a 3D boxed layout, preview it, then confirm world edits using bounded server operations.",190,156,261,MUTED);
  }
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

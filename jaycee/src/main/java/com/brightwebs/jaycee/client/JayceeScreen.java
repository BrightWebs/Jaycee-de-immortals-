package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.network.BrightNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/**
 * Six interactive pages; the status portrait uses the real local player's skin.
 * The right-hand loadout is the same 5-slot x 10-page ability bar shown on the HUD.
 */
public class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private static final String[] TRACKS={"Body","Spirit","Divine","Utility"};
 private static final int ROWS=6;
 // Loadout slot positions (relative), laid out 2-2-1.
 private static final int[] SLOT_X={419,462,419,462,440};
 private static final int[] SLOT_Y={99,99,151,151,203};
 private static int tab=0,selected=0,category=0; // remembered between openings
 private int skill=0,skillTier=0,shownOwned=-1;
 private Ability chosen;
 private int left,top;
 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}

 private void button(String label,int x,int y,int w,int h,Runnable r){addRenderableWidget(new OrnateButton(label,left+x,top+y,w,h,r));}
 private static AbilityData data(){var p=Minecraft.getInstance().player;return p==null?AbilityData.EMPTY:AbilityAPI.get(p);}
 private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload){ClientPacketDistributor.sendToServer(payload);}
 private static int glyph(Identifier id){return Math.floorMod(id.hashCode(),997);}

 private List<Ability> listed(){
  var p=Minecraft.getInstance().player;
  if(p==null)return List.of();
  return AbilityAPI.owned(p).stream()
   .filter(a->!a.isPassive())
   .filter(a->category==0||a.category()==AbilityCategory.values()[category-1])
   .limit(ROWS).toList();
 }

 @Override protected void init(){
  left=(width-520)/2;top=(height-310)/2;
  for(int i=0;i<6;i++){final int t=i;button(TABS[i],8+i*84,10,82,27,()->{tab=t;rebuild();});}
  button("<",420,59,22,20,()->send(new BrightNetwork.SetPage(data().page()+AbilityData.PAGES-1)));
  button(">",490,59,22,20,()->send(new BrightNetwork.SetPage(data().page()+1)));
  for(int i=0;i<AbilityData.SLOTS_PER_PAGE;i++){final int s=i;button(""+(i+1),SLOT_X[i]+1,SLOT_Y[i]+1,39,39,()->selected=s);}
  if(tab==1){
   button("All",149,90,97,25,()->{category=0;rebuild();});
   AbilityCategory[] cats=AbilityCategory.values();
   for(int i=0;i<cats.length;i++){final int c=i+1;button(cats[i].label(),149,90+(i+1)*30,97,25,()->{category=c;rebuild();});}
   List<Ability> list=listed();
   for(int i=0;i<list.size();i++){final Ability a=list.get(i);button(a.name().getString(),250,92+i*29,152,25,()->assign(a));}
  }else if(tab==2){
   for(int i=0;i<4;i++){final int branch=i;button(TRACKS[i],153+i*63,94,58,24,()->{skill=branch;});}
  }
  var p=Minecraft.getInstance().player;
  shownOwned=p==null?0:AbilityAPI.get(p).owned().size();
 }
 private void rebuild(){clearWidgets();init();}

 /** Puts the clicked ability into the selected slot on the current page. */
 private void assign(Ability a){
  chosen=a;
  send(new BrightNetwork.AssignSlot(data().page()*AbilityData.SLOTS_PER_PAGE+selected,a.id().toString()));
 }

 private void box(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean bright){DivineFrame.frame(g,x,y,w,h,bright);}
 private void text(GuiGraphicsExtractor g,String s,int x,int y,int color){g.text(font,s,left+x,top+y,color,false);}
 private void highlight(GuiGraphicsExtractor g,int x,int y,int w,int h){box(g,left+x,top+y,w,h,true);}
 private void bar(GuiGraphicsExtractor g,int x,int y,int w,double ratio,int color){
  g.fill(left+x,top+y,left+x+w,top+y+7,0xFF203B5A);
  g.fill(left+x+1,top+y+1,left+x+1+(int)((w-2)*Math.max(0,Math.min(1,ratio))),top+y+6,color);
 }

 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  var player=Minecraft.getInstance().player;
  // Owned abilities changed while open (e.g. /ability learn): refresh the list.
  if(player!=null&&AbilityAPI.get(player).owned().size()!=shownOwned)rebuild();
  AbilityData d=data();
  extractBackground(g,mx,my,dt);
  box(g,left,top,520,310,false);DivineFrame.ribbon(g,left+6,top+5,508,36);
  box(g,left+5,top+46,130,257,false);box(g,left+141,top+46,270,257,false);box(g,left+415,top+46,100,257,false);
  highlight(g,6+tab*84,8,85,31);
  text(g,"JAYCEE",18,56,0xFFF2F8FF);
  text(g,player==null?"Player":font.plainSubstrByWidth(player.getName().getString(),105),16,73,0xFF9BDAFF);
  text(g,"DIVINE INITIATE",14,91,0xFF8CC7FF);
  if(tab!=0){
   String[] names={"HEALTH","MANA","SOUL HP"};int[] colors={0xFFE96888,0xFF52B7EE,0xFFAE8CFF};
   double[] ratios={player==null?0:player.getHealth()/Math.max(1,player.getMaxHealth()),d.maxMana()==0?0:(double)d.mana()/d.maxMana(),0};
   for(int i=0;i<3;i++){int y=135+i*31;text(g,names[i],15,y,colors[i]);bar(g,15,y+14,104,ratios[i],colors[i]);}
  }
  text(g,TABS[tab].toUpperCase(),154,58,0xFFE5F4FF);
  loadout(g,d);
  if(tab==0)status(g,d);else if(tab==1)abilities(g);else if(tab==2)skills(g);else if(tab==3)magic(g);else if(tab==4)species(g);else quests(g);
  super.extractRenderState(g,mx,my,dt);
 }

 private void loadout(GuiGraphicsExtractor g,AbilityData d){
  text(g,"LOADOUT",436,52,0xFFE0F1FF);DivineFrame.divider(g,left+423,top+79,82);
  text(g,"PAGE "+(d.page()+1)+"/"+AbilityData.PAGES,449,65,0xFF9CD4FF);
  for(int i=0;i<AbilityData.SLOTS_PER_PAGE;i++){
   int xx=SLOT_X[i],yy=SLOT_Y[i];
   highlight(g,xx,yy,41,41);
   var id=d.slot(i);
   if(id.isPresent())DivineGlyph.icon(g,left+xx+8,top+yy+8,25,glyph(id.get()),i==selected);
   else g.fill(left+xx+9,top+yy+9,left+xx+32,top+yy+32,i==selected?0x663DA3EF:0x44101830);
   if(i==selected)g.fill(left+xx+7,top+yy+35,left+xx+33,top+yy+37,0xFFA3E9FF);
  }
  text(g,"Alt + scroll",422,250,0xFF90BBE8);
  text(g,"Right-click: clear",419,262,0xFF6F93BF);
 }

 private void status(GuiGraphicsExtractor g,AbilityData d){
  var player=Minecraft.getInstance().player;
  text(g,"CHARACTER",22,108,0xFFC4E5FF);
  DivineFrame.portrait(g,left+13,top+125,114,130);
  // The actual world player's 3D skin + equipped armor. No fixed Steve texture.
  if(player!=null)InventoryScreen.extractEntityInInventoryFollowsMouse(
   g,left+18,top+133,left+122,top+247,38,0f,224f,123f,player);
  text(g,"LIVE PLAYER MODEL",17,268,0xFF9ED2FA);
  DivineFrame.divider(g,left+19,top+285,101);
  text(g,"STATUS OVERVIEW",155,83,0xFF8FCFFF);
  DivineFrame.divider(g,left+156,top+98,235);
  box(g,left+153,top+106,246,167,false);
  text(g,"ADVENTURER PROFILE",173,119,0xFFE5F5FF);
  DivineFrame.divider(g,left+171,top+132,201);
  if(player!=null)text(g,font.plainSubstrByWidth(player.getName().getString(),184),172,139,0xFF8DD7FF);
  String[] stats={"HEALTH","MANA","SOUL"};
  int[] colors={0xFFE96F91,0xFF67C2FF,0xFFA58AFF};
  double[] ratios={player==null?0:player.getHealth()/Math.max(1,player.getMaxHealth()),d.maxMana()==0?0:(double)d.mana()/d.maxMana(),0};
  String[] values={player==null?"-":(int)Math.ceil(player.getHealth())+"/"+(int)player.getMaxHealth(),d.mana()+"/"+d.maxMana(),"-"};
  for(int i=0;i<3;i++){
   int y=163+i*30;
   text(g,stats[i],169,y,colors[i]);
   text(g,values[i],376-font.width(values[i]),y,0xFF8699BA);
   g.fill(left+169,top+y+12,left+376,top+y+20,0xFF1E3150);
   g.fill(left+170,top+y+13,left+170+(int)(205*Math.max(0,Math.min(1,ratios[i]))),top+y+19,colors[i]);
  }
  text(g,"ABILITIES OWNED: "+d.owned().size(),168,283,0xFFB8DDFF);
 }

 private void abilities(GuiGraphicsExtractor g){
  text(g,"DIVINE ARTS",151,78,0xFF91CFFE);DivineFrame.divider(g,left+154,top+86,237);
  for(int i=0;i<=AbilityCategory.values().length;i++){if(i==category)highlight(g,147,88+i*30,101,29);DivineGlyph.icon(g,left+149,top+93+i*30,14,i,i==category);}
  List<Ability> list=listed();
  if(list.isEmpty())text(g,"No abilities here yet",256,100,0xFF7FA6CF);
  for(int i=0;i<list.size();i++){Ability a=list.get(i);if(a==chosen)highlight(g,249,90+i*29,155,28);DivineGlyph.icon(g,left+251,top+95+i*29,14,glyph(a.id()),a==chosen);}
  highlight(g,153,274,250,23);
  String info=chosen==null?"Pick a slot, then an ability to place it"
   :chosen.name().getString()+"  |  "+chosen.manaCost()+" mana  |  "+(chosen.cooldownTicks()/20f)+"s";
  text(g,font.plainSubstrByWidth(info,235),163,281,0xFFE0EFFF);
 }

 private void magic(GuiGraphicsExtractor g){
  text(g,"ARCANE LIBRARY",151,78,0xFF91CFFE);DivineFrame.divider(g,left+154,top+86,237);
  text(g,"Magic arrives in a later step",170,151,0xFF9ABDDC);
 }

 private void skills(GuiGraphicsExtractor g){
  text(g,"DIVINE SKILL TREE",156,78,0xFF9CCEFF);DivineFrame.divider(g,left+155,top+88,237);
  for(int i=0;i<4;i++){
   int cx=180+i*63;
   g.fill(left+cx,top+128,left+cx+2,top+255,0xFF386EA9);
   for(int j=0;j<4;j++){
    int cy=140+j*34;
    DivineGlyph.icon(g,left+cx-9,top+cy-7,19,(i+j)%6,i==skill&&j==skillTier);
   }
  }
  text(g,TRACKS[skill]+" mastery / Tier "+(skillTier+1)+" (preview)",157,280,0xFFB3D4ED);
 }
 private void species(GuiGraphicsExtractor g){
  text(g,"DIVINE EVOLUTION",158,88,0xFF9AD9FF);highlight(g,159,118,228,121);
  text(g,"Mortal  >  Seer  >  Hero  >  Demigod",166,151,0xFFE4EBFF);
  text(g,"Grades arrive with the Ichor step",157,272,0xFF9ABDDC);
 }
 private void quests(GuiGraphicsExtractor g){
  text(g,"QUEST JOURNAL",155,88,0xFF9AD9FF);
  String[] names={"Awakening","Through the Mist","The First Ascension","Beyond Olympus"};
  for(int i=0;i<names.length;i++){highlight(g,151,116+i*38,250,34);text(g,names[i],161,129+i*38,0xFFE6F3FF);}
 }

 @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
  int x=(int)event.x()-left,y=(int)event.y()-top;
  // Right-click a loadout slot to clear it.
  if(event.button()==1){
   for(int i=0;i<AbilityData.SLOTS_PER_PAGE;i++){
    if(x>=SLOT_X[i]&&x<SLOT_X[i]+41&&y>=SLOT_Y[i]&&y<SLOT_Y[i]+41){
     send(new BrightNetwork.AssignSlot(data().page()*AbilityData.SLOTS_PER_PAGE+i,""));
     return true;
    }
   }
  }
  if(tab==2&&event.button()==0){
   for(int i=0;i<4;i++)for(int j=0;j<4;j++){
    if(Math.abs(x-(180+i*63))<=13&&Math.abs(y-(140+j*34))<=14){skill=i;skillTier=j;return true;}
   }
  }
  return super.mouseClicked(event,doubleClick);
 }
 @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
  if(dy!=0&&Minecraft.getInstance().hasAltDown()){
   send(new BrightNetwork.SetPage(data().page()+(dy<0?1:AbilityData.PAGES-1)));
   return true;
  }
  return super.mouseScrolled(mx,my,dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

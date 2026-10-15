package com.brightwebs.jaycee.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Six interactive pages; the status portrait uses the real local player's skin. */
public class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private static final String[] CATS={"All","Offense","Defense","Support","Movement","Reality"};
 private static final String[] ARTS={"Reality Severance","Celestial Guard","Divine Pulse","Temporal Step","Soul Restore","Aether Flight"};
 private static final String[] TRACKS={"Body","Spirit","Divine","Utility"};
 private int tab=0,page=0,selected=0,category=0,art=0,skill=0;
 private int left,top;
 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 private void button(String label,int x,int y,int w,int h,Runnable r){addRenderableWidget(new OrnateButton(label,left+x,top+y,w,h,r));}
 @Override protected void init(){
  left=(width-520)/2;top=(height-310)/2;
  for(int i=0;i<6;i++){final int t=i;button(TABS[i],8+i*84,10,82,27,()->{tab=t;rebuild();});}
  button("<",420,59,22,20,()->page=(page+9)%10);button(">",490,59,22,20,()->page=(page+1)%10);
  for(int i=0;i<4;i++){final int s=i;button(""+(i+1),420+(i%2)*43,100+(i/2)*52,39,39,()->selected=s);}
  if(tab==1||tab==3){
   for(int i=0;i<CATS.length;i++){final int c=i;button(CATS[i],149,90+i*30,97,25,()->{category=c;rebuild();});}
   for(int i=0;i<ARTS.length;i++){final int a=i;button(ARTS[i],250,92+i*29,152,25,()->{art=a;});}
  }else if(tab==2){
   for(int i=0;i<4;i++){final int branch=i;button(TRACKS[i],153+i*63,94,58,24,()->{skill=branch;});}
  }
 }
 private void rebuild(){clearWidgets();init();}
 private void box(GuiGraphics g,int x,int y,int w,int h,boolean bright){
  int edge=bright?0xFF9EEBFF:0xFF4274AA;
  g.fill(x+4,y+4,x+w-4,y+h-4,0xE40A1A32);
  g.fill(x+6,y+6,x+w-6,y+h-6,0x751A3555);
  g.fill(x+5,y,x+w-5,y+1,edge);g.fill(x+5,y+h-1,x+w-5,y+h,edge);
  g.fill(x,y+5,x+1,y+h-5,edge);g.fill(x+w-1,y+5,x+w,y+h-5,edge);
  for(int i=0;i<5;i++){g.fill(x+5-i,y+i,x+6-i,y+i+1,edge);g.fill(x+w-6+i,y+i,x+w-5+i,y+i+1,edge);}
  g.fill(x+7,y+4,x+Math.min(w-7,26),y+5,0xFF85C8FF);
 }
 private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,left+x,top+y,color,false);}
 private void highlight(GuiGraphics g,int x,int y,int w,int h){box(g,left+x,top+y,w,h,true);}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  renderBackground(g,mx,my,dt);
  box(g,left,top,520,310,false);
  box(g,left+5,top+46,130,257,false);box(g,left+141,top+46,270,257,false);box(g,left+415,top+46,100,257,false);
  highlight(g,6+tab*84,8,85,31);
  text(g,"JAYCEE",18,56,0xFFF2F8FF);
  var player=Minecraft.getInstance().player;
  text(g,player==null?"Player":font.plainSubstrByWidth(player.getName().getString(),105),16,73,0xFF9BDAFF);
  text(g,"SUPREME EXISTENCE",14,91,0xFF8CC7FF);
  for(int i=0;i<3;i++){int y=135+i*31;int color=i==0?0xFFE96888:i==1?0xFF52B7EE:0xFFAE8CFF;
   text(g,new String[]{"HEALTH","MANA","SOUL HP"}[i],15,y,color);
   g.fill(left+15,top+y+14,left+119,top+y+21,0xFF203B5A);
   double ratio=i==0&&player!=null?player.getHealth()/Math.max(1,player.getMaxHealth()):0;
   if(i==0)g.fill(left+16,top+y+15,left+16+(int)(102*Math.max(0,Math.min(1,ratio))),top+y+20,color);
  }
  text(g,TABS[tab].toUpperCase(),154,58,0xFFE5F4FF);
  text(g,"EQUIPMENT",427,52,0xFFE0F1FF);
  text(g,"PAGE "+(page+1)+"/10",449,65,0xFF9CD4FF);
  for(int i=0;i<4;i++){int xx=419+(i%2)*43,yy=99+(i/2)*52;highlight(g,xx,yy,41,41);if(i==selected)g.fill(left+xx+7,top+yy+31,left+xx+33,top+yy+33,0xFFA3E9FF);}
  text(g,"Alt + scroll",422,257,0xFF90BBE8);
  if(tab==0)status(g);else if(tab==1||tab==3)abilities(g);else if(tab==2)skills(g);else if(tab==4)species(g);else quests(g);
  super.render(g,mx,my,dt);
 }
 private void status(GuiGraphics g){
  text(g,"STATUS OVERVIEW",155,83,0xFF8FCFFF);
  highlight(g,173,108,208,157);
  if(Minecraft.getInstance().player!=null)
   InventoryScreen.renderEntityInInventoryFollowsMouse(g,left+205,top+115,left+350,top+250,43,0f,245f,130f,Minecraft.getInstance().player);
  text(g,"PLAYER MODEL",213,266,0xFFE3EFFA);
  text(g,"Player skin updates automatically",155,283,0xFF88B4DD);
 }
 private void abilities(GuiGraphics g){
  text(g,tab==1?"DIVINE ARTS":"ARCANE LIBRARY",151,78,0xFF91CFFE);
  for(int i=0;i<CATS.length;i++)if(i==category)highlight(g,147,88+i*30,101,29);
  for(int i=0;i<ARTS.length;i++)if(i==art)highlight(g,249,90+i*29,155,28);
  highlight(g,153,274,250,23);
  text(g,font.plainSubstrByWidth(ARTS[art]+" - Select a slot to prepare",235),163,281,0xFFE0EFFF);
 }
 private void skills(GuiGraphics g){
  text(g,"DIVINE SKILL TREE",156,78,0xFF9CCEFF);
  for(int i=0;i<4;i++){
   int cx=180+i*63;
   g.fill(left+cx,top+128,left+cx+2,top+255,0xFF386EA9);
   for(int j=0;j<4;j++){
    int cy=140+j*34,glow=i==skill?0xFF9AE7FF:0xFF6E93D0;
    for(int d=0;d<6;d++){int half=5-Math.abs(d-5);g.fill(left+cx-half,top+cy+d,left+cx+half+1,top+cy+d+1,glow);}
   }
  }
  text(g,"Select a branch to inspect its path",157,280,0xFFB3D4ED);
 }
 private void species(GuiGraphics g){
  text(g,"DIVINE EVOLUTION",158,88,0xFF9AD9FF);highlight(g,159,118,228,121);
  text(g,"Human  >  Demigod  >  Divine",171,151,0xFFE4EBFF);
  text(g,"Evolution gameplay coming later",157,272,0xFF9ABDDC);
 }
 private void quests(GuiGraphics g){
  text(g,"QUEST JOURNAL",155,88,0xFF9AD9FF);
  String[] names={"Awakening","Essence of Existence","The First Ascension","A New Reality"};
  for(int i=0;i<names.length;i++){highlight(g,151,116+i*38,250,34);text(g,names[i],161,129+i*38,0xFFE6F3FF);}
 }
 @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
  if(dy!=0 && Screen.hasAltDown()){page=Math.floorMod(page+(dy<0?1:-1),10);return true;}
  return super.mouseScrolled(mx,my,dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

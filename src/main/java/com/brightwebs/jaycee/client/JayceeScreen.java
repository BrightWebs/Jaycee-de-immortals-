package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.client.model.PreviewData.Ability;
import com.brightwebs.jaycee.client.model.JournalData;
import com.brightwebs.jaycee.client.view.*;
import com.brightwebs.jaycee.client.widget.OrnateButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared shell. All drawing and pointer input use the same scaled design coordinates. */
public final class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private final TabView[] views={new StatusView(),new AbilitiesView(),new SkillsView(),
  new JournalView(JournalData.MAGIC),new JournalView(JournalData.SPECIES),new JournalView(JournalData.QUESTS)};
 private final Ability[][] loadout=new Ability[10][4];
 private static final int DESIGN_WIDTH=640,DESIGN_HEIGHT=500;
 private static final float UI_SIZE=0.86f;
 private int tab,page,slot;
 private boolean showLoadout;
 private float scale=1;
 private int originX,originY;

 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 public GreatSageEnergyCore energyCore(){return ((StatusView)views[0]).energyCore();}
 @Override protected void init(){
  scale=UI_SIZE*Math.min(1f,Math.min(width/(float)DESIGN_WIDTH,height/(float)DESIGN_HEIGHT));
  originX=(int)((width-DESIGN_WIDTH*scale)/2);originY=(int)((height-DESIGN_HEIGHT*scale)/2);
  for(int i=0;i<TABS.length;i++){
   final int index=i;
   button(TABS[i],12+i*103,6,101,62,()->{tab=index;rebuild();}).selected(tab==i).navigation(i);
  }
  if(!showLoadout)views[tab].init(this);
  button(showLoadout?"Close loadout":"Loadout",282,472,124,20,()->{showLoadout=!showLoadout;rebuild();});
  if(!showLoadout)return;
  button("<",20,438,22,22,()->{page=(page+9)%10;rebuild();});
  button(">",113,438,22,22,()->{page=(page+1)%10;rebuild();});
  for(int i=0;i<4;i++){
   final int index=i;
   Ability ability=loadout[page][i];
   button((i+1)+": "+(ability==null?"Empty":ability.name()),144+i*120,437,116,24,()->{
    slot=index;rebuild();
   }).selected(slot==i);
  }
 }
 public void rebuild(){clearWidgets();init();}
 public OrnateButton button(String label,int x,int y,int w,int h,Runnable action){
  return addRenderableWidget(new OrnateButton(label,x,y,w,h,action));
 }
 public void equip(Ability ability){loadout[page][slot]=ability;showLoadout=true;rebuild();}
 public void coloredText(GuiGraphics g,String text,int x,int y,int maxWidth,int color){
  g.drawString(font,font.plainSubstrByWidth(text,maxWidth),x,y,color,false);
 }
 public void rightText(GuiGraphics g,String text,int right,int y,int color){
  g.drawString(font,text,right-font.width(text),y,color,false);
 }
 public void largeText(GuiGraphics g,String text,int x,int y,int maxWidth,float size,int color){
  g.pose().pushPose();
  g.pose().translate(x,y,0);
  g.pose().scale(size,size,1);
  coloredText(g,text,0,0,(int)(maxWidth/size),color);
  g.pose().popPose();
 }
 public void text(GuiGraphics g,String text,int x,int y,int maxWidth){
  g.drawString(font,font.plainSubstrByWidth(text,maxWidth),x,y,0xFFC3E6FF,false);
 }
 public void wrapped(GuiGraphics g,String text,int x,int y,int maxWidth){
  g.drawWordWrap(font,Component.literal(text),x,y,maxWidth,0xFF9BBDDE);
 }
 public void panel(GuiGraphics g,int x,int y,int w,int h,String title){
  DivineFrame.frame(g,x,y,w,h,false);
  // Reserve a quiet title band so corner filigree never intersects the glyphs.
  g.fill(x+10,y+10,x+w-10,y+25,0xFF081424);
  text(g,title,x+20,y+14,w-40);
  DivineFrame.divider(g,x+14,y+28,w-28);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0x99040A14);
  int localX=(int)localX(mx),localY=(int)localY(my);
  g.pose().pushPose();
  g.pose().translate(originX,originY,0);
  g.pose().scale(scale,scale,1);
  DivineFrame.frame(g,0,0,640,74,true);
  DivineFrame.frame(g,0,82,640,418,false);
  views[tab].render(this,g,localX,localY);
  g.pose().popPose();
  views[tab].renderUnscaled(this,g,mx,my);
  g.pose().pushPose();
  g.pose().translate(originX,originY,0);
  g.pose().scale(scale,scale,1);
  if(showLoadout){
   panel(g,12,410,616,58,"LOADOUT PREVIEW (NOT SAVED)");
   text(g,"Page "+(page+1)+"/10",46,445,64);
  }
  super.render(g,localX,localY,delta);
  g.pose().popPose();
 }
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){
  // Own the backdrop in render(): vanilla background blur must never post-process our UI.
 }
 private double localX(double x){return (x-originX)/scale;}
 private double localY(double y){return (y-originY)/scale;}
 public int screenX(int x){return Math.round(originX+x*scale);}
 public int screenY(int y){return Math.round(originY+y*scale);}
 public int scaledSize(int size){return Math.round(size*scale);}
 @Override public boolean mouseClicked(double x,double y,int button){
  return super.mouseClicked(localX(x),localY(y),button);
 }
 @Override public boolean mouseReleased(double x,double y,int button){
  return super.mouseReleased(localX(x),localY(y),button);
 }
 @Override public void mouseMoved(double x,double y){super.mouseMoved(localX(x),localY(y));}
 @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
  return super.mouseDragged(localX(x),localY(y),button,dx/scale,dy/scale);
 }
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
  if(dy!=0&&hasAltDown()){page=Math.floorMod(page+(dy<0?1:-1),10);rebuild();return true;}
  if(showLoadout)return super.mouseScrolled(localX(x),localY(y),dx,dy);
  return views[tab].scroll(this,localX(x),localY(y),dy)
   ||super.mouseScrolled(localX(x),localY(y),dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

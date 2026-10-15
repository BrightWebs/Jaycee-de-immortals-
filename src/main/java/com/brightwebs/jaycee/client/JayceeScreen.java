package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.client.model.PreviewData.Ability;
import com.brightwebs.jaycee.client.view.*;
import com.brightwebs.jaycee.client.widget.OrnateButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared shell. All drawing and pointer input use the same scaled design coordinates. */
public final class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private final TabView[] views={new StatusView(),new AbilitiesView(),new SkillsView(),
  new PlaceholderView("Magic","The arcane library awaits your first spell."),
  new PlaceholderView("Species","Human > Demigod > Divine\n\nSample evolution path"),
  new PlaceholderView("Quests","Awakening\nEssence of Existence\nThe First Ascension")};
 private final Ability[][] loadout=new Ability[10][4];
 private int tab,page,slot;
 private float scale=1;
 private int originX,originY;

 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 @Override protected void init(){
  scale=Math.min(1f,Math.min(width/640f,height/360f));
  originX=(int)((width-640*scale)/2);originY=(int)((height-360*scale)/2);
  for(int i=0;i<TABS.length;i++){
   final int index=i;
   button(TABS[i],12+i*103,10,101,28,()->{tab=index;rebuild();}).selected(tab==i);
  }
  button("<",16,325,22,22,()->{page=(page+9)%10;rebuild();});
  button(">",109,325,22,22,()->{page=(page+1)%10;rebuild();});
  for(int i=0;i<4;i++){
   final int index=i;
   Ability ability=loadout[page][i];
   button((i+1)+": "+(ability==null?"Empty":ability.name()),144+i*120,324,116,24,()->{
    slot=index;rebuild();
   }).selected(slot==i);
  }
  views[tab].init(this);
 }
 public void rebuild(){clearWidgets();init();}
 public OrnateButton button(String label,int x,int y,int w,int h,Runnable action){
  return addRenderableWidget(new OrnateButton(label,x,y,w,h,action));
 }
 public void equip(Ability ability){loadout[page][slot]=ability;rebuild();}
 public void text(GuiGraphics g,String text,int x,int y,int maxWidth){
  g.drawString(font,font.plainSubstrByWidth(text,maxWidth),x,y,0xFFC3E6FF,false);
 }
 public void wrapped(GuiGraphics g,String text,int x,int y,int maxWidth){
  g.drawWordWrap(font,Component.literal(text),x,y,maxWidth,0xFF9BBDDE);
 }
 public void panel(GuiGraphics g,int x,int y,int w,int h,String title){
  DivineFrame.frame(g,x,y,w,h,false);
  text(g,title,x+12,y+12,w-24);
  DivineFrame.divider(g,x+12,y+26,w-24);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  renderBackground(g,mx,my,delta);
  int localX=(int)localX(mx),localY=(int)localY(my);
  g.pose().pushPose();
  g.pose().translate(originX,originY,0);
  g.pose().scale(scale,scale,1);
  DivineFrame.frame(g,0,0,640,360,false);
  DivineFrame.ribbon(g,6,5,628,36);
  panel(g,12,304,616,48,"LOADOUT PREVIEW (NOT SAVED)");
  text(g,"Page "+(page+1)+"/10",42,333,64);
  views[tab].render(this,g,localX,localY);
  g.pose().popPose();
  views[tab].renderUnscaled(this,g,mx,my);
  g.pose().pushPose();
  g.pose().translate(originX,originY,0);
  g.pose().scale(scale,scale,1);
  super.render(g,localX,localY,delta);
  g.pose().popPose();
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
  return views[tab].scroll(this,localX(x),localY(y),dy)
   ||super.mouseScrolled(localX(x),localY(y),dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

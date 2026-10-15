package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.ability.LoadoutClientState;
import com.brightwebs.jaycee.ability.LoadoutNetwork;
import com.brightwebs.jaycee.client.model.JournalData;
import com.brightwebs.jaycee.client.view.*;
import com.brightwebs.jaycee.client.widget.OrnateButton;
import com.brightwebs.jaycee.client.widget.EquipSlot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared shell. All drawing and pointer input use the same scaled design coordinates. */
public final class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Quests"};
 private final TabView[] views={new StatusView(),new AbilitiesView(),new SkillsView(),
  new JournalView(JournalData.MAGIC),new JournalView(JournalData.QUESTS)};
 private final TabView speciesView=new JournalView(JournalData.SPECIES);
 private static final int DESIGN_WIDTH=840,DESIGN_HEIGHT=500;
 private static final float UI_SIZE=0.86f;
 private int tab,selectedAbility=-1;
 private boolean species;
 private float scale=1;
 private int originX,originY;
 private EditBox searchBox;

 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 public GreatSageEnergyCore energyCore(){return ((StatusView)views[0]).energyCore();}
 @Override protected void init(){
  scale=UI_SIZE*Math.min(1f,Math.min(width/(float)DESIGN_WIDTH,height/(float)DESIGN_HEIGHT));
  originX=(int)((width-DESIGN_WIDTH*scale)/2);originY=(int)((height-DESIGN_HEIGHT*scale)/2);
  for(int i=0;i<TABS.length;i++){
   final int index=i;
   button(TABS[i],12+i*123,6,121,62,()->{tab=index;species=false;rebuild();}).selected(tab==i).navigation(i==4?5:i);
  }
  currentView().init(this);
  if(species)button("Back to Status",242,472,150,20,()->{species=false;rebuild();});
  button("<",660,141,26,24,()->changePage(-1));
  button(">",792,141,26,24,()->changePage(1));
  for(int i=0;i<4;i++){
   final int index=i;
   addRenderableWidget(new EquipSlot(663+(i%2)*82,190+(i/2)*82,i,()->assign(index)));
   button("Remove "+(i+1),660+(i%2)*82,383+(i/2)*29,78,24,()->{
    var state=LoadoutClientState.get();
    LoadoutNetwork.sendAction(LoadoutNetwork.CLEAR,state.page(),index,-1);
   });
  }
  for(int i=0;i<10;i++){
   final int page=i;
   button(""+(i+1),660+i*16,351,16,20,()->LoadoutNetwork.sendAction(LoadoutNetwork.SELECT_PAGE,page,0,-1));
  }
 }
 private TabView currentView(){return species?speciesView:views[tab];}
 public void openSpecies(){tab=0;species=true;rebuild();}
 public void selectAbility(int id){selectedAbility=id;}
 private void assign(int slot){
  if(selectedAbility<0)return;
  var state=LoadoutClientState.get();
  if(!state.accessible()||selectedAbility>=state.unlocked().size()||!state.unlocked().get(selectedAbility))return;
  LoadoutNetwork.sendAction(LoadoutNetwork.EQUIP,state.page(),slot,selectedAbility);
 }
 private void changePage(int delta){
  LoadoutNetwork.sendAction(LoadoutNetwork.SELECT_PAGE,Math.floorMod(LoadoutClientState.get().page()+delta,10),0,-1);
 }
 public void rebuild(){clearWidgets();init();}
 public OrnateButton button(String label,int x,int y,int w,int h,Runnable action){
  return addRenderableWidget(new OrnateButton(label,x,y,w,h,action));
 }
 public EditBox search(String value,java.util.function.Consumer<String> responder){
  EditBox box=new EditBox(font,158,137,234,20,Component.literal("Search abilities"));
  box.setMaxLength(64);box.setValue(value);box.setResponder(responder);
  searchBox=box;return addRenderableWidget(box);
 }
 public void rebuildSearch(){
  int cursor=searchBox==null?0:searchBox.getCursorPosition();
  rebuild();
  if(searchBox!=null){setFocused(searchBox);searchBox.setCursorPosition(cursor);}
 }
 @Override public void tick(){
  super.tick();
  if(currentView() instanceof AbilitiesView abilities)abilities.tick(this);
 }
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
  currentView().render(this,g,localX,localY);
  renderEquipPage(g);
  g.pose().popPose();
  currentView().renderUnscaled(this,g,mx,my);
  g.pose().pushPose();
  g.pose().translate(originX,originY,0);
  g.pose().scale(scale,scale,1);
  super.render(g,localX,localY,delta);
  g.pose().popPose();
 }
 private void renderEquipPage(GuiGraphics g){
  var state=LoadoutClientState.get();
  panel(g,648,82,184,418,"Equip Page");
  text(g,"Page "+(state.page()+1)+" / 10",695,149,94);
  g.fill(660+state.page()*16,373,674+state.page()*16,375,0xFFD2A5FF);
  wrapped(g,selectedAbility<0?"Select an unlocked ability, then click a slot.":"Click a slot to assign the selected ability.",662,445,154);
  text(g,"MP "+state.mana(),662,476,154);
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
  if(dy!=0&&hasAltDown()){changePage(dy<0?1:-1);return true;}
  return currentView().scroll(this,localX(x),localY(y),dy)
   ||super.mouseScrolled(localX(x),localY(y),dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

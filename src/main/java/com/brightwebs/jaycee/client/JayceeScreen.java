package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Sapphire pages; race details are accessed from Status, not a separate tab. */
public class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Quests"};
 private static final String[] CATS={"All","Offense","Defense","Support","Movement","Reality"};
 private static final String[] TRACKS={"Body","Spirit","Divine","Utility"};
 private static final int INK=0xFFB6CCDD, BLUE=0xFF91C4F4, MUTED=0xFF788DAB;
 private final Map<Button,String> hints=new LinkedHashMap<>();
 private final Button[] slots=new Button[PlayerState.SLOTS], pages=new Button[PlayerState.PAGES];
 private SapphireLayout layout;
 private int tab,selected,category,skill,skillTier;
 private int art=-1;
 private Button portraitButton;
 private Button selectedArtButton,selectedCategoryButton,selectedSkillButton;
 private boolean speciesOpen;

 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 private PlayerState state(){return PlayerState.get(Minecraft.getInstance().player);}
 private Button button(String label,int x,int y,int w,int h,int style,Runnable action,String hint){
  var widget=addRenderableWidget(new Button(x,y,w,h,Component.literal(label),b->action.run(),
   narration->narration.get().copy().append(hint==null?"":". "+hint)){
   @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
    boolean hover=isHoveredOrFocused()||this==slots[selected]||this==selectedArtButton||
     this==selectedCategoryButton||this==selectedSkillButton;
    if(style==1){
     if(hover)DivineFrame.sapphire(g,getX(),getY(),width,height,true);
    }else DivineFrame.sapphire(g,getX(),getY(),width,height,hover);
    if(style==0)center(g,getMessage().getString(),getX()+width/2,getY()+(height-6)/2,active?INK:MUTED);
   }
  });
  if(hint!=null)hints.put(widget,hint);
  return widget;
 }
 @Override protected void init(){
  layout=new SapphireLayout(width,height);
  hints.clear();
  selectedArtButton=null;selectedCategoryButton=null;selectedSkillButton=null;
  for(int i=0;i<TABS.length;i++){
   final int t=i;
   button(TABS[i],10+i*103,7,99,49,1,()->{tab=t;speciesOpen=false;rebuild();},TABS[i]);
  }
  button("<",554,118,21,20,0,()->GameplayActions.pageDelta(-1),"Previous saved loadout page");
  button(">",665,118,21,20,0,()->GameplayActions.pageDelta(1),"Next saved loadout page");
  for(int i=0;i<PlayerState.SLOTS;i++){
   final int s=i;
   var b=SapphireLayout.slot(i);
   slots[i]=button("Slot "+(i+1),b.x(),b.y(),b.width(),b.height(),2,()->{
    selected=s;
    if(art>=0)GameplayActions.equip(state().page(),s,AbilityCatalog.ALL.get(art).id());
   },null);
   button("Remove "+(i+1),554+(i%2)*69,321+(i/2)*24,65,20,0,
    ()->GameplayActions.equip(state().page(),s,""),"Clear slot "+(i+1)+" on the current page");
  }
  for(int i=0;i<PlayerState.PAGES;i++){
   final int p=i;
   pages[i]=button(""+(i+1),554+i*13,294,12,17,0,()->GameplayActions.page(p),"Select saved page "+(i+1));
  }
  portraitButton=null;
  if(tab==0){
   portraitButton=button("Face",72,153,24,14,0,()->GameplayActions.portrait(!state().portrait3d()),
    "Switch Face / 3D portrait; shared with the gameplay HUD");
   button("Race: Human",106,117,151,18,0,()->{speciesOpen=!speciesOpen;rebuild();},
    "Open species and evolution details. Human is a preview, not saved progression.");
   if(speciesOpen)button("Back to Status",160,370,195,24,0,()->{speciesOpen=false;rebuild();},null);
  }else if(tab==1||tab==3){
   for(int i=0;i<CATS.length;i++){
    final int c=i;
    var b=button(CATS[i],18,105+i*38,98,28,0,()->{category=c;rebuild();},"Category preview; filtering and progression are unavailable");
    if(i==category)selectedCategoryButton=b;
   }
   for(int i=0;i<AbilityCatalog.ALL.size();i++){
    final int a=i;
    var ability=AbilityCatalog.ALL.get(i);
    var b=button(ability.name()+(ability.implemented()?"":" [Preview]"),128,105+i*43,385,34,0,
     ()->{art=a;rebuild();},"Select "+ability.name()+", then click an equipment slot. "+
      (ability.implemented()?"Requires Sapphire unlock.":"Preview only; cannot activate."));
    if(i==art)selectedArtButton=b;
   }
  }else if(tab==2){
   for(int i=0;i<TRACKS.length;i++){
    final int branch=i;
    button(TRACKS[i],32+i*122,108,110,26,0,()->{skill=branch;rebuild();},"Skill tree preview only");
    for(int j=0;j<4;j++){
     final int tier=j;
     var b=button("Tier "+(j+1),52+i*122,156+j*49,70,29,0,()->{skill=branch;skillTier=tier;rebuild();},"Preview; no mastery awarded");
     if(i==skill&&j==skillTier)selectedSkillButton=b;
    }
   }
  }
 }
 private void rebuild(){clearWidgets();init();}
 private void text(GuiGraphics g,String s,int x,int y,int color){
  text(g,s,x,y,color,.8f);
 }
 private void text(GuiGraphics g,String s,int x,int y,int color,float size){
  g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);
  g.drawString(font,s,0,0,color,false);g.pose().popPose();
 }
 private void right(GuiGraphics g,String s,int x,int y,int color){text(g,s,x-(int)Math.ceil(font.width(s)*.8),y,color);}
 private void center(GuiGraphics g,String s,int x,int y,int color){text(g,s,x-(int)(font.width(s)*.4),y,color);}
 private void panel(GuiGraphics g,int x,int y,int w,int h,String title){
  DivineFrame.sapphire(g,x,y,w,h,false);
  text(g,title,x+16,y+12,INK);
  DivineFrame.divider(g,x+11,y+24,w-22);
 }
 // Screen.render invokes this after our panels; draw the background only before them.
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float dt){}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  super.renderBackground(g,mx,my,dt);
  if(Minecraft.getInstance().player==null)return;
  int lx=(int)Math.floor(layout.localX(mx)),ly=(int)Math.floor(layout.localY(my));
  var snapshot=state();
  updateNarration(snapshot);
  g.pose().pushPose();
  g.pose().translate(layout.left,layout.top,0);
  g.pose().scale((float)layout.scale,(float)layout.scale,1);
  DivineFrame.sapphire(g,0,0,536,62,false);
  DivineFrame.sapphire(g,0,68,536,352,false);
  for(int i=0;i<TABS.length;i++){
   if(i==tab)DivineFrame.sapphire(g,10+i*103,7,99,49,true);
   if(i>0)g.fill(8+i*103,11,9+i*103,50,0xFF35485E);
  }
  panel(g,544,68,156,352,"Equip Page");
  center(g,"Page "+(snapshot.page()+1)+" / 10",621,124,INK);
  if(tab==0)status(g,snapshot,dt);
  else if(tab==1||tab==3)abilities(g);
  else if(tab==2)skills(g);
  else quests(g);
  super.render(g,lx,ly,dt);
  for(int i=0;i<TABS.length;i++){
   DivineFrame.tabIcon(g,60+i*103,23,i,i==tab);
   center(g,TABS[i],60+i*103,43,INK);
  }
  if(tab==1||tab==3)for(int i=0;i<AbilityCatalog.ALL.size();i++)
   AbilityIcons.render(g,AbilityCatalog.ALL.get(i),134,112+i*43,19,i==art);
  equipment(g,snapshot);
  g.pose().popPose();
  // Entity rendering uses screen-space scissoring and lighting, not the scaled UI pose.
  if(tab==0&&snapshot.portrait3d()){
   InventoryScreen.renderEntityInInventoryFollowsMouse(g,layout.screenX(24),layout.screenY(96),
    layout.screenX(91),layout.screenY(154),layout.size(29),0f,mx,my,Minecraft.getInstance().player);
  }
  tooltips(g,lx,ly,mx,my,snapshot);
 }
 private void updateNarration(PlayerState snapshot){
  if(portraitButton!=null){
   portraitButton.setMessage(Component.literal(snapshot.portrait3d()?"Face":"3D"));
   hints.put(portraitButton,"Showing "+(snapshot.portrait3d()?"3D":"Face")+" portrait. Switch to "+
    (snapshot.portrait3d()?"Face":"3D")+"; shared with the gameplay HUD.");
  }
  for(int i=0;i<slots.length;i++){
   var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
   slots[i].setMessage(Component.literal("Slot "+(i+1)+": "+(equipped==null?"Empty":equipped.name()+
    (equipped.implemented()?", "+equipped.modes().get(snapshot.mode(snapshot.page(),i)):", preview only"))+
    (art>=0?". Equip selected "+AbilityCatalog.ALL.get(art).name():". Select an ability first")));
  }
  for(int i=0;i<pages.length;i++)pages[i].setMessage(Component.literal(""+(i+1)));
 }
 private void equipment(GuiGraphics g,PlayerState snapshot){
  for(int i=0;i<PlayerState.SLOTS;i++){
   var b=SapphireLayout.slot(i);
   var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
   if(i==selected)DivineFrame.sapphire(g,b.x(),b.y(),b.width(),b.height(),true);
   text(g,""+(i+1),b.x()+5,b.y()+6,0xFFBC97DC);
   if(equipped!=null){
    AbilityIcons.render(g,equipped,b.x()+16,b.y()+9,28,i==selected);
    g.enableScissor(b.x()+4,b.y()+39,b.x()+b.width()-4,b.y()+51);
    center(g,equipped.name(),b.x()+b.width()/2,b.y()+43,INK);
    g.disableScissor();
    if(!equipped.implemented())center(g,"Preview",b.x()+b.width()/2,b.y()+52,MUTED);
   }else center(g,"Empty",b.x()+b.width()/2,b.y()+43,MUTED);
  }
  int px=554+snapshot.page()*13;
  g.fill(px,312,px+12,314,0xFFB08BDD);
  text(g,"Select an unlocked ability,",555,373,INK);
  text(g,"then click a slot.",555,382,INK);
  text(g,"MP "+number(snapshot.mana()),555,400,INK);
 }
 private void status(GuiGraphics g,PlayerState snapshot,float dt){
  var player=Minecraft.getInstance().player;
  DivineFrame.sapphire(g,20,88,78,78,false);
  DivineFrame.gem(g,59,92,3,DivineFrame.GOLD);
  DivineFrame.gem(g,59,161,3,DivineFrame.GOLD);
  if(!snapshot.portrait3d())PlayerPortrait.render(g,player,32,101,52,false);
  text(g,font.plainSubstrByWidth(player.getName().getString(),140),110,94,INK,1.15f);
  text(g,"Title: None",110,139,INK);
  text(g,"Faction: None",110,155,INK);
  if(speciesOpen){species(g);return;}
  text(g,"Lv. "+player.experienceLevel,22,184,BLUE,1.6f);
  g.fill(23,213,214,223,0xFF030F1B);
  g.fill(24,214,24+(int)(188*Math.clamp(player.experienceProgress,0,1)),222,0xFF6CA6CF);
  right(g,(int)(player.experienceProgress*player.getXpNeededForNextLevel())+" / "+
   player.getXpNeededForNextLevel()+" EXP",212,230,INK);
  panel(g,18,250,208,152,"Stats");
  String[] labels={"HP","MP","SP","Attack","Defense","Speed","Luck"};
  String[] values={number(player.getHealth())+" / "+number(player.getMaxHealth()),
   number(snapshot.mana())+" / "+number(snapshot.maxMana()),number(snapshot.soul())+" / "+number(snapshot.maxSoul()),
   number(player.getAttributeValue(Attributes.ATTACK_DAMAGE)),number(player.getArmorValue()),
   String.format(Locale.ROOT,"%.2f",player.getAttributeValue(Attributes.MOVEMENT_SPEED)),
   number(player.getAttributeValue(Attributes.LUCK))};
  int[] colors={0xFFE96389,0xFF95D4B2,0xFFAA9DDD,0xFFB6C8DF,0xFFB6C8DF,0xFF98D8EB,0xFF82D5B5};
  for(int i=0;i<labels.length;i++){
   int y=285+i*16;
   DivineFrame.gem(g,32,y+3,2,colors[i]);
   text(g,labels[i],43,y,INK);right(g,values[i],213,y,BLUE);
   if(i<6)g.fill(27,y+12,216,y+13,0xFF293C50);
  }
  DivineFrame.orbit(g,300,194,Minecraft.getInstance().level.getGameTime()+dt);
  panel(g,382,85,134,50,"Titles");
  text(g,"- No Title Equipped -",393,117,INK);
  panel(g,382,142,134,88,"Racial Trait");
  text(g,"None",416,181,INK);
  DivineFrame.tabIcon(g,399,183,1,false);
  text(g,"No racial traits unlocked.",392,207,INK);
  panel(g,382,238,134,164,"Elemental Affinity");
  String[] elements={"Fire","Water","Wind","Earth","Light","Dark"};
  int[] colors2={0xFFEF6478,0xFF56B6FA,0xFF8CD8AC,0xFFEAB963,0xFFF9DF8C,0xFFAD7CF0};
  for(int i=0;i<elements.length;i++){
   int y=272+i*21;
   DivineFrame.gem(g,396,y+3,3,colors2[i]);
   text(g,elements[i],405,y,INK);right(g,"N/A",505,y,MUTED);
   if(i<5)g.fill(392,y+14,506,y+15,0xFF293C50);
  }
  text(g,"Traits / evolution: preview",242,365,MUTED);
  text(g,"Stats and EXP: live",242,377,MUTED);
 }
 private void abilities(GuiGraphics g){
  text(g,tab==1?"Abilities":"Magic",20,84,INK);
  text(g,"Select an art, then an equipment slot. Preview arts cannot activate.",20,389,MUTED);
 }
 private void skills(GuiGraphics g){
  text(g,"Skills - preview only",20,84,INK);
  for(int i=0;i<4;i++)g.fill(86+i*122,139,87+i*122,330,0xFF354F6A);
  text(g,TRACKS[skill]+" / Tier "+(skillTier+1)+" - mastery progression unavailable",25,378,MUTED);
 }
 private void species(GuiGraphics g){
  panel(g,20,178,496,179,"Species / Evolution");
  text(g,"Human  >  Demigod  >  Divine",42,227,INK);
  text(g,"Design preview only; no live race or evolution data is available.",42,255,MUTED);
  text(g,"Jaycee's 3-billion EP requirement is not implemented progression.",42,275,MUTED);
  text(g,"EP is separate from HP, SP and mana.",42,295,MUTED);
 }
 private void quests(GuiGraphics g){
  text(g,"Quests - preview only",20,84,INK);
  String[] names={"Awakening","Essence of Existence","The First Ascension","A New Reality"};
  for(int i=0;i<names.length;i++)panel(g,20,110+i*62,496,52,names[i]);
  text(g,"Quest tracking and rewards are not implemented.",26,384,MUTED);
 }
 private static String number(double value){return String.format(Locale.ROOT,"%.0f",value);}
 private void tooltips(GuiGraphics g,int x,int y,int mx,int my,PlayerState snapshot){
  // Pointer targets take precedence over retained keyboard focus.
  for(int pass=0;pass<2;pass++){
   for(int i=0;i<slots.length;i++)if(pass==0?SapphireLayout.slot(i).contains(x,y):slots[i].isFocused()){
    var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
    if(equipped!=null){
     int mode=snapshot.mode(snapshot.page(),i);
     long ticks=snapshot.cooldownRemaining(equipped.id(),Minecraft.getInstance().level.getGameTime());
     tooltip(g,AbilityIcons.tooltip(Component.literal(equipped.name()),
      Component.literal(equipped.implemented()?(mode==1?"Fortify: +30 spiritual barrier":"Restore: +30 SP; requires missing SP"):"Preview only - no gameplay effect"),
      Component.literal("Mana: "+equipped.manaCost()+" | Right-click: remove"),
      Component.literal(equipped.modes().get(mode)+" | Alt-click: switch mode"),ticks),mx,my);
    }else tooltip(g,List.of(Component.literal("Empty slot "+(i+1)+" - select an ability, then click here")),mx,my);
    return;
   }
   for(var entry:hints.entrySet()){
    var b=entry.getKey();
    if(pass==0?(x>=b.getX()&&x<b.getX()+b.getWidth()&&y>=b.getY()&&y<b.getY()+b.getHeight()):b.isFocused()){
     tooltip(g,List.of(Component.literal(entry.getValue())),mx,my);return;
    }
   }
   if(pass==0&&tab==0&&!speciesOpen&&x>=382&&x<516&&y>=85&&y<402){
    tooltip(g,List.of(Component.literal("Titles, race and affinities: unavailable / preview; not live progression")),mx,my);return;
   }
  }
  private void tooltip(GuiGraphics g,List<Component> lines,int mx,int my){
   int wrap=Math.max(1,Math.min(260,width-16));
   g.renderTooltip(font,lines.stream().flatMap(line->font.split(line,wrap).stream()).toList(),mx,my);
  }
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  double x=layout.localX(mx),y=layout.localY(my);
  for(int i=0;i<slots.length;i++)if(SapphireLayout.slot(i).contains(x,y)){
   if(button==1){GameplayActions.equip(state().page(),i,"");return true;}
   if(button==0&&Screen.hasAltDown()){GameplayActions.cycleMode(state().page(),i);return true;}
  }
  return super.mouseClicked(x,y,button);
 }
 @Override public boolean mouseReleased(double mx,double my,int button){return super.mouseReleased(layout.localX(mx),layout.localY(my),button);}
 @Override public void mouseMoved(double mx,double my){super.mouseMoved(layout.localX(mx),layout.localY(my));}
 @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
  return super.mouseDragged(layout.localX(mx),layout.localY(my),button,dx/layout.scale,dy/layout.scale);
 }
 @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
  if(dy!=0&&Screen.hasAltDown()){GameplayActions.pageDelta(dy<0?1:-1);return true;}
  return super.mouseScrolled(layout.localX(mx),layout.localY(my),dx,dy);
 }
 @Override public boolean isPauseScreen(){return false;}
}

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
 private final AbilityBrowser abilityBrowser=new AbilityBrowser(index->art=index);

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
    if(style==0){
     int padding=height>28?24:width<16?3:width<=32?4:8;
     int iconSpace=(tab==1||tab==3)&&getX()==128?18:0;
     UiText.line(g,getMessage().getString(),getX()+padding+iconSpace,
      getY()+(height-(int)Math.ceil(font.lineHeight*.8))/2,
      width-padding*2-iconSpace,.8f,active?INK:MUTED,true);
    }
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
   var b=SapphireLayout.pageSelector(i);
   pages[i]=button(""+(i+1),b.x(),b.y(),b.width(),b.height(),0,()->GameplayActions.page(p),"Select saved page "+(i+1));
  }
  portraitButton=null;
  if(tab==0){
   portraitButton=button("Face",68,153,28,14,0,()->GameplayActions.portrait(!state().portrait3d()),
    "Switch Face / 3D portrait; shared with the gameplay HUD");
   button("Race: Human",106,117,151,18,0,()->{speciesOpen=!speciesOpen;rebuild();},
    "Open species and evolution details. Human is a preview, not saved progression.");
   if(speciesOpen)button("Back to Status",160,370,195,24,0,()->{speciesOpen=false;rebuild();},null);
  }else if(tab==1){
   abilityBrowser.mount(widget->addRenderableWidget(widget));
  }else if(tab==3){
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
     var b=button("Tier "+(j+1),44+i*122,156+j*49,86,29,0,()->{skill=branch;skillTier=tier;rebuild();},"Preview; no mastery awarded");
     if(i==skill&&j==skillTier)selectedSkillButton=b;
    }
   }
  }
 }
 private void rebuild(){clearWidgets();init();}
 private void right(GuiGraphics g,String s,int x,int y,int width,int color){
  String fitted=UiText.fit(s,width,.8f);
  label(g,fitted,x-(int)Math.ceil(font.width(fitted)*.8),y,width,color);
 }
 private void label(GuiGraphics g,String s,int x,int y,int width,int color){
  UiText.line(g,s,x,y,width,.8f,color,false);
 }
 private void label(GuiGraphics g,String s,SapphireLayout.Bounds b,int color){
  label(g,s,b.x(),b.y(),b.width(),color);
 }
 private void paragraph(GuiGraphics g,String s,int x,int y,int width,int height,int color){
  UiText.paragraph(g,s,x,y,width,height,.8f,color);
 }
 private void panel(GuiGraphics g,int x,int y,int w,int h,String title){
  DivineFrame.sapphire(g,x,y,w,h,false);
  label(g,title,x+24,y+12,w-48,INK);
  DivineFrame.divider(g,x+11,y+26,w-22);
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
  UiText.line(g,"Page "+(snapshot.page()+1)+" / 10",579,124,82,.8f,INK,true);
  if(tab==0)status(g,snapshot,dt);
  else if(tab==1)abilityBrowser.render(g,snapshot,selected);
  else if(tab==3)abilities(g);
  else if(tab==2)skills(g);
  else quests(g);
  super.render(g,lx,ly,dt);
  for(int i=0;i<TABS.length;i++){
   DivineFrame.tabIcon(g,60+i*103,23,i,i==tab);
   var b=SapphireLayout.tabLabel(i);
   UiText.line(g,TABS[i],b.x(),b.y(),b.width(),1f,INK,true);
  }
  if(tab==3)for(int i=0;i<AbilityCatalog.ALL.size();i++)
   AbilityIcons.render(g,AbilityCatalog.ALL.get(i),152,115+i*43,14,i==art);
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
    (art>=0?". Equip selected "+AbilityCatalog.ALL.get(art).name():". Select an ability first")+
    ". Key: "+JayceeClient.ABILITY_KEYS[i].getTranslatedKeyMessage().getString()));
  }
  for(int i=0;i<pages.length;i++)pages[i].setMessage(Component.literal(""+(i+1)));
 }
 private void equipment(GuiGraphics g,PlayerState snapshot){
  for(int i=0;i<PlayerState.SLOTS;i++){
   var b=SapphireLayout.slot(i);
   var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
   if(i==selected)DivineFrame.sapphire(g,b.x(),b.y(),b.width(),b.height(),true);
   var number=SapphireLayout.slotNumber(i);
   UiText.line(g,""+(i+1),number.x(),number.y(),number.width(),1f,0xFFBC97DC,false);
   label(g,equipped==null?"Empty":equipped.implemented()?"Equipped":"Preview",SapphireLayout.slotState(i),
    equipped==null?MUTED:INK);
   var caption=SapphireLayout.slotCaption(i);
   if(equipped!=null){
    AbilityIcons.render(g,equipped,b.x()+18,b.y()+10,24,i==selected);
    UiText.line(g,equipped.name(),caption.x(),caption.y(),caption.width(),.8f,INK,true);
   }else UiText.line(g,"Empty",caption.x(),caption.y(),caption.width(),.8f,MUTED,true);
  }
  var page=SapphireLayout.pageSelector(snapshot.page());
  g.fill(page.x(),312,page.x()+page.width(),314,0xFFB08BDD);
  paragraph(g,"Select an unlocked ability, then click a slot.",559,371,126,24,INK);
  label(g,"MP "+number(snapshot.mana()),568,399,108,INK);
 }
 private void status(GuiGraphics g,PlayerState snapshot,float dt){
  var player=Minecraft.getInstance().player;
  DivineFrame.sapphire(g,20,88,78,78,false);
  DivineFrame.gem(g,59,92,3,DivineFrame.GOLD);
  DivineFrame.gem(g,59,161,3,DivineFrame.GOLD);
  if(!snapshot.portrait3d())PlayerPortrait.render(g,player,32,101,52,false);
  UiText.line(g,player.getName().getString(),110,94,150,1.15f,INK,false);
  label(g,"Title: None",110,140,150,INK);
  label(g,"Faction: None",110,156,150,INK);
  if(speciesOpen){species(g);return;}
  UiText.line(g,"Lv. "+player.experienceLevel,24,184,190,1.6f,BLUE,false);
  g.fill(23,213,214,223,0xFF030F1B);
  g.fill(24,214,24+(int)(188*Math.clamp(player.experienceProgress,0,1)),222,0xFF6CA6CF);
  right(g,(int)(player.experienceProgress*player.getXpNeededForNextLevel())+" / "+
   player.getXpNeededForNextLevel()+" EXP",212,230,188,INK);
  panel(g,18,250,208,152,"Stats");
  String[] labels={"HP","MP","SP","Attack","Defense","Speed","Luck"};
  String[] values={number(player.getHealth())+" / "+number(player.getMaxHealth()),
   number(snapshot.mana())+" / "+number(snapshot.maxMana()),number(snapshot.soul())+" / "+number(snapshot.maxSoul()),
   number(player.getAttributeValue(Attributes.ATTACK_DAMAGE)),number(player.getArmorValue()),
   String.format(Locale.ROOT,"%.2f",player.getAttributeValue(Attributes.MOVEMENT_SPEED)),
   number(player.getAttributeValue(Attributes.LUCK))};
  int[] colors={0xFFE96389,0xFF95D4B2,0xFFAA9DDD,0xFFB6C8DF,0xFFB6C8DF,0xFF98D8EB,0xFF82D5B5};
  for(int i=0;i<labels.length;i++){
   int y=282+i*15;
   DivineFrame.gem(g,32,y+3,2,colors[i]);
   label(g,labels[i],43,y,52,INK);right(g,values[i],212,y,106,BLUE);
   if(i<6)g.fill(27,y+12,216,y+13,0xFF293C50);
  }
  DivineFrame.orbit(g,300,194,Minecraft.getInstance().level.getGameTime()+dt);
  panel(g,382,85,134,50,"Titles");
  UiText.line(g,"- No Title Equipped -",406,119,86,.8f,INK,true);
  panel(g,382,142,134,88,"Racial Trait");
  label(g,"None",416,181,76,INK);
  DivineFrame.tabIcon(g,399,183,1,false);
  paragraph(g,"No racial traits unlocked.",406,198,86,22,INK);
  panel(g,382,238,134,164,"Elemental Affinity");
  String[] elements={"Fire","Water","Wind","Earth","Light","Dark"};
  int[] colors2={0xFFEF6478,0xFF56B6FA,0xFF8CD8AC,0xFFEAB963,0xFFF9DF8C,0xFFAD7CF0};
  for(int i=0;i<elements.length;i++){
   int y=272+i*21;
   DivineFrame.gem(g,396,y+3,3,colors2[i]);
   label(g,elements[i],405,y,60,INK);right(g,"N/A",499,y,28,MUTED);
   if(i<5)g.fill(392,y+14,506,y+15,0xFF293C50);
  }
  paragraph(g,"Traits / evolution: preview",242,360,126,22,MUTED);
  label(g,"Stats and EXP: live",242,387,126,MUTED);
 }
 private void abilities(GuiGraphics g){
  label(g,tab==1?"Abilities":"Magic",26,86,484,INK);
  paragraph(g,"Select an art, then an equipment slot. Preview arts cannot activate.",36,376,464,24,MUTED);
 }
 private void skills(GuiGraphics g){
  label(g,"Skills - preview only",26,86,484,INK);
  for(int i=0;i<4;i++)g.fill(86+i*122,139,87+i*122,330,0xFF354F6A);
  paragraph(g,TRACKS[skill]+" / Tier "+(skillTier+1)+" - mastery progression unavailable",36,376,464,24,MUTED);
 }
 private void species(GuiGraphics g){
  panel(g,20,178,496,179,"Species / Evolution");
  label(g,"Human  >  Demigod  >  Divine",44,221,448,INK);
  paragraph(g,"Design preview only; no live race or evolution data is available.",44,246,448,24,MUTED);
  paragraph(g,"Jaycee's 3-billion EP requirement is not implemented progression.",44,278,448,24,MUTED);
  paragraph(g,"EP is separate from HP, SP and mana.",44,310,448,24,MUTED);
 }
 private void quests(GuiGraphics g){
  label(g,"Quests - preview only",26,86,484,INK);
  String[] names={"Awakening","Essence of Existence","The First Ascension","A New Reality"};
  for(int i=0;i<names.length;i++)panel(g,20,110+i*62,496,52,names[i]);
  paragraph(g,"Quest tracking and rewards are not implemented.",36,376,464,24,MUTED);
 }
 private static String number(double value){return String.format(Locale.ROOT,"%.0f",value);}
 private void tooltips(GuiGraphics g,int x,int y,int mx,int my,PlayerState snapshot){
  if(tab==1){
   var lines=abilityBrowser.tooltip(x,y);
   if(!lines.isEmpty()){tooltip(g,lines,mx,my);return;}
  }
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
      Component.literal(equipped.modes().get(mode)+" | Alt-click: switch mode | Key: "+
       JayceeClient.ABILITY_KEYS[i].getTranslatedKeyMessage().getString()),ticks),mx,my);
    }else tooltip(g,List.of(Component.literal("Empty slot "+(i+1)+" - select an ability, then click here"),
     Component.literal("Key: "+JayceeClient.ABILITY_KEYS[i].getTranslatedKeyMessage().getString())),mx,my);
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
 }
 private void tooltip(GuiGraphics g,List<Component> lines,int mx,int my){
  UiText.tooltip(g,lines,mx,my);
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
  if(tab==1&&abilityBrowser.scroll(layout.localX(mx),layout.localY(my),dy))return true;
  return super.mouseScrolled(layout.localX(mx),layout.localY(my),dx,dy);
 }
 @Override public boolean keyPressed(int key,int scan,int modifiers){
  if(tab==1&&getFocused() instanceof net.minecraft.client.gui.components.AbstractWidget widget
   &&abilityBrowser.key(key,widget))return true;
  return super.keyPressed(key,scan,modifiers);
 }
 @Override public boolean isPauseScreen(){return false;}
}

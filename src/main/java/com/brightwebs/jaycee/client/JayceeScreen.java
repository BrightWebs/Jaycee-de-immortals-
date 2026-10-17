package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.brightwebs.jaycee.gameplay.StatusData;
import com.brightwebs.jaycee.config.HudConfig;
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
 private static final String[] ASCENSION_TABS={"Status","Abilities","Skills","Magic","Divine Bonds","Quests"};
 private static final String[] CATS={"All","Offense","Defense","Support","Movement","Reality"};
 private static final int INK=0xFFB6CCDD, BLUE=0xFF91C4F4, MUTED=0xFF788DAB;
 private final Map<Button,String> hints=new LinkedHashMap<>();
 private final Map<Button,String> statusMetrics=new LinkedHashMap<>();
 private final Button[] slots=new Button[PlayerState.SLOTS], pages=new Button[PlayerState.PAGES];
 private SapphireLayout layout;
 private int tab,selected,category;
 private final SkillMagicBrowser skillsBrowser=new SkillMagicBrowser(false,()->selected),magicBrowser=new SkillMagicBrowser(true,()->selected);
 private int art=0;
 private int artScroll;
 private Button portraitButton;
 private Button traitsFocus,affinitiesFocus;
 private int traitScroll,affinityScroll,traitHeight,affinityHeight;
 private Button selectedArtButton,selectedCategoryButton,selectedSkillButton;
 private boolean speciesOpen;
 private final boolean ordinary;
 private final SkillMagicBrowser abilityBrowser=new SkillMagicBrowser(true,true,()->selected);

 public JayceeScreen(){this(false);}
 JayceeScreen(boolean ordinary){super(Component.literal(ordinary?"Divine Ascension":"Jaycee : Di Immortales"));this.ordinary=ordinary;}
 private String[] tabs(){return ordinary?ASCENSION_TABS:TABS;}
 private int tabStep(){return ordinary?86:103;}
 private int tabIcon(int index){return ordinary&&index==5?4:Math.min(index,4);}
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
  statusMetrics.clear();
  selectedArtButton=null;selectedCategoryButton=null;selectedSkillButton=null;
  for(int i=0;i<tabs().length;i++){
   final int t=i;
   var bounds=SapphireLayout.tabButton(i,ordinary);
   button(tabs()[i],bounds.x(),bounds.y(),bounds.width(),bounds.height(),1,()->{
    if(ordinary&&t==4){Minecraft.getInstance().setScreen(new DivineBondsScreen(false));return;}
    tab=t;speciesOpen=false;rebuild();
   },tabs()[i]);
  }
  button("<",554,118,21,20,0,()->GameplayActions.pageDelta(-1),"Previous saved loadout page");
  button(">",665,118,21,20,0,()->GameplayActions.pageDelta(1),"Next saved loadout page");
  for(int i=0;i<PlayerState.SLOTS;i++){
   final int s=i;
   var b=SapphireLayout.slot(i);
   slots[i]=button("Slot "+(i+1),b.x(),b.y(),b.width(),b.height(),2,()->{
    selected=s;
    if(tab==2||tab==3){
     String browserSelectedId=browser().equippableSelection();
     if(browserSelectedId!=null)GameplayActions.equip(state().page(),s,browserSelectedId);
    }else if(ordinary&&tab==1){
     String id=abilityBrowser.equippableSelection();
     if(id!=null)GameplayActions.equip(state().page(),s,id);
    }else if(art>=0&&AbilityCatalog.ALL.get(art).implemented())GameplayActions.equip(state().page(),s,AbilityCatalog.ALL.get(art).id());
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
  traitsFocus=null;affinitiesFocus=null;
  if(tab==0){
   portraitButton=button("Face",68,153,28,14,0,()->GameplayActions.portrait(!state().portrait3d()),
    "Switch Face / 3D portrait; shared with the gameplay HUD");
   button("Race: Human",106,117,151,18,0,()->{speciesOpen=!speciesOpen;rebuild();},
    "Open species and evolution details. Human is a preview, not saved progression.");
   if(speciesOpen)button("Back to Status",160,370,195,24,0,()->{speciesOpen=false;rebuild();},null);
   if(!speciesOpen){
    traitsFocus=statusFocus(390,174,118,46,"Trait");
    affinitiesFocus=statusFocus(390,270,118,120,"Elemental Affinity");
    metricFocus(24,180,96,23,"Level");
    var gradeBadge=MobBestowalUi.GRADE_BADGE;
    metricFocus(gradeBadge.x(),gradeBadge.y(),gradeBadge.width(),gradeBadge.height(),"Grade");
    metricFocus(106,105,154,12,"Rank");
    metricFocus(242,382,126,11,"Essence");

    metricFocus(43,297,169,12,"Mana");
   }
   if(Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&
      (hit.getEntity() instanceof net.minecraft.world.entity.player.Player||hit.getEntity() instanceof net.minecraft.world.entity.Mob)&&
      hit.getEntity()!=Minecraft.getInstance().player)
    button("Bestowal",242,314,126,22,0,()->Minecraft.getInstance().setScreen(new DivineBondsScreen(true)),
     "Inspect the targeted recipient through the server profile request. Opening spends nothing.");
  }else if(tab==1&&ordinary)abilityBrowser.init(widget->addRenderableWidget(widget));
  else if(tab==1){
   for(int i=0;i<CATS.length;i++){
    final int c=i;
    var b=button(CATS[i],18,105+i*38,98,28,0,()->{category=c;artScroll=0;var entries=visibleArts();if(!entries.contains(art))art=entries.isEmpty()?-1:entries.getFirst();rebuild();},"Filter abilities by category");
    if(i==category)selectedCategoryButton=b;
   }
   java.util.List<Integer> visible=visibleArts();
    artScroll=Math.clamp(artScroll,0,Math.max(0,visible.size()-5));
    for(int row=0;row<5&&row+artScroll<visible.size();row++){
     final int a=visible.get(row+artScroll);
     var ability=AbilityCatalog.ALL.get(a);
     var b=button(ability.name()+(ability.implemented()?"":" [Preview]"),137,229+row*30,365,26,0,
      ()->{art=a;rebuild();},"Show "+ability.name()+" details. "+(ability.implemented()?"Click an equipment slot to equip.":"Preview only."));
     if(a==art)selectedArtButton=b;
    }
  }else if(tab==2||tab==3)browser().init(widget->addRenderableWidget(widget));
 }
 private Button statusFocus(int x,int y,int w,int h,String name){
  return addRenderableWidget(new Button(x,y,w,h,Component.literal(name),b->{},narration->narration.get()){
   @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
    if(isFocused())g.fill(getX()-2,getY(),getX()-1,getY()+height,BLUE);
   }
  });
 }
 private void metricFocus(int x,int y,int w,int h,String metric){
  var focus=statusFocus(x,y,w,h,metric);
  statusMetrics.put(focus,metric);
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
  boolean showOrdinary=SapphireLayout.ordinaryMenu(snapshot.supreme(),snapshot.playerMenuPreview());
  if(ordinary!=showOrdinary){
   Minecraft.getInstance().setScreen(new JayceeScreen(showOrdinary));return;
  }
  updateNarration(snapshot);
  g.pose().pushPose();
  g.pose().translate(layout.left,layout.top,0);
  g.pose().scale((float)layout.scale,(float)layout.scale,1);
  DivineFrame.sapphire(g,0,0,536,62,false);
  DivineFrame.sapphire(g,0,68,536,352,false);
  for(int i=0;i<tabs().length;i++){
   if(i==tab)DivineFrame.sapphire(g,10+i*tabStep(),7,tabStep()-4,49,true);
   if(i>0)g.fill(8+i*tabStep(),11,9+i*tabStep(),50,0xFF35485E);
  }
  panel(g,544,68,156,352,"Equip Page");
  UiText.line(g,"Page "+(snapshot.page()+1)+" / 10",579,124,82,.8f,INK,true);
  if(tab==0)status(g,snapshot,dt);
  else if(tab==1){if(ordinary)abilityBrowser.render(g);else abilities(g);}
  else if(tab==2||tab==3)browser().render(g);
  else quests(g);
  super.render(g,lx,ly,dt);
  for(int i=0;i<tabs().length;i++){
   DivineFrame.tabIcon(g,ordinary?10+i*tabStep()+(tabStep()-4)/2:60+i*103,23,tabIcon(i),i==tab);
   var b=SapphireLayout.tabLabel(i,ordinary);
   UiText.line(g,tabs()[i],b.x(),b.y(),b.width(),ordinary?.8f:1f,INK,true);
  }
  if(tab==0&&ordinary){
   var theme=PatronTheme.palette(PatronTheme.retained(StatusData.patrons(snapshot)));
   g.fill(18,54,10+tabStep()-12,55,theme.accent());
   DivineFrame.gem(g,10+(tabStep()-4)/2,9,2,theme.secondary());
  }
  if(tab==1&&!ordinary){java.util.List<Integer> visible=visibleArts();
   for(int row=0;row<5&&row+artScroll<visible.size();row++){int index=visible.get(row+artScroll);
    AbilityIcons.render(g,AbilityCatalog.ALL.get(index),152,235+row*30,14,index==art);
   }
  }
  if(tab==1&&ordinary){abilityBrowser.renderRows(g);abilityBrowser.renderOutput(g);}
  if(tab==2||tab==3){browser().renderRows(g);browser().renderOutput(g);}
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
  for(var entry:statusMetrics.entrySet()){
   String metric=entry.getValue();
   String value=switch(metric){
    case "Level" -> snapshot.divinity().contains("DivineLevel")?snapshot.divinity().getInt("DivineLevel")+
     "; Essence progress "+snapshot.divinity().getLong("LevelProgress")+" / "+snapshot.divinity().getLong("LevelRequired"):"Unavailable";
    case "Grade" -> snapshot.divinity().getString("Grade");
    case "Rank" -> snapshot.divinity().getString("Rank").replace('_',' ');
    case "Essence" -> snapshot.divinity().getLong("Essence")+" reserve; "+snapshot.divinity().getLong("EarnedEssence")+" lifetime earned";

    default -> number(snapshot.mana())+" / "+number(snapshot.maxMana());
   };
   String hint=metric+": "+(value.isEmpty()?"Unavailable":value)+
    (metric.equals("Rank")&&snapshot.supreme()?"; Authority: Supreme Existence":"")+". "+MobBestowalUi.metricHint(metric);
   entry.getKey().setMessage(Component.literal(hint));
   hints.put(entry.getKey(),hint);
  }
  if(portraitButton!=null){
   portraitButton.setMessage(Component.literal(snapshot.portrait3d()?"Face":"3D"));
   hints.put(portraitButton,"Showing "+(snapshot.portrait3d()?"3D":"Face")+" portrait. Switch to "+
    (snapshot.portrait3d()?"Face":"3D")+"; shared with the gameplay HUD.");
  }
  String browserSelectedId=tab==2||tab==3?browser().equippableSelection():ordinary&&tab==1?abilityBrowser.equippableSelection():null;
  for(int i=0;i<slots.length;i++){
   var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
   slots[i].setMessage(Component.literal("Slot "+(i+1)+": "+(equipped==null?"Empty":equipped.name()+
    (equipped.implemented()?", "+equipped.modes().get(snapshot.mode(snapshot.page(),i)):", preview only"))+
    (tab==2||tab==3||ordinary&&tab==1?(browserSelectedId==null?". Select an unlocked equippable entry first":". Equip selected "+AbilityCatalog.find(browserSelectedId).name()):
     art>=0?". Equip selected "+AbilityCatalog.ALL.get(art).name():". Select an ability first")+
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
 }
 private void status(GuiGraphics g,PlayerState snapshot,float dt){
  var player=Minecraft.getInstance().player;
  var patrons=PatronTheme.retained(StatusData.patrons(snapshot));
  var theme=ordinary?PatronTheme.palette(patrons):PatronTheme.NEUTRAL;
  int accent=theme.accent();
  DivineFrame.sapphire(g,20,88,78,78,false);
  DivineFrame.gem(g,59,92,3,ordinary?accent:DivineFrame.GOLD);
  DivineFrame.gem(g,59,161,3,ordinary?accent:DivineFrame.GOLD);
  if(!snapshot.portrait3d())PlayerPortrait.render(g,player,32,101,52,false);
  UiText.line(g,player.getGameProfile().getName(),110,94,150,1.15f,INK,false);
  String rank=snapshot.divinity().getString("Rank").replace('_',' ');
  label(g,MobBestowalUi.standing(snapshot.supreme(),rank),110,108,150,accent);
  String title=StatusData.title(snapshot);
  label(g,"Title: "+(title.isEmpty()?"None":title),110,140,150,INK);
  String affiliation=patrons.isEmpty()?snapshot.divinity().getCompound("PatronProfile").getString("Username"):
   String.join(", ",patrons.stream().map(PatronTheme.Theme::name).toList());
  label(g,"Patron: "+(affiliation.isEmpty()?"None":affiliation),110,156,150,INK);
  if(speciesOpen){species(g);return;}
  var divine=snapshot.divinity();
  UiText.line(g,divine.contains("DivineLevel")?"Lv. "+divine.getInt("DivineLevel"):"Lv. —",24,184,96,1.6f,accent,false);
  var gradeBadge=MobBestowalUi.GRADE_BADGE;
  int gradeColor=MobBestowalUi.gradeColor(divine.getString("Grade"));
  DivineFrame.sapphire(g,gradeBadge.x(),gradeBadge.y(),gradeBadge.width(),gradeBadge.height(),false);
  DivineFrame.gem(g,gradeBadge.x()+9,gradeBadge.y()+9,3,gradeColor);
  DivineFrame.gem(g,gradeBadge.x()+9,gradeBadge.y()+9,2,DivineFrame.DARK);
  UiText.line(g,"Grade "+(divine.getString("Grade").isEmpty()?"—":divine.getString("Grade")),
   gradeBadge.x()+18,gradeBadge.y()+6,gradeBadge.width()-24,.8f,gradeColor,false);
  UiText.line(g,"ESSENCE PROGRESS",24,204,188,.65f,MUTED,false);
  g.fill(23,213,214,223,0xFF030F1B);
  g.fill(24,214,24+(int)(188*MobBestowalUi.progress(divine.getLong("LevelProgress"),divine.getLong("LevelRequired"))),222,accent);
  right(g,divine.getLong("LevelRequired")>0?divine.getLong("LevelProgress")+" / "+divine.getLong("LevelRequired"):
   "Progress unavailable",212,230,188,INK);
  panel(g,18,250,208,152,"Stats");
  statusAccent(g,18,250,208,accent);
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
   label(g,labels[i],43,y,52,INK);right(g,values[i],212,y,106,accent);
   if(i<6)g.fill(27,y+12,216,y+13,0xFF293C50);
  }
  boolean reduced=HudConfig.REDUCED_STATUS_MOTION.get()||Minecraft.getInstance().options.hideLightningFlash().get();
  double ticks=Minecraft.getInstance().level.getGameTime()+dt;
  if(ordinary)PatronEmblem.render(g,patrons,ticks,reduced);
  else DivineFrame.orbit(g,300,194,reduced?0:ticks);
  panel(g,382,85,134,50,"Titles");
  statusAccent(g,382,85,134,accent);
  UiText.line(g,title.isEmpty()?"No Title Equipped":title,396,119,106,.8f,INK,true);
  panel(g,382,142,134,88,"Trait");
  statusAccent(g,382,142,134,accent);
  var traits=StatusData.traits(snapshot);
  traitHeight=entries(g,traits,390,174,118,46,traitScroll,"No unlocked traits.",traitsFocus);
  traitScroll=Math.clamp(traitScroll,0,Math.max(0,traitHeight-46));
  panel(g,382,238,134,164,"Elemental Affinity");
  statusAccent(g,382,238,134,accent);
  var affinities=StatusData.affinities(snapshot);
  if(patrons.size()==1)affinities=affinities.stream().sorted(java.util.Comparator.comparingInt(e->
   PatronTheme.relevant(patrons.getFirst(),e.id(),e.name())?0:1)).toList();
  affinityHeight=entries(g,affinities,390,270,118,120,affinityScroll,"No unlocked affinities.",affinitiesFocus);
  affinityScroll=Math.clamp(affinityScroll,0,Math.max(0,affinityHeight-120));
  paragraph(g,snapshot.supreme()?"Authority: Supreme Existence"+(snapshot.playerMenuPreview()?". Player layout preview.":""):
   snapshot.playerMenuPreview()?"Player layout preview; your real permissions remain active.":
   ordinary?(patrons.isEmpty()?"Unblessed / no active deity lineage.":String.join(" / ",patrons.stream().map(PatronTheme.Theme::name).toList())):
   "Traits / evolution: see race",242,344,126,32,MUTED);
  label(g,"Essence: "+divine.getLong("Essence"),242,382,126,MUTED);

 }
 private void statusAccent(GuiGraphics g,int x,int y,int w,int color){
  if(!ordinary)return;
  g.fill(x+12,y+26,x+w-12,y+27,color);
  DivineFrame.gem(g,x+w/2,y+3,2,color);
 }
 private int entries(GuiGraphics g,List<StatusData.Entry> entries,int x,int y,int w,int h,int scroll,String empty,Button focus){
  if(focus!=null)focus.setMessage(Component.literal(entries.isEmpty()?empty:String.join(". ",entries.stream().map(e->
   e.name()+". "+e.description()+(e.level()<0?"":". Level "+e.level())+
   (e.required()>0?". Progress "+e.progress()+" / "+e.required():"")).toList())+". Scroll with mouse wheel or Page Up / Down."));
  g.enableScissor(x,y,x+w,y+h);
  int cursor=y-Math.max(0,scroll);
  if(entries.isEmpty())paragraph(g,empty,x+4,y+3,w-8,h-4,MUTED);
  for(var entry:entries){
   int top=cursor;
   var texture=net.minecraft.resources.ResourceLocation.tryParse(entry.icon());
   if(texture!=null&&Minecraft.getInstance().getResourceManager().getResource(texture).isPresent())
    g.blit(texture,x+2,cursor+1,0,0,14,14,14,14);
   else DivineFrame.gem(g,x+8,cursor+7,3,MUTED);
   label(g,entry.name(),x+20,cursor,w-24,INK);cursor+=11;
   if(entry.level()>=0){label(g,"Lv. "+entry.level(),x+20,cursor,w-24,BLUE);cursor+=11;}
   for(var line:font.split(Component.literal(entry.description()),(int)((w-8)/.8f))){
    g.pose().pushPose();g.pose().translate(x+4,cursor,0);g.pose().scale(.8f,.8f,1);
    g.drawString(font,line,0,0,INK,false);g.pose().popPose();cursor+=10;
   }
   if(entry.required()>0){
    label(g,entry.progress()+" / "+entry.required(),x+4,cursor,w-8,MUTED);cursor+=11;
    g.fill(x+4,cursor,x+w-4,cursor+2,0xFF293C50);
    g.fill(x+4,cursor,x+4+(int)((w-8)*Math.clamp(entry.progress()/(double)entry.required(),0,1)),cursor+2,BLUE);
    cursor+=5;
   }
   cursor=Math.max(cursor,top+18)+6;
  }
  g.disableScissor();
  int content=cursor-y+Math.max(0,scroll);
  if(content>h){
   int max=content-h,thumb=Math.max(6,h*h/content);
   int sy=y+(int)((h-thumb)*Math.min(scroll,max)/(double)max);
   g.fill(x+w-2,sy,x+w-1,sy+thumb,BLUE);
  }
  return content;
 }
 private java.util.List<Integer> visibleArts(){
  java.util.List<Integer> result=new java.util.ArrayList<>();
  for(int i=0;i<AbilityCatalog.ALL.size();i++){
   String id=AbilityCatalog.ALL.get(i).id();
   int type=id.contains("severance")||id.contains("pulse")?1:
    id.contains("guard")?2:id.contains("restore")?3:
    id.contains("step")||id.contains("flight")?4:5;
   if(category==0||category==type)result.add(i);
  }
  return result;
 }
 private void abilities(GuiGraphics g){
  label(g,"Abilities",26,86,484,INK);
  panel(g,125,91,391,124,"Selected Ability");
  if(art>=0&&art<AbilityCatalog.ALL.size()){
   var ability=AbilityCatalog.ALL.get(art);
   AbilityIcons.render(g,ability,149,131,28,true);
   label(g,ability.name(),188,127,290,BLUE);
   paragraph(g,ability.implemented()?
    "Select an equipped slot on the right to use this unlocked art.":
    "Preview only. This ability cannot be equipped or activated yet.",188,146,298,24,INK);
   label(g,"Mana: "+(ability.implemented()?number(ability.manaCost()):"N/A"),146,179,103,MUTED);
   label(g,"Cooldown: "+(ability.implemented()?number(ability.cooldownTicks()/20.0)+"s":"N/A"),253,179,126,MUTED);
   label(g,"Mode: "+String.join(", ",ability.modes()),380,179,120,MUTED);
  }
  label(g,"Available Arts (scroll to browse)",136,215,358,INK);
  paragraph(g,"Select an art to see details. Only usable arts can be equipped.",136,386,365,21,MUTED);
 }
 private SkillMagicBrowser browser(){return tab==3?magicBrowser:skillsBrowser;}
 private void species(GuiGraphics g){
  panel(g,20,178,496,179,"Species / Evolution");
  label(g,"Human  >  Demigod  >  Divine",44,221,448,INK);
  paragraph(g,"Design preview only; no live race or evolution data is available.",44,246,448,24,MUTED);
  paragraph(g,"Divine power progresses through earned Essence.",44,278,448,24,MUTED);
  paragraph(g,"Essence progression is separate from HP, SP and mana.",44,310,448,24,MUTED);
 }
 private void quests(GuiGraphics g){
  DivineFrame.sapphire(g,76,158,384,166,false);
  g.fill(89,171,447,172,0xFF91A9CD);
  g.fill(89,310,447,311,0xFF9E82C4);
  UiText.line(g,"QUEST SYSTEM",96,191,344,1.15f,INK,true);
  UiText.line(g,"WORK IN PROGRESS",96,229,344,1f,0xFFBC97DC,true);
  UiText.line(g,"The divine quest system is currently under development.",90,268,356,.8f,MUTED,true);
 }
 private static String number(double value){return String.format(Locale.ROOT,"%.0f",value);}
 private void tooltips(GuiGraphics g,int x,int y,int mx,int my,PlayerState snapshot){
  // Pointer targets take precedence over retained keyboard focus.
  for(int pass=0;pass<2;pass++){
   if((tab==2||tab==3||ordinary&&tab==1)&&pass==0){
    String hint=(tab==1?abilityBrowser:browser()).tooltip(x,y,false);
    if(hint!=null){tooltip(g,List.of(Component.literal(hint)),mx,my);return;}
   }
   for(int i=0;i<slots.length;i++)if(pass==0?SapphireLayout.slot(i).contains(x,y):slots[i].isFocused()){
    var equipped=AbilityCatalog.find(snapshot.ability(snapshot.page(),i));
    if(equipped!=null){
     int mode=snapshot.mode(snapshot.page(),i);
     long ticks=snapshot.cooldownRemaining(equipped.id(),Minecraft.getInstance().level.getGameTime());
     var intrinsic=com.brightwebs.jaycee.gameplay.SkillDefinitions.find(equipped.id());
     if(intrinsic!=null&&intrinsic.intrinsic()){
      tooltip(g,AbilityIcons.tooltip(Component.literal(equipped.name()),Component.literal(intrinsic.description()),
       Component.literal("Activation mana: "+intrinsic.activationMana(snapshot)+" | Upkeep: up to "+intrinsic.upkeepMana(snapshot)+"/s"),
       Component.literal(equipped.modes().get(mode)+" | Alt-click: switch mode | Right-click: remove"),ticks),mx,my);
      return;
     }
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
   if((tab==2||tab==3||ordinary&&tab==1)&&pass==1){
    String hint=(tab==1?abilityBrowser:browser()).tooltip(x,y,true);
    if(hint!=null){tooltip(g,List.of(Component.literal(hint)),mx,my);return;}
   }
   if(tab==0&&!speciesOpen){
    if(pass==0&&x>=106&&x<260&&y>=140&&y<168){
     var patrons=PatronTheme.retained(StatusData.patrons(snapshot));
     String affiliation=patrons.isEmpty()?snapshot.divinity().getCompound("PatronProfile").getString("Username"):
      String.join(", ",patrons.stream().map(PatronTheme.Theme::name).toList());
     tooltip(g,List.of(Component.literal("Title: "+(StatusData.title(snapshot).isEmpty()?"None":StatusData.title(snapshot))),
      Component.literal("Patron: "+(affiliation.isEmpty()?"None":affiliation))),mx,my);return;
    }
    if(pass==0&&x>=382&&x<516&&y>=85&&y<135){
     String title=StatusData.title(snapshot);
     tooltip(g,List.of(Component.literal(title.isEmpty()?"No Title Equipped. Title management is unfinished.":title)),mx,my);return;
    }
    if((pass==0&&x>=390&&x<508&&y>=174&&y<220)||(pass==1&&traitsFocus!=null&&traitsFocus.isFocused())){
     tooltip(g,List.of(Component.literal("Trait: scroll this card or focus and use Page Up / Down.")),mx,my);return;
    }
    if((pass==0&&x>=390&&x<508&&y>=270&&y<390)||(pass==1&&affinitiesFocus!=null&&affinitiesFocus.isFocused())){
     tooltip(g,List.of(Component.literal("Only unlocked affinities. Scroll this card or focus and use Page Up / Down.")),mx,my);return;
    }
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
  if(tab==0&&!speciesOpen&&dy!=0){
   double x=layout.localX(mx),y=layout.localY(my);
   if(SapphireLayout.statusScrollTarget(x,y)==1){
    traitScroll=SapphireLayout.scrollOffset(traitScroll,dy<0?12:-12,traitHeight,46);return true;
   }
   if(SapphireLayout.statusScrollTarget(x,y)==2){
    affinityScroll=SapphireLayout.scrollOffset(affinityScroll,dy<0?12:-12,affinityHeight,120);return true;
   }
  }
  if(dy!=0&&Screen.hasAltDown()){GameplayActions.pageDelta(dy<0?1:-1);return true;}
  if(tab==1&&!ordinary&&layout.localX(mx)>=125&&layout.localX(mx)<=516&&layout.localY(my)>=215&&layout.localY(my)<=386&&dy!=0){
   artScroll=Math.clamp(artScroll+(dy<0?1:-1),0,Math.max(0,visibleArts().size()-5));rebuild();return true;
  }
  if((tab==2||tab==3)&&browser().scroll(layout.localX(mx),layout.localY(my),dy))return true;
  if(ordinary&&tab==1&&abilityBrowser.scroll(layout.localX(mx),layout.localY(my),dy))return true;
  return super.mouseScrolled(layout.localX(mx),layout.localY(my),dx,dy);
 }
 @Override public boolean keyPressed(int key,int scan,int modifiers){
  if(tab==0&&!speciesOpen&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP||key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN)){
   int delta=key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN?36:-36;
   if(traitsFocus!=null&&traitsFocus.isFocused()){traitScroll=Math.clamp(traitScroll+delta,0,Math.max(0,traitHeight-46));return true;}
   if(affinitiesFocus!=null&&affinitiesFocus.isFocused()){affinityScroll=Math.clamp(affinityScroll+delta,0,Math.max(0,affinityHeight-120));return true;}
  }
  if((tab==2||tab==3)&&browser().keyPressed(key))return true;
  if(ordinary&&tab==1&&abilityBrowser.keyPressed(key))return true;
  return super.keyPressed(key,scan,modifiers);
 }
 @Override public boolean isPauseScreen(){return false;}
}

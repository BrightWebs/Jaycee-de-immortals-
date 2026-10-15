package com.brightwebs.jaycee.client.widget;

import com.brightwebs.jaycee.ability.AbilityCatalog;
import com.brightwebs.jaycee.ability.LoadoutClientState;
import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.DivineGlyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Always reads the latest server snapshot rather than keeping a second loadout copy. */
public final class EquipSlot extends Button {
 private final int slot;
 public EquipSlot(int x,int y,int slot,Runnable click){
  super(x,y,72,72,Component.literal("Equip slot "+(slot+1)),b->click.run(),DEFAULT_NARRATION);
  this.slot=slot;
 }
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
  var state=LoadoutClientState.get();
  int index=state.page()*4+slot,id=state.abilities().get(index);
  int x=getX(),y=getY();
  DivineFrame.frame(g,x,y,width,height,isHoveredOrFocused());
  if(id>=0&&id<AbilityCatalog.ALL.size()){
   DivineGlyph.icon(g,x+20,y+12,32,id,true);
   var ability=AbilityCatalog.ALL.get(id);
   g.drawCenteredString(Minecraft.getInstance().font,
    Minecraft.getInstance().font.plainSubstrByWidth(ability.name(),64),x+36,y+51,0xFFD0DCFF);
   setMessage(Component.literal("Slot "+(slot+1)+": "+ability.name()));
  }else{
   g.drawCenteredString(Minecraft.getInstance().font,"Empty",x+36,y+29,0xFF71839C);
   setMessage(Component.literal("Empty slot "+(slot+1)));
  }
  g.drawString(Minecraft.getInstance().font,""+(slot+1),x+7,y+7,0xFFE2B7FF,false);
 }
}

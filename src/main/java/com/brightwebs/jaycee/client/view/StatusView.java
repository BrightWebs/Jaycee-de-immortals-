package com.brightwebs.jaycee.client.view;

import com.brightwebs.jaycee.client.DivineFrame;
import com.brightwebs.jaycee.client.JayceeScreen;
import com.brightwebs.jaycee.client.model.PreviewData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class StatusView implements TabView {
 @Override public void init(JayceeScreen s) {}
 @Override public void render(JayceeScreen s,GuiGraphics g,int mx,int my) {
  var player=Minecraft.getInstance().player;
  s.panel(g,12,48,150,250,"ADVENTURER");
  s.panel(g,170,48,260,250,"CHARACTER");
  s.panel(g,438,48,190,250,"ATTRIBUTES");
  s.text(g,player==null?"Player":player.getName().getString(),24,83,126);
  s.text(g,"Divine Initiate",24,105,126);
  s.text(g,"Level "+(player==null?0:player.experienceLevel),24,142,126);
  s.text(g,"EXP "+(player==null?0:(int)(player.experienceProgress*100))+" / 100%",24,164,126);
  s.text(g,"Level / EXP: live",24,213,126);
  s.text(g,"Other stats: sample",24,235,126);
  DivineFrame.portrait(g,200,82,200,190);
  if(player!=null)InventoryScreen.renderEntityInInventoryFollowsMouse(g,210,90,390,264,65,0f,mx,my,player);
  s.text(g,"HP  "+(player==null?"-":(int)player.getHealth()+" / "+(int)player.getMaxHealth()),450,88,165);
  int y=120;
  for(var stat:PreviewData.STATS){s.text(g,stat.name()+"  "+stat.value(),450,y,165);y+=28;}
 }
}

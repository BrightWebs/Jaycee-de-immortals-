package com.brightwebs.jaycee.client;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.JayceeNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=JayceeMod.ID,value=Dist.CLIENT)
public final class JayceeHud {
 @SubscribeEvent public static void draw(RenderGuiEvent.Post event){
  var mc=Minecraft.getInstance();var p=mc.player;
  if(p==null||mc.options.hideGui||mc.screen!=null||!JayceeMod.unlocked(p))return;
  var g=event.getGuiGraphics();int page=JayceeNetwork.page(p);int[] slots=JayceeNetwork.slots(p);
  int x=mc.getWindow().getGuiScaledWidth()-148,y=mc.getWindow().getGuiScaledHeight()-69;
  UiArt.frame(g,x,y,140,61,false);
  for(int i=0;i<4;i++){int sx=x+7+i*32;UiArt.frame(g,sx,y+8,30,30,false);UiArt.icon(g,sx+4,y+12,22,slots[page*4+i]);}
  g.drawCenteredString(mc.font,"Page "+(page+1)+" / 10",x+70,y+43,0xFFB7D9FA);
 }
 @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event){
  var mc=Minecraft.getInstance();var p=mc.player;
  if(mc.screen!=null||p==null||!Screen.hasAltDown()||!JayceeMod.unlocked(p)||event.getScrollDeltaY()==0)return;
  int page=Math.floorMod(JayceeNetwork.page(p)+(event.getScrollDeltaY()<0?1:-1),10);
  p.getPersistentData().putInt("JayceePage",page);PacketDistributor.sendToServer(new JayceeNetwork.Edit(page,-1,-1));event.setCanceled(true);
 }
}

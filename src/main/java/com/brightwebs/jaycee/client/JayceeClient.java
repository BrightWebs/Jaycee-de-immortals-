package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.gameplay.GameplayActions;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=JayceeMod.ID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public class JayceeClient {
 public static final KeyMapping OPEN=new KeyMapping("key.jaycee.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,"key.categories.jaycee");
 public static final KeyMapping BESTOW=new KeyMapping("key.jaycee.bestow",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.jaycee");
 public static final KeyMapping[] ABILITY_KEYS={
  new KeyMapping("key.jaycee.slot1",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Z,"key.categories.jaycee"),
  new KeyMapping("key.jaycee.slot2",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_X,"key.categories.jaycee"),
  new KeyMapping("key.jaycee.slot3",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_C,"key.categories.jaycee"),
  new KeyMapping("key.jaycee.slot4",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.jaycee")
 };
 @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(OPEN);e.register(BESTOW);for(KeyMapping key:ABILITY_KEYS)e.register(key);JayceeHud.setSlotKeyProvider(slot->ABILITY_KEYS[slot].getTranslatedKeyMessage());}
 @EventBusSubscriber(modid=JayceeMod.ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 public static class Tick {
  @SubscribeEvent public static void tick(ClientTickEvent.Post e){
   Minecraft mc=Minecraft.getInstance();
   while(OPEN.consumeClick()){
    if(mc.player!=null&&mc.screen==null){
     var state=PlayerState.get(mc.player);
     mc.setScreen(new JayceeScreen(SapphireLayout.ordinaryMenu(state.supreme(),state.playerMenuPreview())));
    }
   }
   while(BESTOW.consumeClick()){
    if(mc.player!=null&&mc.screen==null)mc.setScreen(new DivineBondsScreen(true));
   }
   for(int slot=0;slot<ABILITY_KEYS.length;slot++)while(ABILITY_KEYS[slot].consumeClick()){
    if(mc.player==null||mc.screen!=null||!JayceeMod.unlocked(mc.player))continue;
    if(Screen.hasAltDown())GameplayActions.cycleMode(PlayerState.get(mc.player).page(),slot);
    else GameplayActions.activate(slot);
   }
  }
  @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){
   Minecraft mc=Minecraft.getInstance();
   if(mc.player!=null&&mc.screen==null&&JayceeMod.unlocked(mc.player)&&Screen.hasAltDown()&&e.getScrollDeltaY()!=0){
    GameplayActions.pageDelta(e.getScrollDeltaY()<0?1:-1);
    e.setCanceled(true);
   }
  }
 }
}

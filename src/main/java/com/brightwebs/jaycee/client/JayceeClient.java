package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=JayceeMod.ID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public class JayceeClient {
 public static boolean serverVerifiedJaycee = false;
 public static int consumedCores = 0;
 public static void setServerRole(boolean verified, int cores) { serverVerifiedJaycee=verified; consumedCores=cores; }
 public static final KeyMapping BESTOW=new KeyMapping("key.jaycee.bestow",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.jaycee");
 public static final KeyMapping OPEN=new KeyMapping("key.jaycee.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,"key.categories.jaycee");
 @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(OPEN);e.register(BESTOW);}
 @EventBusSubscriber(modid=JayceeMod.ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 public static class Tick { @SubscribeEvent public static void tick(ClientTickEvent.Post e){while(BESTOW.consumeClick()) DivineBestowalScreen.openForTarget();while(OPEN.consumeClick()){Minecraft mc=Minecraft.getInstance();if(mc.player!=null){if(serverVerifiedJaycee && consumedCores>=3) mc.setScreen(new JayceeScreen()); else mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Jaycee-only menu requires the unique Jaycee advancement and 3 consumed cores."),true);}}}}
}

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

@EventBusSubscriber(modid=JayceeMod.ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public class JayceeClient {
 public static final KeyMapping.Category CATEGORY=new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath(JayceeMod.ID,"main"));
 public static final KeyMapping OPEN=new KeyMapping("key.jaycee.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,CATEGORY);
 @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.registerCategory(CATEGORY);e.register(OPEN);}
 @EventBusSubscriber(modid=JayceeMod.ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 public static class Tick { @SubscribeEvent public static void tick(ClientTickEvent.Post e){while(OPEN.consumeClick()){Minecraft mc=Minecraft.getInstance();if(mc.player!=null){mc.setScreen(new JayceeScreen());}}}}
}

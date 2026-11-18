package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.client.BrightCoreClient;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** The M key opens the menu; client setup gives Bright Core's ability bar the Jaycee look. */
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
public final class JayceeClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(JayceeMod.ID, "main"));
    public static final KeyMapping OPEN = new KeyMapping("key.jaycee.open", InputConstants.KEY_M, CATEGORY);

    private JayceeClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        BrightCoreClient.setBarRenderer(JayceeAbilityBar::render);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean needsRace = !RaceAPI.data(mc.player).chosen() && !RaceRegistry.starting().isEmpty();
        while (OPEN.consumeClick()) {
            mc.setScreenAndShow(needsRace ? new RaceSelectScreen() : new JayceeScreen());
        }
        if (needsRace && mc.gui.screen() == null && mc.player.tickCount > 40) {
            mc.setScreenAndShow(new RaceSelectScreen());
        }
    }
}

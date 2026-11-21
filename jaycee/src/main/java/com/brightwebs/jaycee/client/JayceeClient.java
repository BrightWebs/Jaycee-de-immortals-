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

// B opens the menu, N names stuff. client setup gives bright core's skill bar the jaycee look
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
public final class JayceeClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(JayceeMod.ID, "main"));
    public static final KeyMapping MENU = new KeyMapping("key.jaycee.open_menu", InputConstants.KEY_B, CATEGORY);
    // name whatever you're looking at (shift: flip whether others can name you)
    public static final KeyMapping NAME = new KeyMapping("key.jaycee.name", InputConstants.KEY_N, CATEGORY);

    private JayceeClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        BrightCoreClient.setBarRenderer(JayceeAbilityBar::render);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(MENU);
        event.register(NAME);
        com.brightwebs.jaycee.network.JayceeNetwork.openNaming = NamingScreen::open;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("JayceeClient.tick", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            boolean needsRace = (!RaceAPI.data(mc.player).chosen() || RaceAPI.race(mc.player).isEmpty()) && !RaceRegistry.starting().isEmpty();
            while (MENU.consumeClick()) {
                mc.setScreenAndShow(needsRace && !RaceSelectScreen.awaitingServer() ? new RaceSelectScreen() : new MenuScreen());
            }
            while (NAME.consumeClick()) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.brightwebs.jaycee.network.JayceeNetwork.NamingKey(mc.player.isShiftKeyDown()));
            }
            if (needsRace && mc.gui.screen() == null && mc.player.tickCount > 40 && !RaceSelectScreen.awaitingServer()) {
                mc.setScreenAndShow(new RaceSelectScreen());
            }
        });
    }
}

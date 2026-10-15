package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.ability.LoadoutClientState;
import com.brightwebs.jaycee.ability.LoadoutNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JayceeMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JayceeClient {
    public static final KeyMapping OPEN = key("open", GLFW.GLFW_KEY_M);
    public static final KeyMapping[] SLOTS = {
        key("slot1", GLFW.GLFW_KEY_Z), key("slot2", GLFW.GLFW_KEY_X),
        key("slot3", GLFW.GLFW_KEY_C), key("slot4", GLFW.GLFW_KEY_V)
    };

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.jaycee." + name, InputConstants.Type.KEYSYM, code, "key.categories.jaycee");
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        for (KeyMapping slot : SLOTS) event.register(slot);
    }

    static boolean gameplay(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.screen == null
            && mc.getOverlay() == null && !mc.player.isSpectator();
    }

    @EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
    public static final class Tick {
        private static boolean requested;

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null && mc.getConnection() != null && !requested) {
                LoadoutNetwork.sendAction(LoadoutNetwork.REQUEST, 0, 0, -1);
                requested = true;
            }
            while (OPEN.consumeClick()) {
                if (gameplay(mc) && LoadoutClientState.get().accessible()) mc.setScreen(new JayceeScreen());
            }
            for (int slot = 0; slot < SLOTS.length; slot++) {
                while (SLOTS[slot].consumeClick()) {
                    if (gameplay(mc) && LoadoutClientState.get().accessible()) {
                        LoadoutNetwork.sendAction(Screen.hasAltDown() ? LoadoutNetwork.CYCLE_MODE : LoadoutNetwork.ACTIVATE,
                            LoadoutClientState.get().page(), slot, -1);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void scroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            double delta = event.getScrollDeltaY();
            if (!gameplay(mc) || !Screen.hasAltDown() || delta == 0
                || !LoadoutClientState.get().accessible()) return;
            int page = Math.floorMod(LoadoutClientState.get().page() + (delta > 0 ? -1 : 1), 10);
            LoadoutNetwork.sendAction(LoadoutNetwork.SELECT_PAGE, page, 0, -1);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            requested = false;
            LoadoutClientState.clear();
            while (OPEN.consumeClick()) {}
            for (KeyMapping slot : SLOTS) while (slot.consumeClick()) {}
        }
    }
}

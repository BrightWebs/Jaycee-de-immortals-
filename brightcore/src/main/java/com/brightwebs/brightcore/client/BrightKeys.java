package com.brightwebs.brightcore.client;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.network.BrightNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Four ability keys (Z, X, C, V by default) and a preset key (Y), all rebindable under
 * Controls > "Abilities". Hold Switch Mode (Left Alt) and press a skill key to switch that skill's mode. Pressing one only sends the
 * slot number; the server does the rest.
 */
@EventBusSubscriber(modid = BrightCore.ID, value = Dist.CLIENT)
public final class BrightKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(BrightCore.id("abilities"));

    private static final int[] DEFAULTS = {
            InputConstants.KEY_Z, InputConstants.KEY_X, InputConstants.KEY_C, InputConstants.KEY_V};

    public static final KeyMapping[] ABILITY = new KeyMapping[AbilityData.SLOTS_PER_PAGE];
    /** Held together with a skill key to switch that skill's mode, like Tensura: Reincarnated. */
    public static final KeyMapping MODE_SWITCH = new KeyMapping("key.brightcore.mode_switch", InputConstants.KEY_LALT, CATEGORY);
    public static final KeyMapping NEXT_PAGE = new KeyMapping("key.brightcore.next_page", InputConstants.KEY_Y, CATEGORY);

    static {
        for (int i = 0; i < ABILITY.length; i++) {
            ABILITY[i] = new KeyMapping("key.brightcore.skill_slot_" + (i + 1), DEFAULTS[i], CATEGORY);
        }
    }

    private BrightKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        for (KeyMapping key : ABILITY) event.register(key);
        event.register(MODE_SWITCH);
        event.register(NEXT_PAGE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("BrightKeys.onClientTick", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            for (int i = 0; i < ABILITY.length; i++) {
                while (ABILITY[i].consumeClick()) {
                    ClientPacketDistributor.sendToServer(MODE_SWITCH.isDown()
                            ? new BrightNetwork.CycleMode(i)
                            : new BrightNetwork.ActivateSlot(i));
                }
            }
            while (NEXT_PAGE.consumeClick()) {
                ClientPacketDistributor.sendToServer(new BrightNetwork.SetPage(AbilityAPI.get(mc.player).page() + 1));
            }
        });
    }

    /** The key's current binding as a short label, e.g. "R". */
    public static String label(int slot) {
        return ABILITY[slot].getTranslatedKeyMessage().getString();
    }
}

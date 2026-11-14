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
 * Five ability keys (R, G, V, B, N by default) and a page key (Y), all rebindable under
 * Controls > "Abilities". Pressing one only sends the slot number; the server does the rest.
 */
@EventBusSubscriber(modid = BrightCore.ID, value = Dist.CLIENT)
public final class BrightKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(BrightCore.id("abilities"));

    private static final int[] DEFAULTS = {
            InputConstants.KEY_R, InputConstants.KEY_G, InputConstants.KEY_V, InputConstants.KEY_B, InputConstants.KEY_N};

    public static final KeyMapping[] ABILITY = new KeyMapping[AbilityData.SLOTS_PER_PAGE];
    public static final KeyMapping NEXT_PAGE = new KeyMapping("key.brightcore.next_page", InputConstants.KEY_Y, CATEGORY);

    static {
        for (int i = 0; i < ABILITY.length; i++) {
            ABILITY[i] = new KeyMapping("key.brightcore.ability_" + (i + 1), DEFAULTS[i], CATEGORY);
        }
    }

    private BrightKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        for (KeyMapping key : ABILITY) event.register(key);
        event.register(NEXT_PAGE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (int i = 0; i < ABILITY.length; i++) {
            while (ABILITY[i].consumeClick()) {
                ClientPacketDistributor.sendToServer(new BrightNetwork.ActivateSlot(i));
            }
        }
        while (NEXT_PAGE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new BrightNetwork.SetPage(AbilityAPI.get(mc.player).page() + 1));
        }
    }

    /** The key's current binding as a short label, e.g. "R". */
    public static String label(int slot) {
        return ABILITY[slot].getTranslatedKeyMessage().getString();
    }
}

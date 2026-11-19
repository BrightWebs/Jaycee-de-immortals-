package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.util.Safe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Skills held down (breaths, chanted spells): ticks them while the key is down and tells them on release. */
@EventBusSubscriber(modid = BrightCore.ID)
public final class AbilityHolds {
    private static final class Hold {
        final Ability ability; final int mode; int ticks;
        Hold(Ability ability, int mode) { this.ability = ability; this.mode = mode; }
    }
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    private AbilityHolds() {}

    static void start(ServerPlayer player, Ability ability, int mode) {
        HOLDS.put(player.getUUID(), new Hold(ability, mode));
    }

    /** The skill this player is holding right now, or null. */
    public static Ability holding(ServerPlayer player) {
        Hold h = HOLDS.get(player.getUUID());
        return h == null ? null : h.ability;
    }

    public static int heldTicks(ServerPlayer player) {
        Hold h = HOLDS.get(player.getUUID());
        return h == null ? 0 : h.ticks;
    }

    static void release(ServerPlayer player, Ability ability) {
        Hold h = HOLDS.get(player.getUUID());
        if (h == null || h.ability != ability) return;
        HOLDS.remove(player.getUUID());
        Safe.run("release " + ability.id(), () -> h.ability.onRelease(player, (ServerLevel) player.level(), h.mode, h.ticks));
    }

    /** Stops a hold without the release effect (death, leaving, out of energy). */
    public static void cancel(ServerPlayer player) {
        HOLDS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Safe.run("AbilityHolds.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            Hold h = HOLDS.get(player.getUUID());
            if (h == null) return;
            if (!player.isAlive() || !AbilityAPI.has(player, h.ability)) { cancel(player); return; }
            boolean keep = h.ability.onHeld(player, (ServerLevel) player.level(), h.mode, h.ticks);
            h.ticks++;
            if (!keep) cancel(player);
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDS.remove(event.getEntity().getUUID());
    }
}

package com.brightwebs.brightcore.api.race;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.util.Safe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// race abilities by race id, plus key hold state and cooldowns
@EventBusSubscriber(modid = BrightCore.ID)
public final class RaceAbilities {
    private static final Map<Identifier, RaceAbility> BY_RACE = new HashMap<>();
    private static final Map<UUID, Integer> HELD = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private RaceAbilities() {}

    public static void register(Identifier race, RaceAbility ability) { BY_RACE.put(race, ability); }

    public static RaceAbility of(ServerPlayer player) {
        return RaceAPI.race(player).map(r -> BY_RACE.get(r.id())).orElse(null);
    }

    // starts a cooldown in seconds (tensura's instance.setCooldown)
    public static void setCooldown(ServerPlayer player, int seconds) {
        COOLDOWN.put(player.getUUID(), player.level().getGameTime() + seconds * 20L);
    }

    public static boolean onCooldown(ServerPlayer player) {
        return COOLDOWN.getOrDefault(player.getUUID(), 0L) > player.level().getGameTime();
    }

    // from the network: race ability key went down or up
    public static void key(ServerPlayer player, boolean down) {
        RaceAbility a = of(player);
        if (down) {
            if (a == null) {
                player.sendOverlayMessage(Component.translatable("brightcore.race.no_ability"));
                return;
            }
            if (player.isSpectator() || !player.isAlive()) return;
            if (onCooldown(player)) {
                long left = COOLDOWN.get(player.getUUID()) - player.level().getGameTime();
                player.sendOverlayMessage(Component.translatable("brightcore.race.cooldown", String.format("%.1f", left / 20f)));
                return;
            }
            Safe.run("race ability press", () -> a.press(player));
            HELD.put(player.getUUID(), 0);
        } else {
            Integer t = HELD.remove(player.getUUID());
            if (t != null && a != null) Safe.run("race ability release", () -> a.release(player, t));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Safe.run("RaceAbilities.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            RaceAbility a = of(player);
            if (a == null) { HELD.remove(player.getUUID()); return; }
            a.tick(player);
            Integer t = HELD.get(player.getUUID());
            if (t != null) {
                if (!a.held(player, t)) HELD.remove(player.getUUID());
                else HELD.put(player.getUUID(), t + 1);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
    }
}

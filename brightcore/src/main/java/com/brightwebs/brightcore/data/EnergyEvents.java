package com.brightwebs.brightcore.data;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** EP from kills, regeneration, and keeping the race's body across logins and respawns. */
@EventBusSubscriber(modid = BrightCore.ID)
public final class EnergyEvents {
    /** Per half second, as a fraction of the maximum. */
    public static final double AURA_REGEN = 0.005, MANA_REGEN = 0.005, SPIRIT_REGEN = 0.0025;

    private EnergyEvents() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == victim) return;
        Alignment alignment = killer instanceof Player p
                ? RaceAPI.race(p).map(Race::alignment).orElse(Alignment.NEUTRAL)
                : Alignment.NEUTRAL;
        double gained = EnergyAPI.absorbKill(killer, victim, alignment);
        if (killer instanceof ServerPlayer player && gained >= 1) {
            player.sendOverlayMessage(Component.translatable("brightcore.energy.gained", EnergyAPI.format(gained)));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        EnergyData d = EnergyAPI.get(player);
        if (!d.initialized()) return; // no race yet
        if (d.aura() >= d.maxAura() && d.mana() >= d.maxMana() && d.spirit() >= d.maxSpirit()) return;
        EnergyAPI.set(player, d.withAura(d.aura() + d.maxAura() * AURA_REGEN)
                .withMana(d.mana() + d.maxMana() * MANA_REGEN)
                .withSpirit(d.spirit() + d.maxSpirit() * SPIRIT_REGEN));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RaceAPI.race(p).ifPresent(r -> RaceAPI.applyBody(p, r));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            RaceAPI.race(p).ifPresent(r -> {
                RaceAPI.applyBody(p, r);
                p.setHealth(p.getMaxHealth());
            });
            EnergyAPI.set(p, EnergyAPI.get(p).refilled());
        }
    }
}

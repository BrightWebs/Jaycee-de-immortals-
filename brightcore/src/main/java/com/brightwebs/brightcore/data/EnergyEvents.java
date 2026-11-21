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

// EP from kills, regen, and keeping your race's body through logins and respawns
@EventBusSubscriber(modid = BrightCore.ID)
public final class EnergyEvents {
    // per half second, as a fraction of the max
    public static final double AURA_REGEN = 0.005, MANA_REGEN = 0.005, SPIRIT_REGEN = 0.0025;

    private EnergyEvents() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("EnergyEvents.onDeath", () -> {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return;
            if (!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == victim) return;
            Alignment alignment = killer instanceof Player p
                    ? RaceAPI.race(p).map(Race::alignment).orElse(Alignment.NEUTRAL)
                    : Alignment.NEUTRAL;
            double gained = EnergyAPI.absorbKill(killer, victim, alignment);
            if (gained >= 1 && victim.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                // souls like tensura: wisps come out of the body and float to whoever killed it
                var from = victim.position().add(0, victim.getBbHeight() / 2, 0);
                com.brightwebs.brightcore.fx.Fx.burst(sl, com.brightwebs.brightcore.fx.BrightParticles.SOUL, from, 6, 0.08);
                com.brightwebs.brightcore.fx.Fx.beam(sl, com.brightwebs.brightcore.fx.BrightParticles.SOUL, from, killer.position().add(0, killer.getBbHeight() * 0.6, 0), 0.7);
            }
            if (killer instanceof ServerPlayer player && gained >= 1) {
                player.sendOverlayMessage(Component.translatable("brightcore.energy.gained", EnergyAPI.format(gained)));
            }
        });
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("EnergyEvents.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
            EnergyData d = EnergyAPI.get(player);
            if (!d.initialized()) return;  // no race yet
            if (d.aura() >= d.maxAura() && d.mana() >= d.maxMana() && d.spirit() >= d.maxSpirit()) return;
            EnergyAPI.set(player, d.withAura(d.aura() + d.maxAura() * AURA_REGEN)
                    .withMana(d.mana() + d.maxMana() * MANA_REGEN)
                    .withSpirit(d.spirit() + d.maxSpirit() * SPIRIT_REGEN));
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("EnergyEvents.onLogin", () -> {
            if (event.getEntity() instanceof ServerPlayer p) RaceAPI.race(p).ifPresent(r -> RaceAPI.applyBody(p, r));
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("EnergyEvents.onRespawn", () -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                RaceAPI.race(p).ifPresent(r -> {
                    RaceAPI.applyBody(p, r);
                    p.setHealth(p.getMaxHealth());
                });
                EnergyAPI.set(p, EnergyAPI.get(p).refilled());
            }
        });
    }
}

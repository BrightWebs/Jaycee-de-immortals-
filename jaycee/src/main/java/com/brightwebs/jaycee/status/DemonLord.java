package com.brightwebs.jaycee.status;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.api.skill.DamageKind;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The path to True Demon Lord, as Tensura: Reincarnated does it.
 *  Seed:      a race that may become a Demon Lord (not Holy) reaching 200,000 EP becomes a Demon Lord Seed.
 *  Souls:     only a Seed gathers souls. Each kill adds 50% of the victim's EP as soul points
 *             (shown as souls = points / 1,000; animals give none).
 *  Awakening: at 10,000 souls you may begin the Harvest Festival (/jaycee harvest); at 20,000 it begins
 *             on its own. Starting it spends 10,000 souls.
 *  Festival:  3 minutes (3600 ticks). You can't fly; souls swirl around you; in the last stretch you are
 *             slowed, then blinded and bound. It ends in awakening: max aura and magicules x3, every
 *             resistance becomes its nullification, the race takes its awakening evolution, and the
 *             world is told a True Demon Lord was born.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class DemonLord {
    static final double SEED_EP = 200_000, SOUL_RATE = 0.5, EP_MULTIPLIER = 3.0;
    static final int AWAKEN_SOULS = 10_000, FORCE_SOULS = 20_000, FESTIVAL_TICKS = 3600;

    private DemonLord() {}

    public static boolean isSeed(Player p) { return StatusData.counter(p, "demon_lord_seed") > 0; }
    public static boolean isTrue(Player p) { return StatusData.counter(p, "true_demon_lord") > 0; }
    public static double soulPoints(Player p) { return StatusData.counter(p, "soul_points"); }

    private static void setFlag(ServerPlayer p, String key, double v) { StatusData.addCounter(p, key, v - StatusData.counter(p, key)); }

    private static void syncSouls(ServerPlayer p) { p.setData(StatusData.SOULS, (int) Math.min(Integer.MAX_VALUE, soulPoints(p) / 1000)); }

    static boolean canBecome(ServerPlayer p) {
        return RaceAPI.race(p).map(r -> r.alignment() != Alignment.HOLY).orElse(false);
    }

    static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (!isSeed(killer) || isTrue(killer) || victim instanceof Animal) return;
        double gain = EnergyAPI.ep(victim) * SOUL_RATE;
        if (gain <= 0) return;
        StatusData.addCounter(killer, "soul_points", gain);
        syncSouls(killer);
        Fx.beam((ServerLevel) killer.level(), BrightParticles.SOUL, victim.position().add(0, victim.getBbHeight() / 2, 0), killer.position().add(0, 1, 0), 0.4);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Safe.run("DemonLord.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            int festival = (int) StatusData.counter(p, "harvest_tick");
            if (festival > 0) { festivalTick(p, festival); return; }
            if (p.tickCount % 20 != 0 || isTrue(p)) return;
            if (!isSeed(p) && canBecome(p) && EnergyAPI.ep(p) >= SEED_EP) {
                setFlag(p, "demon_lord_seed", 1);
                p.sendSystemMessage(Component.translatable("jaycee.demon_lord.seed").withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1, 1);
            }
            if (!isSeed(p)) return;
            int souls = (int) (soulPoints(p) / 1000);
            if (souls >= FORCE_SOULS) begin(p);
            else if (souls >= AWAKEN_SOULS && StatusData.counter(p, "awaken_told") == 0) {
                setFlag(p, "awaken_told", 1);
                p.sendSystemMessage(Component.translatable("jaycee.demon_lord.can_awaken").withStyle(ChatFormatting.GOLD));
            }
        });
    }

    /** Begins the Harvest Festival if the souls are there. */
    public static boolean begin(ServerPlayer p) {
        if (!isSeed(p) || isTrue(p) || StatusData.counter(p, "harvest_tick") > 0) return false;
        if (soulPoints(p) < AWAKEN_SOULS * 1000.0) return false;
        StatusData.addCounter(p, "soul_points", -AWAKEN_SOULS * 1000.0);
        syncSouls(p);
        setFlag(p, "harvest_tick", FESTIVAL_TICKS);
        p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 0.6f);
        for (ServerPlayer everyone : p.level().getServer().getPlayerList().getPlayers()) {
            everyone.sendSystemMessage(Component.translatable("jaycee.demon_lord.festival", p.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
        }
        return true;
    }

    private static void festivalTick(ServerPlayer p, int tick) {
        ServerLevel l = (ServerLevel) p.level();
        if (p.getAbilities().flying) { p.getAbilities().flying = false; p.onUpdateAbilities(); }
        Vec3 c = p.position().add(0, 1, 0);
        int max = FESTIVAL_TICKS;
        if (tick <= Math.min(200, max * 0.1)) {
            if (tick % 10 == 0) { Fx.sphere(l, BrightParticles.SOUL, c, 2.5, 30); Fx.shockwave(l, c, 1); }
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
            p.setDeltaMovement(0, 0.02, 0);
        } else if (tick <= Math.min(600, max * 0.2)) {
            if (tick % 15 == 0) Fx.spiral(l, BrightParticles.SOUL, p.position(), 2, 3, 2, 30, tick * 0.1);
        } else if (tick <= Math.min(1200, max * 0.4)) {
            if (tick % 20 == 0) Fx.cloud(l, BrightParticles.SOUL, c, 20, 2.5);
        } else if (tick <= Math.min(2400, max * 0.7)) {
            if (tick % 25 == 0) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4, false, false, false));
        } else if (tick % 30 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 7, false, false, false));
        }
        if (tick % 10 == 0) p.level().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
        setFlag(p, "harvest_tick", tick - 1);
        if (tick - 1 <= 0) awaken(p);
    }

    /** True Demon Lord. */
    static void awaken(ServerPlayer p) {
        EnergyData d = EnergyAPI.get(p);
        EnergyAPI.set(p, d.withMax(d.maxAura() * EP_MULTIPLIER, d.maxMana() * EP_MULTIPLIER, d.maxSpirit()).refilled());
        for (DamageKind kind : DamageKind.values()) {
            Ability res = BrightSkills.resistance(kind), nul = BrightSkills.nullification(kind);
            if (res != null && nul != null && AbilityAPI.has(p, res) && AbilityAPI.learn(p, nul)) {
                p.sendSystemMessage(Component.translatable("brightcore.message.acquired", nul.name()));
            }
        }
        RaceAPI.race(p).ifPresent(r -> {
            if (!r.evolutions().isEmpty()) {
                Race next = RaceRegistry.get(r.evolutions().get(0).target());
                if (next != null) RaceAPI.set(p, next);
            }
        });
        setFlag(p, "true_demon_lord", 1);
        p.removeEffect(MobEffects.BLINDNESS);
        p.removeEffect(MobEffects.SLOWNESS);
        ServerLevel l = (ServerLevel) p.level();
        Fx.spiral(l, BrightParticles.BLACK_FLAME, p.position(), 1.2, 3, 3, 80, 0);
        Fx.spiral(l, BrightParticles.SOUL, p.position(), 1.2, 3, 3, 80, Math.PI);
        Fx.shockwave(l, p.position().add(0, 1, 0), 3);
        l.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1, 0.8f);
        Titles.grant(p, "true_demon_lord");
        for (ServerPlayer everyone : l.getServer().getPlayerList().getPlayers()) {
            everyone.sendSystemMessage(Component.translatable("jaycee.demon_lord.success", p.getDisplayName()).withStyle(ChatFormatting.GOLD));
        }
    }
}

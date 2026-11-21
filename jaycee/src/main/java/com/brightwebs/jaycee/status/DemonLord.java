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

// the road to true demon lord, same as tensura
//   seed:      a race that can be a demon lord (not holy) hitting 200,000 EP becomes a demon lord seed
//   souls:     only a seed collects souls. each kill adds 50% of the victim's EP as soul points
//              (souls = points / 1,000, animals give none)
//   awakening: at 10,000 souls you can start the harvest festival (/jaycee harvest), at 20,000 it
//              starts by itself. starting it uses 10,000 souls
//   festival:  3 minutes (3600 ticks). no flying, souls swirl around you, near the end you get
//              slowed, then blinded and stuck. it ends in awakening: max aura and magicules x3, every
//              resistance turns into its nullification, the race takes its awakening evolution,
//              and the whole server gets told a true demon lord was born
@EventBusSubscriber(modid = JayceeMod.ID)
public final class DemonLord {
    static final double SEED_EP = 200_000, SOUL_RATE = 0.5, EP_MULTIPLIER = 3.0;
    static final int AWAKEN_SOULS = 10_000, FORCE_SOULS = 20_000, FESTIVAL_TICKS = 3600;

    private DemonLord() {}

    public static boolean isSeed(Player p) { return StatusData.counter(p, "demon_lord_seed") > 0; }
    public static boolean isTrue(Player p) { return StatusData.counter(p, "true_demon_lord") > 0; }
    public static double soulPoints(Player p) { return StatusData.counter(p, "soul_points"); }

    public static void setFlag(ServerPlayer p, String key, double v) { StatusData.addCounter(p, key, v - StatusData.counter(p, key)); }

    /** for commands, sets soul points straight up */
    public static void setSoulPoints(ServerPlayer p, double points) {
        setFlag(p, "soul_points", Math.max(0, points));
        syncSouls(p);
    }

    public static void forceAwaken(ServerPlayer p) { awaken(p); }

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

    // starts the harvest festival if you have the souls
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

    // true demon lord
    static void awaken(ServerPlayer p) {
        EnergyData d = EnergyAPI.get(p);
        EnergyAPI.set(p, d.withMax(d.maxAura() * EP_MULTIPLIER, d.maxMana() * EP_MULTIPLIER, d.maxSpirit()).refilled());
        for (DamageKind kind : DamageKind.values()) {
            Ability res = BrightSkills.resistance(kind), nul = BrightSkills.nullification(kind);
            if (res != null && nul != null && AbilityAPI.has(p, res) && AbilityAPI.learn(p, nul)) {
                p.sendSystemMessage(Component.translatable("brightcore.message.acquired", nul.name()));
            }
        }
        // tensura: awakening jumps to the race's awakening form (human -> human saint...) if it has one
        RaceAPI.race(p).ifPresent(r -> {
            String to = com.brightwebs.jaycee.race.TensuraRequirements.AWAKENING.get(r.id().getPath());
            Race next = to == null ? null : RaceRegistry.get(com.brightwebs.jaycee.race.JayceeRaces.id(to));
            if (next != null) RaceAPI.set(p, next);
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

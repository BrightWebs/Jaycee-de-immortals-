package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.brightwebs.jaycee.skill.SkillHelper.give;

/** Short-lived skill state: spell casting delays, Chant Annulment and Future Attack Prediction. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class SkillState {
    private record Cast(ServerPlayer player, long at, Runnable effect) {}
    private static final List<Cast> CASTS = new ArrayList<>();
    private static final Map<UUID, Long> CHANT = new HashMap<>();
    private static final Map<UUID, Long> FORESIGHT = new HashMap<>();

    private SkillState() {}

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    /** Thought Acceleration: the world slows around you, you speed up. */
    static void accelerate(ServerPlayer p, double radius, int seconds, int slow, int haste) {
        for (LivingEntity e : SkillHelper.around(p, radius)) give(e, MobEffects.SLOWNESS, seconds, slow);
        give(p, MobEffects.SPEED, seconds, haste);
        give(p, MobEffects.HASTE, seconds, haste);
        com.brightwebs.brightcore.fx.Fx.spiral((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.RUNE, p.position(), 1.2, 2.4, 2, 36, p.tickCount * 0.2); com.brightwebs.brightcore.fx.Fx.shockwave((ServerLevel) p.level(), p.position().add(0, 1, 0), 0.6);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
        p.sendOverlayMessage(Component.translatable("jaycee.skill.thought_acceleration.on"));
    }

    static void chantAnnulment(ServerPlayer p, int seconds) { CHANT.put(p.getUUID(), now(p) + seconds * 20L); }
    static void foresight(ServerPlayer p, int seconds) { FORESIGHT.put(p.getUUID(), now(p) + seconds * 20L); }

    /** True when spells cast instantly: Chant Annulment is active, or the player has Raphael. */
    public static boolean instantCast(ServerPlayer p) {
        if (CHANT.getOrDefault(p.getUUID(), 0L) > now(p)) return true;
        Ability raphael = AbilityRegistry.get(JayceeSkills.id("raphael"));
        return raphael != null && AbilityAPI.has(p, raphael);
    }

    /** Runs a spell's effect after its casting time (or now, if casting is instant). */
    static void cast(ServerPlayer p, Ability spell, Runnable effect) {
        int ticks = instantCast(p) ? 0 : spell.castTicks();
        if (ticks <= 0) { effect.run(); return; }
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.6, 0.5, 0.6);
        CASTS.add(new Cast(p, now(p) + ticks, effect));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("SkillState.onTick", () -> {
            for (Iterator<Cast> it = CASTS.iterator(); it.hasNext(); ) {
                Cast c = it.next();
                if (c.player().isRemoved() || !c.player().isAlive()) { it.remove(); continue; }
                if (now(c.player()) >= c.at()) { it.remove(); c.effect().run(); }
            }
        });
    }

    /** Future Attack Prediction: a 50% chance to see an attack coming and dodge it. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("SkillState.onIncoming", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p) || event.getSource().getEntity() == null) return;
            if (FORESIGHT.getOrDefault(p.getUUID(), 0L) <= now(p) || p.getRandom().nextFloat() >= 0.5f) return;
            event.setCanceled(true);
            com.brightwebs.brightcore.fx.Fx.burst((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.HOLY, p.position().add(0, 1, 0), 10, 0.15); com.brightwebs.brightcore.fx.Fx.move((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.SHOCKWAVE, p.position().add(0, 1, 0), net.minecraft.world.phys.Vec3.ZERO);
            p.sendOverlayMessage(Component.translatable("jaycee.skill.dodged"));
        });
    }
}

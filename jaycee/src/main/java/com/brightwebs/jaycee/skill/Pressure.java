package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityHolds;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.effect.JayceeEffects;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// pressure, aka haki. this is the "your aura is so much bigger than mine i can't move" thing from the anime.
// numbers and rules are straight from tensura (CoercionSkill, HakiSkill, HakiField, DemonLordHakiSkill, MortalFearSkill).
//
// the core rule: compare your EP to theirs. if you're more than 2x stronger they get Fear,
// fear level = multiplier x (ratio - 2), max 20. fear slows them, and at higher levels hurts them
// and stops mobs from attacking at all. they also get shoved away from you a bit.
//
// like rimuru holding back his aura, shift + press changes how much of your power you let out
// (100% / 50% / 10% / 1%) once the skill is mastered.
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Pressure {
    private Pressure() {}

    // tensura configs
    static final double COERCION_COST = 50, COERCION_RANGE = 14, COERCION_MULT = 0.25;
    static final int COERCION_FEAR_TICKS = 200, COERCION_COOLDOWN = 2;
    static final double HAKI_COST = 25, HAKI_RADIUS = 15, HAKI_MULT = 0.25, HAKI_SPEED = 0.05, HAKI_SPEED_MASTERED = 0.1;
    static final int HAKI_FEAR_TICKS = 200, HAKI_COOLDOWN = 5, HAKI_COOLDOWN_MASTERED = 3;
    static final double DEMON_LORD_MULT = 0.5, DEMON_LORD_COST = 50, COAT_COST = 100;
    static final int COAT_TICKS = 2400;
    static final double HERO_MULT = 0.375, HERO_COST = 50;
    static final double MORTAL_FEAR_RADIUS = 7, MORTAL_FEAR_MULT = 0.25;
    static final int MAX_FEAR = 20;
    static final double[] SCALES = {1.0, 0.5, 0.1, 0.01};

    private static final Identifier HAKI_SPEED_ID = Identifier.fromNamespaceAndPath(JayceeMod.ID, "haki_hold");
    // who scared who, so fear damage only hits you when you're near the one who did it
    private static final Map<UUID, UUID> FEAR_SOURCE = new HashMap<>();

    public enum Variant {
        DEFAULT(BrightParticles.HAKI), DEMON_LORD(BrightParticles.HAKI_DEMON), HERO(BrightParticles.HAKI_HERO),
        MORTAL_FEAR(BrightParticles.HAKI_FEAR), SACRED(BrightParticles.HAKI_SACRED);
        final Supplier<SimpleParticleType> particle;
        Variant(Supplier<SimpleParticleType> p) { this.particle = p; }
    }

    // ------------------------------------------------------------ power scale

    static double scale(ServerPlayer p, Ability a) {
        int i = (int) StatusData.counter(p, "scale_" + a.id().getPath());
        return SCALES[Math.floorMod(i, SCALES.length)];
    }

    /** shift + press on a mastered pressure skill: let out less of your power */
    static boolean cycleScale(ServerPlayer p, Ability a) {
        if (!AbilityAPI.isMastered(p, a)) {
            p.sendOverlayMessage(Component.translatable("jaycee.skill.scale.not_mastered").withStyle(ChatFormatting.RED));
            return false;
        }
        StatusData.addCounter(p, "scale_" + a.id().getPath(), 1);
        p.sendOverlayMessage(Component.translatable("jaycee.skill.power_scale", (int) Math.round(scale(p, a) * 100) + "%").withStyle(ChatFormatting.DARK_AQUA));
        return false;
    }

    // ------------------------------------------------------------ fear

    /** tensura's HakiField.applyEffect / CoercionSkill. returns the fear level given (or -1 if they weren't scared) */
    static int scare(LivingEntity source, LivingEntity target, double sourceEp, double mult, int ticks) {
        double ratio = sourceEp / Math.max(1, EnergyAPI.ep(target));
        if (ratio <= 2) return -1;
        int level = Math.min(MAX_FEAR, (int) (mult * (ratio - 2)));
        MobEffectInstance now = target.getEffect(JayceeEffects.FEAR);
        if (now == null || now.getAmplifier() <= level) {
            target.addEffect(new MobEffectInstance(JayceeEffects.FEAR, ticks, level, true, false, true), source);
            FEAR_SOURCE.put(target.getUUID(), source.getUUID());
        }
        push(target, source, level);
        return level;
    }

    /** tensura's hakiPush: shoves them away from you, harder the more scared they are (caps out at 0.2) */
    static void push(LivingEntity target, Entity source, int level) {
        if (level < 1) return;
        double resist = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double m = Math.min(0.04 * level, 0.2) * resist;
        Vec3 v = target.getEyePosition().subtract(source.getEyePosition()).normalize().scale(m);
        target.push(v.x, v.y, v.z);
        if (target instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
    }

    /** runs every 2 seconds on anyone with fear (tensura's FearEffect.applyEffectTick) */
    public static void fearTick(ServerLevel level, LivingEntity e, int amp) {
        if (amp < 4) return;
        UUID src = FEAR_SOURCE.get(e.getUUID());
        Entity source = src == null ? null : level.getEntity(src);
        if (source == null) return;
        // players right next to the one scaring them crack under it
        if (e instanceof ServerPlayer && e.distanceTo(source) < 7) e.hurtServer(level, level.damageSources().indirectMagic(source, source), 2 * (amp - 3));
        // at level 10+ it hurts no matter where you are
        if (amp >= 9) e.hurtServer(level, level.damageSources().indirectMagic(source, source), 3 * (amp - 8));
    }

    /** tensura: anything with fear 5+ is too scared to go after anyone */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Pressure.onTarget", () -> {
            MobEffectInstance fear = event.getEntity().getEffect(JayceeEffects.FEAR);
            if (fear != null && fear.getAmplifier() >= 4 && event.getNewAboutToBeSetTarget() != null) event.setCanceled(true);
        });
    }

    // ------------------------------------------------------------ coercion

    /** tensura's coercion: a roar along where you look, 14 blocks. scares and weakens whatever it passes */
    static boolean coercion(ServerPlayer p, Ability a) {
        if (p.isShiftKeyDown()) return cycleScale(p, a);
        if (!Tk.spend(p, a, COERCION_COST)) return false;
        ServerLevel level = (ServerLevel) p.level();
        double ep = EnergyAPI.ep(p) * scale(p, a);
        Vec3 look = p.getLookAngle(), from = p.getEyePosition().add(look.scale(2));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1, 1.2f);
        boolean hit = false;
        java.util.Set<LivingEntity> done = new java.util.HashSet<>();
        for (int i = 1; i < COERCION_RANGE - 2; i++) {
            Vec3 at = from.add(look.scale(i));
            Fx.move(level, BrightParticles.SONIC, at, look.scale(0.05));
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4), t -> t != p && !t.isAlliedTo(p) && !Bonds.isSubordinateOf(t, p))) {
                if (!done.add(t)) continue;
                hit = true;
                int lvl = scare(p, t, ep, COERCION_MULT, COERCION_FEAR_TICKS);
                if (lvl >= 0) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, COERCION_FEAR_TICKS, lvl, false, false), p);
            }
        }
        if (hit) {
            AbilityAPI.addMasteryPoint(p, a);
            AbilityAPI.setCooldown(p, a, 20 * COERCION_COOLDOWN);
        }
        return true;
    }

    // ------------------------------------------------------------ haki (held)

    /** first press of a hold. shift instead changes the power scale */
    static boolean hakiStart(ServerPlayer p, Ability a) {
        if (p.isShiftKeyDown()) { cycleScale(p, a); return false; }
        return true;
    }

    /**
     * hold to let your haki out (tensura's HakiField). you barely move while doing it, costs magicules
     * every second, and every quarter second everything weaker in 15 blocks gets hit with fear
     */
    static boolean hakiHeld(ServerPlayer p, Ability a, int held, Variant v, double mult, double cost) {
        if (held % 20 == 0 && !Tk.spend(p, a, cost)) { stopHolding(p); return false; }
        if (held % Tk.MASTERY_HOLD_TICK == 0 && held > 0) AbilityAPI.addMasteryPoint(p, a);
        ServerLevel level = (ServerLevel) p.level();
        boolean mastered = AbilityAPI.isMastered(p, a);
        slowWhileHolding(p, mastered);
        double ep = EnergyAPI.ep(p) * scale(p, a);
        if (held == 1) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1, 0.8f);
        else if (held >= 40 && held % 30 == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 0.7f);
        hakiVisual(level, p, v, held, 1);
        if (held % 5 == 0) {
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(HAKI_RADIUS),
                    t -> t != p && !t.isAlliedTo(p) && !Bonds.isSubordinateOf(t, p) && t.distanceTo(p) <= HAKI_RADIUS)) {
                scare(p, t, ep, mult, HAKI_FEAR_TICKS);
            }
        }
        return true;
    }

    static void hakiReleased(ServerPlayer p, Ability a) {
        stopHolding(p);
        AbilityAPI.setCooldown(p, a, 20 * HAKI_COOLDOWN);
    }

    /** tensura holds you almost still while you let it out (x0.05 speed, x0.1 mastered) */
    private static void slowWhileHolding(ServerPlayer p, boolean mastered) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(HAKI_SPEED_ID,
                (mastered ? HAKI_SPEED_MASTERED : HAKI_SPEED) - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    static void stopHolding(ServerPlayer p) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(HAKI_SPEED_ID);
    }

    /** if a hold got cut off some other way (death, logout) make sure you're not stuck slow */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        com.brightwebs.brightcore.api.util.Safe.run("Pressure.onPlayerTick", () -> {
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null || speed.getModifier(HAKI_SPEED_ID) == null) return;
            Ability held = AbilityHolds.holding(p);
            if (held == null || !held.id().getNamespace().equals(JayceeMod.ID) || !HELD_PRESSURE.contains(held.id().getPath())) stopHolding(p);
        });
    }

    private static final java.util.Set<String> HELD_PRESSURE = java.util.Set.of("haki", "demon_lord_haki", "hero_haki", "mortal_fear");

    /**
     * what tensura's haki model looks like, done with particles: two tilted rings of wisps spinning
     * opposite ways around your chest, flares licking up from your feet, and a pressure wave rolling
     * out across the ground every second
     */
    static void hakiVisual(ServerLevel level, LivingEntity who, Variant v, int held, double size) {
        Vec3 c = who.position().add(0, who.getBbHeight() * 0.55, 0);
        double r = 2.2 * size * Math.max(1, who.getBbHeight() / 1.8);
        double t = held * 0.18;
        for (int ring = 0; ring < 2; ring++) {
            double tilt = ring == 0 ? 0.35 : -0.35, dir = ring == 0 ? 1 : -1;
            for (int i = 0; i < 6; i++) {
                double ang = dir * t + i * Math.PI / 3;
                double x = Math.cos(ang) * r, z = Math.sin(ang) * r, y = Math.sin(ang) * r * tilt;
                Vec3 at = c.add(x, y, z);
                Fx.move(level, v.particle, at, new Vec3(-z, 0, x).normalize().scale(0.08 * dir).add(0, 0.03, 0));
            }
        }
        if (held % 3 == 0) {
            for (int i = 0; i < 4; i++) {
                double ang = level.getRandom().nextDouble() * Math.PI * 2, rr = r * 0.6 * level.getRandom().nextDouble();
                Fx.move(level, v.particle, who.position().add(Math.cos(ang) * rr, 0.1, Math.sin(ang) * rr), new Vec3(0, 0.25, 0));
            }
        }
        if (held % 20 == 1) {
            Fx.ring(level, v.particle, who.position().add(0, 0.2, 0), 1.0, 36, 0.55);
            Fx.ring(level, BrightParticles.SONIC, c, 0.6, 12, 0.35);
        }
    }

    // ------------------------------------------------------------ demon lord haki coat

    static boolean toggleCoat(ServerPlayer p, Ability a) {
        ServerLevel level = (ServerLevel) p.level();
        if (p.hasEffect(JayceeEffects.HAKI_COAT)) {
            p.removeEffect(JayceeEffects.HAKI_COAT);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1, 1.2f);
            return false;
        }
        if (!Tk.spend(p, a, COAT_COST)) return false;
        p.addEffect(new MobEffectInstance(JayceeEffects.HAKI_COAT, COAT_TICKS, 0, false, false, true));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1, 0.8f);
        Fx.spiral(level, BrightParticles.HAKI_DEMON, p.position(), 0.8, 2.2, 2, 30, 0);
        return true;
    }

    /** haki coat wraps your weapon and fists in haki: your hits punch through resistance (x1.25 here) */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Pressure.onHurt", () -> {
            if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || !attacker.hasEffect(JayceeEffects.HAKI_COAT)) return;
            event.setAmount(event.getAmount() * 1.25f);
            if (attacker.level() instanceof ServerLevel l)
                Fx.burst(l, BrightParticles.HAKI_DEMON, event.getEntity().position().add(0, event.getEntity().getBbHeight() / 2, 0), 6, 0.15);
        });
    }

    // ------------------------------------------------------------ mortal fear

    /**
     * tensura's mortal fear: if you have subordinates the haki comes out of all of them at once and
     * pumps them up with strength. no subordinates and it's just you. costs per subordinate
     */
    static boolean mortalFearHeld(ServerPlayer p, Ability a, int held) {
        ServerLevel level = (ServerLevel) p.level();
        List<LivingEntity> subs = new java.util.ArrayList<>();
        for (Bonds.Sub s : Bonds.subs(p)) {
            try {
                if (level.getEntity(UUID.fromString(s.uuid())) instanceof LivingEntity le && le.isAlive()) subs.add(le);
            } catch (IllegalArgumentException ignored) {}
        }
        boolean self = subs.isEmpty();
        if (self) subs.add(p);
        if (held % 20 == 0 && !Tk.spend(p, a, HAKI_COST * subs.size())) { stopHolding(p); return false; }
        if (held % Tk.MASTERY_HOLD_TICK == 0 && held > 0) AbilityAPI.addMasteryPoint(p, a);
        slowWhileHolding(p, AbilityAPI.isMastered(p, a));
        double ep = EnergyAPI.ep(p) * scale(p, a);
        if (held == 1) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.6f, 1.1f);
        for (LivingEntity from : subs) {
            double radius = MORTAL_FEAR_RADIUS * Math.max(1, from.getBbHeight() / 1.8);
            hakiVisual(level, from, Variant.MORTAL_FEAR, held, 0.8);
            if (!self && held % 20 == 0) {
                from.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1200, AbilityAPI.isMastered(p, a) ? 6 : 2, false, false, true), p);
            }
            if (held % 5 == 0) {
                for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(radius),
                        t -> t != p && !subs.contains(t) && !t.isAlliedTo(p) && !Bonds.isSubordinateOf(t, p) && t.distanceTo(from) <= radius)) {
                    scare(p, t, ep, MORTAL_FEAR_MULT, HAKI_FEAR_TICKS);
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------ learning

    static Ability skill(String id) { return AbilityRegistry.get(Identifier.fromNamespaceAndPath(JayceeMod.ID, id)); }

    /**
     * how you pick these up, same conditions as tensura:
     *   coercion          over 5,000 EP
     *   haki              coercion mastered and over 100,000 EP
     *   demon lord haki   have haki, can become a demon lord (not holy), over 200,000 EP
     *   hero's haki       have haki and you're holy, over 200,000 EP (tensura wants a hero egg, we don't have heroes yet)
     *   mortal fear       haki mastered
     */
    static void learnChecks(ServerPlayer p) {
        double ep = EnergyAPI.ep(p);
        Ability coercion = skill("coercion"), haki = skill("haki");
        if (coercion == null || haki == null) return;
        if (ep > 5_000) learn(p, coercion);
        if (AbilityAPI.has(p, coercion) && AbilityAPI.isMastered(p, coercion) && ep > 100_000) learn(p, haki);
        if (AbilityAPI.has(p, haki)) {
            boolean holy = RaceAPI.race(p).map(r -> r.alignment() == Alignment.HOLY).orElse(false);
            if (ep > 200_000) learn(p, skill(holy ? "hero_haki" : "demon_lord_haki"));
            if (AbilityAPI.isMastered(p, haki)) learn(p, skill("mortal_fear"));
        }
    }

    private static void learn(ServerPlayer p, Ability a) {
        if (a != null && AbilityAPI.learn(p, a)) p.sendSystemMessage(Component.translatable("brightcore.message.acquired", a.name()));
    }

    /** all the pressure skills */
    static final List<DeferredHolder<Ability, Ability>> ALL = new java.util.ArrayList<>();
}

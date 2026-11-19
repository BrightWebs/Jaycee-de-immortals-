package com.brightwebs.jaycee.race;

import com.brightwebs.brightcore.api.race.RaceAbilities;
import com.brightwebs.brightcore.api.race.RaceAbility;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Race abilities on the Race Ability key (R), as Tensura: Reincarnated 2.0.1.4 defines them:
 *  Lesser / Greater Daemon   toggle flight (we extend it to the rest of the daemon line)
 *  Harpy line                wing boost upward into a glide (cooldown 3 s)
 *  Dragonewt line            dash along your look into a glide, costs hunger (cooldown 3 s)
 *  Slime line                hold to charge a jump, release to leap where you look
 *  Vampire line              drain blood from a target in reach, otherwise switch bat flight
 * Not in Tensura (our own lines): Pixie and Lesser Angel lines fly like daemons; the lesser
 * dragon line dashes like dragonewts.
 */
public final class RaceSkills {
    private static final Set<UUID> GLIDING = new HashSet<>();

    private RaceSkills() {}

    private static Identifier id(String s) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, s); }

    public static void init() {
        for (String r : List.of("lesser_daemon", "greater_daemon", "arch_daemon", "daemon_lord", "devil_lord", "pixie", "fairy", "chatty_pixie",
                "lesser_angel", "angel", "cherub", "power", "dominion", "seraph")) RaceAbilities.register(id(r), FLIGHT);
        reg("harpy", harpy(0.75f)); reg("harpy_queen", harpy(0.85f)); reg("spirit_bird", harpy(0.95f)); reg("divine_bird", harpy(1.05f));
        reg("dragonewt", dragon(0.1f)); reg("true_dragonewt", dragon(0.25f)); reg("divine_dragon", dragon(0.5f));
        reg("lesser_dragon", dragon(0.1f)); reg("medium_dragon", dragon(0.25f)); reg("dragon_lord", dragon(0.5f));
        reg("element_dragon", dragon(0.5f)); reg("archdragon", dragon(0.5f));
        reg("slime", slime(40)); reg("metal_slime", slime(50)); reg("demon_slime", slime(60)); reg("god_slime", slime(80));
        for (String r : List.of("vampire", "vampire_lord", "vampire_overcomer", "divine_vampire")) RaceAbilities.register(id(r), VAMPIRE);
    }

    private static void reg(String race, RaceAbility a) { RaceAbilities.register(id(race), a); }

    /** Tensura's riptide push: a shove along where you're looking. */
    static void riptidePush(LivingEntity e, float level) {
        float yaw = e.getYHeadRot(), pitch = e.getXRot();
        float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
        float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        float len = Mth.sqrt(x * x + y * y + z * z), f = 3.0f * ((1.0f + level) / 4.0f);
        e.push(x * f / len, y * f / len, z * f / len);
    }

    /** Players move on their own client, so a changed velocity has to be sent to them. */
    static void sync(ServerPlayer p) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
    }

    private static void setFlight(ServerPlayer p, boolean on) {
        if (p.isCreative() || p.isSpectator()) return;
        p.getAbilities().mayfly = on;
        p.getAbilities().flying = on;
        p.onUpdateAbilities();
    }

    /** Keeps race flight through respawns and relogs, which reset it. */
    private static void keepFlight(ServerPlayer p, String key) {
        if (p.tickCount % 20 == 0 && StatusData.counter(p, key) > 0 && !p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
        }
    }

    private static void toggleStored(ServerPlayer p, String key, boolean on) {
        StatusData.addCounter(p, key, (on ? 1 : 0) - StatusData.counter(p, key));
    }

    // ---- flight toggle (Lesser Daemon) ---------------------------------------------

    static final RaceAbility FLIGHT = new RaceAbility() {
        @Override public void press(ServerPlayer p) {
            if (p.isCreative() || p.isSpectator()) return;
            boolean on = !p.getAbilities().mayfly;
            setFlight(p, on);
            toggleStored(p, "race_flight", on);
            p.level().playSound(null, p.blockPosition(), on ? SoundEvents.PHANTOM_FLAP : SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
            com.brightwebs.brightcore.fx.Fx.ring((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.GUST, p.position().add(0, 0.2, 0), 0.6, 14, on ? 0.15 : -0.05);
        }
        @Override public void tick(ServerPlayer p) { keepFlight(p, "race_flight"); }
    };

    // ---- wings and dashes (Harpy, Dragonewt) ---------------------------------------------

    private static RaceAbility harpy(float upward) {
        return new RaceAbility() {
            @Override public void press(ServerPlayer p) {
                if (p.isPassenger()) return;
                p.resetFallDistance();
                RaceAbilities.setCooldown(p, 3);
                if (p.onGround()) riptidePush(p, -0.6f);
                Vec3 d = p.getDeltaMovement();
                double dy = d.y <= 0 ? upward : d.y + upward;
                p.setDeltaMovement(new Vec3(d.x * (0.3f + upward), dy, d.z * (0.3f + upward)));
                sync(p);
                startGlide(p);
                com.brightwebs.brightcore.fx.Fx.ring((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.GUST, p.position(), 0.5, 16, 0.3); com.brightwebs.brightcore.fx.Fx.burst((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.PETAL, p.position().add(0, 1, 0), 10, 0.2);
                p.level().playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1, 1);
            }
            @Override public void tick(ServerPlayer p) { glide(p); }
        };
    }

    private static RaceAbility dragon(float boost) {
        return new RaceAbility() {
            @Override public void press(ServerPlayer p) {
                if (p.isPassenger()) return;
                if (p.getFoodData().getFoodLevel() <= 0 && !p.getAbilities().invulnerable) {
                    p.sendOverlayMessage(Component.translatable("jaycee.race.too_hungry").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!p.getAbilities().invulnerable) p.getFoodData().addExhaustion(2.0f);
                if (p.onGround()) {
                    Vec3 d = p.getDeltaMovement();
                    p.setDeltaMovement(new Vec3(d.x, d.y <= 0 ? 0.5 : d.y + 0.5, d.z));
                }
                riptidePush(p, boost);
                RaceAbilities.setCooldown(p, 3);
                sync(p);
                startGlide(p);
                com.brightwebs.brightcore.fx.Fx.cone((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.FLAME, p.position().add(0, 0.8, 0), p.getLookAngle().reverse(), 0.3, 0.5, 18); com.brightwebs.brightcore.fx.Fx.shockwave((ServerLevel) p.level(), p.position().add(0, 0.5, 0), 1);
                p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.1f);
            }
            @Override public void tick(ServerPlayer p) { glide(p); }
        };
    }

    private static void startGlide(ServerPlayer p) { GLIDING.add(p.getUUID()); }

    /**
     * Tensura starts an elytra glide here. Without an elytra the game won't hold one, so the glide
     * is kept by hand: a slow fall that carries your speed until you land, swim or sneak.
     */
    private static void glide(ServerPlayer p) {
        if (!GLIDING.contains(p.getUUID())) return;
        if (p.onGround() || p.isInWater() || p.isShiftKeyDown() || p.getAbilities().flying) { GLIDING.remove(p.getUUID()); return; }
        Vec3 d = p.getDeltaMovement(), look = p.getLookAngle();
        double fall = Math.max(d.y, -0.12);
        Vec3 h = new Vec3(d.x, 0, d.z).scale(0.99).add(look.x * 0.02, 0, look.z * 0.02);
        p.setDeltaMovement(h.x, fall, h.z);
        p.resetFallDistance();
        sync(p);
    }

    // ---- charge jump (Slime) ---------------------------------------------

    private static RaceAbility slime(int maxCharge) {
        return new RaceAbility() {
            @Override public void press(ServerPlayer p) { }
            @Override public boolean held(ServerPlayer p, int t) {
                if (!p.onGround() || p.isInLiquid()) return false;
                int c = Math.min(t + 1, maxCharge);
                p.sendOverlayMessage(Component.translatable("jaycee.race.power_scale",
                        String.format("%.1f/%.1f", Math.round(c * 10 / 20.0) / 10.0, maxCharge / 20.0)).withStyle(ChatFormatting.DARK_GREEN));
                if (t % 4 == 0) com.brightwebs.brightcore.fx.Fx.ring((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.BUBBLE, p.position().add(0, 0.1, 0), 0.4 + c / (float) maxCharge * 0.6, 8, 0);
                return true;
            }
            @Override public void release(ServerPlayer p, int t) {
                if (!p.onGround() || p.isInLiquid() || t <= 0) return;
                float charge = 1.0f / maxCharge * Math.min(t, maxCharge);
                Vec3 look = p.getLookAngle().normalize();
                look = look.multiply(3.5 * charge, 3.3 * charge, 3.5 * charge).add(0, p.getDeltaMovement().y, 0);
                p.setDeltaMovement(look);
                sync(p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1, 0.8f);
            }
        };
    }

    // ---- blood drain / bat flight (Vampire) ---------------------------------------------

    static final RaceAbility VAMPIRE = new RaceAbility() {
        @Override public void press(ServerPlayer p) {
            if (StatusData.counter(p, "bat_mode") > 0) {
                toggleStored(p, "bat_mode", false);
                setFlight(p, false);
                p.sendOverlayMessage(Component.translatable("jaycee.race.bat_mode.off"));
                return;
            }
            LivingEntity target = target(p, 5);
            if (target != null && !bloodless(target) && (p.getHealth() < p.getMaxHealth() || p.getFoodData().needsFood() || p.isCreative())) {
                if (target.hurtServer((ServerLevel) p.level(), p.damageSources().playerAttack(p), 2.0f)) {
                    p.heal(2.0f);
                    p.getFoodData().eat(2, 0.0f);
                    com.brightwebs.brightcore.fx.Fx.beam((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.BLOOD, target.position().add(0, target.getBbHeight() / 2, 0), p.position().add(0, 1.2, 0), 0.3);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1, 0.7f);
                }
                return;
            }
            toggleStored(p, "bat_mode", true);
            setFlight(p, true);
            com.brightwebs.brightcore.fx.Fx.burst((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.VOID, p.position().add(0, 1, 0), 24, 0.25); com.brightwebs.brightcore.fx.Fx.cloud((ServerLevel) p.level(), com.brightwebs.brightcore.fx.BrightParticles.MIST, p.position().add(0, 1, 0), 10, 0.4);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1, 1);
            p.sendOverlayMessage(Component.translatable("jaycee.race.bat_mode.on"));
        }
        @Override public void tick(ServerPlayer p) { keepFlight(p, "bat_mode"); }
    };

    /** Undead and constructs have no blood (Tensura's RaceUtils.isBloodless). */
    private static boolean bloodless(LivingEntity e) {
        return e.isInvertedHealAndHarm()
                || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().contains("golem");
    }

    private static LivingEntity target(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        AABB box = p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1);
        LivingEntity best = null; double bd = range * range;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            var hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bd) { bd = eye.distanceToSqr(hit.get()); best = e; }
        }
        return best;
    }

    /** Leaving a flying race turns its flight off. */
    public static void raceChanged(Player p) {
        if (p instanceof ServerPlayer sp) {
            RaceAbility now = RaceAbilities.of(sp);
            if (now == FLIGHT || now == VAMPIRE) return;
            if (StatusData.counter(sp, "race_flight") > 0 || StatusData.counter(sp, "bat_mode") > 0) {
                toggleStored(sp, "race_flight", false);
                toggleStored(sp, "bat_mode", false);
                setFlight(sp, false);
            }
        }
    }
}

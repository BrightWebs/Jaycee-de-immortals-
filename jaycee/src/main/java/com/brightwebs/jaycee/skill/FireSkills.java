package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

// flame breath and fire ball like tensura
@EventBusSubscriber(modid = JayceeMod.ID)
public final class FireSkills {
    // flame breath (IntrinsicSkillConfig.FlameBreath)
    static final double BREATH_COST = 30;
    static final int BREATH_DAMAGE = 8, BREATH_DAMAGE_MASTERED = 16, BREATH_RANGE = 7;
    // fire ball (AspectualMagicConfig.FireBall)
    static final int CAST = 60, CAST_MASTERED = 20, CD = 3, CD_MASTERED = 1, BURN = 100;
    static final double COST = 30_000, COST_CHARGED = 50_000;
    static final float MAGIC = 100, FIRE = 30, RADIUS = 3, MAGIC_CHARGED = 250, FIRE_CHARGED = 60, RADIUS_CHARGED = 6, SPEED = 1.5f;

    private FireSkills() {}

    // flame breath: hold the key

    static boolean breathHeld(ServerPlayer p, Ability a, int held) {
        if (held % 20 == 0 && !Tk.spend(p, a, BREATH_COST)) return false;
        if (held > 0 && held % Tk.MASTERY_HOLD_TICK == 0) AbilityAPI.addMasteryPoint(p, a);
        ServerLevel l = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition().add(0, -0.15, 0), look = p.getLookAngle();
        for (int i = 1; i <= BREATH_RANGE; i++) {
            Vec3 at = eye.add(look.scale(i * 0.9));
            double s = 0.06 * i;
            if (i == 1) com.brightwebs.brightcore.fx.Fx.cone(l, com.brightwebs.brightcore.fx.BrightParticles.FLAME, at, look, 0.45, 0.22, 4);
            if (i > 3 && held % 3 == 0) com.brightwebs.brightcore.fx.Fx.move(l, com.brightwebs.brightcore.fx.BrightParticles.EMBER, at, look.scale(0.2));
        }
        if (held % 2 == 0) l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.75f, 1.0f);
        if (held % 5 == 0) {
            float dps = Tk.mastered(p, a) ? BREATH_DAMAGE_MASTERED : BREATH_DAMAGE;
            for (LivingEntity t : SkillHelper.around(p, BREATH_RANGE + 1)) {
                Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
                if (to.length() > BREATH_RANGE + 0.5 || to.normalize().dot(look) < 0.82) continue;
                t.igniteForTicks(60);
                t.hurtServer(l, l.damageSources().onFire(), dps / 4);
            }
        }
        return true;
    }

    // fire ball: hold to chant, let go to fire

    // ticks to chant: 3s (1s mastered), half with great sage, none with raphael
    static int castTicks(ServerPlayer p, Ability a) {
        if (SkillState.instantCast(p)) return 0;
        int base = Tk.mastered(p, a) ? CAST_MASTERED : CAST;
        return (int) Math.ceil(base / GreatSage.chantSpeed(p));
    }

    static boolean fireballHeld(ServerPlayer p, Ability a, int held) {
        int cast = castTicks(p, a), full = (int) Math.ceil(CAST / GreatSage.chantSpeed(p));
        ServerLevel l = (ServerLevel) p.level();
        if (cast > 1) {
            // the magic circle, a ring of fire turning at your feet
            double r = 0.75 + Math.min(1, held / (double) cast) * 0.5;
            Vec3 c = p.position().add(0, 0.05, 0);
            for (int i = 0; i < 3; i++) {
                double ang = (held * 0.25) + i * Math.PI * 2 / 3;
                com.brightwebs.brightcore.fx.Fx.move(l, com.brightwebs.brightcore.fx.BrightParticles.FLAME, new Vec3(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r), new Vec3(0, 0.02, 0));
            }
            if (held % 5 == 0) com.brightwebs.brightcore.fx.Fx.magicCircle(l, com.brightwebs.brightcore.fx.BrightParticles.EMBER, c, r, held);
        }
        boolean mastered = Tk.mastered(p, a);
        double shown = Math.min(held, mastered ? full : cast) / 20.0, of = (mastered && held >= cast ? full : cast) / 20.0;
        p.sendOverlayMessage(Component.translatable("jaycee.magic.cast_time", String.format(Locale.US, "%.1f", shown), String.format(Locale.US, "%.1f", of))
                .withStyle(held >= cast ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        return true;
    }

    static void fireballRelease(ServerPlayer p, Ability a, int held) {
        int cast = castTicks(p, a), full = (int) Math.ceil(CAST / GreatSage.chantSpeed(p));
        if (held < cast) {
            p.sendOverlayMessage(Component.translatable("jaycee.magic.cast_cancelled").withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean charged = Tk.mastered(p, a) && held >= full;
        if (!Tk.spend(p, a, charged ? COST_CHARGED : COST)) return;
        Vec3 dir = p.getLookAngle().normalize();
        BALLS.add(new Ball(p, (ServerLevel) p.level(), p.getEyePosition().add(0, -0.25, 0).add(dir), dir.scale(SPEED),
                charged ? FIRE_CHARGED : FIRE, charged ? MAGIC_CHARGED : MAGIC, charged ? RADIUS_CHARGED : RADIUS, charged ? 3 : 2));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1, charged ? 0.7f : 1.0f);
        AbilityAPI.addMasteryPoint(p, a);
        AbilityAPI.setCooldown(p, a, (Tk.mastered(p, a) ? CD_MASTERED : CD) * 20);
    }

    private static final class Ball {
        final ServerPlayer owner; final ServerLevel level; Vec3 pos; final Vec3 vel;
        final float fire, magic, radius, size; int age;
        Ball(ServerPlayer owner, ServerLevel level, Vec3 pos, Vec3 vel, float fire, float magic, float radius, float size) {
            this.owner = owner; this.level = level; this.pos = pos; this.vel = vel;
            this.fire = fire; this.magic = magic; this.radius = radius; this.size = size;
        }
    }
    private static final List<Ball> BALLS = new ArrayList<>();

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        Safe.run("FireSkills.balls", () -> {
            for (Iterator<Ball> it = BALLS.iterator(); it.hasNext(); ) {
                Ball b = it.next();
                if (b.age++ > 100 || b.owner.isRemoved()) { it.remove(); continue; }
                if (step(b)) it.remove();
            }
        });
    }

    // moves a fireball one tick. true when it hit something and blew up
    private static boolean step(Ball b) {
        Vec3 next = b.pos.add(b.vel);
        BlockHitResult hit = b.level.clip(new ClipContext(b.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, b.owner));
        Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : next;
        AABB sweep = new AABB(b.pos, end).inflate(b.size * 0.35);
        boolean touched = !b.level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != b.owner && e.isAlive()).isEmpty();
        // the ball itself
        double s = b.size * 0.18;
        com.brightwebs.brightcore.fx.Fx.sphere(b.level, com.brightwebs.brightcore.fx.BrightParticles.FLAME, end, s, (int) (5 * b.size));
        com.brightwebs.brightcore.fx.Fx.move(b.level, com.brightwebs.brightcore.fx.BrightParticles.EMBER, b.pos, b.vel.scale(-0.1));
        if (hit.getType() == HitResult.Type.BLOCK || touched) { burst(b, end); return true; }
        b.pos = next;
        return false;
    }

    private static void burst(Ball b, Vec3 at) {
        ServerLevel l = b.level;
        l.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        com.brightwebs.brightcore.fx.Fx.shockwave(l, at, b.radius / 3);
        com.brightwebs.brightcore.fx.Fx.burst(l, com.brightwebs.brightcore.fx.BrightParticles.FLAME, at, (int) (24 * b.radius), 0.12 * b.radius);
        com.brightwebs.brightcore.fx.Fx.burst(l, com.brightwebs.brightcore.fx.BrightParticles.EMBER, at, (int) (12 * b.radius), 0.18 * b.radius);
        l.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.1f);
        for (LivingEntity t : l.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(b.radius), e -> e != b.owner && e.isAlive())) {
            if (t.position().distanceTo(at) > b.radius + t.getBbWidth()) continue;
            t.hurtServer(l, b.owner.damageSources().playerAttack(b.owner), b.magic + b.fire);
            t.igniteForTicks(BURN);
        }
    }
}

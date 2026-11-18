package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Small tools every skill uses: aiming, area picks, lingering effects, mastery scaling. */
final class SkillHelper {
    private SkillHelper() {}

    /** The living thing the player is looking at within range, or null (with a hint). */
    static LivingEntity target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.5);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.4).clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        if (best == null) player.sendOverlayMessage(Component.translatable("jaycee.message.no_target"));
        return best;
    }

    /** The block the player is looking at, or null (with a hint). */
    static BlockHitResult block(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 0f, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) return b;
        player.sendOverlayMessage(Component.translatable("jaycee.message.look_at_block"));
        return null;
    }

    /** Everything alive near the player except the player. */
    static List<LivingEntity> around(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());
    }

    /** 1.0 at no mastery, up to 2.0 once mastered. */
    static float power(ServerPlayer player, Ability skill) {
        return 1f + (float) AbilityAPI.masteryFraction(player, skill);
    }

    /** Keeps an effect up without flicker. For passives and toggles, called from tick. */
    static void keep(LivingEntity e, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance now = e.getEffect(effect);
        if (now == null || now.getDuration() < 220 || now.getAmplifier() < amplifier) {
            e.addEffect(new MobEffectInstance(effect, 300, amplifier, true, false, true));
        }
    }

    static void give(LivingEntity e, Holder<MobEffect> effect, int seconds, int amplifier) {
        e.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier));
    }

    static void bolt(ServerLevel level, Vec3 at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("lightning_bolt"));
        Entity bolt = type == null ? null : type.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.setPos(at.x, at.y, at.z);
        level.addFreshEntity(bolt);
    }

    static void puff(ServerLevel level, ParticleOptions p, Entity at, int count, double spread) {
        level.sendParticles(p, at.getX(), at.getY() + at.getBbHeight() / 2, at.getZ(), count, spread, spread, spread, 0.05);
    }
}

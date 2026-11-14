package com.brightwebs.jaycee.ability;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Five test abilities so the ability bar and menu have something real in them.
 * They'll be replaced by the real Ichor abilities as we design them step by step.
 */
public final class TestAbilities {
    private static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(AbilityRegistry.REGISTRY, JayceeMod.ID);

    /** Zeus: lightning where you look. */
    public static final DeferredHolder<Ability, Ability> THUNDERBOLT = ABILITIES.register("thunderbolt", () ->
            new Ability(new Ability.Properties().manaCost(30).cooldownSeconds(3).category(AbilityCategory.OFFENSE)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level) {
                    BlockHitResult hit = lookedAtBlock(player);
                    if (hit == null) return false;
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt == null) return false;
                    Vec3 at = hit.getLocation();
                    bolt.setPos(at.x, at.y, at.z);
                    level.addFreshEntity(bolt);
                    return true;
                }
            });

    /** Hermes: a burst of speed where you're facing. */
    public static final DeferredHolder<Ability, Ability> HERMES_DASH = ABILITIES.register("hermes_dash", () ->
            new Ability(new Ability.Properties().manaCost(12).cooldownSeconds(1.5f).category(AbilityCategory.MOVEMENT)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level) {
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(player.getDeltaMovement().add(look.x * 1.3, Math.max(0.2, look.y * 0.55), look.z * 1.3));
                    player.hurtMarked = true;
                    return true;
                }
            });

    /** Apollo: heal four hearts. */
    public static final DeferredHolder<Ability, Ability> APOLLOS_MENDING = ABILITIES.register("apollos_mending", () ->
            new Ability(new Ability.Properties().manaCost(20).cooldownSeconds(4).category(AbilityCategory.SUPPORT)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level) {
                    if (player.getHealth() >= player.getMaxHealth()) {
                        player.sendOverlayMessage(Component.translatable("jaycee.message.full_health"));
                        return false;
                    }
                    player.heal(8.0f);
                    return true;
                }
            });

    /** Hecate: step through shadow to the block you're looking at. */
    public static final DeferredHolder<Ability, Ability> SHADOW_STEP = ABILITIES.register("shadow_step", () ->
            new Ability(new Ability.Properties().manaCost(22).cooldownSeconds(2.5f).category(AbilityCategory.REALITY)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level) {
                    BlockHitResult hit = lookedAtBlock(player);
                    if (hit == null) return false;
                    BlockPos target = hit.getBlockPos().relative(hit.getDirection());
                    if (!level.getBlockState(target).getCollisionShape(level, target).isEmpty()
                            || !level.getBlockState(target.above()).getCollisionShape(level, target.above()).isEmpty()) {
                        player.sendOverlayMessage(Component.translatable("jaycee.message.no_room"));
                        return false;
                    }
                    player.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                    return true;
                }
            });

    /** Athena: the Aegis, a shield of absorption hearts. */
    public static final DeferredHolder<Ability, Ability> AEGIS = ABILITIES.register("aegis", () ->
            new Ability(new Ability.Properties().manaCost(18).cooldownSeconds(1.5f).category(AbilityCategory.DEFENSE)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 15, 1));
                    return true;
                }
            });

    private TestAbilities() {}

    /** The block the player is looking at within 24 blocks, or null (with a hint) if none. */
    private static BlockHitResult lookedAtBlock(ServerPlayer player) {
        HitResult hit = player.pick(24.0, 0f, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) return block;
        player.sendOverlayMessage(Component.translatable("jaycee.message.look_at_block"));
        return null;
    }

    public static void init(IEventBus modBus) {
        ABILITIES.register(modBus);
    }
}

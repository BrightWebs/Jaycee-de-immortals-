package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.api.skill.DamageKind;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Predator's Predation: devour a weakened creature (its drops go to the Stomach) or a block. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Predation {
    private static final Set<UUID> BEING_DEVOURED = new HashSet<>();

    private Predation() {}

    static boolean canDevour(ServerPlayer p, LivingEntity t) {
        if (p.isCreative()) return true;
        return t.getHealth() <= t.getMaxHealth() * 0.3f || EnergyAPI.get(t).ep() < EnergyAPI.get(p).ep() * 0.5;
    }

    static boolean devour(ServerPlayer p, ServerLevel level, Ability predator) {
        LivingEntity t = SkillHelper.targetQuiet(p, 6);
        if (t != null) {
            if (!canDevour(p, t)) {
                p.sendOverlayMessage(Component.translatable("jaycee.skill.predation.resisted"));
                return false;
            }
            boolean flame = t.fireImmune();
            BEING_DEVOURED.add(t.getUUID());
            t.hurtServer(level, p.damageSources().playerAttack(p), Float.MAX_VALUE);
            BEING_DEVOURED.remove(t.getUUID());
            if (!t.isAlive()) {
                EnergyAPI.absorbKill(p, t, Alignment.NEUTRAL); // the Predator keeps an extra share
                SkillHelper.puff(level, ParticleTypes.SQUID_INK, t, 40, 0.5);
                level.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1, 0.5f);
                p.sendOverlayMessage(Component.translatable("jaycee.skill.predation.done", t.getDisplayName()));
                if (flame) {
                    Ability res = BrightSkills.resistance(DamageKind.FLAME);
                    if (res != null && AbilityAPI.learn(p, res)) p.sendSystemMessage(Component.translatable("brightcore.message.acquired", res.name()));
                }
            }
            return true;
        }
        HitResult hit = p.pick(6, 0, false);
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = level.getBlockState(pos);
            float hard = state.getDestroySpeed(level, pos);
            if (hard < 0 || hard > 50 || state.isAir() || !p.mayInteract(level, pos)) {
                p.sendOverlayMessage(Component.translatable("jaycee.skill.predation.resisted"));
                return false;
            }
            ItemStack item = new ItemStack(state.getBlock().asItem());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (!item.isEmpty()) Stomach.store(p, item);
            level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.01);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
            return true;
        }
        p.sendOverlayMessage(Component.translatable("jaycee.message.no_target"));
        return false;
    }

    /** A devoured creature's drops go into the Stomach instead of the ground. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!BEING_DEVOURED.contains(event.getEntity().getUUID())) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        for (ItemEntity drop : event.getDrops()) Stomach.store(p, drop.getItem().copy());
        event.getDrops().clear();
    }
}

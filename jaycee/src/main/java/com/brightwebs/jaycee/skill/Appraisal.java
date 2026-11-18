package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.RaceAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Analytical Appraisal (Great Sage, Raphael) and Analysis (Predator). */
final class Appraisal {
    private Appraisal() {}

    private static Component head(String key) {
        return Component.literal("《 ").withStyle(ChatFormatting.AQUA).append(Component.translatable(key)).append(" 》");
    }

    static boolean appraise(ServerPlayer p, boolean detailed) {
        LivingEntity t = SkillHelper.targetQuiet(p, 32);
        if (t != null) {
            double ep = EnergyAPI.get(t).ep(), mine = EnergyAPI.get(p).ep();
            p.sendSystemMessage(head("jaycee.appraisal.title"));
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.name", t.getDisplayName()));
            if (t instanceof Player other) {
                RaceAPI.race(other).ifPresent(r -> p.sendSystemMessage(Component.translatable("jaycee.appraisal.race", r.name())));
            }
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.health", Math.round(t.getHealth()), Math.round(t.getMaxHealth())));
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.ep", EnergyAPI.format(ep)));
            String threat = ep > mine * 1.5 ? "stronger" : ep < mine * 0.5 ? "weaker" : "equal";
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.threat." + threat));
            if (detailed) {
                boolean devour = Predation.canDevour(p, t);
                p.sendSystemMessage(Component.translatable(devour ? "jaycee.appraisal.devour.yes" : "jaycee.appraisal.devour.no"));
                if (t.fireImmune()) p.sendSystemMessage(Component.translatable("jaycee.appraisal.grants_flame"));
                if (t instanceof Player other) {
                    StringBuilder b = new StringBuilder();
                    for (Ability a : AbilityAPI.owned(other)) { if (b.length() > 0) b.append(", "); b.append(a.name().getString()); }
                    if (b.length() > 0) p.sendSystemMessage(Component.translatable("jaycee.appraisal.skills", b.toString()));
                }
            }
            return true;
        }
        HitResult hit = p.pick(16, 0, false);
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = p.level().getBlockState(pos);
            p.sendSystemMessage(head("jaycee.appraisal.title"));
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.block", state.getBlock().getName()));
            p.sendSystemMessage(Component.translatable("jaycee.appraisal.hardness", String.format("%.1f", state.getDestroySpeed(p.level(), pos))));
            return true;
        }
        p.sendOverlayMessage(Component.translatable("jaycee.message.no_target"));
        return false;
    }
}

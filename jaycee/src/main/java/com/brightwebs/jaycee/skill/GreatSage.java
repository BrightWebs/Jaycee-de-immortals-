package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityEvent;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

/**
 * Great Sage, as in Tensura: Reincarnated.
 *  Always on while owned: chant speed x2, Thought Acceleration (+0.01 speed, +0.2 attack speed)
 *  and +9 mastery per point earned by any skill.
 *  Mode 1 Analytical Appraisal: press to switch the scan on/off; Shift+press cycles entity / block / both.
 *  Mode 2 Analysis: copy a Common, Extra or Intrinsic skill from a target within 10 blocks
 *         (25% chance, 50% mastered; 10 s cooldown on success, 5 s on failure).
 *  Mode 3 Refine: opens the Sage's storage, where smeltable items are refined over time.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class GreatSage {
    // Tensura defaults (UniqueSkillConfig.GreatSage).
    static final double CHANT_SPEED = 2.0, LEARNING_POINT = 9, MASTERY_POINT = 9;
    static final int ANALYSIS_LEVEL = 8, ANALYSIS_LEVEL_MASTERED = 18, RADIUS = 15, RADIUS_MASTERED = 25;
    static final float COPY_RANGE = 10, COPY_CHANCE = 25, COPY_CHANCE_MASTERED = 50;
    static final int COPY_CD_SUCCESS = 10, COPY_CD_FAIL = 5;
    // ThoughtAcceleration (ExtraSkillConfig).
    static final double TA_SPEED = 0.01, TA_SPEED_MASTERED = 0.02, TA_ATTACK = 0.2, TA_ATTACK_MASTERED = 0.4;
    private static final Identifier MOD = Identifier.fromNamespaceAndPath(JayceeMod.ID, "great_sage");

    private GreatSage() {}

    static Ability sage() { return AbilityRegistry.get(JayceeSkills.id("great_sage")); }
    static Ability raphael() { return AbilityRegistry.get(JayceeSkills.id("raphael")); }

    /** Great Sage or Raphael owned: the analytical bonuses apply. */
    static boolean hasSage(Player p) {
        Ability s = sage(), r = raphael();
        return (s != null && AbilityAPI.has(p, s)) || (r != null && AbilityAPI.has(p, r));
    }

    /** How much faster spells chant (Great Sage doubles it). */
    static double chantSpeed(Player p) { return hasSage(p) ? CHANT_SPEED : 1.0; }

    // ---- passive ----------------------------------------------------------------

    static void tick(ServerPlayer p, Ability a) {
        if (p.tickCount % 20 == 0) {
            boolean m = Tk.mastered(p, a);
            modifier(p.getAttribute(Attributes.MOVEMENT_SPEED), m ? TA_SPEED_MASTERED : TA_SPEED);
            modifier(p.getAttribute(Attributes.ATTACK_SPEED), m ? TA_ATTACK_MASTERED : TA_ATTACK);
        }
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
        if (p.tickCount % 10 == 0 && StatusData.counter(p, "sage_scan") > 0) scan(p, a);
        Refining.tick(p);
    }

    private static void modifier(AttributeInstance inst, double amount) {
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(MOD);
        if (cur != null && cur.amount() == amount) return;
        if (cur != null) inst.removeModifier(MOD);
        inst.addTransientModifier(new AttributeModifier(MOD, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    static void forgotten(ServerPlayer p) {
        AttributeInstance s = p.getAttribute(Attributes.MOVEMENT_SPEED), at = p.getAttribute(Attributes.ATTACK_SPEED);
        if (s != null) s.removeModifier(MOD);
        if (at != null) at.removeModifier(MOD);
        StatusData.addCounter(p, "sage_scan", -StatusData.counter(p, "sage_scan"));
    }

    /** +9 mastery per point, for every skill, while Great Sage is owned. */
    @SubscribeEvent
    public static void onMastery(AbilityEvent.MasteryGain event) {
        Safe.run("GreatSage.onMastery", () -> {
            if (hasSage(event.getPlayer())) event.setPoints(event.getPoints() + MASTERY_POINT);
        });
    }

    // ---- mode 1: Analytical Appraisal ----------------------------------------

    static boolean appraisal(ServerPlayer p, Ability a) {
        if (p.isShiftKeyDown()) {
            int next = ((int) StatusData.counter(p, "sage_scan_mode") + 1) % 3; // 0 both, 1 entity, 2 block
            StatusData.addCounter(p, "sage_scan_mode", next - StatusData.counter(p, "sage_scan_mode"));
            p.sendOverlayMessage(Component.translatable("jaycee.skill.analytical.mode." + (next == 1 ? "entity" : next == 2 ? "block" : "both"))
                    .withStyle(ChatFormatting.DARK_AQUA));
        } else {
            boolean on = StatusData.counter(p, "sage_scan") <= 0;
            StatusData.addCounter(p, "sage_scan", (on ? 1 : 0) - StatusData.counter(p, "sage_scan"));
            p.sendOverlayMessage(Component.translatable(on ? "jaycee.skill.analytical.on" : "jaycee.skill.analytical.off").withStyle(ChatFormatting.DARK_AQUA));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 1.4f);
        return true;
    }

    /** While the scan is on, whatever you look at is analysed on screen. Mastery widens and deepens it. */
    private static void scan(ServerPlayer p, Ability a) {
        boolean m = Tk.mastered(p, a) || hasRaphael(p);
        int radius = m ? RADIUS_MASTERED : RADIUS, level = m ? ANALYSIS_LEVEL_MASTERED : ANALYSIS_LEVEL;
        int mode = (int) StatusData.counter(p, "sage_scan_mode");
        if (mode != 2) {
            LivingEntity t = SkillHelper.targetQuiet(p, radius);
            if (t != null) { p.sendOverlayMessage(describe(p, t, level)); return; }
        }
        if (mode != 1) {
            HitResult hit = p.pick(radius, 0, false);
            if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = bh.getBlockPos();
                BlockState st = p.level().getBlockState(pos);
                p.sendOverlayMessage(Component.translatable("jaycee.skill.analytical.block", st.getBlock().getName(),
                        String.format("%.1f", st.getDestroySpeed(p.level(), pos))).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private static boolean hasRaphael(Player p) { Ability r = raphael(); return r != null && AbilityAPI.has(p, r); }

    private static Component describe(ServerPlayer p, LivingEntity t, int level) {
        double ep = EnergyAPI.ep(t), mine = EnergyAPI.ep(p);
        String threat = ep > mine * 1.5 ? "stronger" : ep < mine * 0.5 ? "weaker" : "equal";
        Component race = t instanceof Player other ? RaceAPI.race(other).map(r -> r.name()).orElse(Component.empty()) : Component.empty();
        String skills = "";
        if (level >= ANALYSIS_LEVEL_MASTERED && t instanceof Player other) {
            List<String> names = new ArrayList<>();
            for (Ability s : AbilityAPI.owned(other)) names.add(s.name().getString());
            skills = String.join(", ", names);
        }
        return Component.translatable("jaycee.skill.analytical.entity", t.getDisplayName(), race,
                Math.round(t.getHealth()), Math.round(t.getMaxHealth()), EnergyAPI.format(ep),
                Component.translatable("jaycee.appraisal.threat." + threat), skills).withStyle(ChatFormatting.AQUA);
    }

    // ---- mode 2: Analysis (copy) ---------------------------------------------

    /** Tensura copies Intrinsic, Common and Extra skills only. */
    static boolean copiable(Ability s) {
        SkillType t = s.type();
        return t == SkillType.INTRINSIC || t == SkillType.COMMON || t == SkillType.EXTRA;
    }

    static boolean analysis(ServerPlayer p, Ability a) {
        LivingEntity target = SkillHelper.targetQuiet(p, COPY_RANGE);
        ServerLevel level = (ServerLevel) p.level();
        if (target == null || !target.isAlive()) {
            Tk.fail(p, "jaycee.targeting.not_targeted");
            level.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.6f);
            return false;
        }
        if (target instanceof Player other && other.getAbilities().invulnerable) {
            Tk.fail(p, "jaycee.targeting.not_allowed");
            return false;
        }
        double chance = Tk.mastered(p, a) ? COPY_CHANCE_MASTERED : COPY_CHANCE;
        boolean ok = false;
        if (p.getRandom().nextInt(100) <= chance) {
            List<Ability> pool = new ArrayList<>();
            if (target instanceof Player other) {
                for (Ability s : AbilityAPI.owned(other)) if (copiable(s) && !AbilityAPI.has(p, s)) pool.add(s);
            }
            if (pool.isEmpty()) {
                Tk.fail(p, "jaycee.skill.copy.empty");
            } else {
                Ability got = pool.get(p.getRandom().nextInt(pool.size()));
                AbilityAPI.addMasteryPoint(p, a);
                if (AbilityAPI.learn(p, got)) {
                    ok = true;
                    p.sendSystemMessage(Component.translatable("brightcore.message.acquired", got.name()));
                } else {
                    Tk.fail(p, "jaycee.skill.copy.failed", got.name());
                }
            }
        } else {
            Tk.fail(p, "jaycee.skill.activation_failed");
        }
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 40, 0.4, 0.6, 0.4, 0.8);
        level.playSound(null, p.blockPosition(), ok ? SoundEvents.PLAYER_LEVELUP : SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, ok ? 1.4f : 1.6f);
        AbilityAPI.setCooldown(p, a, (ok ? COPY_CD_SUCCESS : COPY_CD_FAIL) * 20);
        return true;
    }
}

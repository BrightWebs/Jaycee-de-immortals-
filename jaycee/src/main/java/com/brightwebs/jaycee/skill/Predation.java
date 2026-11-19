package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.api.skill.DamageKind;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Predator, as in Tensura: Reincarnated.
 *  Predation: breathe out a devouring mist for 3 s (range 3, 10 damage per bite, drains 100 magicules
 *  and takes a skill per bite). A kill devours every skill and 30% of the victim's EP into your
 *  maximum. Shift + press cycles what the mist also eats: nothing / blocks / fluids / everything.
 *  Mastered, the mist swallows projectiles too. Cooldown 5 s (3 s mastered).
 *  Isolation: empty hand clears harmful effects from your target (or yourself) for 200 magicules;
 *  holding an item dissolves it into magicules and health.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Predation {
    // Tensura defaults (UniqueSkillConfig.Predator).
    static final float RANGE = 3, DAMAGE = 10, EP_DRAIN = 100, SKILL_CHANCE = 10, EP_STEAL = 0.3f;
    static final double ISOLATION_COST = 200, DISSOLVE_MAGICULE = 2.0, DISSOLVE_HEALTH = 2.0;
    static final int ISOLATION_CD = 5, ISOLATION_CD_MASTERED = 3, MIST_LIFE = 60;

    private static final class Mist {
        final ServerPlayer owner; final Ability skill; final int blockMode; final boolean eatsProjectiles; int age;
        Mist(ServerPlayer owner, Ability skill, int blockMode, boolean eatsProjectiles) {
            this.owner = owner; this.skill = skill; this.blockMode = blockMode; this.eatsProjectiles = eatsProjectiles;
        }
    }
    private static final List<Mist> MISTS = new ArrayList<>();
    private static final Set<UUID> BEING_DEVOURED = new HashSet<>();
    private static final Map<UUID, UUID> LAST_BITTEN_BY = new HashMap<>();

    private Predation() {}

    // ---- Predation -----------------------------------------------------------

    static boolean predation(ServerPlayer p, Ability a) {
        if (p.isShiftKeyDown()) {
            int mode = (int) StatusData.counter(p, "predator_block_mode");
            int next = mode == 1 ? 2 : mode == 2 ? 3 : mode == 3 ? 4 : 1;
            StatusData.addCounter(p, "predator_block_mode", next - mode);
            String key = next == 1 ? "none" : next == 2 ? "blocks" : next == 3 ? "fluid" : "all";
            p.sendOverlayMessage(Component.translatable("jaycee.skill.predator.block_mode." + key, a.name()).withStyle(ChatFormatting.DARK_AQUA));
            return true;
        }
        MISTS.add(new Mist(p, a, (int) StatusData.counter(p, "predator_block_mode"), Tk.mastered(p, a)));
        p.swing(InteractionHand.MAIN_HAND, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.5f, 1.6f);
        AbilityAPI.addMasteryPoint(p, a);
        AbilityAPI.setCooldown(p, a, (Tk.mastered(p, a) ? 3 : 5) * 20);
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        Safe.run("Predation.onTick", () -> {
            for (Iterator<Mist> it = MISTS.iterator(); it.hasNext(); ) {
                Mist m = it.next();
                if (m.age++ >= MIST_LIFE || !m.owner.isAlive() || m.owner.isRemoved()) { it.remove(); continue; }
                tickMist(m);
            }
        });
    }

    private static void tickMist(Mist m) {
        ServerPlayer p = m.owner;
        ServerLevel level = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        // the mist: a dark cloud rolling out in front of you
        for (int i = 0; i < 3; i++) {
            double d = 0.8 + p.getRandom().nextDouble() * RANGE;
            Vec3 at = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y - 0.2, at.z, 2, 0.25 * d, 0.2 * d, 0.25 * d, 0.005);
        }
        if (m.age % 6 == 0) level.sendParticles(ParticleTypes.SCULK_SOUL, eye.x + look.x * 2, eye.y + look.y * 2 - 0.2, eye.z + look.z * 2, 2, 0.6, 0.4, 0.6, 0.01);
        AABB box = p.getBoundingBox().inflate(RANGE + 1);
        if (m.age % 10 == 0) {
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
                Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
                if (to.length() > RANGE + t.getBbWidth() || to.normalize().dot(look) < 0.4) continue;
                bite(p, m.skill, level, t);
            }
        }
        if (m.eatsProjectiles) {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() != p)) {
                level.sendParticles(ParticleTypes.SQUID_INK, pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
                pr.discard();
            }
        }
        if (m.age % 5 == 0 && m.blockMode >= 2) eatBlock(p, level, m.blockMode);
    }

    /** One bite: damage, drain magicules, and take one of the target's skills. */
    private static void bite(ServerPlayer p, Ability predator, ServerLevel level, LivingEntity t) {
        if (t instanceof Player other && other.getAbilities().invulnerable) return;
        double drained = Tk.drainMana(t, EP_DRAIN);
        if (drained > 0) Tk.gainMana(p, drained, false);
        if (t instanceof Player other && p.getRandom().nextFloat() < SKILL_CHANCE / 10f) devourRandomSkill(p, other);
        LAST_BITTEN_BY.put(t.getUUID(), p.getUUID());
        BEING_DEVOURED.add(t.getUUID());
        t.hurtServer(level, p.damageSources().playerAttack(p), DAMAGE);
        BEING_DEVOURED.remove(t.getUUID());
        level.sendParticles(ParticleTypes.SQUID_INK, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
    }

    private static void devourRandomSkill(ServerPlayer p, Player target) {
        List<Ability> pool = new ArrayList<>();
        for (Ability s : AbilityAPI.owned(target)) if (devourable(s) && !AbilityAPI.has(p, s)) pool.add(s);
        if (pool.isEmpty()) return;
        Ability got = pool.get(p.getRandom().nextInt(pool.size()));
        if (AbilityAPI.learn(p, got)) {
            p.sendSystemMessage(Component.translatable("brightcore.message.acquired", got.name()));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1);
        }
    }

    /** Uniques and Ultimates can't be devoured; everything else can (magic only some of the time). */
    private static boolean devourable(Ability s) {
        return switch (s.type()) {
            case UNIQUE, ULTIMATE -> false;
            default -> true;
        };
    }

    /** A kill by the mist devours the victim: all its skills, 30% of its EP into your maximum, its drops into the Stomach. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Safe.run("Predation.onDeath", () -> {
            LivingEntity t = event.getEntity();
            if (!BEING_DEVOURED.contains(t.getUUID()) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
            if (t instanceof Player other) {
                for (Ability s : AbilityAPI.owned(other)) {
                    if (devourable(s) && (!s.isMagic() || p.getRandom().nextFloat() <= 0.5f) && AbilityAPI.learn(p, s)) {
                        p.sendSystemMessage(Component.translatable("brightcore.message.acquired", s.name()));
                    }
                }
            } else if (t.fireImmune()) { // creatures born of fire carry Flame Attack Resistance
                Ability res = BrightSkills.resistance(DamageKind.FLAME);
                if (res != null && AbilityAPI.learn(p, res)) p.sendSystemMessage(Component.translatable("brightcore.message.acquired", res.name()));
            }
            double ep = EnergyAPI.ep(t) * EP_STEAL;
            if (ep > 0) Tk.gainMana(p, ep, true);
            ServerLevel level = (ServerLevel) p.level();
            level.sendParticles(ParticleTypes.SQUID_INK, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 40, 0.5, 0.5, 0.5, 0.05);
            level.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1, 0.5f);
            p.sendOverlayMessage(Component.translatable("jaycee.skill.predation.done", t.getDisplayName()));
        });
    }

    /** Drops of a devoured creature go into the Stomach. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Safe.run("Predation.onDrops", () -> {
            if (!BEING_DEVOURED.contains(event.getEntity().getUUID())) return;
            if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
            for (ItemEntity drop : event.getDrops()) Stomach.store(p, drop.getItem().copy());
            event.getDrops().clear();
        });
    }

    /** Blocks (mode 2/4) go into the Stomach; fluid sources (mode 3/4) are drunk. */
    private static void eatBlock(ServerPlayer p, ServerLevel level, int blockMode) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(RANGE + 1));
        boolean fluids = blockMode == 3 || blockMode == 4, blocks = blockMode == 2 || blockMode == 4;
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                fluids ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!p.mayInteract(level, pos)) return;
        BlockState st = level.getBlockState(pos);
        FluidState fl = st.getFluidState();
        if (fluids && !fl.isEmpty() && fl.isSource()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            StatusData.addCounter(p, "predator_fluid", 1);
        } else if (blocks && !st.isAir() && fl.isEmpty()) {
            float hard = st.getDestroySpeed(level, pos);
            if (hard < 0) return;
            ItemStack item = new ItemStack(st.getBlock().asItem());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (!item.isEmpty()) Stomach.store(p, item);
        } else return;
        level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.01);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.6f, 0.7f);
    }

    // ---- Isolation -----------------------------------------------------------

    static boolean isolation(ServerPlayer p, Ability a) {
        ItemStack held = p.getMainHandItem();
        ServerLevel level = (ServerLevel) p.level();
        if (held.isEmpty()) {
            LivingEntity target = SkillHelper.targetQuiet(p, 5);
            LivingEntity who = target != null ? target : p;
            List<Holder<MobEffect>> bad = new ArrayList<>();
            for (MobEffectInstance e : who.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e.getEffect());
            if (bad.isEmpty()) return false;
            int removed = 0;
            for (Holder<MobEffect> e : bad) {
                if (!Tk.spend(p, a, ISOLATION_COST)) break;
                who.removeEffect(e);
                removed++;
            }
            if (removed == 0) return false;
            level.sendParticles(ParticleTypes.SQUID_INK, who.getX(), who.getY() + who.getBbHeight() * 0.66, who.getZ(), 30, who.getBbWidth(), 0.3, who.getBbWidth(), 0.02);
            level.sendParticles(ParticleTypes.SMOKE, who.getX(), who.getY() + who.getBbHeight() * 0.33, who.getZ(), 30, who.getBbWidth() * 1.2, 0.2, who.getBbWidth() * 1.2, 0.02);
            p.swing(InteractionHand.MAIN_HAND, true);
            AbilityAPI.setCooldown(p, a, (Tk.mastered(p, a) ? ISOLATION_CD_MASTERED : ISOLATION_CD) * 20);
            level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1, 0.6f);
            return true;
        }
        // Dissolving: what the item is worth in magicules and health.
        FoodProperties food = held.get(DataComponents.FOOD);
        if (food == null) {
            Tk.fail(p, "jaycee.skill.activation_failed.item");
            return false;
        }
        Tk.gainMana(p, food.nutrition() * 10 * DISSOLVE_MAGICULE, false);
        p.heal((float) (food.nutrition() * DISSOLVE_HEALTH / 2));
        AbilityAPI.addMasteryPoint(p, a);
        held.shrink(1);
        p.swing(InteractionHand.MAIN_HAND, true);
        level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    /** For Appraisal: whether this target is worth devouring right now. */
    static boolean canDevour(ServerPlayer p, LivingEntity t) {
        return p.isCreative() || t.getHealth() <= t.getMaxHealth() * 0.3f || EnergyAPI.ep(t) < EnergyAPI.ep(p) * 0.5;
    }
}

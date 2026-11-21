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

// predator like tensura
//   predation: breathe out a mist that eats stuff for 3s (range 3, 10 damage a bite, drains 100
//   magicules and takes a skill each bite). a kill eats every skill it had and 30% of its EP into
//   your max. shift + press changes what else the mist eats: nothing / blocks / fluids / everything
//   mastered, it eats projectiles too. cooldown 5s (3s mastered)
//   isolation: empty hand clears bad effects off your target (or you) for 200 magicules,
//   holding an item dissolves it into magicules and health
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Predation {
    // tensura defaults (UniqueSkillConfig.Predator)
    static final float RANGE = 3, DAMAGE = 10, EP_DRAIN = 100, SKILL_CHANCE = 10, EP_STEAL = 0.3f;
    static final double ISOLATION_COST = 200, DISSOLVE_MAGICULE = 2.0, DISSOLVE_HEALTH = 2.0;
    static final int ISOLATION_CD = 5, ISOLATION_CD_MASTERED = 3, MIST_LIFE = 60;

    private static final class Mist {
        final ServerPlayer owner; final Ability skill; final int blockMode; final boolean eatsProjectiles; int age;
        float range;  // grows to RANGE, stays, then shrinks (tensura's PredatorMistProjectile)
        Mist(ServerPlayer owner, Ability skill, int blockMode, boolean eatsProjectiles) {
            this.owner = owner; this.skill = skill; this.blockMode = blockMode; this.eatsProjectiles = eatsProjectiles;
        }
    }
    private static final List<Mist> MISTS = new ArrayList<>();
    private static final Set<UUID> BEING_DEVOURED = new HashSet<>();
    private static final Map<UUID, UUID> LAST_BITTEN_BY = new HashMap<>();

    private Predation() {}

    // predation

    static boolean predation(ServerPlayer p, Ability a) {
        if (p.isShiftKeyDown()) {
            int mode = (int) StatusData.counter(p, "predator_block_mode");
            int next = mode == 1 ? 2 : mode == 2 ? 3 : mode == 3 ? 4 : 1;
            StatusData.addCounter(p, "predator_block_mode", next - mode);
            String key = next == 1 ? "none" : next == 2 ? "blocks" : next == 3 ? "fluid" : "all";
            p.sendOverlayMessage(Component.translatable("jaycee.skill.predator.block_mode." + key, a.name()).withStyle(ChatFormatting.DARK_AQUA));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 1);
            return true;
        }
        MISTS.add(new Mist(p, a, (int) StatusData.counter(p, "predator_block_mode"), Tk.mastered(p, a)));
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

    // tensura's mist: grows 0.25 blocks a tick out to 3 blocks and eats stuff for the first 31 ticks
    // (mobs, xp orbs, projectiles when mastered, and blocks or fluids depending on block mode), then
    // shrinks away. mobs always get eaten no matter the block mode
    // tensura draws the mist as a black vortex: rings of black streaks swirling along it and
    // flowing back into you, with a spinning seven armed swirl at the front
    private static void mistVisual(ServerLevel level, Mist m, Vec3 eye, Vec3 look) {
        var type = com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST;
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = look.cross(up).normalize(), v = u.cross(look).normalize();
        Vec3 base = eye.add(0, -0.25, 0);
        double phase = m.age * 0.35, mouth = 0.3 + m.range * 0.5;
        // the swirl at the front, seven curved arms
        Vec3 front = base.add(look.scale(m.range));
        for (int arm = 0; arm < 7; arm++) {
            for (int s = 0; s < 4; s++) {
                double t = (s + 1) / 4.0, a = phase + arm * (Math.PI * 2 / 7) + t * 1.3, r = mouth * t;
                Vec3 off = u.scale(Math.cos(a) * r).add(v.scale(Math.sin(a) * r));
                Vec3 tangent = u.scale(-Math.sin(a)).add(v.scale(Math.cos(a)));
                com.brightwebs.brightcore.fx.Fx.move(level, type, front.add(off), tangent.scale(0.06).add(look.scale(-0.04)));
            }
        }
        // the body: rings swirling along it, pulled back toward you
        for (double d = 1.2; d < m.range; d += 0.55) {
            double r = 0.15 + (d / Math.max(1, m.range)) * mouth;
            for (int k = 0; k < 4; k++) {
                double a = phase * 1.4 + d * 1.7 + k * (Math.PI / 2);
                Vec3 off = u.scale(Math.cos(a) * r).add(v.scale(Math.sin(a) * r));
                Vec3 tangent = u.scale(-Math.sin(a)).add(v.scale(Math.cos(a)));
                com.brightwebs.brightcore.fx.Fx.move(level, type, base.add(look.scale(d)).add(off), tangent.scale(0.05).add(look.scale(-0.07)));
            }
        }
    }

    private static void tickMist(Mist m) {
        ServerPlayer p = m.owner;
        ServerLevel level = (ServerLevel) p.level();
        boolean active = MIST_LIFE - m.age >= 29;
        if (!active && m.range > 1) m.range = Math.max(1, m.range - RANGE / 30f);
        else if (m.range < RANGE) m.range = Math.min(RANGE, m.range + 0.25f);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        mistVisual(level, m, eye, look);
        if (!active) return;
        AABB box = p.getBoundingBox().inflate(m.range + 1);
        if (m.age % 10 == 0) {
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
                if (inMist(eye, look, m.range, t.position().add(0, t.getBbHeight() / 2, 0), t.getBbWidth())) bite(p, m.skill, level, t);
            }
        }
        for (net.minecraft.world.entity.ExperienceOrb orb : level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box, e -> true)) {
            if (inMist(eye, look, m.range, orb.position(), 0.5f)) orb.playerTouch(p);
        }
        if (m.eatsProjectiles) {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() != p)) {
                if (!inMist(eye, look, m.range, pr.position(), 0.5f)) continue;
                com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, pr.position(), 6, 0.05);
                pr.discard();
            }
        }
        if (m.blockMode >= 2) {
            float cone = (float) (Math.PI / 6);
            for (int i = 0; i < m.range; i++) {
                Vec3 ray = look.normalize()
                        .xRot(p.getRandom().nextFloat() * cone * 2 - cone)
                        .yRot(p.getRandom().nextFloat() * cone * 2 - cone)
                        .zRot(p.getRandom().nextFloat() * cone * 2 - cone);
                eatBlock(p, level, m.blockMode, ray, m.range);
            }
        }
    }

    private static boolean inMist(Vec3 eye, Vec3 look, float range, Vec3 at, float width) {
        Vec3 to = at.subtract(eye);
        return to.length() <= range + width && to.normalize().dot(look) >= 0.4;
    }

    // one bite: damage, drain magicules, take one of its skills
    private static void bite(ServerPlayer p, Ability predator, ServerLevel level, LivingEntity t) {
        if (t instanceof Player other && other.getAbilities().invulnerable) return;
        double drained = Tk.drainMana(t, EP_DRAIN);
        if (drained > 0) Tk.gainMana(p, drained, false);
        if (t instanceof Player other && p.getRandom().nextFloat() < SKILL_CHANCE / 10f) devourRandomSkill(p, other);
        LAST_BITTEN_BY.put(t.getUUID(), p.getUUID());
        BEING_DEVOURED.add(t.getUUID());
        t.hurtServer(level, p.damageSources().playerAttack(p), DAMAGE);
        BEING_DEVOURED.remove(t.getUUID());
        com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, t.position().add(0, t.getBbHeight() / 2, 0), 10, 0.08); com.brightwebs.brightcore.fx.Fx.beam(level, com.brightwebs.brightcore.fx.BrightParticles.SOUL, t.position().add(0, t.getBbHeight() / 2, 0), p.position().add(0, 1, 0), 0.6);
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

    // uniques and ultimates can't be eaten, everything else can (magic only sometimes)
    private static boolean devourable(Ability s) {
        return switch (s.type()) {
            case UNIQUE, ULTIMATE -> false;
            default -> true;
        };
    }

    // a kill by the mist eats the victim: all its skills, 30% of its EP into your max, its drops into the stomach
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
            } else if (t.fireImmune()) {  // fire mobs have flame attack resistance
                Ability res = BrightSkills.resistance(DamageKind.FLAME);
                if (res != null && AbilityAPI.learn(p, res)) p.sendSystemMessage(Component.translatable("brightcore.message.acquired", res.name()));
            }
            double ep = EnergyAPI.ep(t) * EP_STEAL;
            if (ep > 0) Tk.gainMana(p, ep, true);
            ServerLevel level = (ServerLevel) p.level();
            com.brightwebs.brightcore.fx.Fx.sphere(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, t.position().add(0, t.getBbHeight() / 2, 0), t.getBbWidth() + 0.3, 30); com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.SOUL, t.position().add(0, t.getBbHeight() / 2, 0), 12, 0.15);
            level.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1, 0.5f);
            p.sendOverlayMessage(Component.translatable("jaycee.skill.predation.done", t.getDisplayName()));
        });
    }

    // a devoured mob's drops go in the stomach
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Safe.run("Predation.onDrops", () -> {
            if (!BEING_DEVOURED.contains(event.getEntity().getUUID())) return;
            if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
            for (ItemEntity drop : event.getDrops()) Stomach.store(p, drop.getItem().copy());
            event.getDrops().clear();
        });
    }

    // blocks (mode 2/4) go in the stomach, fluid sources (mode 3/4) get drunk
    private static void eatBlock(ServerPlayer p, ServerLevel level, int blockMode, Vec3 ray, float range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(ray.scale(range + 1));
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
        com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, net.minecraft.world.phys.Vec3.atCenterOf(pos), 8, 0.06);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.6f, 0.7f);
    }

    // isolation

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
            com.brightwebs.brightcore.fx.Fx.ring(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, who.position().add(0, who.getBbHeight() * 0.66, 0), who.getBbWidth() * 1.2, 20, -0.04);
            com.brightwebs.brightcore.fx.Fx.ring(level, com.brightwebs.brightcore.fx.BrightParticles.DARK_SPARK, who.position().add(0, who.getBbHeight() * 0.33, 0), who.getBbWidth() * 1.5, 12, -0.05);
            AbilityAPI.setCooldown(p, a, (Tk.mastered(p, a) ? ISOLATION_CD_MASTERED : ISOLATION_CD) * 20);
            level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1, 0.6f);
            return true;
        }
        // dissolving: what the item's worth in magicules and health
        FoodProperties food = held.get(DataComponents.FOOD);
        if (food == null) {
            Tk.fail(p, "jaycee.skill.activation_failed.item");
            return false;
        }
        Tk.gainMana(p, food.nutrition() * 10 * DISSOLVE_MAGICULE, false);
        p.heal((float) (food.nutrition() * DISSOLVE_HEALTH / 2));
        AbilityAPI.addMasteryPoint(p, a);
        held.shrink(1);
        com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.PREDATOR_MIST, p.position().add(0, 1.2, 0).add(p.getLookAngle().scale(0.6)), 12, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    // for appraisal: is this target worth eating right now
    static boolean canDevour(ServerPlayer p, LivingEntity t) {
        return p.isCreative() || t.getHealth() <= t.getMaxHealth() * 0.3f || EnergyAPI.ep(t) < EnergyAPI.ep(p) * 0.5;
    }
}

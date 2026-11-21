package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.effect.JayceeEffects;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

// a bunch of tensura's common / extra / intrinsic skills. numbers come from tensura's configs,
// the particle looks follow what tensura spawns for each one (waves, bubbles, sonic rings, breaths)
@net.neoforged.fml.common.EventBusSubscriber(modid = JayceeMod.ID)
public final class MoreSkills {
    private MoreSkills() {}

    static Identifier id(String s) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, s); }

    /** tensura's "isInSlot": touch skills work while they're on your bar (or toggled on once mastered) */
    static boolean inSlot(ServerPlayer p, Ability a) {
        AbilityData d = AbilityAPI.get(p);
        for (int i = 0; i < AbilityData.SLOTS_PER_PAGE; i++) if (d.slot(i).map(a.id()::equals).orElse(false)) return true;
        return false;
    }

    // ------------------------------------------------------------ strength / steel strength

    /**
     * tensura's strength: press for the strengthen effect (+6 attack a level), press again to drop it early.
     * the cooldown counts the time the buff lasts. the waves close in on you like tensura's reversed wave
     */
    static boolean strengthen(ServerPlayer p, Ability a, double cost, int ticks, int ticksMastered, int level, int levelMastered, int cooldown) {
        ServerLevel l = (ServerLevel) p.level();
        if (p.hasEffect(JayceeEffects.STRENGTHEN)) {
            p.removeEffect(JayceeEffects.STRENGTHEN);
            AbilityAPI.setCooldown(p, a, cooldown * 20);
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1, 1.4f);
            return false;
        }
        if (!Tk.spend(p, a, cost)) return false;
        boolean m = AbilityAPI.isMastered(p, a);
        int dur = m ? ticksMastered : ticks;
        p.addEffect(new MobEffectInstance(JayceeEffects.STRENGTHEN, dur, (m ? levelMastered : level) - 1, true, false, true));
        AbilityAPI.setCooldown(p, a, (cooldown + dur / 20) * 20);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 0.5f);
        double r = p.getBbWidth() * 3;
        Fx.ring(l, BrightParticles.SONIC, p.position().add(0, p.getBbHeight() * 0.5, 0), r, 16, -0.15);
        Fx.ring(l, BrightParticles.AURA, p.position().add(0, 0.2, 0), r, 20, -0.12);
        return true;
    }

    /** while the buff is up the skill gets a mastery point every skill tick, like tensura */
    static void strengthTick(ServerPlayer p, Ability a) {
        if (p.tickCount % Tk.SKILL_TICK == 0 && p.hasEffect(JayceeEffects.STRENGTHEN)) AbilityAPI.addMasteryPoint(p, a);
    }

    // ------------------------------------------------------------ strengthen body (passive)

    private static final Identifier BODY = id("strengthen_body");

    /** tensura's strengthen body: +5 armor and you take 80% damage (the damage part is in onHurt below) */
    static void strengthenBody(ServerPlayer p, boolean on) {
        AttributeInstance armor = p.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        if (on) armor.addOrUpdateTransientModifier(new AttributeModifier(BODY, 5, AttributeModifier.Operation.ADD_VALUE));
        else armor.removeModifier(BODY);
    }

    /** strengthen body's other half: you take 80% damage while it's on */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("MoreSkills.onHurt", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            Ability body = com.brightwebs.brightcore.api.AbilityRegistry.get(id("strengthen_body"));
            if (body != null && AbilityAPI.has(p, body) && AbilityAPI.isSwitchedOn(p, body)) event.setAmount(event.getAmount() * 0.8f);
        });
    }

    // ------------------------------------------------------------ touch skills (paralysis, poison, corrosion)

    /** runs on every hit you land. tensura gives a mastery point every 10th time it procs */
    static void touch(ServerPlayer p, Ability a, LivingEntity t, Consumer<LivingEntity> effect, Supplier<SimpleParticleType> particle) {
        if (!inSlot(p, a) && !(a.isToggle() && AbilityAPI.isToggled(p, a))) return;
        effect.accept(t);
        ServerLevel l = (ServerLevel) p.level();
        l.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
        Fx.burst(l, particle, t.position().add(0, t.getBbHeight() / 2, 0), 20, 0.15);
        Tk.activatedTime(p, a);
    }

    /** tensura's corrosion press: rots the raw meat in your hand into rotten flesh */
    static boolean rotMeat(ServerPlayer p, Ability a) {
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() || hand.is(Items.ROTTEN_FLESH) || !hand.is(net.minecraft.tags.ItemTags.MEAT)) return false;
        hand.shrink(1);
        AbilityAPI.addMasteryPoint(p, a);
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
        if (!p.addItem(flesh)) p.spawnAtLocation((ServerLevel) p.level(), flesh);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    // ------------------------------------------------------------ breaths

    interface BreathHit { void hit(ServerLevel l, ServerPlayer p, LivingEntity t, float dps); }

    /**
     * hold to breathe, same frame as flame breath: costs every second, a mastery point every 3 seconds,
     * a cone in front of you that hits 4 times a second
     */
    static boolean breath(ServerPlayer p, Ability a, int held, double cost, float dps, float dpsMastered, int range,
                          Supplier<SimpleParticleType> main, Supplier<SimpleParticleType> trail, SoundEvent sound, BreathHit hit) {
        if (held % 20 == 0 && !Tk.spend(p, a, cost)) return false;
        if (held > 0 && held % Tk.MASTERY_HOLD_TICK == 0) AbilityAPI.addMasteryPoint(p, a);
        ServerLevel l = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition().add(0, -0.15, 0), look = p.getLookAngle();
        Fx.cone(l, main, eye.add(look.scale(0.9)), look, 0.45, 0.22, 4);
        for (int i = 4; i <= range; i += 2) if (held % 3 == 0) Fx.move(l, trail, eye.add(look.scale(i * 0.9)), look.scale(0.2));
        if (held % 2 == 0) l.playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, 0.75f, 1.0f);
        if (held % 5 == 0) {
            float d = Tk.mastered(p, a) ? dpsMastered : dps;
            for (LivingEntity t : SkillHelper.around(p, range + 1)) {
                Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
                if (to.length() > range + 0.5 || to.normalize().dot(look) < 0.82) continue;
                hit.hit(l, p, t, d);
            }
        }
        return true;
    }

    static void iceHit(ServerLevel l, ServerPlayer p, LivingEntity t, float dps) {
        t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 40));
        t.hurtServer(l, l.damageSources().freeze(), dps / 4);
    }

    static void thunderHit(ServerLevel l, ServerPlayer p, LivingEntity t, float dps) {
        t.hurtServer(l, l.damageSources().indirectMagic(p, p), dps / 4);
        Fx.burst(l, BrightParticles.SPARK, t.position().add(0, t.getBbHeight() / 2, 0), 4, 0.2);
    }

    static void poisonHit(ServerLevel l, ServerPlayer p, LivingEntity t, float dps) {
        t.hurtServer(l, l.damageSources().indirectMagic(p, p), dps / 4);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0), p);
    }

    static void paralysisHit(ServerLevel l, ServerPlayer p, LivingEntity t, float dps) {
        t.hurtServer(l, l.damageSources().indirectMagic(p, p), dps / 4);
        t.addEffect(new MobEffectInstance(JayceeEffects.PARALYSIS, 200, 2), p);
    }

    // ------------------------------------------------------------ sonic stuff

    /** tensura's ultrasonic waves: sonic booms along where you look, 8 blocks, 8 damage, deafens (we use nausea) */
    static boolean ultrasonic(ServerPlayer p, Ability a) {
        if (!Tk.spend(p, a, 30)) return false;
        ServerLevel l = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle(), from = p.getEyePosition();
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.6f);
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (int i = 1; i <= 8; i++) {
            Vec3 at = from.add(look.scale(i));
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            for (LivingEntity t : l.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(1.2), t -> t != p)) {
                if (!hit.add(t)) continue;
                t.hurtServer(l, l.damageSources().sonicBoom(p), 8);
                t.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0), p);
            }
        }
        Tk.activatedTime(p, a);
        AbilityAPI.setCooldown(p, a, 60);
        return true;
    }

    /** tensura's voice cannon: a shout that blasts a line, 20 damage (30 mastered), 15 blocks (20 mastered) */
    static boolean voiceCannon(ServerPlayer p, Ability a) {
        if (!Tk.spend(p, a, 500)) return false;
        ServerLevel l = (ServerLevel) p.level();
        boolean m = Tk.mastered(p, a);
        int range = m ? 20 : 15;
        float dmg = m ? 30 : 20;
        Vec3 look = p.getLookAngle(), from = p.getEyePosition().add(look.scale(1.5));
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 1.6f);
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (int i = 0; i < range; i++) {
            Vec3 at = from.add(look.scale(i));
            Fx.move(l, BrightParticles.SONIC, at, look.scale(0.05));
            for (LivingEntity t : l.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(1.5), t -> t != p)) {
                if (!hit.add(t)) continue;
                t.hurtServer(l, l.damageSources().sonicBoom(p), dmg);
                Vec3 push = look.scale(0.8);
                t.push(push.x, 0.2, push.z);
            }
        }
        Tk.activatedTime(p, a);
        AbilityAPI.setCooldown(p, a, 60);
        return true;
    }

    // ------------------------------------------------------------ water blade

    /** tensura's water blade: a fast slicing disc of water, 40 damage, goes 30 blocks or until it hits */
    static boolean waterBlade(ServerPlayer p, Ability a) {
        if (!Tk.spend(p, a, 10)) return false;
        ServerLevel l = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle(), at = p.getEyePosition().add(look.scale(1));
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1, 1.4f);
        for (int i = 0; i < 30; i++) {
            at = at.add(look);
            Fx.move(l, BrightParticles.DROP, at, Vec3.ZERO);
            Fx.ring(l, BrightParticles.BUBBLE, at, 0.4, 6, 0);
            if (!l.getBlockState(net.minecraft.core.BlockPos.containing(at)).getCollisionShape(l, net.minecraft.core.BlockPos.containing(at)).isEmpty()) break;
            LivingEntity hit = null;
            for (LivingEntity t : l.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.8), t -> t != p)) { hit = t; break; }
            if (hit != null) {
                hit.hurtServer(l, l.damageSources().indirectMagic(p, p), 40);
                Fx.burst(l, BrightParticles.DROP, at, 20, 0.25);
                break;
            }
        }
        Tk.activatedTime(p, a);
        AbilityAPI.setCooldown(p, a, 20);
        return true;
    }

    // ------------------------------------------------------------ movement

    /** tensura's hydraulic propulsion: a riptide shove where you look, works in water or rain */
    static boolean hydraulic(ServerPlayer p, Ability a) {
        if (!p.isInWaterOrRain()) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jaycee.skill.need_water").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        if (!Tk.spend(p, a, 2)) return false;
        Vec3 look = p.getLookAngle().scale(3.0);
        p.push(look.x, look.y, look.z);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        Fx.cone(((ServerLevel) p.level()), BrightParticles.BUBBLE, p.position(), look.normalize().scale(-1), 0.3, 0.3, 12);
        Tk.activatedTime(p, a);
        return true;
    }

    /** tensura's gravity flight: a toggle that lets you fly, 5 magicules a second */
    static void gravityFlight(ServerPlayer p, Ability a) {
        if (p.tickCount % 20 == 0 && !p.isCreative() && !Tk.spend(p, a, 5)) {
            AbilityAPI.setToggled(p, a, false);
            return;
        }
        if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
        if (p.getAbilities().flying && p.tickCount % 4 == 0) Fx.move((ServerLevel) p.level(), BrightParticles.VOID, p.position(), new Vec3(0, -0.05, 0));
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
    }

    static void gravityFlightOff(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    // ------------------------------------------------------------ senses

    /** sense heat source / universal perception: everything warm in range glows for you */
    static void sense(ServerPlayer p, Ability a, double cost, double radius) {
        if (cost > 0 && p.tickCount % Tk.SKILL_TICK == 0) {
            if (!Tk.spend(p, a, cost * 5)) { AbilityAPI.setToggled(p, a, false); MagicSense.clear(p); return; }
            Tk.activatedTime(p, a);
        }
        if (p.tickCount % 10 == 0) MagicSense.glowAround(p, radius);
    }

    // ------------------------------------------------------------ thought acceleration (passive)

    private static final Identifier THOUGHT = id("thought_acceleration");

    /** tensura's thought acceleration: a bit faster on your feet and a lot faster swings */
    static void thought(ServerPlayer p, Ability a, boolean on) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED), atk = p.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null || atk == null) return;
        if (!on) { speed.removeModifier(THOUGHT); atk.removeModifier(THOUGHT); return; }
        boolean m = AbilityAPI.isMastered(p, a);
        speed.addOrUpdateTransientModifier(new AttributeModifier(THOUGHT, m ? 0.02 : 0.01, AttributeModifier.Operation.ADD_VALUE));
        atk.addOrUpdateTransientModifier(new AttributeModifier(THOUGHT, m ? 0.4 : 0.2, AttributeModifier.Operation.ADD_VALUE));
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
    }
}

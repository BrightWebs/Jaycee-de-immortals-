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
        Vec3 eye = LoreFx.mouth(p), look = p.getLookAngle();
        if (main == BrightParticles.SPARK) {
            // tensura's thunder breath colours (its lightning effect is a blue-violet), bolts running out along your aim
            if (held % 2 == 0) for (int k = 0; k < 2; k++) {
                Vec3 end = eye.add(look.scale(range)).add((Math.random() - 0.5) * range * 0.3, (Math.random() - 0.5) * range * 0.2, (Math.random() - 0.5) * range * 0.3);
                LoreFx.bolt(l, 0x9FB0FF, 0xFFFFFF, BrightParticles.SPARK, eye, end, Math.max(5, range), 0.45);
            }
        } else {
            Fx.cone(l, main, eye.add(look.scale(0.3)), look, 0.45, 0.18, 5);
            for (int i = 4; i <= range; i += 2) if (held % 3 == 0) Fx.move(l, trail, eye.add(look.scale(i * 0.9)), look.scale(0.2));
        }
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

    // ------------------------------------------------------------ batch 2

    /**
     * tensura's shadow motion, mode 0: hold to sink into the shadows. only works somewhere dark
     * (light 10 or less) until mastered. you go invisible and quick, ink puffs off you
     */
    static boolean shadowHeld(ServerPlayer p, Ability a, int held) {
        ServerLevel l = (ServerLevel) p.level();
        if (l.getMaxLocalRawBrightness(p.blockPosition()) > 10 && !AbilityAPI.isMastered(p, a)) {
            if (held == 0) p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jaycee.skill.too_bright").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        if (held <= 2) {
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
            if (held == 0) l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1, 0.6f);
        }
        if (held % 20 == 0 && !Tk.spend(p, a, 10)) return false;
        if (held % Tk.MASTERY_HOLD_TICK == 0 && held > 0) AbilityAPI.addMasteryPoint(p, a);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 5, 1, false, false, false));
        if (held % 4 == 0) l.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
        return true;
    }

    /** shadow motion, mode 1 (mastered): step through the shadows to right behind what you're looking at, 20 blocks */
    static boolean shadowStep(ServerPlayer p, Ability a) {
        LivingEntity t = SkillHelper.target(p, 20);
        if (t == null) return false;
        if (!Tk.spend(p, a, 200)) return false;
        ServerLevel l = (ServerLevel) p.level();
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        p.teleportTo(behind.x, t.getY(), behind.z);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 0.5f);
        Tk.activatedTime(p, a);
        return true;
    }

    /** tensura's ultra instinct: 10% to just not get hit, melee or projectile. checked in onDodge */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onDodge(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("MoreSkills.onDodge", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p) || event.getSource().getEntity() == null) return;
            Ability ui = com.brightwebs.brightcore.api.AbilityRegistry.get(id("ultra_instinct"));
            if (ui == null || !AbilityAPI.has(p, ui) || !AbilityAPI.isToggled(p, ui)) return;
            if (p.getRandom().nextFloat() >= 0.10f) return;
            event.setCanceled(true);
            ServerLevel l = (ServerLevel) p.level();
            Fx.burst(l, BrightParticles.GUST, p.position().add(0, 1, 0), 10, 0.2);
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1, 1.6f);
            Tk.activatedTime(p, ui);
        });
    }

    /**
     * tensura's danger sense: when something starts hunting you, you hear a bell from the direction
     * it's coming from (only you hear it). a mastery point now and then
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onTargeted(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("MoreSkills.onTargeted", () -> {
            if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
            if (mob.getTarget() == p) return;
            Ability ds = com.brightwebs.brightcore.api.AbilityRegistry.get(id("danger_sense"));
            if (ds == null || !AbilityAPI.has(p, ds) || !AbilityAPI.isToggled(p, ds)) return;
            if (p.getRandom().nextBoolean()) AbilityAPI.addMasteryPoint(p, ds);
            Vec3 eye = p.getEyePosition(), at = eye.add(mob.getEyePosition().subtract(eye).normalize().scale(5));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BELL_BLOCK), SoundSource.PLAYERS,
                    at.x, eye.y, at.z, 1, 1, p.getRandom().nextLong()));
        });
    }

    /** tensura's drain: bite something within 3 blocks for 6 damage and heal that much. no blood, no drain */
    static boolean drain(ServerPlayer p, Ability a) {
        LivingEntity t = SkillHelper.target(p, 3);
        if (t == null) return false;
        if (t.typeHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD) || t instanceof net.minecraft.world.entity.animal.golem.AbstractGolem) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jaycee.skill.no_blood").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        ServerLevel l = (ServerLevel) p.level();
        if (t.hurtServer(l, l.damageSources().playerAttack(p), 6)) {
            p.heal(6);
            AbilityAPI.addMasteryPoint(p, a);
            Fx.burst(l, BrightParticles.BLOOD, t.position().add(0, t.getBbHeight() / 2, 0), 16, 0.12);
            for (int k = 0; k < 3; k++) LoreFx.essence(l, 0xB0122A, 0xFF7799, t, p);
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1, 0.7f);
        }
        return true;
    }

    /**
     * tensura's ogre berserker: a huge boost (+30 attack, +10 armor) for 3 minutes (6 mastered), costs 5,000.
     * press again to calm down. 10 minute cooldown on top of the time it lasts
     */
    static boolean ogreBerserker(ServerPlayer p, Ability a) {
        ServerLevel l = (ServerLevel) p.level();
        if (p.hasEffect(JayceeEffects.OGRE_BERSERKER)) {
            p.removeEffect(JayceeEffects.OGRE_BERSERKER);
            AbilityAPI.setCooldown(p, a, 600 * 20);
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1, 0.6f);
            return false;
        }
        if (!Tk.spend(p, a, 5000)) return false;
        int dur = AbilityAPI.isMastered(p, a) ? 7200 : 3600;
        p.addEffect(new MobEffectInstance(JayceeEffects.OGRE_BERSERKER, dur, 0, false, false, true));
        AbilityAPI.setCooldown(p, a, (dur / 20 + 600) * 20);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 0.7f);
        Fx.shockwave(l, p.position().add(0, 0.2, 0), 2);
        Fx.spiral(l, BrightParticles.FLAME, p.position(), 0.9, 2.4, 2, 40, 0);
        LoreFx.fade(l, 0xC01010, 0xFF4A2A, p.position().add(0, 1, 0), 40, 0.6, 1.6f);
        return true;
    }

    /** while berserk you smoke red, and the skill earns mastery like tensura's activatedTimes */
    static void ogreTick(ServerPlayer p, Ability a) {
        if (!p.hasEffect(JayceeEffects.OGRE_BERSERKER)) return;
        if (p.tickCount % 4 == 0) {
            ServerLevel l = (ServerLevel) p.level();
            for (int i = 0; i < 4; i++) LoreFx.dust(l, i % 2 == 0 ? 0xC01010 : 0xFF4A2A, p.position().add((Math.random() - 0.5) * 1.1, Math.random() * 2, (Math.random() - 0.5) * 1.1), 1, 0, 1.3f);
            Fx.aura(l, BrightParticles.FLAME, p, 1);
        }
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
    }

    private static final Identifier SCALES = id("scale_armor"), DRAGON_SKIN = id("dragon_skin");

    /** tensura's scale armor: +6 armor and you swim twice as fast (dolphin's grace here) */
    static void scaleArmor(ServerPlayer p, Ability a, boolean on) {
        AttributeInstance armor = p.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        if (!on) { armor.removeModifier(SCALES); return; }
        armor.addOrUpdateTransientModifier(new AttributeModifier(SCALES, 6, AttributeModifier.Operation.ADD_VALUE));
        if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
    }

    /**
     * tensura's dragon skin: your skin is as good as armor, and better the more EP you have.
     * tensura goes magisteel (50k) / adamantite (400k) / hihiirokane (800k), we use iron / diamond / netherite / beyond.
     * only works with no armor on, same as tensura
     */
    static void dragonSkin(ServerPlayer p, Ability a, boolean on) {
        AttributeInstance armor = p.getAttribute(Attributes.ARMOR), tough = p.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor == null || tough == null) return;
        boolean wearing = false;
        for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            if (!p.getItemBySlot(s).isEmpty() && !BodyArmor.isScale(p.getItemBySlot(s))) wearing = true;
        }
        if (!on || wearing) { armor.removeModifier(DRAGON_SKIN); tough.removeModifier(DRAGON_SKIN); return; }
        double ep = com.brightwebs.brightcore.api.EnergyAPI.ep(p);
        double ar = ep >= 800_000 ? 24 : ep >= 400_000 ? 20 : ep >= 50_000 ? 20 : 15;
        double tg = ep >= 800_000 ? 16 : ep >= 400_000 ? 12 : ep >= 50_000 ? 8 : 0;
        armor.addOrUpdateTransientModifier(new AttributeModifier(DRAGON_SKIN, ar, AttributeModifier.Operation.ADD_VALUE));
        ServerLevel dl = (ServerLevel) p.level();
        for (int i = 0; i < 3; i++) LoreFx.dust(dl, i == 0 ? 0xBFE0FF : 0x2F6FD8, p.position().add((Math.random() - 0.5) * 0.8, 0.2 + Math.random() * 1.6, (Math.random() - 0.5) * 0.8), 1, 0, 0.9f);
        tough.addOrUpdateTransientModifier(new AttributeModifier(DRAGON_SKIN, tg, AttributeModifier.Operation.ADD_VALUE));
    }

    /**
     * tensura's snake eye: hold your gaze on something within 20 blocks and it gets worse the longer
     * you stare. modes: corrosion, poison, paralysis, petrification, insanity. 80 magicules a second
     */
    static boolean snakeEye(ServerPlayer p, Ability a, int mode, int held) {
        if (held % 20 == 0 && !Tk.spend(p, a, 80)) return false;
        LivingEntity t = SkillHelper.targetQuiet(p, 20);
        if (t == null) return true;
        if (held % Tk.MASTERY_HOLD_TICK == 0 && held > 0) AbilityAPI.addMasteryPoint(p, a);
        ServerLevel l = (ServerLevel) p.level();
        if (held % 10 == 0) l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PARROT_IMITATE_SPIDER, SoundSource.PLAYERS, 1, 1);
        var effect = switch (mode) {
            case 0 -> MobEffects.WITHER;
            case 1 -> MobEffects.POISON;
            case 2 -> JayceeEffects.PARALYSIS;
            case 3 -> MobEffects.SLOWNESS;
            default -> MobEffects.NAUSEA;
        };
        // tensura: the effect builds a level every few seconds you keep staring (we cap it at V)
        MobEffectInstance now = t.getEffect(effect);
        int level = now == null ? 0 : Math.min(4, now.getAmplifier() + (held % 60 == 0 ? 1 : 0));
        t.addEffect(new MobEffectInstance(effect, 60, level, false, true), p);
        if (mode == 3 && level >= 3) t.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 2), p);
        if (mode == 4) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), p);
        if (held % 4 == 0) Fx.beam(l, mode == 2 ? BrightParticles.SPARK : mode == 3 ? BrightParticles.SHARD : BrightParticles.ACID,
                p.getEyePosition(), t.getEyePosition(), 1.2);
        return true;
    }
}

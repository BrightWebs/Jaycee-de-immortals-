package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.skill.LoreSkills.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// the gluttony family (starved -> gluttony -> beelzebub), built from how tensura and manas reborn do gluttony:
//   corrosion (tensura): hold to rot everything around you, 5 blocks, 10 damage every half second, you slow down,
//     whatever it kills gives up its skills and 40% of its EP. 200 magicules a second
//   ravenous grasp (manas reborn): hold and tendrils lash out at what's near you every half second,
//     4 of them (8 mastered), 7 blocks (14 mastered), 5 damage each, drain 200, 20% (50% mastered) to steal a skill
//   devouring void (manas reborn): a black hole where you look. pulls in everything within 8 blocks, faster the closer
//     it gets, the core hits for 50 and drains 1000 magicules, and what dies in it is devoured (all skills, 50% EP)
//   receive / provide (tensura's starved, the food chain in the show): take skills your subordinates learned,
//     or share yours with them
//   mimicry (the show): take the form of the last thing you devoured. you get what it's good at and its kind ignores you
// beelzebub is the ultimate form, so it gets everything at a higher tier (see TIER)
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Gluttonous {
    private Gluttonous() {}

    // how strong each member of the family is: corrosion radius/damage, grasp tendrils/range/damage, void size/suction/damage
    record Tier(double corrRadius, float corrDamage, float corrSteal, int tendrils, int tendrilsMastered, double graspRange, double graspRangeMastered,
                float graspDamage, float graspDrain, float graspSkill, float graspSkillMastered, float voidSize, double voidSuction, float voidDamage, float voidDrain) {}
    // tensura's starved / gluttony numbers, manas reborn's gluttony numbers, and beelzebub above both
    static final Tier STARVED = new Tier(5, 5, 0.2f, 2, 4, 5, 10, 4, 100, 0.1f, 0.3f, 1.5f, 6, 30, 500);
    static final Tier GLUTTONY = new Tier(5, 10, 0.4f, 4, 8, 7, 14, 5, 200, 0.2f, 0.5f, 2f, 8, 50, 1000);
    static final Tier BEELZEBUB = new Tier(8, 20, 0.6f, 8, 12, 12, 20, 12, 500, 0.4f, 0.7f, 3f, 12, 100, 3000);

    static Tier tier(Spec s) {
        if (s.id().startsWith("beelzeb")) return BEELZEBUB;
        if (s.type() == SkillType.ULTIMATE) return BEELZEBUB;
        if (s.id().equals("starved")) return STARVED;
        return GLUTTONY;
    }

    static Predation.Stats predation(Spec s) {
        return s.id().startsWith("beelzeb") || s.type() == SkillType.ULTIMATE ? Predation.BEELZEBUB : Predation.GLUTTONY;
    }

    // ------------------------------------------------------------ corrosion (held)

    static boolean corrosion(ServerPlayer p, Ability a, Spec s, int held) {
        Tier t = tier(s);
        ServerLevel l = (ServerLevel) p.level();
        if (held % 20 == 0 && !Tk.spend(p, a, 200)) return false;
        if (held > 0 && held % Tk.MASTERY_HOLD_TICK == 0) AbilityAPI.addMasteryPoint(p, a);
        // you slow down while you rot everything (tensura: x0.5 speed while held)
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 1, false, false, false));
        if (held % 5 == 0) {
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 0.5f);
            // tensura's purple aura rising off you
            for (int i = 0; i < 10; i++) {
                double ang = Math.random() * Math.PI * 2, r = 0.4 + Math.random() * 0.5;
                LoreFx.fade(l, 0x6a1ea0, 0x1a0a2a, p.position().add(Math.cos(ang) * r, Math.random() * 2, Math.sin(ang) * r), 1, 0.05, 1.6f);
            }
            Fx.aura(l, BrightParticles.ALL.get("acid_drop"), p, 2);
        }
        if (held % 10 == 0) {
            for (LivingEntity e : LoreEngine.near(l, p, p.position(), t.corrRadius)) {
                Predation.devourHit(p, e, t.corrDamage, new Predation.Stats(0, 0, t.corrDamage, 0, 0, t.corrSteal, 99), l.damageSources().wither());
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), p);
                LoreFx.dust(l, 0x7a2ab0, e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.3, 1.2f);
            }
        }
        return true;
    }

    // ------------------------------------------------------------ ravenous grasp (held)

    static boolean grasp(ServerPlayer p, Ability a, Spec s, int held) {
        Tier t = tier(s);
        if (held % 10 != 0) return true;
        if (!Tk.spend(p, a, 500)) return false;
        ServerLevel l = (ServerLevel) p.level();
        boolean m = AbilityAPI.isMastered(p, a);
        int tendrils = m ? t.tendrilsMastered : t.tendrils;
        double range = m ? t.graspRangeMastered : t.graspRange;
        List<LivingEntity> foes = LoreEngine.near(l, p, p.position(), range);
        foes.sort((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p)));
        int used = 0;
        for (LivingEntity e : foes) {
            if (used >= tendrils) break;
            used++;
            tendril(l, p, e, held + used);
            Predation.devourHit(p, e, t.graspDamage, new Predation.Stats(0, 0, t.graspDamage, t.graspDrain, (m ? t.graspSkillMastered : t.graspSkill) * 100, 0.5f, 1), null);
            // drag it in
            Vec3 d = p.position().subtract(e.position());
            if (d.lengthSqr() > 4) { d = d.normalize().scale(0.6); e.push(d.x, 0.15, d.z); }
        }
        if (used > 0) l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1, 0.6f);
        else {
            // nothing to grab: the tendrils just writhe around you
            for (int i = 0; i < tendrils; i++) {
                double ang = i * Math.PI * 2 / tendrils + held * 0.1;
                tendrilTo(l, p, p.position().add(Math.cos(ang) * 2.5, 1.2 + Math.sin(held * 0.2 + i) * 0.5, Math.sin(ang) * 2.5), held + i);
            }
        }
        if (held > 0 && held % Tk.MASTERY_HOLD_TICK == 0) AbilityAPI.addMasteryPoint(p, a);
        return true;
    }

    private static void tendril(ServerLevel l, ServerPlayer p, LivingEntity e, int seed) {
        tendrilTo(l, p, e.position().add(0, e.getBbHeight() / 2, 0), seed);
        Fx.burst(l, BrightParticles.PREDATOR_MIST, e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.06);
    }

    // a black tendril growing out of your back and curving to the point
    private static void tendrilTo(ServerLevel l, ServerPlayer p, Vec3 to, int seed) {
        Vec3 from = p.position().add(0, 1.3, 0).subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.3));
        Vec3 mid = from.lerp(to, 0.5).add(Math.sin(seed) * 1.2, 1.0, Math.cos(seed) * 1.2);
        for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            Vec3 a = from.lerp(mid, t), b = mid.lerp(to, t), at = a.lerp(b, t);
            LoreFx.dust(l, i % 3 == 0 ? 0x5a1a8a : 0x0c0610, at, 1, 0.02, 1.3f - (float) t * 0.5f);
        }
    }

    // ------------------------------------------------------------ devouring void

    private record Hole(ServerLevel level, ServerPlayer owner, Vec3 at, Tier tier, long until) {}
    private static final List<Hole> HOLES = new ArrayList<>();

    static boolean devouringVoid(ServerPlayer p, Ability a, Spec s) {
        Tier t = tier(s);
        Vec3 at = LoreActions.aim(p, 20).add(0, 1.5, 0);
        ServerLevel l = (ServerLevel) p.level();
        HOLES.add(new Hole(l, p, at, t, l.getGameTime() + 100));
        l.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 0.4f);
        return true;
    }

    private static void tickHole(Hole h, int tick) {
        ServerLevel l = h.level;
        Vec3 c = h.at;
        float size = h.tier.voidSize;
        // manas reborn's look: mycelium spores spiralling into the centre, and a black core
        for (int i = 0; i < 6; i++) {
            double ang = Math.random() * Math.PI * 2, r = (1 + Math.random() * 3) * size, hgt = (Math.random() - 0.5) * size;
            double sx = c.x + Math.cos(ang) * r, sy = c.y + hgt, sz = c.z + Math.sin(ang) * r, swirl = 0.22 * size / (r + 0.2);
            l.sendParticles(ParticleTypes.MYCELIUM, sx, sy, sz, 0, (c.x - sx) * 0.06 - Math.sin(ang) * swirl, (c.y - sy) * 0.06, (c.z - sz) * 0.06 + Math.cos(ang) * swirl, 1);
        }
        LoreFx.dust(l, 0x050208, c, 8, size * 0.25, 2.5f);
        if (tick % 4 == 0) LoreFx.dustRing(l, 0x3a0a5a, c, size * 0.9, 20, 1.4f, 0);
        // pull: stronger the closer it is (manas reborn: speed x (1 + closeness^2 x 4))
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(h.tier.voidSuction), e -> LoreEngine.foe(h.owner, e))) {
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist < 0.1) continue;
            double close = 1 - Math.min(1, dist / h.tier.voidSuction);
            Vec3 v = d.normalize().scale(0.08 * (1 + close * close * 4));
            e.push(v.x, v.y + 0.04, v.z);
            if (dist <= size && tick % 10 == 0) Predation.devourHit(h.owner, e, h.tier.voidDamage, new Predation.Stats(0, 0, h.tier.voidDamage, h.tier.voidDrain, 50, 0.5f, 99), null);
        }
        // it swallows projectiles too
        for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new net.minecraft.world.phys.AABB(c, c).inflate(size * 1.5), pr -> pr.getOwner() != h.owner)) pr.discard();
        if (tick % 20 == 0) l.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

    // ------------------------------------------------------------ receive / provide (the food chain)

    static boolean receive(ServerPlayer p, Ability a) {
        LivingEntity t = SkillHelper.target(p, 8);
        if (t == null) return false;
        if (!(t instanceof Player sub) || !Bonds.isSubordinateOf(sub, p)) {
            if (Bonds.isSubordinateOf(t, p)) {
                // a mob subordinate shares a slice of its power instead
                double share = EnergyAPI.ep(t) * 0.1;
                Tk.gainMana(p, share, true);
                LoreFx.essence((ServerLevel) p.level(), 0xffd34a, 0xffffff, t, p);
                p.sendOverlayMessage(Component.translatable("jaycee.gluttony.received_power", t.getName()).withStyle(ChatFormatting.GOLD));
                return true;
            }
            Tk.fail(p, "jaycee.gluttony.not_subordinate");
            return false;
        }
        int got = 0;
        for (Ability s : AbilityAPI.owned(sub)) {
            if (s.type() == SkillType.UNIQUE || s.type() == SkillType.ULTIMATE || AbilityAPI.has(p, s)) continue;
            if (AbilityAPI.learn(p, s)) { got++; p.sendSystemMessage(Component.translatable("brightcore.message.acquired", s.name())); }
        }
        if (got == 0) { Tk.fail(p, "jaycee.gluttony.nothing_to_receive"); return false; }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1);
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 20, 0.08, 0.08, 0.08, 0.15);
        return true;
    }

    static boolean provide(ServerPlayer p, Ability a) {
        LivingEntity t = SkillHelper.target(p, 8);
        if (t == null || !Bonds.isSubordinateOf(t, p)) { Tk.fail(p, "jaycee.gluttony.not_subordinate"); return false; }
        ServerLevel l = (ServerLevel) p.level();
        if (t instanceof ServerPlayer sub) {
            List<Ability> pool = new ArrayList<>();
            for (Ability s : AbilityAPI.owned(p)) if (s.type() != SkillType.UNIQUE && s.type() != SkillType.ULTIMATE && !AbilityAPI.has(sub, s)) pool.add(s);
            if (pool.isEmpty()) { Tk.fail(p, "jaycee.gluttony.nothing_to_provide"); return false; }
            Ability give = pool.get(p.getRandom().nextInt(pool.size()));
            AbilityAPI.learn(sub, give);
            sub.sendSystemMessage(Component.translatable("jaycee.gluttony.provided", p.getName(), give.name()).withStyle(ChatFormatting.GOLD));
        } else if (t instanceof Mob m) {
            // a mob gets a share of your strength for good (once)
            var hp = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            var atk = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(JayceeMod.ID, "food_chain");
            if (hp != null && hp.getModifier(id) == null) {
                hp.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                if (atk != null) atk.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                m.heal(m.getMaxHealth());
            } else { Tk.fail(p, "jaycee.gluttony.already_provided"); return false; }
        }
        LoreFx.essence(l, 0xffd34a, 0xffffff, p, t);
        l.sendParticles(ParticleTypes.WAX_ON, t.getX(), t.getY() + 1, t.getZ(), 20, 0.08, 0.08, 0.08, 0.15);
        l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1.3f);
        return true;
    }

    // ------------------------------------------------------------ mimicry

    // what you've eaten last, per player (set by Predation when something gets devoured)
    static final Map<UUID, EntityType<?>> LAST_EATEN = new HashMap<>();

    static boolean mimicry(ServerPlayer p, Ability a, Spec s) {
        EntityType<?> form = LAST_EATEN.get(p.getUUID());
        if (form == null) { Tk.fail(p, "jaycee.gluttony.nothing_to_mimic"); return false; }
        ServerLevel l = (ServerLevel) p.level();
        int dur = 20 * 60 * 3;
        String k = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(form).getPath();
        // what that kind of thing is good at
        if (k.matches(".*(phantom|bat|ghast|blaze|vex|allay|parrot|bee|breeze|happy_ghast).*")) { p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur, 0)); p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, dur, 2)); }
        if (k.matches(".*(fish|cod|salmon|squid|dolphin|guardian|drowned|axolotl|turtle|frog|nautilus).*")) { p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, dur, 0)); p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, dur, 0)); }
        if (k.matches(".*(spider|wolf|cat|ocelot|fox|horse|rabbit|camel|goat).*")) { p.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, 1)); p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, dur, 1)); }
        if (k.matches(".*(zombie|skeleton|wither|stray|husk|phantom|zoglin|bogged).*")) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur, 0));
        if (k.matches(".*(golem|ravager|hoglin|warden|ogre|bear|sniffer).*")) { p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, dur, 1)); p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur, 0)); }
        if (k.matches(".*(blaze|magma|strider|ghast).*")) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur, 0));
        if (k.matches(".*(enderman|shulker|endermite).*")) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur / 3, 0));
        MIMIC.put(p.getUUID(), new Mimic(form, l.getGameTime() + dur));
        Fx.sphere(l, BrightParticles.PREDATOR_MIST, p.position().add(0, 1, 0), 1.2, 40);
        l.playSound(null, p.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1, 0.6f);
        p.sendOverlayMessage(Component.translatable("jaycee.gluttony.mimic", Component.translatable(form.getDescriptionId())).withStyle(ChatFormatting.DARK_AQUA));
        return true;
    }

    private record Mimic(EntityType<?> form, long until) {}
    private static final Map<UUID, Mimic> MIMIC = new HashMap<>();

    // mobs of the kind you're mimicking take you for one of their own
    @SubscribeEvent
    public static void onTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer p)) return;
        Mimic m = MIMIC.get(p.getUUID());
        if (m == null) return;
        if (p.level().getGameTime() > m.until) { MIMIC.remove(p.getUUID()); return; }
        if (event.getEntity().getType() == m.form) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Safe.run("Gluttonous.tick", () -> {
            int tick = event.getServer().getTickCount();
            Iterator<Hole> it = HOLES.iterator();
            while (it.hasNext()) {
                Hole h = it.next();
                if (!h.owner.isAlive() || h.level.getGameTime() > h.until) {
                    LoreFx.dust(h.level, 0x3a0a5a, h.at, 30, 1, 1.4f);
                    it.remove(); continue;
                }
                tickHole(h, tick);
            }
        });
    }
}

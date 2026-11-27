package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.effect.JayceeEffects;
import com.brightwebs.jaycee.skill.LoreSkills.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// what lore skills actually do. every skill is one of a dozen kinds (shot, ray, breath, storm, strike,
// aura, guard, sight, restore, shift, summon, awaken) plus resistances, and its element decides the
// extra stuff that happens on a hit (burning, freezing, paralysis...). the look is the skill's own
// particle recipe: two particle kinds from fx_recipes.json shaped by what the skill does
@EventBusSubscriber(modid = com.brightwebs.jaycee.JayceeMod.ID)
public final class LoreEngine {
    private LoreEngine() {}

    // ------------------------------------------------------------ using a skill

    static boolean use(ServerPlayer p, Ability a, Spec s, LoreSkills.Act act) {
        ServerLevel l = (ServerLevel) p.level();
        boolean ok = LoreActions.run(p, a, s, act);
        if (!ok) return false;
        AbilityAPI.addMasteryPoint(p, a);
        int cd = LoreActions.cooldown(s, act, AbilityAPI.isMastered(p, a));
        if (cd > 0) AbilityAPI.setCooldown(p, a, cd);
        sound(l, p, s, LoreActions.kind(act.a()));
        return true;
    }

    // how long a spell has to be held. chant annulment (or great sage's instant cast) skips the chant
    static int castNeed(ServerPlayer p, Ability a, Spec s) {
        if (SkillState.instantCast(p)) return 0;
        Map<String, Active> mine = ACTIVE.get(p.getUUID());
        if (mine != null) for (Active x : mine.values()) if (x.arch.equals("chant")) return 0;
        return AbilityAPI.isMastered(p, a) ? s.castm() : s.cast();
    }

    // magic: the circle under you while you build up the cast, like tensura's castMagicCircle
    static void casting(ServerPlayer p, Ability a, Spec s, int held) {
        ServerLevel l = (ServerLevel) p.level();
        int need = AbilityAPI.isMastered(p, a) ? s.castm() : s.cast();
        if (held % 2 == 0) {
            double r = 1.2 + Math.min(1, held / (double) Math.max(1, need));
            Fx.magicCircle(l, LoreFx.sprite(s.f1()), p.position().add(0, 0.05, 0), r, held);
            LoreFx.dustRing(l, s.c1(), p.position().add(0, 0.06, 0), r, 24, 1f, 0);
            LoreFx.dustRing(l, s.c2(), p.position().add(0, 0.06, 0), r * 0.62, 14, 0.8f, 0);
        }
        if (held % 5 == 0) {
            if (held >= need) p.sendOverlayMessage(Component.translatable("jaycee.lore.cast_ready", a.name()).withStyle(ChatFormatting.GOLD));
            else p.sendOverlayMessage(Component.translatable("jaycee.lore.casting", a.name(), String.format("%.1f", (need - held) / 20f)).withStyle(ChatFormatting.AQUA));
        }
        if (held == need) l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 1.4f);
    }

    // ------------------------------------------------------------ the kinds

    // a bolt that flies, hits the first thing and splashes a bit
    private static final List<Bolt> BOLTS = new ArrayList<>();

    private static final class Bolt {
        final ServerLevel level; final ServerPlayer owner; final Spec spec; final float dmg;
        Vec3 pos; final Vec3 vel; int life;
        Bolt(ServerLevel l, ServerPlayer o, Spec s, float d, Vec3 pos, Vec3 vel, int life) {
            level = l; owner = o; spec = s; dmg = d; this.pos = pos; this.vel = vel; this.life = life;
        }
    }

    static boolean shoot(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 dir = p.getLookAngle();
        double speed = s.type() == SkillType.ULTIMATE ? 2.4 : 1.6;
        int life = (int) Math.ceil(s.range() / speed) + 2;
        Vec3 start = p.getEyePosition().add(dir.scale(0.8)).add(0, -0.2, 0);
        switch (s.pat("projectile")) {
            case "triple" -> {
                for (int k = -1; k <= 1; k++) BOLTS.add(new Bolt(l, p, s, dmg * 0.45f, start, dir.yRot(k * 0.14f).scale(speed), life));
            }
            case "barrage" -> {
                for (int k = 0; k < 5; k++) {
                    Vec3 d = dir.add((Math.random() - 0.5) * 0.12, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.12).normalize();
                    BOLTS.add(new Bolt(l, p, s, dmg * 0.3f, start.add(d.scale(-k * 0.8)), d.scale(speed), life + k));
                }
            }
            case "meteor" -> {
                // drops out of the sky onto where you look
                Vec3 at = aim(p, s.range());
                Vec3 sky = at.add(-3, 14, -2);
                Vec3 d = at.subtract(sky).normalize();
                BOLTS.add(new Bolt(l, p, s, dmg, sky, d.scale(speed * 1.2), (int) Math.ceil(at.distanceTo(sky) / (speed * 1.2)) + 3));
            }
            default -> BOLTS.add(new Bolt(l, p, s, dmg, start, dir.scale(speed), life));
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Safe.run("LoreEngine.bolts", () -> {
            Iterator<Bolt> it = BOLTS.iterator();
            while (it.hasNext()) {
                Bolt b = it.next();
                if (!b.owner.isAlive() || b.owner.level() != b.level || b.life-- <= 0) { it.remove(); continue; }
                if (stepBolt(b)) it.remove();
            }
            tickSummons(event.getServer().getTickCount());
        });
    }

    // moves one bolt one tick. true when it hit something
    private static boolean stepBolt(Bolt b) {
        Vec3 from = b.pos, to = b.pos.add(b.vel);
        LoreFx.boltTrail(b.level, b.spec, from, to, b.life);
        BlockHitResult wall = b.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b.owner));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        LivingEntity hit = null; double best = Double.MAX_VALUE;
        for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1), e -> foe(b.owner, e))) {
            var h = e.getBoundingBox().inflate(0.3).clip(from, end);
            if (h.isPresent() && from.distanceToSqr(h.get()) < best) { best = from.distanceToSqr(h.get()); hit = e; }
        }
        if (hit == null && wall.getType() == HitResult.Type.MISS) { b.pos = to; return false; }
        Vec3 at = hit != null ? hit.position().add(0, hit.getBbHeight() / 2, 0) : end;
        if (hit != null) hurt(b.owner, hit, b.spec, b.dmg);
        float splash = splash(b.spec);
        if (splash > 0) for (LivingEntity e : near(b.level, b.owner, at, splash)) if (e != hit) hurt(b.owner, e, b.spec, b.dmg * 0.5f);
        LoreFx.boltImpact(b.level, b.spec, at, splash);
        b.level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    private static float splash(Spec s) {
        return switch (s.type()) {
            case MAGIC, ULTIMATE, UNIQUE -> Math.min(s.radius(), 6);
            default -> 1;
        };
    }

    // a straight ray, hits everything on the line
    static boolean beam(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(s.range()));
        BlockHitResult wall = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        final Vec3 stop = end;
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(eye, stop).inflate(1), e -> foe(p, e))) {
            if (e.getBoundingBox().inflate(0.5).clip(eye, stop).isPresent()) hurt(p, e, s, dmg);
        }
        LoreFx.beam(l, s, eye.add(p.getLookAngle().scale(0.6)).add(0, -0.15, 0), stop);
        return true;
    }

    // breaths and sprays: everything in front of you within range
    static boolean cone(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 eye = LoreFx.mouth(p), look = p.getLookAngle();
        double range = Math.min(s.range(), 12);
        for (LivingEntity e : near(l, p, p.position(), range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.normalize().dot(look) > 0.8 && p.hasLineOfSight(e)) hurt(p, e, s, dmg);
        }
        LoreFx.cone(l, s, p, range);
        return true;
    }

    // storms, rains, explosions: lands where you look
    static boolean area(ServerPlayer p, Spec s, float dmg, Vec3 at) {
        ServerLevel l = (ServerLevel) p.level();
        float r = Math.max(2, s.radius());
        for (LivingEntity e : near(l, p, at, r)) hurt(p, e, s, dmg);
        // gravity storms drag everything to the middle, vortexes too
        if (s.tag("gravity") || s.pat("area").equals("vortex")) for (LivingEntity e : near(l, p, at, r + 2)) pull(e, at, 0.6);
        LoreFx.area(l, s, at, r, p.tickCount);
        return true;
    }

    // melee arts and claws: a sweep in front of you. your own attack adds in, like tensura's attack multipliers
    static boolean slash(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double reach = Math.max(4, Math.min(s.range(), 8));
        float total = dmg + (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean any = false;
        for (LivingEntity e : near(l, p, p.position(), reach)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.normalize().dot(look) > 0.45) { hurt(p, e, s, total); any = true; }
        }
        LoreFx.slash(l, s, p);
        return true;
    }

    // heals you a lot and people near you some. cures and antidotes clear bad effects
    static boolean heal(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        float amount = Math.max(6, dmg);
        p.heal(amount);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(s.radius() + 2), e -> e != p && !foe(p, e))) {
            e.heal(amount * 0.5f);
            Fx.aura(l, part(s.acc()), e, 6);
        }
        if (s.id().matches(".*(cure|antidote|purif|clean|recovery).*")) {
            p.getActiveEffects().stream().filter(i -> !i.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList().forEach(p::removeEffect);
        }
        LoreFx.heal(l, s, p);
        return true;
    }

    // dashes and teleports: straight to where you look (or right behind someone if you look at them)
    static boolean move(ServerPlayer p, Spec s, boolean behind) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 from = p.position();
        LivingEntity t = SkillHelper.targetQuiet(p, s.range());
        Vec3 to;
        if (behind && t != null && foe(p, t)) {
            to = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        } else {
            Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(s.range()));
            BlockHitResult hit = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 stop = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(p.getLookAngle().scale(0.8));
            to = new Vec3(stop.x, Math.max(stop.y - p.getEyeHeight(), l.getMinY()), stop.z);
        }
        if (!l.noCollision(p, p.getBoundingBox().move(to.subtract(from)))) to = to.add(0, 1, 0);
        if (!l.noCollision(p, p.getBoundingBox().move(to.subtract(from)))) {
            p.sendOverlayMessage(Component.translatable("jaycee.lore.blocked").withStyle(ChatFormatting.RED));
            return false;
        }
        LoreFx.move(l, s, from, to);
        p.teleportTo(to.x, to.y, to.z);
        p.resetFallDistance();
        return true;
    }

    // ------------------------------------------------------------ summons

    private record Summon(ServerLevel level, UUID mob, UUID owner, long until) {}
    private static final List<Summon> SUMMONS = new ArrayList<>();

    // what shows up depends on the element. they fight for you and leave after a while
    static boolean summon(ServerPlayer p, Spec s, EntityType<?> forced, int countMult) {
        ServerLevel l = (ServerLevel) p.level();
        EntityType<?> type = forced != null ? forced : switch (s.elem()) {
            case "flame", "ember", "black_flame" -> EntityTypes.BLAZE;
            case "frost" -> EntityTypes.STRAY;
            case "gust" -> EntityTypes.BREEZE;
            case "void", "soul", "dark_spark", "mist" -> EntityTypes.WITHER_SKELETON;
            case "spirit", "rune", "spark" -> EntityTypes.VEX;
            case "bubble", "drop" -> EntityTypes.DROWNED;
            case "acid" -> EntityTypes.CAVE_SPIDER;
            case "holy", "shard", "shockwave", "aura" -> EntityTypes.IRON_GOLEM;
            default -> EntityTypes.WOLF;
        };
        int count = countMult * switch (s.type()) { case ULTIMATE -> 3; case UNIQUE, MAGIC -> 2; default -> 1; };
        Vec3 at = aim(p, Math.min(s.range(), 8));
        for (int i = 0; i < count; i++) {
            Entity e = type.create(l, EntitySpawnReason.MOB_SUMMONED);
            if (!(e instanceof Mob m)) continue;
            m.setPos(at.x + (i - count / 2.0), at.y, at.z);
            if (m instanceof TamableAnimal pet) pet.tame(p);
            m.setPersistenceRequired();
            m.setData(Bonds.OWNER, p.getStringUUID());
            l.addFreshEntity(m);
            SUMMONS.add(new Summon(l, m.getUUID(), p.getUUID(), l.getGameTime() + Math.max(600, s.dur())));
            LoreFx.summon(l, s, m.position(), p.getLookAngle(), p.tickCount);
        }
        return true;
    }

    // summons go after whatever you hit or whatever hit you, and fade out when their time's up
    private static void tickSummons(int tick) {
        if (tick % 20 != 0 || SUMMONS.isEmpty()) return;
        Iterator<Summon> it = SUMMONS.iterator();
        while (it.hasNext()) {
            Summon s = it.next();
            Entity e = s.level.getEntity(s.mob);
            if (!(e instanceof Mob m) || !m.isAlive()) { it.remove(); continue; }
            if (s.level.getGameTime() > s.until) {
                Fx.burst(s.level, BrightParticles.SPIRIT, m.position().add(0, 1, 0), 20, 0.15);
                m.discard(); it.remove(); continue;
            }
            if (s.level.getPlayerByUUID(s.owner) instanceof ServerPlayer o) {
                LivingEntity enemy = o.getLastHurtMob() != null && o.getLastHurtMob().isAlive() ? o.getLastHurtMob() : o.getLastHurtByMob();
                if (enemy != null && enemy.isAlive() && enemy != m && foe(o, enemy)) m.setTarget(enemy);
                else if (m.getTarget() instanceof Player) m.setTarget(null);
                if (m.distanceToSqr(o) > 32 * 32) m.teleportTo(o.getX(), o.getY(), o.getZ());
            }
        }
    }

    // ------------------------------------------------------------ auras, guards, senses, awakenings

    // what's switched on right now, per player: skill id -> game time it ends (Long.MAX for toggles)
    private record Active(Spec spec, String arch, long until) {}
    private static final Map<UUID, Map<String, Active>> ACTIVE = new HashMap<>();

    static boolean timed(ServerPlayer p, Spec s, String act, int dur) {
        ACTIVE.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(s.id() + "/" + act, new Active(s, act, p.level().getGameTime() + dur));
        started(p, s, act);
        return true;
    }

    // the moment it switches on: the big version of its look, and a sound (toggles come through here too)
    static void started(ServerPlayer p, Spec s, String act) {
        ServerLevel l = (ServerLevel) p.level();
        String arch = LoreActions.kind(act);
        LoreFx.self(l, s, p, arch, p.tickCount, true);
        if (arch.equals("awaken")) {
            LoreFx.self(l, s, p, "awaken", p.tickCount, true);
            Fx.shockwave(l, p.position().add(0, 1, 0), 1.5);
        }
        if (s.tag("undying")) { p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, tier(s))); }
        sound(l, p, s, arch);
    }

    // toggles: on until you switch it off, costs a tenth of the skill's price every second
    static void toggledTick(ServerPlayer p, Ability a, Spec s) {
        Map<String, Active> mine = ACTIVE.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        String act = s.act(0).a();
        mine.putIfAbsent(s.id() + "/" + act, new Active(s, act, Long.MAX_VALUE));
        if (p.tickCount % 20 == 0 && !p.isCreative()) {
            int mp = Math.max(0, s.mp() / 10), ap = Math.max(0, s.ap() / 10);
            boolean paid = (mp == 0 || EnergyAPI.spendMana(p, mp)) && (ap == 0 || EnergyAPI.spendAura(p, ap));
            if (!paid) {
                p.sendOverlayMessage(Component.translatable("jaycee.skill.lack_magicule", a.name()).withStyle(ChatFormatting.RED));
                AbilityAPI.setToggled(p, a, false);
                return;
            }
        }
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
    }

    static void stop(ServerPlayer p, Spec s) {
        Map<String, Active> mine = ACTIVE.get(p.getUUID());
        if (mine == null) return;
        mine.keySet().removeIf(k -> k.startsWith(s.id() + "/"));
        clearBuffs(p, s);
    }

    static boolean isActive(ServerPlayer p, Spec s) {
        Map<String, Active> mine = ACTIVE.get(p.getUUID());
        return mine != null && mine.keySet().stream().anyMatch(k -> k.startsWith(s.id() + "/"));
    }

    private static Identifier mod(Spec s, String what) {
        return Identifier.fromNamespaceAndPath(com.brightwebs.jaycee.JayceeMod.ID, "lore/" + s.id() + "/" + what);
    }

    // how strong buffs are by rank
    static int tier(Spec s) {
        return switch (s.type()) { case ULTIMATE -> 3; case UNIQUE -> 2; case EXTRA, MAGIC -> 1; default -> 0; };
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Map<String, Active> mine = ACTIVE.get(p.getUUID());
        if (mine == null || mine.isEmpty()) return;
        Safe.run("LoreEngine.active", () -> {
            long now = p.level().getGameTime();
            Iterator<Map.Entry<String, Active>> it = mine.entrySet().iterator();
            while (it.hasNext()) {
                Active a = it.next().getValue();
                if (now > a.until) { it.remove(); clearBuffs(p, a.spec); continue; }
                apply(p, a.spec, a.arch);
            }
        });
    }

    // what an action that sits on you does every tick while it's on (toggles and timed modes)
    private static void apply(ServerPlayer p, Spec s, String act) {
        ServerLevel l = (ServerLevel) p.level();
        int t = tier(s);
        String kind = LoreActions.kind(act);
        switch (act) {
            case "buff" -> {
                // the skill's power pours off you: more attack, and its element on your hits (see onHit)
                attr(p, Attributes.ATTACK_DAMAGE, mod(s, "atk"), 2 + t * 3);
                if (s.elem().matches("gust|spark|ember")) attr(p, Attributes.MOVEMENT_SPEED, mod(s, "spd"), 0.01 + 0.01 * t);
                if (s.elem().matches("frost|shard|rune|holy|aura")) attr(p, Attributes.ARMOR, mod(s, "arm"), 2 + t * 2);
                if (s.elem().matches("heal|petal|holy")) SkillHelper.keep(p, MobEffects.REGENERATION, Math.min(1, t));
                if (s.tag("beast")) { SkillHelper.keep(p, MobEffects.SPEED, 0); SkillHelper.keep(p, MobEffects.STRENGTH, 0); }
            }
            case "rage" -> {
                // gets stronger the longer it stays on, like wrath
                Map<String, Active> mine = ACTIVE.get(p.getUUID());
                Active me = mine == null ? null : mine.get(s.id() + "/rage");
                long on = me == null || me.until == Long.MAX_VALUE ? (p.tickCount % 2400) : (s.dur() - (me.until - l.getGameTime()));
                SkillHelper.keep(p, MobEffects.STRENGTH, (int) Math.min(2 + t, on / 300));
                SkillHelper.keep(p, MobEffects.SPEED, 0);
                attr(p, Attributes.ARMOR, mod(s, "arm"), -2);
            }
            case "haste" -> { SkillHelper.keep(p, MobEffects.SPEED, 1 + Math.min(1, t)); SkillHelper.keep(p, MobEffects.HASTE, 1); attr(p, Attributes.ATTACK_SPEED, mod(s, "spd"), 0.5 + 0.3 * t); }
            case "invis", "mimic" -> {
                SkillHelper.keep(p, MobEffects.INVISIBILITY, 0);
                if (p.tickCount % 10 == 0) for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), m -> m.getTarget() == p)) m.setTarget(null);
            }
            case "barrier", "reflect" -> {
                // the damage cut (and reflecting) is in onIncomingDamage
                attr(p, Attributes.ARMOR, mod(s, "arm"), 4 + t * 3);
                attr(p, Attributes.KNOCKBACK_RESISTANCE, mod(s, "kb"), 0.25 + 0.15 * t);
            }
            case "protect", "music" -> {
                // you and everyone on your side nearby
                if (p.tickCount % 20 == 0) for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12), e -> e == p || !foe(p, e))) {
                    e.addEffect(new MobEffectInstance(act.equals("music") ? MobEffects.SPEED : MobEffects.RESISTANCE, 60, Math.min(1, t), true, true));
                    e.addEffect(new MobEffectInstance(act.equals("music") ? MobEffects.STRENGTH : MobEffects.REGENERATION, 60, 0, true, true));
                    if (e != p && p.tickCount % 40 == 0) LoreFx.self(l, s, e, kind, p.tickCount, false);
                }
                if (act.equals("music") && p.tickCount % 20 == 0) for (LivingEntity e : near(l, p, p.position(), 10)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, t), p);
                if (act.equals("music") && p.tickCount % 8 == 0) l.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 1, 0.4, 0.1, 0.4, 1);
            }
            case "regen" -> SkillHelper.keep(p, MobEffects.REGENERATION, 1 + Math.min(1, t));
            case "undying" -> SkillHelper.keep(p, MobEffects.REGENERATION, 0);
            case "sense" -> {
                if (p.tickCount % 10 == 0) MagicSense.glowAround(p, 16 + 8 * t);
                SkillHelper.keep(p, MobEffects.NIGHT_VISION, 0);
            }
            case "awaken" -> {
                SkillHelper.keep(p, MobEffects.STRENGTH, t);
                SkillHelper.keep(p, MobEffects.RESISTANCE, Math.min(2, t));
                SkillHelper.keep(p, MobEffects.SPEED, Math.min(2, t));
                SkillHelper.keep(p, MobEffects.REGENERATION, Math.min(1, t));
                attr(p, Attributes.MAX_HEALTH, mod(s, "hp"), 10 + 10 * t);
            }
            default -> {}
        }
        int every = kind.equals("awaken") ? 4 : 6;
        if (p.tickCount % every == 0) LoreFx.self(l, s, p, kind, p.tickCount, false);
    }

    private static void attr(ServerPlayer p, Holder<net.minecraft.world.entity.ai.attributes.Attribute> at, Identifier id, double v) {
        AttributeInstance inst = p.getAttribute(at);
        if (inst != null && inst.getModifier(id) == null) inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void clearBuffs(ServerPlayer p, Spec s) {
        for (String w : new String[]{"atk", "spd", "arm", "kb", "hp"}) {
            for (var at : List.of(Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE, Attributes.MAX_HEALTH, Attributes.ATTACK_SPEED)) {
                AttributeInstance inst = p.getAttribute(at);
                if (inst != null) inst.removeModifier(mod(s, w));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (s.arch().equals("sense") || (s.modes().contains("sense"))) MagicSense.clear(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Map<String, Active> mine = ACTIVE.remove(p.getUUID());
            if (mine != null) for (Active a : mine.values()) clearBuffs(p, a.spec);
        }
    }

    // ------------------------------------------------------------ damage, elements, resistances

    // the element of whatever lore hit is landing right now, so resistances can see it
    private static final ThreadLocal<String> HITTING = new ThreadLocal<>();

    static void hurt(ServerPlayer p, LivingEntity t, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        DamageSource src = s.type() == SkillType.MAGIC || s.type() == SkillType.ULTIMATE
                ? l.damageSources().indirectMagic(p, p) : l.damageSources().playerAttack(p);
        if (s.elem().equals("holy") && t.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) dmg *= 2;
        HITTING.set(s.elem());
        try {
            if (t.hurtServer(l, src, dmg)) element(p, t, s, dmg);
        } finally {
            HITTING.remove();
        }
    }

    // what each element does on top of the damage, then what the skill's name says it does
    static void element(ServerPlayer p, LivingEntity t, Spec s, float dmg) {
        int t2 = tier(s);
        tags(p, t, s, dmg, t2);
        switch (s.elem()) {
            case "flame", "ember" -> t.igniteForSeconds(4 + t2 * 2);
            case "black_flame" -> { t.igniteForSeconds(6 + t2 * 2); t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, t2), p); }
            case "frost" -> { t.setTicksFrozen(Math.max(t.getTicksFrozen(), 200)); t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1 + t2), p); }
            case "spark" -> t.addEffect(new MobEffectInstance(JayceeEffects.PARALYSIS, 20 + 10 * t2, 0), p);
            case "dark_spark" -> { t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), p); t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, t2), p); }
            case "void" -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1 + t2), p);
            case "acid" -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1 + t2), p);
            case "mist" -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), p);
            case "soul", "spirit" -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, t2), p);
            case "rune" -> t.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, t2), p);
            case "shard" -> t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), p);
            case "blood" -> p.heal(dmg * 0.3f);
            case "petal", "heal" -> p.heal(dmg * 0.15f);
            case "holy" -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), p);
            case "gust", "shockwave", "bubble", "drop" -> {
                Vec3 push = t.position().subtract(p.position()).normalize();
                double kb = 0.6 + 0.3 * t2;
                t.push(push.x * kb, 0.25, push.z * kb);
                if (s.elem().equals("gust")) t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 15, 0), p);
                if (!s.elem().equals("gust") && !s.elem().equals("shockwave")) t.clearFire();
            }
            default -> {}
        }
    }

    private static void tags(ServerPlayer p, LivingEntity t, Spec s, float dmg, int t2) {
        ServerLevel l = (ServerLevel) p.level();
        for (String tag : s.tags()) switch (tag) {
            // dominating / commanding skills drag the target to you and break its will
            case "control" -> { pull(t, p.position(), 0.5); t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), p); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, t2), p); }
            case "gravity" -> { t.push(0, -0.8, 0); t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2 + t2), p); }
            case "time" -> { t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3 + t2), p); t.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 2), p); }
            case "space" -> {
                // flung somewhere close by through space
                Vec3 to = t.position().add((Math.random() - 0.5) * 8, 0.5, (Math.random() - 0.5) * 8);
                if (l.noCollision(t, t.getBoundingBox().move(to.subtract(t.position())))) { LoreFx.move(l, s, t.position(), to); t.teleportTo(to.x, to.y, to.z); }
            }
            case "drain" -> { p.heal(dmg * 0.25f); LoreFx.essence(l, s.c1(), s.c2(), t, p); }
            case "bind" -> { t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 6), p); t.addEffect(new MobEffectInstance(JayceeEffects.PARALYSIS, 20, 0), p); }
            case "fear" -> t.addEffect(new MobEffectInstance(JayceeEffects.FEAR, 100, 1 + t2), p);
            case "blind" -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), p);
            case "beast" -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), p);
            case "chain" -> {
                // the lightning jumps on to two more nearby
                int jumps = 0;
                for (LivingEntity e : near(l, p, t.position(), 5)) {
                    if (e == t || jumps >= 2) continue;
                    LoreFx.zigzag(l, s, t.position().add(0, t.getBbHeight() / 2, 0), e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.5, true);
                    HITTING.set(s.elem());
                    try { e.hurtServer(l, l.damageSources().indirectMagic(p, p), dmg * 0.5f); } finally { HITTING.remove(); }
                    jumps++;
                }
            }
            default -> {}
        }
    }

    // drags something toward a point
    private static void pull(LivingEntity e, Vec3 to, double k) {
        Vec3 d = to.subtract(e.position());
        if (d.lengthSqr() < 1) return;
        d = d.normalize().scale(k);
        e.push(d.x, 0.15, d.z);
    }

    // undying skills (immortality and friends) keep you alive once a minute while they're on
    private static final Map<UUID, Long> LAST_SAVE = new HashMap<>();

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getAmount() >= p.getHealth()) {
            Map<String, Active> mine = ACTIVE.get(p.getUUID());
            long now = p.level().getGameTime();
            if (mine != null && now - LAST_SAVE.getOrDefault(p.getUUID(), -100000L) > 1200) {
                for (Active a : mine.values()) if (a.spec.tag("undying") || a.arch.equals("undying")) {
                    LAST_SAVE.put(p.getUUID(), now);
                    event.setCanceled(true);
                    p.setHealth(Math.max(p.getHealth(), 4));
                    LoreFx.self((ServerLevel) p.level(), a.spec, p, "awaken", p.tickCount, true);
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1.2f);
                    return;
                }
            }
        }
        onIncomingDamage2(event);
    }

    private static void onIncomingDamage2(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getAmount() <= 0) return;
        Safe.run("LoreEngine.defend", () -> {
            float amount = event.getAmount();
            // guards cut what comes in
            Map<String, Active> mine = ACTIVE.get(p.getUUID());
            if (mine != null) for (Active a : mine.values()) {
                if (a.arch.equals("barrier") || a.arch.equals("protect")) amount *= switch (tier(a.spec)) { case 3 -> 0.5f; case 2 -> 0.6f; case 1 -> 0.7f; default -> 0.8f; };
                if (a.arch.equals("reflect") && event.getSource().getEntity() instanceof LivingEntity att && att != p) {
                    float back = amount * 0.5f;
                    amount *= 0.6f;
                    att.hurtServer((ServerLevel) p.level(), p.level().damageSources().thorns(p), back);
                    LoreFx.dustRing((ServerLevel) p.level(), a.spec.c1(), p.position().add(0, 1, 0), 1, 16, 1f, 0.6);
                }
            }
            // resistances, tensura's rule: resistance halves it unless one hit is over half your current health, nullification blocks it.
            // nullification still breaks against someone with twice your EP (tensura's EP gap rule)
            DamageSource src = event.getSource();
            String el = HITTING.get();
            for (Ability ab : AbilityAPI.owned(p)) {
                Spec s = LoreSkills.spec(ab);
                if (s == null || !s.resist() || !AbilityAPI.isSwitchedOn(p, ab) || !resists(s.id(), src, el)) continue;
                boolean nul = s.type() == SkillType.NULLIFICATION;
                if (nul && src.getEntity() instanceof LivingEntity att && EnergyAPI.ep(att) >= EnergyAPI.ep(p) * 2) nul = false;
                if (nul) amount = 0;
                else if (amount <= p.getHealth() * 0.5f) amount *= 0.5f;
                if (p.tickCount % 20 == 0) Tk.activatedTime(p, ab);
                LoreFx.dustRing((ServerLevel) p.level(), s.c1(), p.position().add(0, 1, 0), 0.9, 14, 0.9f, 0.6);
                if (amount <= 0) break;
            }
            if (amount <= 0) event.setCanceled(true);
            else event.setAmount(amount);
        });
    }

    // which damage a resistance (by its name) covers
    static boolean resists(String id, DamageSource src, String el) {
        boolean fire = src.is(DamageTypeTags.IS_FIRE) || "flame".equals(el) || "ember".equals(el) || "black_flame".equals(el);
        boolean cold = src.is(DamageTypeTags.IS_FREEZING) || "frost".equals(el);
        boolean shock = src.is(DamageTypeTags.IS_LIGHTNING) || "spark".equals(el) || "dark_spark".equals(el);
        boolean physical = src.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) || src.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                || src.is(DamageTypeTags.IS_PROJECTILE) || "slash".equals(el) || "shard".equals(el) || "shockwave".equals(el);
        boolean spirit = "soul".equals(el) || "spirit".equals(el) || "rune".equals(el) || src.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
        boolean holyDark = "holy".equals(el) || "void".equals(el) || "dark_spark".equals(el) || "black_flame".equals(el);
        boolean decay = src.is(net.minecraft.world.damagesource.DamageTypes.WITHER) || "acid".equals(el) || "void".equals(el);
        boolean poison = (src.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) && src.getEntity() == null) || "acid".equals(el);
        if (id.startsWith("cold")) return cold;
        if (id.startsWith("heat") || id.startsWith("flame")) return fire;
        if (id.startsWith("thermal")) return fire || cold;
        if (id.startsWith("electricity")) return shock;
        if (id.startsWith("natural_effects")) return fire || cold || shock || src.is(DamageTypeTags.IS_DROWNING) || src.is(DamageTypeTags.IS_FALL);
        if (id.startsWith("physical")) return physical && !fire;
        if (id.startsWith("pierce")) return src.is(DamageTypeTags.IS_PROJECTILE) || "shard".equals(el);
        if (id.startsWith("pain")) return physical;
        if (id.startsWith("spiritual")) return spirit;
        if (id.startsWith("holy")) return holyDark;
        if (id.startsWith("corrosion")) return decay;
        if (id.startsWith("poison")) return poison;
        return false;
    }

    // bad effects a resistance keeps off you. resistance: half the time, nullification: always
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Safe.run("LoreEngine.effects", () -> {
            Holder<MobEffect> e = event.getEffectInstance().getEffect();
            if (e.value().isBeneficial()) return;
            for (Ability ab : AbilityAPI.owned(p)) {
                Spec s = LoreSkills.spec(ab);
                if (s == null || !s.resist() || !AbilityAPI.isSwitchedOn(p, ab) || !blocksEffect(s.id(), e)) continue;
                if (s.type() == SkillType.NULLIFICATION || p.getRandom().nextBoolean()) {
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                    return;
                }
            }
        });
    }

    static boolean blocksEffect(String id, Holder<MobEffect> e) {
        if (id.startsWith("poison")) return e.is(MobEffects.POISON);
        if (id.startsWith("paralysis")) return e.is(JayceeEffects.PARALYSIS) || e.is(MobEffects.SLOWNESS);
        if (id.startsWith("corrosion")) return e.is(MobEffects.WITHER);
        if (id.startsWith("sleep")) return e.is(MobEffects.BLINDNESS) || e.is(MobEffects.DARKNESS) || e.is(MobEffects.NAUSEA);
        if (id.startsWith("pain")) return e.is(MobEffects.WEAKNESS);
        if (id.startsWith("cold")) return e.is(MobEffects.SLOWNESS);
        if (id.startsWith("abnormal_condition")) return true;
        return false;
    }

    // ------------------------------------------------------------ bits

    // where your look lands: a block, or something alive, or the full range out
    static Vec3 aim(ServerPlayer p, double range) {
        LivingEntity t = SkillHelper.targetQuiet(p, range);
        if (t != null) return t.position();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // things a skill should hit: alive, not you, not your subordinates or pets
    static boolean foe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (Bonds.isSubordinateOf(e, p)) return false;
        if (e instanceof TamableAnimal pet && pet.isOwnedBy(p)) return false;
        return true;
    }

    static List<LivingEntity> near(ServerLevel l, ServerPlayer p, Vec3 at, double r) {
        return l.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), e -> foe(p, e) && e.position().distanceToSqr(at) <= r * r + 4);
    }

    static Supplier<SimpleParticleType> part(String family) {
        var h = BrightParticles.ALL.get(family);
        return h != null ? h : BrightParticles.AURA;
    }

    private static void sound(ServerLevel l, ServerPlayer p, Spec s, String arch) {
        SoundEvent snd = switch (s.elem()) {
            case "flame", "ember", "black_flame" -> SoundEvents.BLAZE_SHOOT;
            case "frost", "shard" -> SoundEvents.GLASS_BREAK;
            case "spark", "dark_spark" -> SoundEvents.TRIDENT_THUNDER.value();
            case "gust" -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
            case "bubble", "drop" -> SoundEvents.PLAYER_SPLASH;
            case "holy", "heal", "petal" -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case "void", "soul", "spirit", "mist" -> SoundEvents.SOUL_ESCAPE.value();
            default -> arch.equals("slash") ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.EVOKER_CAST_SPELL;
        };
        l.playSound(null, p.getX(), p.getY(), p.getZ(), snd, SoundSource.PLAYERS, 1, 0.9f + p.getRandom().nextFloat() * 0.2f);
    }
}

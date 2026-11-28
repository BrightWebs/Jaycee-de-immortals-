package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.effect.JayceeEffects;
import com.brightwebs.jaycee.skill.LoreSkills.Act;
import com.brightwebs.jaycee.skill.LoreSkills.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// the actions lore skill modes are made of. each mode in lore_skills.json says which one it is
// (tools/skill_design.py), so a mode called "Infinite Prison" really imprisons and "Predation" really devours
@EventBusSubscriber(modid = com.brightwebs.jaycee.JayceeMod.ID)
public final class LoreActions {
    private LoreActions() {}

    // which look kind (LoreFx) each action draws with
    static String kind(String act) {
        if (Sins.handles(act)) return "sense";
        return switch (act) {
            case "bolt", "lance", "barrage", "meteor" -> "projectile";
            case "beam", "chain" -> "beam";
            case "breath" -> "cone";
            case "blast", "nova", "blackhole", "field", "gravity", "explode", "pull", "repel", "timestop", "weather" -> "area";
            case "strike", "flurry", "cleave", "execute" -> "slash";
            case "dash", "blink", "behind", "teleport", "swap" -> "move";
            case "heal", "rest", "regen" -> "heal";
            case "summon", "clone", "weapon" -> "summon";
            case "void" -> "area";
            case "corrosion", "grasp" -> "aura";
            case "receive", "provide", "mimicry" -> "sense";
            case "barrier", "reflect", "protect", "imprison" -> "guard";
            case "sense", "analyze", "storage", "craft", "copy", "bind" -> "sense";
            case "awaken", "rage", "undying", "fear", "dominate" -> "awaken";
            default -> "aura";
        };
    }

    // how much a mode costs and how long it waits, compared with the skill's base numbers
    static double costMult(String act) {
        return switch (act) {
            case "analyze", "storage", "craft", "sense" -> 0.2;
            case "timestop", "blackhole", "dominate", "undying", "explode" -> 3;
            case "execute", "summon", "clone", "awaken", "imprison", "copy", "devour" -> 2;
            default -> 1;
        };
    }

    // modes whose cost is their own (from tensura / manas reborn), not the skill's base price. -1 = use the base
    static int fixedCost(String act) {
        if (Sins.handles(act)) return Sins.cost(act);
        return switch (act) {
            case "devour", "storage", "isolate", "receive", "provide", "corrosion", "grasp", "darkness", "soul_steal", "soul_devour" -> 0;
            case "food_chain" -> 15000;  // they spend their own as they go
            case "void" -> 2000;
            case "mimicry" -> 1000;
            default -> -1;
        };
    }

    static int cooldown(Spec s, Act act, boolean mastered) {
        // always the unmastered number: AbilityAPI.setCooldown shortens it as mastery grows
        mastered = false;
        int base = s.cd();
        if (Sins.handles(act.a())) return Sins.cooldown(act.a(), mastered);
        return switch (act.a()) {
            case "devour", "isolate" -> 0;  // predation sets its own (5s, 3s mastered)
            case "storage", "receive", "provide" -> 20;
            case "corrosion", "grasp", "darkness", "soul_steal" -> 20;
            case "soul_devour" -> 10;
            case "food_chain" -> 100;
            case "void" -> mastered ? 300 : 600;
            case "mimicry" -> 200;
            case "analyze", "craft" -> 20;
            case "timestop" -> Math.max(base * 6, 600);
            case "undying" -> Math.max(base * 8, 1200);
            case "dominate", "charm" -> Math.max(base * 4, 400);
            case "execute", "blackhole", "explode" -> Math.max(base * 3, 200);
            case "summon", "clone", "weapon" -> Math.max(base * 3, 600);
            case "awaken", "rage" -> Math.max(base, s.dur() + 600);
            case "teleport", "swap" -> Math.max(base, 100);
            default -> base;
        };
    }

    // ------------------------------------------------------------ doing it

    static boolean run(ServerPlayer p, Ability a, Spec s, Act act) {
        ServerLevel l = (ServerLevel) p.level();
        float dmg = s.dmg() * act.p() * (1f + 0.25f * AbilityAPI.masteryFraction(p, a));
        int t = LoreEngine.tier(s);
        String k = act.a();
        boolean ok = switch (k) {
            // ---- attacks
            case "bolt" -> LoreEngine.shoot(p, s, dmg);
            case "lance" -> LoreEngine.shoot(p, s.with("projectile", "lance"), dmg * 1.1f);
            case "barrage" -> LoreEngine.shoot(p, s.with("projectile", "barrage"), dmg);
            case "meteor" -> LoreEngine.shoot(p, s.with("projectile", "meteor"), dmg * 1.2f);
            case "beam" -> LoreEngine.beam(p, s, dmg);
            case "chain" -> chain(p, s, dmg);
            case "breath" -> LoreEngine.cone(p, s, dmg);
            case "blast" -> LoreEngine.area(p, s, dmg, aim(p, s.range()));
            case "nova", "explode" -> LoreEngine.area(p, s.with("area", "nova"), dmg * (k.equals("explode") ? 1.6f : 1f), p.position());
            case "blackhole" -> blackhole(p, s, dmg);
            case "field" -> field(p, s, dmg, aim(p, s.range()), 160, false);
            case "gravity" -> gravity(p, s, dmg);
            case "strike" -> LoreEngine.slash(p, s, dmg);
            case "flurry" -> { boolean any = false; for (int i = 0; i < 3; i++) any |= LoreEngine.slash(p, s.with("slash", "flurry"), dmg * 0.5f); yield any; }
            case "cleave" -> LoreEngine.slash(p, s.with("slash", "cleave"), dmg * 1.3f);
            case "execute" -> execute(p, s, dmg);
            // ---- moving
            case "dash" -> dash(p, s, dmg);
            case "blink" -> LoreEngine.move(p, s.withRange(Math.min(s.range(), 16)), false);
            case "behind" -> LoreEngine.move(p, s.withRange(Math.max(s.range(), 20)), true);
            case "teleport" -> LoreEngine.move(p, s.withRange(64), false);
            case "swap" -> swap(p, s);
            // ---- control
            case "pull" -> push(p, s, -1.2, 12);
            case "repel" -> push(p, s, 1.6, 8);
            case "bind" -> onTarget(p, s, 16, e -> { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 6), p); e.addEffect(new MobEffectInstance(JayceeEffects.PARALYSIS, 40, 0), p); LoreFx.dustRing(l, s.c1(), e.position().add(0, 0.6, 0), 0.7, 14, 1f, 0); });
            case "imprison" -> onTarget(p, s, 20, e -> imprison(l, p, s, e, 140 + 40 * t));
            case "sleep" -> around(p, s, 8 + 2 * t, e -> { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 9), p); e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0), p); e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 4), p); if (e instanceof Mob m) m.setTarget(null); LoreFx.dust(l, s.c2(), e.getEyePosition().add(0, 0.5, 0), 4, 0.2, 1f); });
            case "timestop" -> timestop(p, s, 10 + 3 * t, 80 + 30 * t);
            case "fear" -> around(p, s, 10 + 2 * t, e -> e.addEffect(new MobEffectInstance(JayceeEffects.FEAR, 160, 2 + t), p));
            case "charm" -> onTarget(p, s, 16, e -> charm(p, s, e, 300 + 100 * t));
            case "dominate" -> onTarget(p, s, 16, e -> dominate(p, s, e));
            case "dispel" -> dispel(p, s, 10);
            case "suppress" -> around(p, s, 10, e -> { e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1 + t), p); e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 2), p); strip(e, true); });
            case "debuff" -> around(p, s, 10, e -> { LoreEngine.element(p, e, s, dmg * 0.3f); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, t), p); e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, t), p); });
            // ---- taking
            case "drain" -> onTarget(p, s, 12, e -> { LoreEngine.hurt(p, e, s, dmg * 0.7f); p.heal(dmg * 0.5f); for (int i = 0; i < 3; i++) LoreFx.essence(l, s.c1(), s.c2(), e, p); });
            case "essence" -> onTarget(p, s, 12, e -> { double got = Tk.drainMana(e, s.mp() * 2.0); Tk.gainMana(p, got, false); p.heal(2); for (int i = 0; i < 3; i++) LoreFx.essence(l, s.c2(), s.c1(), e, p); });
            case "copy" -> GreatSage.analysis(p, a);
            case "devour" -> Predation.predation(p, a, Gluttonous.predation(s));
            case "void" -> Gluttonous.devouringVoid(p, a, s);
            case "receive" -> Gluttonous.receive(p, a);
            case "provide" -> Gluttonous.provide(p, a);
            case "mimicry" -> Gluttonous.mimicry(p, a, s);
            case "soul_devour" -> Gluttonous.soulDevour(p);
            case "food_chain" -> Gluttonous.foodChain(p, a);
            case "corrosion", "grasp" -> true;  // these are held, see held()
            case "isolate" -> Predation.isolation(p, a);
            case "analyze" -> LoreEngine.timed(p, s, "analyze", 600);
            case "storage" -> { Stomach.open(p); yield true; }
            case "craft" -> { Refining.openSage(p); yield true; }
            // ---- on you (for a while)
            case "buff", "rage", "haste", "invis", "barrier", "reflect", "protect", "regen", "undying", "awaken", "mimic", "sense", "music", "chant" -> LoreEngine.timed(p, s, k, k.equals("awaken") || k.equals("rage") ? s.dur() : 600);
            case "heal" -> LoreEngine.heal(p, s, dmg);
            case "rest" -> { p.heal(p.getMaxHealth() * 0.3f); p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2)); LoreFx.heal(l, s, p); yield true; }
            // ---- other
            case "summon" -> LoreEngine.summon(p, s, null, 1);
            case "clone" -> LoreEngine.summon(p, s, net.minecraft.world.entity.EntityTypes.ZOMBIE, 2);
            case "weapon" -> weapon(p, s, dmg);
            case "weather" -> weather(p, s);
            default -> Sins.handles(k) && Sins.run(p, a, s, k);
        };
        return ok;
    }

    // actions you hold the key down for (like tensura's corrosion and manas reborn's ravenous grasp)
    static final java.util.Set<String> HELD = union(java.util.Set.of("corrosion", "grasp", "darkness", "soul_steal"), Sins.HELD);
    private static java.util.Set<String> union(java.util.Set<String> a, java.util.Set<String> b) { var u = new java.util.HashSet<>(a); u.addAll(b); return java.util.Set.copyOf(u); }

    static boolean held(ServerPlayer p, Ability a, Spec s, Act act, int ticks) {
        return switch (act.a()) {
            case "corrosion" -> Gluttonous.corrosion(p, a, s, ticks);
            case "grasp" -> Gluttonous.grasp(p, a, s, ticks);
            case "darkness" -> Gluttonous.darkness(p, a, s, ticks);
            case "soul_steal" -> Gluttonous.soulSteal(p, a, s, ticks);
            default -> Sins.HELD.contains(act.a()) && Sins.held(p, a, s, act.a(), ticks);
        };
    }

    // ------------------------------------------------------------ helpers for the actions above

    private interface Each { void on(LivingEntity e); }

    private static boolean onTarget(ServerPlayer p, Spec s, double range, Each each) {
        LivingEntity t = SkillHelper.target(p, range);
        if (t == null || !LoreEngine.foe(p, t)) return false;
        each.on(t);
        LoreFx.beam((ServerLevel) p.level(), s.with("beam", "straight"), p.getEyePosition(), t.position().add(0, t.getBbHeight() / 2, 0));
        return true;
    }

    private static boolean around(ServerPlayer p, Spec s, double r, Each each) {
        ServerLevel l = (ServerLevel) p.level();
        List<LivingEntity> hit = LoreEngine.near(l, p, p.position(), r);
        for (LivingEntity e : hit) each.on(e);
        LoreFx.area(l, s.with("area", "nova"), p.position(), (float) Math.min(r, 8), p.tickCount);
        return true;
    }

    static Vec3 aim(ServerPlayer p, double range) {
        LivingEntity t = SkillHelper.targetQuiet(p, range);
        if (t != null) return t.position();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // lightning that jumps from the first thing hit to more around it
    private static boolean chain(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        LivingEntity first = SkillHelper.targetQuiet(p, s.range());
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6));
        if (first == null || !LoreEngine.foe(p, first)) { LoreFx.beam(l, s.with("beam", "zigzag"), from, aim(p, s.range())); return true; }
        LoreFx.beam(l, s.with("beam", "zigzag"), from, first.position().add(0, first.getBbHeight() / 2, 0));
        LoreEngine.hurt(p, first, s, dmg);
        LivingEntity at = first;
        List<LivingEntity> done = new ArrayList<>(List.of(first));
        for (int jump = 0; jump < 3; jump++) {
            LivingEntity next = null;
            for (LivingEntity e : LoreEngine.near(l, p, at.position(), 6)) if (!done.contains(e)) { next = e; break; }
            if (next == null) break;
            LoreFx.zigzag(l, s, at.position().add(0, at.getBbHeight() / 2, 0), next.position().add(0, next.getBbHeight() / 2, 0), 6, 0.5, true);
            LoreEngine.hurt(p, next, s, dmg * 0.6f);
            done.add(next); at = next;
        }
        return true;
    }

    // a hole that drags everything in and grinds it for a couple of seconds
    private static boolean blackhole(ServerPlayer p, Spec s, float dmg) {
        Vec3 at = aim(p, s.range());
        return field(p, s.with("area", "vortex"), dmg, at, 60, true);
    }

    // lingering zone: hurts and applies the element every second, optionally pulls things in
    private record Zone(ServerLevel level, ServerPlayer owner, Spec spec, Vec3 at, float dmg, float r, long until, boolean pull) {}
    private static final List<Zone> ZONES = new ArrayList<>();

    private static boolean field(ServerPlayer p, Spec s, float dmg, Vec3 at, int ticks, boolean pull) {
        ServerLevel l = (ServerLevel) p.level();
        float r = Math.max(3, Math.min(s.radius() + 1, 8));
        ZONES.add(new Zone(l, p, s, at, dmg, r, l.getGameTime() + ticks, pull));
        LoreFx.area(l, s, at, r, p.tickCount);
        return true;
    }

    private static boolean gravity(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 at = aim(p, s.range());
        float r = Math.max(3, Math.min(s.radius() + 1, 7));
        for (LivingEntity e : LoreEngine.near(l, p, at, r)) {
            e.push(0, -1.5, 0);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3 + LoreEngine.tier(s)), p);
            LoreEngine.hurt(p, e, s, dmg * 0.8f);
        }
        LoreFx.area(l, s.with("area", "quake"), at, r, p.tickCount);
        return true;
    }

    // finishes off something weak (under a quarter of its health), otherwise just a heavy hit
    private static boolean execute(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        LivingEntity t = SkillHelper.target(p, Math.max(6, Math.min(s.range(), 16)));
        if (t == null || !LoreEngine.foe(p, t)) return false;
        boolean weak = t.getHealth() <= t.getMaxHealth() * 0.25f || t.getHealth() <= dmg;
        if (weak && !(t instanceof Player pl && pl.isCreative())) {
            t.hurtServer(l, l.damageSources().indirectMagic(p, p), Float.MAX_VALUE);
            LoreFx.fade(l, s.c1(), s.c2(), t.position().add(0, 1, 0), 40, 0.6, 1.8f);
            l.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8f, 1.4f);
        } else LoreEngine.hurt(p, t, s, dmg * 1.5f);
        LoreFx.slash(l, s.with("slash", "cross"), p);
        return true;
    }

    // charges forward, hitting everything on the way
    private static boolean dash(ServerPlayer p, Spec s, float dmg) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 from = p.position(), dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        double dist = Math.min(s.range(), 12);
        Vec3 to = from;
        for (double d = 0.5; d <= dist; d += 0.5) {
            Vec3 next = from.add(dir.scale(d));
            if (!l.noCollision(p, p.getBoundingBox().move(next.subtract(from)))) break;
            to = next;
        }
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.2, 1, 1.2), e -> LoreEngine.foe(p, e))) LoreEngine.hurt(p, e, s, dmg);
        LoreFx.move(l, s.with("move", "dash"), from, to);
        p.teleportTo(to.x, to.y, to.z);
        p.resetFallDistance();
        return true;
    }

    private static boolean swap(ServerPlayer p, Spec s) {
        ServerLevel l = (ServerLevel) p.level();
        LivingEntity t = SkillHelper.target(p, Math.max(16, s.range()));
        if (t == null) return false;
        Vec3 a = p.position(), b = t.position();
        LoreFx.move(l, s, a, b);
        p.teleportTo(b.x, b.y, b.z);
        t.teleportTo(a.x, a.y, a.z);
        return true;
    }

    // dir -1 pulls things to you, +1 throws them off
    private static boolean push(ServerPlayer p, Spec s, double k, double r) {
        ServerLevel l = (ServerLevel) p.level();
        for (LivingEntity e : LoreEngine.near(l, p, p.position(), r)) {
            Vec3 d = e.position().subtract(p.position());
            if (d.lengthSqr() < 0.5) continue;
            d = d.normalize().scale(k);
            e.push(d.x, 0.35, d.z);
            if (k > 0) LoreEngine.element(p, e, s, s.dmg() * 0.3f);
        }
        LoreFx.area(l, s.with("area", k < 0 ? "vortex" : "nova"), p.position(), (float) Math.min(r, 8), p.tickCount);
        return true;
    }

    // a cage it can't get out of for a while
    private record Cage(ServerLevel level, UUID who, Vec3 at, Spec spec, long until) {}
    private static final List<Cage> CAGES = new ArrayList<>();

    private static void imprison(ServerLevel l, ServerPlayer p, Spec s, LivingEntity e, int ticks) {
        CAGES.add(new Cage(l, e.getUUID(), e.position(), s, l.getGameTime() + ticks));
        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9), p);
        e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 3), p);
        if (e instanceof Mob m) m.setTarget(null);
    }

    // everything nearby stops dead for a few seconds (mobs lose their brains, players crawl)
    private record Frozen(ServerLevel level, UUID who, long until) {}
    private static final List<Frozen> FROZEN = new ArrayList<>();

    private static boolean timestop(ServerPlayer p, Spec s, double r, int ticks) {
        ServerLevel l = (ServerLevel) p.level();
        for (LivingEntity e : LoreEngine.near(l, p, p.position(), r)) {
            if (e instanceof Mob m && !m.isNoAi()) { m.setNoAi(true); FROZEN.add(new Frozen(l, m.getUUID(), l.getGameTime() + ticks)); }
            else e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9), p);
            e.setDeltaMovement(Vec3.ZERO);
        }
        LoreFx.area(l, s.with("area", "dome"), p.position(), (float) Math.min(r, 10), p.tickCount);
        for (int i = 0; i < 3; i++) LoreFx.dustRing(l, s.c2(), p.position().add(0, 0.1 + i * 0.05, 0), r * (0.4 + i * 0.3), 40, 1.2f, 0);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);
        return true;
    }

    // a charmed mob turns on whatever's near it for a while, then goes back to normal
    private record Charmed(ServerLevel level, UUID who, long until) {}
    private static final List<Charmed> CHARMED = new ArrayList<>();

    private static void charm(ServerPlayer p, Spec s, LivingEntity e, int ticks) {
        if (!(e instanceof Mob m)) { e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks / 2, 0), p); return; }
        if (!Bonds.owner(m).isEmpty()) return;
        m.setData(Bonds.OWNER, p.getStringUUID());
        m.setTarget(null);
        CHARMED.add(new Charmed((ServerLevel) p.level(), m.getUUID(), p.level().getGameTime() + ticks));
        LoreFx.self((ServerLevel) p.level(), s, m, "aura", 0, true);
    }

    // breaks something weaker than you and makes it yours for good (needs it hurt, or much weaker than you)
    private static void dominate(ServerPlayer p, Spec s, LivingEntity e) {
        if (!(e instanceof Mob m)) { e.addEffect(new MobEffectInstance(JayceeEffects.FEAR, 200, 3), p); return; }
        boolean can = m.getHealth() <= m.getMaxHealth() * 0.5f || EnergyAPI.ep(m) * 2 <= EnergyAPI.ep(p) || p.isCreative();
        if (!can) {
            p.sendOverlayMessage(Component.translatable("jaycee.lore.too_strong", m.getName()).withStyle(ChatFormatting.RED));
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2), p);
            return;
        }
        m.setData(Bonds.OWNER, p.getStringUUID());
        m.setTarget(null);
        m.setPersistenceRequired();
        Bonds.add(p, m, m.getName().getString());
        LoreFx.self((ServerLevel) p.level(), s, m, "awaken", 0, true);
        p.sendOverlayMessage(Component.translatable("jaycee.lore.dominated", m.getName()).withStyle(ChatFormatting.GOLD));
    }

    // takes the good effects off enemies and the bad ones off you
    private static boolean dispel(ServerPlayer p, Spec s, double r) {
        strip(p, false);
        return around(p, s, r, e -> strip(e, true));
    }

    private static void strip(LivingEntity e, boolean good) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> out = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) {
            boolean helpful = i.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
            if (helpful == good) out.add(i.getEffect());
        }
        out.forEach(e::removeEffect);
    }

    // a weapon made of the skill's power, gone after a minute
    static final String CONJURED = "jaycee_conjured_until";

    private static boolean weapon(ServerPlayer p, Spec s, float dmg) {
        ItemStack w = new ItemStack(Items.NETHERITE_SWORD);
        CompoundTag t = new CompoundTag();
        t.putLong(CONJURED, p.level().getGameTime() + 1200);
        w.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        w.set(DataComponents.ITEM_NAME, Component.translatable("jaycee.lore.conjured", Component.translatable("ability.jaycee." + s.id())));
        w.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        w.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
        w.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID, Math.max(8, dmg * 0.4), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build());
        if (!p.getInventory().add(w)) return false;
        LoreFx.summon((ServerLevel) p.level(), s, p.position().add(p.getLookAngle().scale(1.2)), p.getLookAngle(), p.tickCount);
        return true;
    }

    private static boolean weather(ServerPlayer p, Spec s) {
        ServerLevel l = (ServerLevel) p.level();
        boolean storm = s.elem().contains("spark"), rain = storm || s.elem().matches("drop|bubble|frost");
        var w = l.getWeatherData();
        if (rain) { w.setClearWeatherTime(0); w.setRaining(true); w.setRainTime(6000); w.setThundering(storm); w.setThunderTime(storm ? 6000 : 0); }
        else { w.setClearWeatherTime(6000); w.setRaining(false); w.setRainTime(0); w.setThundering(false); w.setThunderTime(0); }
        w.setDirty();
        LoreFx.self(l, s, p, "awaken", p.tickCount, true);
        return true;
    }

    // ------------------------------------------------------------ keeping zones, cages, frozen and charmed things going

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Safe.run("LoreActions.tick", () -> {
            int tick = event.getServer().getTickCount();
            Iterator<Zone> zi = ZONES.iterator();
            while (zi.hasNext()) {
                Zone z = zi.next();
                if (!z.owner.isAlive() || z.level.getGameTime() > z.until) { zi.remove(); continue; }
                if (tick % 4 == 0) LoreFx.area(z.level, z.spec, z.at, z.r, tick);
                for (LivingEntity e : LoreEngine.near(z.level, z.owner, z.at, z.r)) {
                    if (z.pull) { Vec3 d = z.at.subtract(e.position()); if (d.lengthSqr() > 1) e.push(d.normalize().x * 0.25, 0.05, d.normalize().z * 0.25); }
                    if (tick % 20 == 0) LoreEngine.hurt(z.owner, e, z.spec, z.dmg * (z.pull ? 0.4f : 0.25f));
                }
            }
            Iterator<Cage> ci = CAGES.iterator();
            while (ci.hasNext()) {
                Cage c = ci.next();
                var e = c.level.getEntity(c.who);
                if (!(e instanceof LivingEntity le) || !le.isAlive() || c.level.getGameTime() > c.until) { ci.remove(); continue; }
                if (le.position().distanceToSqr(c.at) > 1) le.teleportTo(c.at.x, c.at.y, c.at.z);
                if (tick % 5 == 0) for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    LoreFx.dustLine(c.level, c.spec.c1(), c.at.add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8), c.at.add(Math.cos(a) * 0.8, 2.2, Math.sin(a) * 0.8), 0.25, 1f);
                }
            }
            Iterator<Frozen> fi = FROZEN.iterator();
            while (fi.hasNext()) {
                Frozen f = fi.next();
                var e = f.level.getEntity(f.who);
                if (!(e instanceof Mob m) || !m.isAlive()) { fi.remove(); continue; }
                if (f.level.getGameTime() > f.until) { m.setNoAi(false); fi.remove(); }
            }
            Iterator<Charmed> hi = CHARMED.iterator();
            while (hi.hasNext()) {
                Charmed c = hi.next();
                var e = c.level.getEntity(c.who);
                if (!(e instanceof Mob m) || !m.isAlive()) { hi.remove(); continue; }
                if (c.level.getGameTime() > c.until) { m.setData(Bonds.OWNER, ""); m.setTarget(null); hi.remove(); continue; }
                if (tick % 20 == 0 && (m.getTarget() == null || !m.getTarget().isAlive())) {
                    for (LivingEntity o : c.level.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(12), o -> o != m && o.isAlive() && !(o instanceof Player) && Bonds.owner(o).isEmpty())) { m.setTarget(o); break; }
                }
            }
            // conjured weapons go away when their time's up
            if (tick % 40 == 0) for (ServerPlayer pl : event.getServer().getPlayerList().getPlayers()) {
                var inv = pl.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    CustomData d = inv.getItem(i).get(DataComponents.CUSTOM_DATA);
                    if (d != null && d.contains(CONJURED) && d.copyTag().getLongOr(CONJURED, 0) < pl.level().getGameTime()) inv.setItem(i, ItemStack.EMPTY);
                }
            }
        });
    }
}

package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.effect.LoreEffects;
import com.brightwebs.jaycee.skill.LoreSkills.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// the six other sins and their ultimates, built from tensura's and manas reborn's code (numbers are theirs):
//   wrath / satanael   breeder reactor, enrage, seismic fury, growing rampage, crimson rupture (+ combo hits)
//   envy / leviathan   absorb, strength sap, envious field, resentful barrier, disordered desire (+ luck, dodge)
//   lust / asmodeus    drain, invigorate, rebirth, embracing drain, death blessing, manipulation, transmission, siphoning, implode
//   greed / mammon     spiritual domination, greed flare, death wish, energy seize, endless avarice
//   pride / lucifer    copy (anything that hits you), declare supremacy, supreme order, imperial ascension (+ oppressive aura)
//   sloth / belphegor  deep hypno, fallen hypno, deprive, rest, phantasmal style, fallen strike, lethargy, effort, final lullaby
// the ultimate version of each always gets the mastered numbers, and bigger ones on top (see big())
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Sins {
    private Sins() {}

    // modes you hold the key down for. the rest go off on a press
    static final Set<String> HELD = Set.of("breeder", "enrage", "rampage_cast", "rupture", "sap", "desire", "blessing", "manipulate",
            "transmit", "siphon", "implode", "domination", "death_wish", "ascension", "deep_hypno", "fallen_hypno", "deprive", "sloth_rest", "lullaby");
    static final Set<String> PRESS = Set.of("fury", "absorb", "envy_field", "resent", "lust_drain", "invigorate", "rebirth", "embrace",
            "flare", "seize", "avarice", "pride_copy", "declare", "order", "phantasmal", "fallen_strike", "lethargy", "effort");

    static boolean handles(String act) { return HELD.contains(act) || PRESS.contains(act); }

    // tensura / manas reborn's magicule cost for each mode (held ones pay as they go, so 0 here)
    static int cost(String act) {
        return switch (act) {
            case "fury" -> 5000;
            case "envy_field" -> 2000;
            case "resent" -> 3000;
            case "lust_drain" -> 100;
            case "rebirth" -> 3000;
            case "embrace" -> 500;
            case "death_wish" -> 0;
            case "order" -> 10000;
            case "fallen_strike" -> 5000;
            case "lethargy" -> 2000;
            case "absorb", "invigorate", "flare", "seize", "avarice", "pride_copy", "declare", "phantasmal", "effort" -> 0;
            default -> HELD.contains(act) ? 0 : -1;
        };
    }

    // holds that go off once at the end set their real cooldown when they go off (done()), letting go early only costs a second
    static final Set<String> CAST = Set.of("rampage_cast", "rupture", "desire", "blessing", "transmit", "implode", "death_wish", "ascension", "lullaby", "domination", "manipulate");
    static int cooldown(String act, boolean m) { return CAST.contains(act) ? 20 : castCooldown(act, m); }

    // and the cooldowns, in ticks (theirs are in seconds)
    static int castCooldown(String act, boolean m) {
        return switch (act) {
            case "breeder", "enrage", "sap", "deep_hypno", "fallen_hypno", "deprive", "sloth_rest", "domination", "manipulate", "siphon" -> 20;
            case "fury" -> m ? 100 : 200;
            case "rampage_cast" -> 600;
            case "rupture" -> 600;
            case "absorb" -> 0;  // sets its own
            case "envy_field" -> 400;
            case "resent" -> 200;
            case "desire" -> 400;
            case "lust_drain" -> 20;
            case "invigorate" -> 0;
            case "rebirth" -> 2400;
            case "embrace" -> 200;
            case "blessing" -> 100;
            case "transmit" -> 40;
            case "implode" -> 100;
            case "flare" -> 0;
            case "death_wish" -> 200;
            case "seize" -> 600;
            case "avarice" -> 0;
            case "pride_copy" -> 0;
            case "declare" -> m ? 200 : 400;
            case "order" -> 400;
            case "ascension" -> 6000;
            case "phantasmal" -> 10;
            case "fallen_strike" -> 200;
            case "lethargy" -> m ? 400 : 600;
            case "effort" -> 600;
            case "lullaby" -> 3600;
            default -> 20;
        };
    }

    // ------------------------------------------------------------ shared bits

    static boolean ult(Spec s) { return s.type() == SkillType.ULTIMATE; }
    static boolean mast(ServerPlayer p, Ability a, Spec s) { return ult(s) || AbilityAPI.isMastered(p, a); }
    // how much bigger the ultimate is than the unique
    static double big(Spec s) { return ult(s) ? 1.5 : 1; }
    static double ep(LivingEntity e) { return Math.max(1, EnergyAPI.ep(e)); }
    static ServerLevel lvl(LivingEntity e) { return (ServerLevel) e.level(); }

    static boolean ally(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive()) return false;
        if (Bonds.isSubordinateOf(e, p)) return true;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return true;
        return e.isAlliedTo(p);
    }
    static List<LivingEntity> allies(ServerPlayer p, double r) {
        return lvl(p).getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> ally(p, e));
    }

    static void snd(LivingEntity at, SoundEvent s, float vol, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }
    static void snd(LivingEntity at, net.minecraft.core.Holder<SoundEvent> s, float vol, float pitch) { snd(at, s.value(), vol, pitch); }

    static void bar(ServerPlayer p, String key, ChatFormatting c, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(c));
    }

    // a cast you have to hold: shows the timer, true once it's done
    static boolean timer(ServerPlayer p, int held, int need) {
        if (held % 5 == 0 && held < need)
            bar(p, "jaycee.sin.held", ChatFormatting.GOLD, String.format("%.1f", held / 20f), String.format("%.1f", need / 20f));
        return held >= need;
    }

    // tensura's coloured auras: wisps rising off the body, bigger the bigger you are
    static void aura(LivingEntity e, int c1, int c2, int n) {
        ServerLevel l = lvl(e);
        double w = Math.max(0.4, e.getBbWidth() * 0.7), h = e.getBbHeight();
        for (int i = 0; i < n; i++) {
            double ang = Math.random() * Math.PI * 2, r = w * (0.6 + Math.random() * 0.5);
            LoreFx.fade(l, c1, c2, e.position().add(Math.cos(ang) * r, Math.random() * h * 1.1, Math.sin(ang) * r), 1, 0.04, 1.5f + (float) Math.random());
        }
    }
    // tensura's addServerParticlesAroundSelf: a puff over the whole body
    static void around(LivingEntity e, ParticleOptions part, int n) {
        lvl(e).sendParticles(part, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), n, e.getBbWidth() / 2 + 0.2, e.getBbHeight() / 2, e.getBbWidth() / 2 + 0.2, 0.05);
    }
    static void sprite(LivingEntity e, String name, int n) {
        var h = BrightParticles.ALL.get(name);
        if (h != null) around(e, h.get(), n);
    }
    // a flat wave rolling out from a point (sloth's drowsiness / rest waves, envy's field edge)
    static void wave(ServerLevel l, Vec3 c, double r, int color, int tick) {
        double phase = (tick % 20) / 20.0;
        LoreFx.dustRing(l, color, c.add(0, 0.15, 0), r * phase, (int) (12 + r * 6 * phase), 1.4f, 0);
        LoreFx.dustRing(l, color, c.add(0, 0.15, 0), r * ((phase + 0.5) % 1), (int) (12 + r * 3), 1.0f, 0);
    }

    static void spiritual(ServerPlayer p, LivingEntity t, float dmg) {
        t.hurtServer(lvl(p), lvl(p).damageSources().indirectMagic(p, p), dmg);
    }

    // whoever you're looking at within range
    static LivingEntity look(ServerPlayer p, double r) { return SkillHelper.targetQuiet(p, r); }

    // ------------------------------------------------------------ state

    private static final Map<UUID, Map<String, Object>> TAG = new HashMap<>();
    @SuppressWarnings("unchecked")
    static <T> T get(ServerPlayer p, String k, T def) { Object v = TAG.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).get(k); return v == null ? def : (T) v; }
    static void put(ServerPlayer p, String k, Object v) { TAG.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).put(k, v); }
    static void drop(ServerPlayer p, String k) { var m = TAG.get(p.getUUID()); if (m != null) m.remove(k); }
    static long now(LivingEntity e) { return e.level().getGameTime(); }
    static boolean until(ServerPlayer p, String k) { return get(p, k, 0L) > now(p); }

    // does this player have (one of) these skills
    static Ability owned(Player p, String... ids) {
        for (String id : ids) {
            Ability a = AbilityRegistry.get(Identifier.fromNamespaceAndPath(JayceeMod.ID, id));
            if (a != null && AbilityAPI.has(p, a)) return a;
        }
        return null;
    }

    // ------------------------------------------------------------ dispatch

    static boolean run(ServerPlayer p, Ability a, Spec s, String act) {
        return switch (act) {
            case "fury" -> fury(p, a, s);
            case "absorb" -> absorb(p, a, s);
            case "envy_field" -> envyField(p, a, s);
            case "resent" -> resent(p, a, s);
            case "lust_drain" -> lustDrain(p, a, s);
            case "invigorate" -> invigorate(p, a, s);
            case "rebirth" -> rebirth(p, a, s);
            case "embrace" -> embrace(p, a, s);
            case "flare" -> flare(p, a, s);
            case "seize" -> seize(p, a, s);
            case "avarice" -> avarice(p, a, s);
            case "pride_copy" -> prideCopy(p, a, s);
            case "declare" -> declare(p, a, s);
            case "order" -> order(p, a, s);
            case "phantasmal" -> phantasmal(p, a, s);
            case "fallen_strike" -> fallenStrike(p, a, s);
            case "lethargy" -> lethargy(p, a, s);
            case "effort" -> effort(p, a, s);
            default -> false;
        };
    }

    // held: return false to stop the hold
    static boolean held(ServerPlayer p, Ability a, Spec s, String act, int t) {
        return switch (act) {
            case "breeder" -> breeder(p, a, s, t);
            case "enrage" -> enrage(p, a, s, t);
            case "rampage_cast" -> rampageCast(p, a, s, t);
            case "rupture" -> rupture(p, a, s, t);
            case "sap" -> sap(p, a, s, t);
            case "desire" -> desire(p, a, s, t);
            case "blessing" -> blessing(p, a, s, t);
            case "manipulate", "domination" -> domination(p, a, s, t);
            case "transmit" -> transmit(p, a, s, t);
            case "siphon" -> siphon(p, a, s, t);
            case "implode" -> implode(p, a, s, t);
            case "death_wish" -> deathWish(p, a, s, t);
            case "ascension" -> ascension(p, a, s, t);
            case "deep_hypno" -> deepHypno(p, a, s, t);
            case "fallen_hypno" -> fallenHypno(p, a, s, t);
            case "deprive" -> deprive(p, a, s, t);
            case "sloth_rest" -> slothRest(p, a, s, t);
            case "lullaby" -> lullaby(p, a, s, t);
            default -> false;
        };
    }

    // a hold that ends on its own (a cast that went off) sets the cooldown itself
    static void done(ServerPlayer p, Ability a, String act) {
        AbilityAPI.setCooldown(p, a, castCooldown(act, AbilityAPI.isMastered(p, a)));
        AbilityAPI.addMasteryPoint(p, a);
    }
    static void holdMastery(ServerPlayer p, Ability a, int t) {
        if (t > 0 && t % Tk.MASTERY_HOLD_TICK == 0) AbilityAPI.addMasteryPoint(p, a);
    }

    // ================================================================ WRATH

    // rampage stacks: the level is how far in you are. tensura's wrath: max 10 (20 mastered), manas reborn's: 30 (40)
    static int maxRampage(Spec s, boolean m) { return ult(s) ? 40 : m ? 20 : 10; }

    static void rampage(LivingEntity e, int level, int dur) {
        e.addEffect(new MobEffectInstance(LoreEffects.RAMPAGE, dur, level, true, false, true));
        e.addEffect(new MobEffectInstance(LoreEffects.RAMPAGE_GUARD, dur, 0, true, false, true));
    }
    static int rampageLevel(LivingEntity e) { var i = e.getEffect(LoreEffects.RAMPAGE); return i == null ? -1 : i.getAmplifier(); }

    // breeder reactor: hold to pump magicules into yourself, 2% of your max every 5 seconds (it can go past your max).
    // each pump has a 3% chance (6% once you're over your max, doubled mastered) to stack rampage
    static boolean breeder(ServerPlayer p, Ability a, Spec s, int t) {
        boolean m = mast(p, a, s);
        if (t > 0 && t % 100 == 0) {
            var d = EnergyAPI.get(p);
            boolean over = d.mana() > d.maxMana();
            EnergyAPI.set(p, d.withMana(d.mana() + d.maxMana() * 0.02 * big(s)));
            double chance = (over ? 6 : 3) * (m ? 2 : 1);
            if (p.getRandom().nextFloat() < chance / 100) {
                int lvl = Math.min(rampageLevel(p) + 1, maxRampage(s, m) - 1);
                rampage(p, lvl, 600);
                bar(p, "jaycee.sin.rampage", ChatFormatting.RED, lvl + 1);
            }
        }
        holdMastery(p, a, t);
        aura(p, 0xd01010, 0x400000, 3);
        if (t % 10 == 0) snd(p, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1);
        return true;
    }

    // enrage: everything within 7 that isn't on your side flies into a rampage. the longer you hold, the deeper (+1 level / 10s)
    static boolean enrage(ServerPlayer p, Ability a, Spec s, int t) {
        if (t % 20 == 0 && !Tk.spend(p, a, 100)) return false;
        ServerLevel l = lvl(p);
        if (t % 10 == 0) snd(p, SoundEvents.WARDEN_ANGRY, 2, 1);
        if (t % 3 == 0) aura(p, 0xff2020, 0x600000, 6);
        double r = 7 * big(s);
        for (LivingEntity e : LoreEngine.near(l, p, p.position(), r)) {
            int dur;
            var cur = e.getEffect(LoreEffects.RAMPAGE);
            int lvl = 0;
            if (cur != null && t > 0) { dur = cur.getDuration() + 2; lvl = Math.max(0, (dur - 400) / 200); }
            else dur = 400;
            rampage(e, Math.min(lvl, 9), dur);
            if (t % 10 == 0) around(e, ParticleTypes.ANGRY_VILLAGER, 1);
        }
        holdMastery(p, a, t);
        return true;
    }

    // seismic fury (manas reborn): slam the ground, everything within 10 takes your attack damage x0.5 (x1 mastered) for every combo hit you've stacked
    static boolean fury(ServerPlayer p, Ability a, Spec s) {
        ServerLevel l = lvl(p);
        boolean m = mast(p, a, s);
        int combo = Math.max(1, get(p, "combo", 0));
        float dmg = (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (m ? 1.0 : 0.5) * (1 + combo * 0.1) * big(s));
        double r = 10 * big(s);
        for (LivingEntity e : LoreEngine.near(l, p, p.position(), r)) {
            if (!e.onGround() && e.getY() > p.getY() + 3) continue;
            LoreEngine.hurt(p, e, s, dmg);
            e.push(0, 0.6, 0);
            around(e, ParticleTypes.ANGRY_VILLAGER, 2);
        }
        for (int i = 1; i <= 5; i++) LoreFx.dustRing(l, 0x8a1a0a, p.position().add(0, 0.1, 0), r * i / 5, 20 + i * 8, 2f, 0);
        l.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 6, r / 3, 0.1, r / 3, 0);
        var ring = BrightParticles.ALL.get("shock_ring");
        if (ring != null) l.sendParticles(ring.get(), p.getX(), p.getY() + 0.1, p.getZ(), 1, 0, 0, 0, 0);
        snd(p, SoundEvents.WARDEN_ANGRY, 2, 0.8f);
        snd(p, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2, 0.6f);
        put(p, "combo", 0);
        return true;
    }

    // growing rampage (manas reborn): a 3 second cast, then you rampage for a minute and it keeps stacking every 30 seconds
    static boolean rampageCast(ServerPlayer p, Ability a, Spec s, int t) {
        aura(p, 0xff3030, 0x500000, 4);
        if (t % 20 == 0) snd(p, SoundEvents.WARDEN_AGITATED, 1, 1);
        if (!timer(p, t, 60)) return true;
        boolean m = mast(p, a, s);
        rampage(p, Math.max(0, rampageLevel(p)), 1200);
        put(p, "rampage_grow", now(p) + 1200);
        put(p, "rampage_max", ult(s) ? 40 : m ? 40 : 30);
        snd(p, SoundEvents.WARDEN_ANGRY, 3, 0.7f);
        aura(p, 0xff0000, 0x300000, 40);
        done(p, a, "rampage_cast");
        return false;
    }

    // crimson rupture (manas reborn): needs rampage 10 or higher. hold 5 seconds and it all bursts out of you:
    // 20 damage a rampage level, 5 blocks + half a block a level. your rampage is spent
    static boolean rupture(ServerPlayer p, Ability a, Spec s, int t) {
        int lvl = rampageLevel(p) + 1;
        if (lvl < 10) { if (t <= 1) Tk.fail(p, "jaycee.sin.rupture_need"); return false; }
        if (t % 20 == 0 && !Tk.spend(p, a, 20000 / 5)) return false;
        aura(p, 0xff1010, 0x200000, 8);
        if (t % 10 == 0) snd(p, SoundEvents.WARDEN_HEARTBEAT, 2, 1 + t / 100f);
        if (!timer(p, t, 100)) return true;
        ServerLevel l = lvl(p);
        double r = (5 + 0.5 * lvl) * big(s);
        float dmg = (float) (20 * lvl * big(s));
        for (LivingEntity e : LoreEngine.near(l, p, p.position(), r)) {
            float fall = (float) (1 - e.distanceTo(p) / (r + 1));
            LoreEngine.hurt(p, e, s, dmg * Math.max(0.3f, fall));
            Vec3 d = e.position().subtract(p.position()).normalize().scale(1.5);
            e.push(d.x, 0.5, d.z);
        }
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 4; i++) LoreFx.dustRing(l, i % 2 == 0 ? 0xff1a1a : 0x5a0000, p.position().add(0, 0.5 + i * 0.4, 0), r * (0.4 + i * 0.2), 40, 2.2f, 0);
        Fx.sphere(l, BrightParticles.ALL.getOrDefault("fire_red", BrightParticles.AURA), p.position().add(0, 1, 0), r * 0.6, 60);
        snd(p, SoundEvents.GENERIC_EXPLODE, 4, 0.6f);
        p.removeEffect(LoreEffects.RAMPAGE);
        p.removeEffect(LoreEffects.RAMPAGE_GUARD);
        done(p, a, "rupture");
        return false;
    }

    // ================================================================ ENVY

    // absorb (tensura): someone with more EP than you within 5 blocks. you take the gap between you (half of it 60% of the time),
    // half from their aura and half from their magicules, and keep a quarter of it as your own
    static boolean absorb(ServerPlayer p, Ability a, Spec s) {
        LivingEntity t = SkillHelper.target(p, 5);
        if (t == null) return false;
        double mine = ep(p), theirs = ep(t);
        if (theirs < mine) { Tk.fail(p, "jaycee.sin.ep_not_meet"); snd(p, SoundEvents.FIRE_EXTINGUISH, 1, 1); return false; }
        double gap = Math.min(p.getRandom().nextFloat() >= 0.4 ? (theirs - mine) / 2 : theirs - mine, 1_000_000);
        double got = Tk.drainMana(t, gap / 2);
        var td = EnergyAPI.get(t);
        EnergyAPI.set(t, td.withAura(Math.max(0, td.aura() - gap / 2)));
        Tk.gainMana(p, gap / 4 * big(s), true);
        LoreFx.essence(lvl(p), 0x1a3a1a, 0x5aff8a, t, p);
        aura(t, 0x101010, 0x002000, 20);
        snd(p, SoundEvents.EVOKER_CAST_SPELL, 1, 0.8f);
        AbilityAPI.setCooldown(p, a, (mast(p, a, s) ? 10 : 30) * 20);
        return got > 0 || gap > 0;
    }

    // strength sap (tensura): hold. everything within 15 that's weaker than you gets slower and weaker the bigger the gap,
    // and slowly bleeds EP. it gnaws at you too: every 10 seconds there's a 20% chance you go a bit more insane
    static boolean sap(ServerPlayer p, Ability a, Spec s, int t) {
        holdMastery(p, a, t);
        aura(p, 0x0a0a0a, 0x1a3a1a, 5);
        if (t % 20 != 0) return true;
        if (!Tk.spend(p, a, 1000)) return false;
        if (t % 200 == 0 && p.getRandom().nextFloat() <= 0.2f && !ult(s)) {
            var ins = p.getEffect(LoreEffects.INSANITY);
            p.addEffect(new MobEffectInstance(LoreEffects.INSANITY, 240, ins == null ? 1 : ins.getAmplifier() + 1, true, false, true));
        }
        boolean m = mast(p, a, s);
        double mine = ep(p);
        for (LivingEntity e : LoreEngine.near(lvl(p), p, p.position(), 15 * big(s))) {
            double gap = (m ? 0.8 : 0.6) - ep(e) / mine;
            if (gap < 0) continue;
            int lvl = 1 + (int) (gap / 0.1);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, lvl), p);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, lvl), p);
            Tk.gainMana(p, Tk.drainMana(e, ep(e) * 0.001), false);
            aura(e, 0x101010, 0x0a200a, 4);
        }
        snd(p, SoundEvents.SOUL_ESCAPE, 0.6f, 0.6f);
        return true;
    }

    // envious field (manas reborn): a field 10 blocks wide (15 mastered) that drains everything inside every second
    // (0.1% of their aura and magicules, at least 200, 1000 mastered) and every 5 seconds throws anti skill and weakness on them
    private record Field(ServerLevel level, UUID owner, Vec3 at, double r, double minDrain, long until, boolean ult) {}
    private static final List<Field> FIELDS = new ArrayList<>();

    static boolean envyField(ServerPlayer p, Ability a, Spec s) {
        boolean m = mast(p, a, s);
        FIELDS.removeIf(f -> f.owner.equals(p.getUUID()));
        FIELDS.add(new Field(lvl(p), p.getUUID(), p.position(), (m ? 15 : 10) * big(s), m ? 1000 : 200, now(p) + 600, ult(s)));
        p.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 4, false, false, true));
        snd(p, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.6f);
        return true;
    }

    private static void tickField(Field f, int tick) {
        ServerLevel l = f.level;
        if (tick % 4 == 0) {
            LoreFx.dustRing(l, 0x2a7a3a, f.at.add(0, 0.1, 0), f.r, (int) (f.r * 8), 1.4f, 0);
            for (int i = 0; i < 6; i++) {
                double ang = Math.random() * Math.PI * 2, rr = Math.random() * f.r;
                l.sendParticles(ParticleTypes.ENCHANTED_HIT, f.at.x + Math.cos(ang) * rr, f.at.y + 0.2 + Math.random() * 2, f.at.z + Math.sin(ang) * rr, 1, 0, 0.05, 0, 0.01);
            }
        }
        if (tick % 80 == 0) l.playSound(null, f.at.x, f.at.y, f.at.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1, 0.7f);
        if (tick % 20 != 0) return;
        ServerPlayer owner = l.getServer().getPlayerList().getPlayer(f.owner);
        if (owner == null) return;
        for (LivingEntity e : LoreEngine.near(l, owner, f.at, f.r)) {
            var d = EnergyAPI.get(e);
            double take = Math.max(f.minDrain, (d.maxAura() + d.maxMana()) * 0.001);
            double got = Tk.drainMana(e, take / 2);
            EnergyAPI.set(e, EnergyAPI.get(e).withAura(Math.max(0, EnergyAPI.get(e).aura() - take / 2)));
            Tk.gainMana(owner, got + take / 2, false);
            if (tick % 100 == 0) {
                e.addEffect(new MobEffectInstance(LoreEffects.ANTI_SKILL, 400, 0), owner);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, f.ult ? 2 : 0), owner);
            }
            if (got > 0) l.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.4, 0.3, 0.05);
        }
    }

    // resentful barrier (manas reborn): 20 seconds (30 mastered) where hits come out of your magicules instead of your health
    static boolean resent(ServerPlayer p, Ability a, Spec s) {
        int dur = (mast(p, a, s) ? 600 : 400) * (ult(s) ? 2 : 1);
        p.addEffect(new MobEffectInstance(LoreEffects.RESENTFUL_BARRIER, dur, 0, false, true, true));
        Fx.sphere(lvl(p), BrightParticles.AURA, p.position().add(0, 1, 0), 1.3, 30);
        LoreFx.dustRing(lvl(p), 0x2f8f4f, p.position().add(0, 1, 0), 1.3, 30, 1.2f, 0);
        snd(p, SoundEvents.BEACON_POWER_SELECT, 1, 0.8f);
        return true;
    }

    // disordered desire (manas reborn): a 3 second cast on someone within 10, then a quarter of their EP is yours
    static boolean desire(ServerPlayer p, Ability a, Spec s, int t) {
        LivingEntity e = look(p, 10);
        if (e == null || !LoreEngine.foe(p, e)) { if (t % 20 == 0) bar(p, "jaycee.sin.no_target", ChatFormatting.RED); return t < 200; }
        if (t % 4 == 0) LoreFx.essence(lvl(p), 0x0a2a0a, 0x6aff6a, e, p);
        if (!timer(p, t, 60)) return true;
        if (!Tk.spend(p, a, 50000)) return false;
        double take = ep(e) * 0.25 * big(s);
        double got = Tk.drainMana(e, take);
        var d = EnergyAPI.get(e);
        EnergyAPI.set(e, d.withAura(Math.max(0, d.aura() - (take - got))));
        Tk.gainMana(p, take, true);
        around(e, ParticleTypes.ANGRY_VILLAGER, 4);
        aura(e, 0x050505, 0x103010, 30);
        snd(p, SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.6f);
        done(p, a, "desire");
        return false;
    }

    // ================================================================ LUST

    // drain (tensura): touch someone within 5 and pull 200 EP out of them (more mastered).
    // with nobody there it charges your hands instead: your hits drain for 10 seconds
    static boolean lustDrain(ServerPlayer p, Ability a, Spec s) {
        LivingEntity t = SkillHelper.targetQuiet(p, 5);
        if (t != null && LoreEngine.foe(p, t)) {
            double take = 200 + (mast(p, a, s) ? ep(t) * 0.005 : 0);
            Tk.gainMana(p, Tk.drainMana(t, take * big(s)), false);
            sprite(t, "spark_yellow", 12);
            snd(p, SoundEvents.SOUL_ESCAPE, 1, 1.2f);
            return true;
        }
        put(p, "lust_drain", now(p) + 200);
        put(p, "lust_drain_lvl", mast(p, a, s) ? 1 : 0);
        sprite(p, "spark_yellow", 16);
        snd(p, SoundEvents.BEACON_POWER_SELECT, 1, 1.4f);
        return true;
    }

    // invigorate (tensura): heal back everything you're missing for 80 magicules a heart (40 mastered) and shake off your bad effects.
    // sneak to do it for whoever you're looking at
    static boolean invigorate(ServerPlayer p, Ability a, Spec s) {
        LivingEntity who = p;
        if (p.isShiftKeyDown()) { LivingEntity t = SkillHelper.targetQuiet(p, 5); if (t != null) who = t; }
        double per = mast(p, a, s) ? 40 : 80;
        float lacking = who.getMaxHealth() - who.getHealth();
        double can = p.isCreative() ? 1e18 : EnergyAPI.get(p).mana();
        float heal = (float) Math.min(lacking, can / per);
        boolean cleared = false;
        for (var i : new ArrayList<>(who.getActiveEffects()))
            if (i.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) { who.removeEffect(i.getEffect()); cleared = true; }
        if (heal <= 0 && !cleared) return false;
        if (!p.isCreative()) EnergyAPI.spendMana(p, heal * per);
        who.heal(heal);
        if (who instanceof net.minecraft.world.entity.animal.Animal an && an.getAge() >= 0) an.setInLove(p);
        around(who, ParticleTypes.COMPOSTER, 20);
        snd(who, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.4f);
        AbilityAPI.setCooldown(p, a, (mast(p, a, s) ? 3 : 5) * 20);
        return true;
    }

    // rebirth (the show: lust's power over life). everyone on your side within 15 (you too) is held back from death once
    // for 30 seconds: the hit that would kill them leaves them at full health instead
    static final Map<UUID, Long> REBIRTH = new HashMap<>();
    static boolean rebirth(ServerPlayer p, Ability a, Spec s) {
        long until = now(p) + (long) (600 * big(s));
        REBIRTH.put(p.getUUID(), until);
        for (LivingEntity e : allies(p, 15)) { REBIRTH.put(e.getUUID(), until); sprite(e, "heal_drop", 10); sprite(e, "spark_yellow", 8); }
        sprite(p, "spark_yellow", 20);
        lvl(p).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
        snd(p, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f, 1.4f);
        return true;
    }

    // embracing drain (tensura): lock someone in your arms for 5 seconds. neither of you can look away,
    // they can't feel pain, and it drains them every second (more mastered)
    private record Embrace(ServerLevel level, UUID owner, UUID who, long until, boolean m) {}
    private static final List<Embrace> EMBRACES = new ArrayList<>();
    static boolean embrace(ServerPlayer p, Ability a, Spec s) {
        double reach = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        LivingEntity t = SkillHelper.target(p, reach + 1);
        if (t == null || !LoreEngine.foe(p, t)) return false;
        if (EMBRACES.stream().anyMatch(e -> e.who.equals(t.getUUID()) || e.owner.equals(p.getUUID()))) { Tk.fail(p, "jaycee.sin.already"); return false; }
        EMBRACES.add(new Embrace(lvl(p), p.getUUID(), t.getUUID(), now(p) + (long) (100 * big(s)), mast(p, a, s)));
        snd(p, SoundEvents.EVOKER_CAST_SPELL, 1, 1.5f);
        return true;
    }
    private static void tickEmbrace(Embrace e, int tick) {
        ServerPlayer p = e.level.getServer().getPlayerList().getPlayer(e.owner);
        if (p == null || !(e.level.getEntity(e.who) instanceof LivingEntity t) || !t.isAlive()) return;
        // pinned: both of you face each other
        Vec3 d = p.position().subtract(t.position());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90;
        t.setYRot(yaw); t.setYHeadRot(yaw);
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        if (t instanceof Mob m) m.setTarget(null);
        if (tick % 5 == 0) sprite(t, "spark_yellow", 4);
        if (tick % 20 == 0) {
            double take = 200 + (e.m ? ep(t) * 0.005 : 0);
            Tk.gainMana(p, Tk.drainMana(t, take), false);
            LoreFx.essence(e.level, 0xffe060, 0xffffff, t, p);
            snd(t, SoundEvents.SOUL_ESCAPE, 0.6f, 1.3f);
        }
    }

    // death blessing (tensura): hold 5 seconds. a field 3 wide opens where you look (up to 10 away) and anything in it can't move.
    // let it finish and it judges them by EP against yours: half yours or less dies outright, 3/4 loses half its health,
    // up to yours loses a quarter. what it takes comes back to you
    static boolean blessing(ServerPlayer p, Ability a, Spec s, int t) {
        if (t % 20 == 0 && !Tk.spend(p, a, 7500)) return false;
        holdMastery(p, a, t);
        ServerLevel l = lvl(p);
        Vec3 at = t <= 50 ? LoreEngine.aim(p, 10) : get(p, "bless_at", LoreEngine.aim(p, 10));
        put(p, "bless_at", at);
        double r = 3 * big(s);
        // the field: a golden dome that tightens as it charges
        double phase = Math.min(1, t / 100.0);
        if (t % 2 == 0) {
            LoreFx.dustRing(l, 0xffd34a, at.add(0, 0.1, 0), r, 30, 1.4f, 0);
            LoreFx.dustRing(l, 0xff6ad5, at.add(0, 0.1 + phase * 2, 0), r * (1 - phase * 0.6), 20, 1.1f, 0);
            for (int i = 0; i < 4; i++) {
                double ang = Math.random() * Math.PI * 2, rr = Math.random() * r;
                var spr = BrightParticles.ALL.get("spark_yellow");
                if (spr != null) l.sendParticles(spr.get(), at.x + Math.cos(ang) * rr, at.y + Math.random() * 2.5, at.z + Math.sin(ang) * rr, 1, 0, 0, 0, 0);
            }
        }
        for (LivingEntity e : LoreEngine.near(l, p, at, r)) e.addEffect(new MobEffectInstance(LoreEffects.MOVEMENT_INTERFERENCE, 15, 5, false, false, false), p);
        if (t % 20 == 0) snd(p, SoundEvents.BELL_RESONATE, 0.6f, 1.5f);
        if (!timer(p, t, 100)) return true;
        double mine = ep(p);
        for (LivingEntity e : LoreEngine.near(l, p, at, r + 1)) {
            double theirs = EnergyAPI.ep(e), ratio = theirs / mine;
            float frac; double restore;
            if (ratio <= 0.5) { frac = 10f; restore = 1.0; }
            else if (ratio <= 0.75) { frac = 0.5f; restore = 0.25; }
            else if (ratio <= 1.0) { frac = 0.25f; restore = 0.05; }
            else continue;
            spiritual(p, e, e.getMaxHealth() * frac);
            Tk.gainMana(p, theirs * restore * 0.75, false);
            var pd = EnergyAPI.get(p);
            EnergyAPI.set(p, pd.withAura(Math.min(pd.maxAura(), pd.aura() + theirs * restore * 0.25)));
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.3);
            LoreFx.essence(l, 0xffd34a, 0xff6ad5, e, p);
        }
        var flash = BrightParticles.ALL.get("solar_flash");
        if (flash != null) l.sendParticles(flash.get(), at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        snd(p, SoundEvents.TOTEM_USE, 0.8f, 1.6f);
        done(p, a, "blessing");
        return false;
    }

    // transmission (manas reborn): a 2 second cast, then you pour a fifth of your health into whoever you're looking at (within 5).
    // sneak to take it from them instead (costs half as much)
    static boolean transmit(ServerPlayer p, Ability a, Spec s, int t) {
        LivingEntity e = look(p, 5);
        if (e == null) { if (t % 20 == 0) bar(p, "jaycee.sin.no_target", ChatFormatting.RED); return t < 100; }
        boolean take = p.isShiftKeyDown();
        if (t % 4 == 0) LoreFx.essence(lvl(p), 0xff6080, 0xffd0e0, take ? e : p, take ? p : e);
        if (!timer(p, t, 40)) return true;
        if (!Tk.spend(p, a, take ? 40 : 80)) return false;
        if (take) {
            float amt = (float) ((e.getHealth() - 1) * 0.2 * big(s));
            if (LoreEngine.foe(p, e)) spiritual(p, e, amt); else e.setHealth(Math.max(1, e.getHealth() - amt));
            p.heal(amt);
        } else {
            float amt = (float) ((p.getHealth() - 1) * 0.2);
            p.setHealth(Math.max(1, p.getHealth() - amt));
            e.heal((float) (amt * big(s)));
        }
        around(take ? p : e, ParticleTypes.HEART, 6);
        snd(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.2f);
        done(p, a, "transmit");
        return false;
    }

    // siphoning (manas reborn): hold on someone within 10 (20 mastered) and keep pulling 1% of their life a tick into yourself
    static boolean siphon(ServerPlayer p, Ability a, Spec s, int t) {
        double range = mast(p, a, s) ? 20 : 10;
        UUID id = get(p, "siphon", null);
        LivingEntity e = id != null && lvl(p).getEntity(id) instanceof LivingEntity le && le.isAlive() && le.distanceTo(p) <= range ? le : null;
        if (e == null) {
            e = look(p, range);
            if (e == null || !LoreEngine.foe(p, e)) { drop(p, "siphon"); return t < 20; }
            put(p, "siphon", e.getUUID());
        }
        if (t % 2 != 0) return true;
        float amt = (float) Math.min(e.getHealth(), e.getMaxHealth() * 0.01 * 2 * big(s));
        if (!Tk.spend(p, a, 100 / 10.0)) { drop(p, "siphon"); return false; }
        spiritual(p, e, amt);
        p.heal(amt);
        if (t % 4 == 0) LoreFx.essence(lvl(p), 0xd0306a, 0xff9ac0, e, p);
        if (t % 20 == 0) snd(p, SoundEvents.SOUL_ESCAPE, 0.7f, 0.9f);
        holdMastery(p, a, t);
        return true;
    }

    // implode (manas reborn): a 3 second cast on someone within 10. their own life folds in on them:
    // a tenth of what you've got left in magicules lands as damage
    static boolean implode(ServerPlayer p, Ability a, Spec s, int t) {
        LivingEntity e = look(p, 10);
        if (e == null || !LoreEngine.foe(p, e)) { if (t % 20 == 0) bar(p, "jaycee.sin.no_target", ChatFormatting.RED); return t < 200; }
        if (t % 3 == 0) { LoreFx.dustRing(lvl(p), 0xd0306a, e.position().add(0, e.getBbHeight() / 2, 0), 1.5 - (t % 30) / 30.0, 16, 1.2f, 0); sprite(e, "spark_yellow", 2); }
        if (!timer(p, t, 60)) return true;
        if (!Tk.spend(p, a, 20)) return false;
        float dmg = (float) Math.max(10, Math.min(500, EnergyAPI.get(p).mana() * 0.1 / 100) * big(s));
        spiritual(p, e, dmg);
        lvl(p).sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 1, e.getZ(), 2, 0.2, 0.2, 0.2, 0);
        sprite(e, "shock_ring_in", 1);
        snd(e, SoundEvents.GENERIC_EXPLODE, 1, 1.4f);
        done(p, a, "implode");
        return false;
    }

    // ================================================================ GREED (and lust's manipulation, which works the same)

    // spiritual domination (tensura greed / manas reborn lust): hold on something within 30 until its will breaks.
    // a mob takes 90 seconds, a player 60. 30 less within 5 blocks, 30 more past 20, and less again the more it's traded
    // (greed knows what it wants). let go and the count drains back down
    static boolean domination(ServerPlayer p, Ability a, Spec s, int t) {
        if (p.isShiftKeyDown()) return releaseControl(p);
        LivingEntity e = look(p, 30);
        UUID was = get(p, "dom_target", null);
        int secs = get(p, "dom_secs", 0);
        if (e == null || (was != null && !was.equals(e.getUUID()))) {
            if (t % 20 == 0) { secs = Math.max(0, secs - 1); put(p, "dom_secs", secs); if (secs == 0) drop(p, "dom_target"); }
            return true;
        }
        if (Bonds.isSubordinateOf(e, p)) { if (t <= 1) Tk.fail(p, "jaycee.sin.already_yours"); return false; }
        if (was == null) {
            int want = 0;
            if (e instanceof ServerPlayer sp) want = sp.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TRADED_WITH_VILLAGER)) / 10;
            else if (e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager v) want = v.getOffers().size() * 2;
            put(p, "dom_target", e.getUUID()); put(p, "dom_want", want); put(p, "dom_secs", 0); secs = 0;
        }
        int need = e instanceof Player ? 60 : 90;
        double dist = e.distanceTo(p);
        if (dist < 5) need -= 30; else if (dist > 20) need += 30;
        need -= get(p, "dom_want", 0);
        if (ult(s)) need /= 2;
        need = Math.max(3, need);
        if (t % 20 == 0 && t > 0) { secs++; put(p, "dom_secs", secs); }
        if (t % 10 == 0) { around(e, ParticleTypes.RAID_OMEN, 1); LoreFx.dustLine(lvl(p), 0x8a1a5a, p.getEyePosition(), e.position().add(0, e.getBbHeight() * 0.7, 0), 0.6, 0.8f); }
        if (t % 5 == 0) bar(p, "jaycee.sin.dominating", ChatFormatting.GOLD, secs, need);
        holdMastery(p, a, t);
        if (secs < need) return true;
        if (e instanceof Mob m) {
            m.setData(Bonds.OWNER, p.getStringUUID());
            m.setTarget(null);
            m.setPersistenceRequired();
            m.addEffect(new MobEffectInstance(LoreEffects.MIND_CONTROL, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            Bonds.add(p, m, m.getName().getString());
        } else if (e instanceof ServerPlayer sp) {
            // players aren't taken for good, they just can't fight you for a while
            sp.addEffect(new MobEffectInstance(LoreEffects.MIND_CONTROL, 20 * 30, 0), p);
            sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 30, 4), p);
            sp.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 10, 0), p);
        }
        around(e, ParticleTypes.RAID_OMEN, 12);
        lvl(p).sendParticles(ParticleTypes.RAID_OMEN, e.getX(), e.getY() + 1, e.getZ(), 20, 1, 1, 1, 0.1);
        snd(p, SoundEvents.EVOKER_PREPARE_WOLOLO, 1, 0.8f);
        bar(p, "jaycee.sin.dominated", ChatFormatting.GOLD, e.getName());
        drop(p, "dom_target"); put(p, "dom_secs", 0);
        done(p, a, "domination");
        return false;
    }

    // sneak + use on something you control lets it go
    static boolean releaseControl(ServerPlayer p) {
        LivingEntity e = look(p, 30);
        if (!(e instanceof Mob m) || !m.hasEffect(LoreEffects.MIND_CONTROL) || !Bonds.isSubordinateOf(m, p)) return false;
        m.removeEffect(LoreEffects.MIND_CONTROL);
        m.setData(Bonds.OWNER, "");
        around(m, ParticleTypes.ANGRY_VILLAGER, 4);
        return false;
    }

    // greed flare (tensura): look at an enemy and it burns away a slice of your own power into raw attack (5% of your health, 10% mastered),
    // look at an ally to give it half that. every hit with it costs magicules. sneak to put it out
    static final Identifier GREED = Identifier.fromNamespaceAndPath(JayceeMod.ID, "greed_flare");
    static boolean flare(ServerPlayer p, Ability a, Spec s) {
        boolean m = mast(p, a, s);
        LivingEntity t = SkillHelper.targetQuiet(p, 20);
        LivingEntity who = t != null && ally(p, t) ? t : p;
        AttributeInstance atk = who.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk == null) return false;
        if (atk.getModifier(GREED) != null) {
            atk.removeModifier(GREED);
            around(who, ParticleTypes.WAX_ON, 10);
            snd(who, SoundEvents.BEACON_DEACTIVATE, 1, 1.3f);
            return true;
        }
        if (p.isShiftKeyDown()) return false;
        double mult = who == p ? (m ? 0.1 : 0.05) : (m ? 0.05 : 0.025);
        double boost = who.getMaxHealth() * mult * 10 * big(s);
        if (who != p && !Tk.spend(p, a, 50 * boost)) return false;
        atk.addOrUpdateTransientModifier(new AttributeModifier(GREED, boost, AttributeModifier.Operation.ADD_VALUE));
        around(who, ParticleTypes.WAX_OFF, 14);
        aura(who, 0x101010, 0xffd34a, 16);
        snd(who, SoundEvents.BEACON_POWER_SELECT, 1, 0.7f);
        return true;
    }

    // death wish (tensura): hold on something in reach for 10 seconds. it can barely move the whole time, and when the wish is done it dies
    static boolean deathWish(ServerPlayer p, Ability a, Spec s, int t) {
        double reach = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + (ult(s) ? 6 : 0);
        LivingEntity e = look(p, reach);
        UUID was = get(p, "wish_target", null);
        int secs = get(p, "wish_secs", 0);
        if (e == null || !LoreEngine.foe(p, e) || (was != null && !was.equals(e.getUUID()))) {
            if (t % 20 == 0) { secs = Math.max(0, secs - 1); put(p, "wish_secs", secs); if (secs == 0) drop(p, "wish_target"); }
            return true;
        }
        put(p, "wish_target", e.getUUID());
        int need = ult(s) ? 5 : 10;
        if (t % 20 == 0 && t > 0) { secs++; put(p, "wish_secs", secs); }
        e.addEffect(new MobEffectInstance(LoreEffects.MOVEMENT_INTERFERENCE, 10, 5, false, false, false), p);
        if (t % 4 == 0) around(e, ParticleTypes.WHITE_ASH, 6);
        if (t % 5 == 0) bar(p, "jaycee.sin.held", ChatFormatting.GOLD, secs, need);
        if (secs < need) return true;
        if (!Tk.spend(p, a, 1000)) return false;
        drop(p, "wish_target"); put(p, "wish_secs", 0);
        spiritual(p, e, e.getMaxHealth() * 10);
        aura(e, 0x050505, 0x2a2a2a, 40);
        lvl(p).sendParticles(ParticleTypes.WHITE_ASH, e.getX(), e.getY() + 1, e.getZ(), 60, 0.6, 1, 0.6, 0.05);
        snd(e, SoundEvents.WITHER_SHOOT, 1, 0.5f);
        done(p, a, "death_wish");
        return false;
    }

    // energy seize (manas reborn): everything within 15 has its power siphoned while it's near you, 10% of its regen-able energy a level
    // (level 1, 3 mastered), for 30 seconds
    private record Seize(ServerLevel level, UUID owner, long until, int lvl, double r) {}
    private static final List<Seize> SEIZES = new ArrayList<>();
    static boolean seize(ServerPlayer p, Ability a, Spec s) {
        SEIZES.removeIf(x -> x.owner.equals(p.getUUID()));
        SEIZES.add(new Seize(lvl(p), p.getUUID(), now(p) + 600, mast(p, a, s) ? 3 : 1, 15 * big(s)));
        aura(p, 0x101010, 0xffd34a, 30);
        snd(p, SoundEvents.BEACON_ACTIVATE, 1, 0.5f);
        return true;
    }
    private static void tickSeize(Seize z, int tick) {
        if (tick % 20 != 0) return;
        ServerPlayer p = z.level.getServer().getPlayerList().getPlayer(z.owner);
        if (p == null) return;
        if (tick % 40 == 0) LoreFx.dustRing(z.level, 0xffd34a, p.position().add(0, 0.1, 0), z.r, 50, 1.2f, 0);
        for (LivingEntity e : LoreEngine.near(z.level, p, p.position(), z.r)) {
            var d = EnergyAPI.get(e);
            double take = (d.maxMana() + d.maxAura()) * 0.002 * z.lvl;
            double got = Tk.drainMana(e, take);
            if (got > 0) { Tk.gainMana(p, got, false); LoreFx.essence(z.level, 0x3a2a00, 0xffd34a, e, p); }
        }
    }

    // endless avarice (manas reborn): grab at someone within 10. you rip mastery out of a skill you both have (10%, 30% mastered),
    // or if you don't have it, a 20% chance (always, mastered) to take it outright. it needs 45 seconds to come back if it works
    static boolean avarice(ServerPlayer p, Ability a, Spec s) {
        LivingEntity t = SkillHelper.target(p, 10);
        if (!(t instanceof ServerPlayer o)) {
            // mobs: you just take a chunk of what makes them strong
            if (t == null) return false;
            Tk.gainMana(p, Tk.drainMana(t, ep(t) * 0.1), true);
            LoreFx.essence(lvl(p), 0x3a2a00, 0xffd34a, t, p);
            AbilityAPI.setCooldown(p, a, 90);
            return true;
        }
        boolean m = mast(p, a, s);
        List<Ability> theirs = new ArrayList<>(AbilityAPI.owned(o));
        theirs.removeIf(x -> x.type() == SkillType.ULTIMATE || x == a);
        if (theirs.isEmpty()) { Tk.fail(p, "jaycee.sin.nothing_to_take"); return false; }
        Ability pick = theirs.get(p.getRandom().nextInt(theirs.size()));
        boolean ok;
        if (AbilityAPI.has(p, pick)) {
            int steal = (int) (AbilityAPI.mastery(o, pick) * (m ? 0.3 : 0.1));
            AbilityAPI.setMastery(o, pick, AbilityAPI.mastery(o, pick) - steal);
            AbilityAPI.addMastery(p, pick, steal);
            ok = steal > 0;
        } else {
            ok = (m || p.getRandom().nextFloat() < 0.2f) && pick.type() != SkillType.UNIQUE && AbilityAPI.learn(p, pick);
            if (ok) p.sendSystemMessage(Component.translatable("brightcore.message.acquired", pick.name()));
        }
        AbilityAPI.setCooldown(p, a, ok ? 900 : 90);
        LoreFx.essence(lvl(p), 0x3a2a00, 0xffd34a, o, p);
        around(o, ok ? ParticleTypes.WAX_OFF : ParticleTypes.SMOKE, 10);
        snd(p, ok ? SoundEvents.PLAYER_LEVELUP : SoundEvents.FIRE_EXTINGUISH, 1, 0.8f);
        return true;
    }

    // ================================================================ PRIDE

    // copy (tensura): whatever skill hits you has a 20% chance (always, mastered) to become yours. pressing the mode tries again
    // at the last one that hit you. once it works it waits a while (longer for stronger skills)
    static final Map<UUID, Identifier> LAST_HIT_BY = new HashMap<>();

    static boolean tryCopy(ServerPlayer p, Ability pride, Spec ps, Identifier skill, boolean fromPress) {
        Ability got = AbilityRegistry.get(skill);
        if (got == null || got == pride || AbilityAPI.has(p, got)) { if (fromPress) Tk.fail(p, "jaycee.sin.nothing_to_copy"); return false; }
        if (got.type() == SkillType.ULTIMATE && !ult(ps)) { if (fromPress) Tk.fail(p, "jaycee.sin.too_strong_copy"); return false; }
        if (!fromPress && AbilityAPI.onCooldown(p, pride)) return false;
        double chance = mast(p, pride, ps) ? 1 : 0.2;
        if (p.getRandom().nextDouble() >= chance) { AbilityAPI.setCooldown(p, pride, 90); return false; }
        if (!AbilityAPI.learn(p, got)) return false;
        int cd = switch (got.type()) { case UNIQUE -> 1800; case ULTIMATE -> 3600; case EXTRA -> 900; default -> 400; };
        AbilityAPI.setCooldown(p, pride, cd);
        p.sendSystemMessage(Component.translatable("jaycee.sin.copied", got.name()).withStyle(ChatFormatting.GOLD));
        aura(p, 0xfff2b0, 0xffd34a, 30);
        snd(p, SoundEvents.BEACON_POWER_SELECT, 1, 1.5f);
        return true;
    }

    static boolean prideCopy(ServerPlayer p, Ability a, Spec s) {
        Identifier last = LAST_HIT_BY.get(p.getUUID());
        if (last == null) { Tk.fail(p, "jaycee.sin.nothing_to_copy"); return false; }
        return tryCopy(p, a, s, last, true);
    }

    // declare supremacy (manas reborn): name up to 1 (3 mastered) things within 30 as beneath you. if their EP is no more than half yours,
    // for 3 seconds (6 mastered) they hit you for a quarter and you hit them like they only had a quarter of their health
    private record Beneath(UUID by, UUID who, long until) {}
    private static final List<Beneath> BENEATH = new ArrayList<>();
    static boolean declare(ServerPlayer p, Ability a, Spec s) {
        LivingEntity t = SkillHelper.target(p, 30);
        if (t == null || !LoreEngine.foe(p, t)) return false;
        boolean m = mast(p, a, s);
        if (ep(t) > ep(p) * (ult(s) ? 0.75 : 0.5)) { Tk.fail(p, "jaycee.sin.not_beneath", t.getName()); return false; }
        long mine = BENEATH.stream().filter(b -> b.by.equals(p.getUUID()) && b.until > now(p)).count();
        if (mine >= (m ? 3 : 1)) BENEATH.removeIf(b -> b.by.equals(p.getUUID()));
        BENEATH.add(new Beneath(p.getUUID(), t.getUUID(), now(p) + (long) ((m ? 120 : 60) * big(s))));
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, m ? 120 : 60, 0, false, false), p);
        aura(t, 0xffd34a, 0x6a4a00, 20);
        LoreFx.dustLine(lvl(p), 0xffe9a0, p.getEyePosition(), t.position().add(0, t.getBbHeight(), 0), 0.4, 1f);
        snd(p, SoundEvents.EVOKER_CAST_SPELL, 1, 1.2f);
        return true;
    }

    // supreme order (manas reborn): everything within 30 with no more than 3/4 your EP (all of it, mastered) is ordered to stop,
    // its attacks and its movement shut down for 15 seconds, harder the weaker it is
    static boolean order(ServerPlayer p, Ability a, Spec s) {
        boolean m = mast(p, a, s);
        double mine = ep(p);
        int n = 0;
        for (LivingEntity e : LoreEngine.near(lvl(p), p, p.position(), 30 * big(s))) {
            double ratio = ep(e) / mine;
            if (ratio > (m ? 1.0 : 0.75)) continue;
            int lvl = (int) Math.min(9, (1 - ratio) * 10);
            e.addEffect(new MobEffectInstance(LoreEffects.MOVEMENT_INTERFERENCE, 300, lvl), p);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, lvl), p);
            if (e instanceof Mob mob) mob.setTarget(null);
            around(e, ParticleTypes.ENCHANT, 12);
            n++;
        }
        LoreFx.dustRing(lvl(p), 0xffd34a, p.position().add(0, 0.1, 0), 6, 40, 1.4f, 0);
        snd(p, SoundEvents.RAID_HORN, 0.6f, 1.6f);
        return n > 0 || true;
    }

    // imperial ascension (manas reborn): a 5 second cast, then for 2 minutes your health and magicules double and nothing can knock you around
    static boolean ascension(ServerPlayer p, Ability a, Spec s, int t) {
        if (t % 2 == 0) { aura(p, 0xfff2b0, 0xffd34a, 6); LoreFx.dustRing(lvl(p), 0xffd34a, p.position().add(0, 0.05, 0), 1.5 + t / 50.0, 24, 1.2f, 0); }
        if (t % 20 == 0) snd(p, SoundEvents.BEACON_AMBIENT, 1, 1.2f);
        if (!timer(p, t, 100)) return true;
        if (!Tk.spend(p, a, 20000)) return false;
        int dur = (int) (2400 * big(s));
        p.addEffect(new MobEffectInstance(LoreEffects.IMPERIAL_ASCENSION, dur, 0, false, true, true));
        var d = EnergyAPI.get(p);
        put(p, "ascended_until", now(p) + dur);
        put(p, "ascended_mana", d.maxMana());
        EnergyAPI.set(p, new com.brightwebs.brightcore.data.EnergyData(d.aura(), d.maxAura(), d.mana() * 2, d.maxMana() * 2, d.spirit(), d.maxSpirit(), d.initialized()));
        p.setHealth(p.getMaxHealth());
        lvl(p).sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.25);
        snd(p, SoundEvents.ENDER_DRAGON_GROWL, 1, 1.2f);
        done(p, a, "ascension");
        return false;
    }

    // ================================================================ SLOTH

    // drowsiness: tensura's -25% speed, -4 attack. at level 5 and up you nod off for real
    static void drowse(ServerPlayer p, LivingEntity e, int lvl, int dur) {
        e.addEffect(new MobEffectInstance(LoreEffects.DROWSINESS, dur, Math.min(lvl, 9), false, true, true), p);
        if (lvl >= 4) e.addEffect(new MobEffectInstance(LoreEffects.SLEEP, 100, 0, false, true, true), p);
    }
    static int drowseLevel(LivingEntity e, int dur) {
        var i = e.getEffect(LoreEffects.DROWSINESS);
        return i == null ? 0 : Math.max(0, (i.getDuration() + 2 - dur) / 200);
    }

    // deep hypno (tensura): hold on something within 10 and it gets drowsier the longer you hold (a level every 10 seconds).
    // look at a player in the middle of a spell and the spell falls apart
    static boolean deepHypno(ServerPlayer p, Ability a, Spec s, int t) {
        boolean m = mast(p, a, s);
        LivingEntity e = look(p, 10 * big(s));
        if (e == null) return true;
        int dur = m ? 200 : 100;
        if (t % 20 == 0) {
            var cur = e.getEffect(LoreEffects.DROWSINESS);
            int keep = cur == null ? dur : cur.getDuration() + 20;
            e.addEffect(new MobEffectInstance(LoreEffects.DROWSINESS, keep, Math.min(9, Math.max(0, (keep - dur) / 200)), false, true, true), p);
            if (keep - dur >= 800) e.addEffect(new MobEffectInstance(LoreEffects.SLEEP, 100, 0, false, true, true), p);
            if (e instanceof ServerPlayer sp && sp.isUsingItem()) sp.stopUsingItem();
            if (e instanceof ServerPlayer sp) for (Ability x : AbilityAPI.owned(sp)) if (x instanceof LoreSkills.LoreAbility la && la.spec.magic()) AbilityAPI.setCooldown(sp, x, (m ? 5 : 3) * 20);
        }
        if (t % 4 == 0) wave(lvl(p), e.position().add(0, e.getBbHeight() * 0.5, 0), Math.max(1, e.getBbWidth()), 0xb8a8e8, t);
        if (t % 20 == 0) snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.25f, 1.8f);
        holdMastery(p, a, t);
        return true;
    }

    // fallen hypno (tensura): the same, but on everything within 5 of you (10 mastered)
    static boolean fallenHypno(ServerPlayer p, Ability a, Spec s, int t) {
        boolean m = mast(p, a, s);
        double r = (m ? 10 : 5) * big(s);
        int dur = m ? 200 : 100;
        if (t % 4 == 0) wave(lvl(p), p.position(), r, 0xb8a8e8, t);
        if (t % 20 == 0) {
            for (LivingEntity e : LoreEngine.near(lvl(p), p, p.position(), r)) {
                var cur = e.getEffect(LoreEffects.DROWSINESS);
                int keep = cur == null ? dur : cur.getDuration() + 20;
                e.addEffect(new MobEffectInstance(LoreEffects.DROWSINESS, keep, Math.min(9, Math.max(0, (keep - dur) / 200)), false, true, true), p);
                if (keep - dur >= 800) e.addEffect(new MobEffectInstance(LoreEffects.SLEEP, 100, 0, false, true, true), p);
            }
            snd(p, SoundEvents.WARDEN_SONIC_BOOM, 0.3f, 1.8f);
        }
        holdMastery(p, a, t);
        return true;
    }

    // deprive (tensura): hold. everything within 5 (15 mastered) has 1000 EP a second (0.3% mastered) and some of its soul sucked out into you
    static boolean deprive(ServerPlayer p, Ability a, Spec s, int t) {
        boolean m = mast(p, a, s);
        double r = (m ? 15 : 5) * big(s);
        if (t % 4 == 0) wave(lvl(p), p.position(), r, 0x9a8ad8, t);
        if (t % 20 != 0) return true;
        if (!Tk.spend(p, a, 200)) return false;
        for (LivingEntity e : LoreEngine.near(lvl(p), p, p.position(), r)) {
            double take = m ? ep(e) * 0.003 + 1000 : 1000;
            Tk.gainMana(p, Tk.drainMana(e, take * big(s)), false);
            spiritual(p, e, (float) (1 + (m ? e.getMaxHealth() * 0.01 : 0)) * (float) big(s));
            around(e, ParticleTypes.HEART, 1);
        }
        snd(p, SoundEvents.SOUL_ESCAPE, 0.6f, 0.7f);
        holdMastery(p, a, t);
        return true;
    }

    // rest (tensura): hold and you lie still: 10 health and 10 magicules back a second (and 1% more mastered).
    // sneak + press to rest your allies within 15 instead: 50 health each, 1000 magicules each from you
    static boolean slothRest(ServerPlayer p, Ability a, Spec s, int t) {
        if (p.isShiftKeyDown() && t <= 1) {
            List<LivingEntity> al = allies(p, 15);
            if (al.isEmpty() || !Tk.spend(p, a, 1000 * al.size())) return false;
            for (LivingEntity e : al) { e.heal(50); wave(lvl(p), e.position(), Math.max(1, e.getBbWidth()), 0xc8f0c8, 0); }
            snd(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 0.6f);
            return false;
        }
        boolean m = mast(p, a, s);
        p.addEffect(new MobEffectInstance(LoreEffects.REST, 25, 0, false, false, false));
        if (t % 20 == 0 && t > 0) {
            p.heal((float) ((10 + (m ? p.getMaxHealth() * 0.01 : 0)) * big(s)));
            Tk.gainMana(p, 10 + (m ? EnergyAPI.get(p).maxMana() * 0.01 : 0), false);
            snd(p, SoundEvents.FOX_SLEEP, 0.6f, 0.8f);
        }
        if (t % 6 == 0) wave(lvl(p), p.position(), 1.8, 0xc8f0c8, t);
        holdMastery(p, a, t);
        return true;
    }

    // phantasmal style (tensura): switch it on and your physical hits also strike the soul (10, plus 1% of their health mastered),
    // and you move like a shadow
    static final Set<UUID> PHANTASMAL = new HashSet<>();
    static boolean phantasmal(ServerPlayer p, Ability a, Spec s) {
        if (PHANTASMAL.remove(p.getUUID())) {
            p.removeEffect(LoreEffects.SHADOW_STEP);
            bar(p, "jaycee.sin.phantasmal_off", ChatFormatting.GRAY);
            return true;
        }
        PHANTASMAL.add(p.getUUID());
        put(p, "phantasmal_m", mast(p, a, s));
        put(p, "phantasmal_ult", ult(s));
        bar(p, "jaycee.sin.phantasmal_on", ChatFormatting.LIGHT_PURPLE);
        wave(lvl(p), p.position(), 2, 0xb8a8e8, 10);
        snd(p, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1, 1);
        return true;
    }

    // fallen strike (tensura): one blow at something within 8 that goes straight for the soul (tensura's is 500 spiritual).
    // here: a quarter of its health plus 50, through armor
    static boolean fallenStrike(ServerPlayer p, Ability a, Spec s) {
        LivingEntity t = SkillHelper.target(p, 8);
        if (t == null || !LoreEngine.foe(p, t)) return false;
        float dmg = (float) ((50 + t.getMaxHealth() * 0.25) * big(s));
        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
        LoreFx.move(lvl(p), s, p.position(), behind);
        p.teleportTo(behind.x, t.getY(), behind.z);
        spiritual(p, t, dmg);
        drowse(p, t, 2, 200);
        var flash = BrightParticles.ALL.get("solar_flash");
        if (flash != null) lvl(p).sendParticles(flash.get(), t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
        wave(lvl(p), t.position().add(0, t.getBbHeight() / 2, 0), Math.max(1, t.getBbWidth()) * 1.5, 0xb8a8e8, 10);
        snd(t, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.6f);
        return true;
    }

    // lethargy (manas reborn): everything within 15 is weighed down with lethargy that deepens every 10 seconds, for a minute.
    // ten levels in and it simply falls asleep
    private record Lethargy(ServerLevel level, UUID owner, UUID who, long start, long until) {}
    private static final List<Lethargy> LETHARGY = new ArrayList<>();
    static boolean lethargy(ServerPlayer p, Ability a, Spec s) {
        long until = now(p) + (long) (1200 * big(s));
        int n = 0;
        for (LivingEntity e : LoreEngine.near(lvl(p), p, p.position(), 15 * big(s))) {
            LETHARGY.removeIf(x -> x.who.equals(e.getUUID()));
            LETHARGY.add(new Lethargy(lvl(p), p.getUUID(), e.getUUID(), now(p), until));
            e.addEffect(new MobEffectInstance(LoreEffects.MOVEMENT_INTERFERENCE, 200, 0), p);
            n++;
        }
        for (int i = 0; i < 3; i++) wave(lvl(p), p.position(), 15 * big(s) * (0.4 + i * 0.3), 0x8a7ab8, i * 7);
        snd(p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 0.6f);
        return n > 0;
    }
    private static void tickLethargy(Lethargy x, int tick) {
        if (tick % 20 != 0 || !(x.level.getEntity(x.who) instanceof LivingEntity e) || !e.isAlive()) return;
        int lvl = (int) ((x.level.getGameTime() - x.start) / 200);
        ServerPlayer p = x.level.getServer().getPlayerList().getPlayer(x.owner);
        e.addEffect(new MobEffectInstance(LoreEffects.MOVEMENT_INTERFERENCE, 220, Math.min(9, lvl)), p);
        if (lvl >= 10) e.addEffect(new MobEffectInstance(LoreEffects.SLEEP, 220, 0), p);
        if (tick % 60 == 0) x.level.sendParticles(ParticleTypes.NOTE, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 1, 0.2, 0, 0.2, 0);
    }

    // effort (manas reborn sloth): why do it yourself. up to 4 copies of you (9 mastered) to fight for you, each costs a tenth of your magicules
    private record Clone(ServerLevel level, UUID id, UUID owner, long until) {}
    private static final List<Clone> CLONES = new ArrayList<>();
    static boolean effort(ServerPlayer p, Ability a, Spec s) {
        ServerLevel l = lvl(p);
        boolean m = mast(p, a, s);
        int max = (m ? 9 : 4) * (ult(s) ? 2 : 1);
        long have = CLONES.stream().filter(c -> c.owner.equals(p.getUUID()) && l.getEntity(c.id) != null).count();
        if (have >= max) { Tk.fail(p, "jaycee.sin.max_clones", max); return false; }
        if (!Tk.spend(p, a, EnergyAPI.get(p).maxMana() * 0.1)) return false;
        Husk c = EntityTypes.HUSK.create(l, EntitySpawnReason.MOB_SUMMONED);
        if (c == null) return false;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        c.snapTo(at.x, p.getY(), at.z); c.setYRot(p.getYRot());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
            c.setItemSlot(slot, p.getItemBySlot(slot).copy());
            c.setDropChance(slot, 0);
        }
        if (c.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            var head = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(p.getGameProfile()));
            c.setItemSlot(EquipmentSlot.HEAD, head);
            c.setDropChance(EquipmentSlot.HEAD, 0);
        }
        var hp = c.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(20, p.getMaxHealth() * 0.5));
        c.setHealth(c.getMaxHealth());
        var atk = c.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(3, p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5));
        c.setCustomName(Component.translatable("jaycee.sin.clone_of", p.getName()));
        c.setCanPickUpLoot(false);
        c.setPersistenceRequired();
        c.setData(Bonds.OWNER, p.getStringUUID());
        l.addFreshEntity(c);
        CLONES.add(new Clone(l, c.getUUID(), p.getUUID(), now(p) + 20 * 60 * 3));
        aura(c, 0x101010, 0x3a3a5a, 20);
        snd(p, SoundEvents.ILLUSIONER_CAST_SPELL, 1, 1);
        return true;
    }

    // final lullaby (manas reborn): a 3 second cast. everyone on your side within 15 (you too) gets 2 minutes of final yawn:
    // +20% damage, +10% speed, faster recovery, 5% less damage taken. when it ends, they fall asleep
    static boolean lullaby(ServerPlayer p, Ability a, Spec s, int t) {
        if (t % 4 == 0) wave(lvl(p), p.position(), 15, 0xc8b8f8, t);
        if (t % 20 == 0) lvl(p).sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 3, 0.6, 0.2, 0.6, 1);
        if (!timer(p, t, 60)) return true;
        if (!Tk.spend(p, a, 20000)) return false;
        int dur = (int) (2400 * big(s));
        List<LivingEntity> al = allies(p, 15);
        al.add(p);
        for (LivingEntity e : al) {
            e.addEffect(new MobEffectInstance(LoreEffects.FINAL_YAWN, dur, ult(s) ? 1 : 0, false, true, true));
            aura(e, 0xc8b8f8, 0x6a5aa8, 12);
        }
        snd(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
        done(p, a, "lullaby");
        return false;
    }

    // ================================================================ passives and hooks

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        Safe.run("Sins.onIncoming", () -> {
            LivingEntity victim = event.getEntity();
            DamageSource src = event.getSource();
            float amount = event.getAmount();

            // pride: remember what hit you, and maybe copy it
            if (victim instanceof ServerPlayer vp) {
                Spec hitBy = LoreEngine.SOURCE.get();
                if (hitBy != null && src.getEntity() instanceof Player) {
                    Identifier id = Identifier.fromNamespaceAndPath(JayceeMod.ID, hitBy.id());
                    LAST_HIT_BY.put(vp.getUUID(), id);
                    Ability pride = owned(vp, "lucifer", "pride");
                    if (pride instanceof LoreSkills.LoreAbility la) tryCopy(vp, pride, la.spec, id, false);
                }
                // envy / leviathan: lucky, and a chance hits just miss
                Ability envy = owned(vp, "leviathan", "envy");
                if (envy != null && src.getEntity() != null && src.getEntity() != vp) {
                    float dodge = envy.id().getPath().equals("leviathan") ? 0.2f : 0.1f;
                    if (vp.getRandom().nextFloat() < dodge) {
                        event.setCanceled(true);
                        lvl(vp).sendParticles(ParticleTypes.CLOUD, vp.getX(), vp.getY() + 1, vp.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                        snd(vp, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8f, 1.4f);
                        return;
                    }
                }
            }
            // rebirth: the killing blow leaves you at full health instead (once)
            Long rb = REBIRTH.get(victim.getUUID());
            if (rb != null && rb > now(victim) && amount >= victim.getHealth()) {
                REBIRTH.remove(victim.getUUID());
                event.setCanceled(true);
                victim.setHealth(victim.getMaxHealth());
                lvl(victim).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1, victim.getZ(), 50, 0.5, 0.8, 0.5, 0.4);
                sprite(victim, "spark_yellow", 20);
                snd(victim, SoundEvents.TOTEM_USE, 1, 1.3f);
                return;
            }
            // pride's declaration: what's beneath you can barely touch you, and folds when you hit it
            for (Beneath b : BENEATH) {
                if (b.until < now(victim)) continue;
                if (src.getEntity() != null && b.who.equals(src.getEntity().getUUID()) && b.by.equals(victim.getUUID())) amount *= 0.25f;
                if (b.who.equals(victim.getUUID()) && src.getEntity() != null && b.by.equals(src.getEntity().getUUID())) amount *= 4f;
            }
            // final yawn: 5% less a level
            var yawn = victim.getEffect(LoreEffects.FINAL_YAWN);
            if (yawn != null) amount *= 1 - 0.05f * (yawn.getAmplifier() + 1);

            boolean physical = src.isDirect() && !src.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) && !src.is(net.minecraft.world.damagesource.DamageTypes.MAGIC);
            if (src.getEntity() instanceof ServerPlayer att && physical) {
                // wrath: hits that land 10-30 ticks apart build a combo, each one +1% (manas reborn)
                if (owned(att, "satanael", "wrath") != null) {
                    long last = get(att, "combo_last", 0L), gap = now(att) - last;
                    int combo = get(att, "combo", 0);
                    if (gap < 10) { /* spam doesn't count */ }
                    else if (gap > 30) combo = 0;
                    else { combo = Math.min(100, combo + 1); amount *= 1 + combo * 0.01f; }
                    put(att, "combo", combo); put(att, "combo_last", now(att));
                    if (combo > 1) bar(att, "jaycee.sin.combo", ChatFormatting.RED, combo);
                }
                // lust's charged hands
                if (until(att, "lust_drain")) {
                    double take = 200 + get(att, "lust_drain_lvl", 0) * ep(victim) * 0.0025;
                    Tk.gainMana(att, Tk.drainMana(victim, take), false);
                    sprite(victim, "spark_yellow", 6);
                }
                // greed flare: every swing burns magicules
                AttributeInstance atk = att.getAttribute(Attributes.ATTACK_DAMAGE);
                AttributeModifier gm = atk == null ? null : atk.getModifier(GREED);
                if (gm != null && !att.isCreative() && !EnergyAPI.spendMana(att, gm.amount() * 10)) {
                    atk.removeModifier(GREED);
                    around(att, ParticleTypes.WAX_ON, 8);
                }
                // sloth's phantasmal style: the soul takes a hit too
                if (PHANTASMAL.contains(att.getUUID())) {
                    boolean m = get(att, "phantasmal_m", false);
                    float extra = (float) ((10 + (m ? victim.getMaxHealth() * 0.01 : 0)) * (get(att, "phantasmal_ult", false) ? 1.5 : 1));
                    ServerLevel l = lvl(att);
                    amount += extra;
                    wave(l, victim.position().add(0, victim.getBbHeight() / 2, 0), Math.max(1, victim.getBbWidth()), 0xb8a8e8, 10);
                }
            }
            event.setAmount(amount);
        });
    }

    private static int TICK = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Safe.run("Sins.tick", () -> {
            int tick = ++TICK;
            long t = event.getServer().overworld().getGameTime();
            for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) { Field f = it.next(); if (f.until < f.level.getGameTime()) it.remove(); else tickField(f, tick); }
            for (Iterator<Embrace> it = EMBRACES.iterator(); it.hasNext(); ) { Embrace e = it.next(); if (e.until < e.level.getGameTime()) it.remove(); else tickEmbrace(e, tick); }
            for (Iterator<Seize> it = SEIZES.iterator(); it.hasNext(); ) { Seize z = it.next(); if (z.until < z.level.getGameTime()) it.remove(); else tickSeize(z, tick); }
            for (Iterator<Lethargy> it = LETHARGY.iterator(); it.hasNext(); ) { Lethargy x = it.next(); if (x.until < x.level.getGameTime()) it.remove(); else tickLethargy(x, tick); }
            BENEATH.removeIf(b -> b.until < t);
            REBIRTH.values().removeIf(v -> v < t);
            for (Iterator<Clone> it = CLONES.iterator(); it.hasNext(); ) {
                Clone c = it.next();
                var e = c.level.getEntity(c.id);
                if (e == null || !e.isAlive()) { it.remove(); continue; }
                if (c.until < c.level.getGameTime()) { if (e instanceof LivingEntity le) aura(le, 0x101010, 0x3a3a5a, 10); e.discard(); it.remove(); continue; }
                // clones fight what their owner fights
                ServerPlayer o = c.level.getServer().getPlayerList().getPlayer(c.owner);
                if (o != null && e instanceof Mob m && tick % 10 == 0) {
                    LivingEntity want = o.getLastHurtMob() != null && o.getLastHurtMob().isAlive() ? o.getLastHurtMob() : o.getLastHurtByMob();
                    if (want != null && want != m && LoreEngine.foe(o, want)) m.setTarget(want);
                    else if (m.getTarget() == o || (m.getTarget() != null && !LoreEngine.foe(o, m.getTarget()))) m.setTarget(null);
                    if (m.distanceTo(o) > 24) m.teleportTo(o.getX(), o.getY(), o.getZ());
                }
            }
            if (tick % 20 != 0) return;
            for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) passives(p, tick);
        });
    }

    // the always-on parts of each sin
    private static void passives(ServerPlayer p, int tick) {
        ServerLevel l = lvl(p);
        // envy: luck 3 (leviathan 5), and weakness and slowness can't take hold
        Ability envy = owned(p, "leviathan", "envy");
        if (envy != null) {
            p.addEffect(new MobEffectInstance(MobEffects.LUCK, 240, envy.id().getPath().equals("leviathan") ? 4 : 2, false, false, false));
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.SLOWNESS);
        }
        // pride: what fights you and is far weaker than you feels it (weakness, slowness, fatigue, fragility II)
        Ability pride = owned(p, "lucifer", "pride");
        if (pride != null && tick % 100 == 0) {
            double mine = ep(p);
            for (LivingEntity e : LoreEngine.near(l, p, p.position(), 15)) {
                boolean fighting = (e instanceof Mob m && m.getTarget() == p) || e.getLastHurtByMob() == p || p.getLastHurtByMob() == e;
                if (!fighting || ep(e) > mine * 0.5) continue;
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1), p);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1), p);
                e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 300, 1), p);
                e.addEffect(new MobEffectInstance(LoreEffects.FRAGILITY, 300, 1), p);
            }
        }
        // wrath's growing rampage (manas reborn): +1 level every 30 seconds while it lasts, up to the cap
        if (until(p, "rampage_grow") && tick % 600 == 0) {
            int lvl = Math.min(rampageLevel(p) + 1, get(p, "rampage_max", 30) - 1);
            var cur = p.getEffect(LoreEffects.RAMPAGE);
            rampage(p, lvl, cur == null ? 600 : Math.max(cur.getDuration(), 600));
            bar(p, "jaycee.sin.rampage", ChatFormatting.RED, lvl + 1);
        }
        // a player in rampage that's running out pays for it (tensura: 10% health a second for the last 10 seconds)
        var rp = p.getEffect(LoreEffects.RAMPAGE);
        if (rp != null && rp.getDuration() <= 200 && p.getHealth() > 1 && rp.getAmplifier() >= 4)
            p.hurtServer(l, l.damageSources().magic(), Math.min(p.getMaxHealth() * 0.1f, p.getHealth() - 1));
        // rampaging mobs go after anything near them
        if (tick % 40 == 0) for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), m -> m.hasEffect(LoreEffects.RAMPAGE) && m.getTarget() == null)) {
            List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(15), e -> e != m && e.isAlive() && !(e instanceof Player pl && pl.isCreative()));
            if (!near.isEmpty()) m.setTarget(near.get(m.getRandom().nextInt(near.size())));
        }
        // phantasmal style keeps you in shadow step
        if (PHANTASMAL.contains(p.getUUID())) {
            if (owned(p, "belphegor", "sloth") == null) PHANTASMAL.remove(p.getUUID());
            else p.addEffect(new MobEffectInstance(LoreEffects.SHADOW_STEP, 60, 0, false, false, true));
        }
        // imperial ascension wearing off: your magicules go back to normal
        Long asc = get(p, "ascended_until", 0L);
        if (asc > 0 && asc <= now(p)) {
            double base = get(p, "ascended_mana", EnergyAPI.get(p).maxMana() / 2);
            var d = EnergyAPI.get(p);
            EnergyAPI.set(p, new com.brightwebs.brightcore.data.EnergyData(d.aura(), d.maxAura(), Math.min(d.mana(), base), base, d.spirit(), d.maxSpirit(), d.initialized()));
            drop(p, "ascended_until");
        }
    }
}

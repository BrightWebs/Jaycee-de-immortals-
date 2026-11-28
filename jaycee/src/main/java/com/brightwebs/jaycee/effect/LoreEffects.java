package com.brightwebs.jaycee.effect;

import com.brightwebs.brightcore.api.AbilityEvent;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.bonds.Bonds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

// the status effects tensura's and manas reborn's skills put on things, rebuilt with their numbers.
// attribute ones scale with the level the way minecraft's do; the rest are in their ticks or the damage hook below
@EventBusSubscriber(modid = JayceeMod.ID)
public final class LoreEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, JayceeMod.ID);
    private LoreEffects() {}

    private static Identifier id(String s) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, "lore_" + s); }
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, MULT = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

    // a plain effect with attribute changes set up in 'mods'
    private static DeferredHolder<MobEffect, MobEffect> reg(String name, MobEffectCategory cat, int color, Consumer<MobEffect> mods) {
        return EFFECTS.register(name, () -> { MobEffect e = new MobEffect(cat, color) {}; mods.accept(e); return e; });
    }

    // ------------------------------------------------------------ buffs

    // wrath's rampage: armor and knockback flat, attack / speed / attack speed grow with every level (tensura: attack x level / 20)
    public static final DeferredHolder<MobEffect, MobEffect> RAMPAGE = reg("rampage", MobEffectCategory.BENEFICIAL, 0xC01010, e -> {
        e.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("rampage"), 1.5, ADD);
        e.addAttributeModifier(Attributes.ATTACK_SPEED, id("rampage"), 0.02, ADD);
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("rampage"), 0.01, ADD);
    });
    public static final DeferredHolder<MobEffect, MobEffect> RAMPAGE_GUARD = reg("rampage_guard", MobEffectCategory.BENEFICIAL, 0x801010, e -> {
        e.addAttributeModifier(Attributes.ARMOR, id("rampage_guard"), 5, ADD);
        e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("rampage_guard"), 0.1, ADD);
    });
    // berserk's mad ogre: +45 attack, +20 armor, twice the size, reach +2, no burning
    public static final DeferredHolder<MobEffect, MobEffect> MAD_OGRE = reg("mad_ogre", MobEffectCategory.BENEFICIAL, 0x8A0A0A, e -> {
        e.addAttributeModifier(Attributes.ARMOR, id("mad_ogre"), 20, ADD);
        e.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("mad_ogre"), 45, ADD);
        e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("mad_ogre"), 0.4, ADD);
        e.addAttributeModifier(Attributes.BURNING_TIME, id("mad_ogre"), -1, MULT);
        e.addAttributeModifier(Attributes.STEP_HEIGHT, id("mad_ogre"), 1, ADD);
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("mad_ogre"), 0.04, ADD);
        e.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, id("mad_ogre"), 2, ADD);
        e.addAttributeModifier(Attributes.SCALE, id("mad_ogre"), 1, MULT);
    });
    // dragon mode: +12 attack, +20 armor, faster
    public static final DeferredHolder<MobEffect, MobEffect> DRAGON_MODE = reg("dragon_mode", MobEffectCategory.BENEFICIAL, 0x2F6FD8, e -> {
        e.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("dragon_mode"), 12, ADD);
        e.addAttributeModifier(Attributes.ARMOR, id("dragon_mode"), 20, ADD);
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("dragon_mode"), 0.05, ADD);
    });
    // shadow step: faster, step up blocks, hidden
    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_STEP = EFFECTS.register("shadow_step", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x101018) {
        { addAttributeModifier(Attributes.MOVEMENT_SPEED, id("shadow_step"), 0.1, ADD); addAttributeModifier(Attributes.STEP_HEIGHT, id("shadow_step"), 1, ADD); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 5 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { conceal(l, e, 40); return true; }
    });
    // presence concealment: every quarter second, anything within 40 blocks hunting you loses you
    public static final DeferredHolder<MobEffect, MobEffect> PRESENCE_CONCEALMENT = EFFECTS.register("presence_concealment", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x2A2A38) {
        { addAttributeModifier(Attributes.STEP_HEIGHT, id("presence_concealment"), 1, ADD); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 5 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { conceal(l, e, 40); return true; }
    });
    // rest: you can't move or fight, but you recover fast (health and magicules)
    public static final DeferredHolder<MobEffect, MobEffect> REST = EFFECTS.register("rest", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9FD8A6) {
        {
            for (var at : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.JUMP_STRENGTH, Attributes.BLOCK_INTERACTION_RANGE))
                addAttributeModifier(at, id("rest"), -1, MULT);
        }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            e.heal(e.getMaxHealth() * 0.05f * (a + 1));
            var d = EnergyAPI.get(e);
            EnergyAPI.set(e, d.withMana(Math.min(d.maxMana(), d.mana() + d.maxMana() * 0.03 * (a + 1))));
            return true;
        }
    });
    // instant regeneration: heals a chunk every second
    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_REGENERATION = EFFECTS.register("instant_regeneration", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFF6D9A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { e.heal(4f * (a + 1)); return true; }
    });
    // barriers: each level blocks one hit of that kind completely, then the level drops (see the damage hook)
    public static final DeferredHolder<MobEffect, MobEffect> PHYSICAL_BARRIER = reg("physical_barrier", MobEffectCategory.BENEFICIAL, 0xC8C8D8, e -> {});
    public static final DeferredHolder<MobEffect, MobEffect> MAGIC_BARRIER = reg("magic_barrier", MobEffectCategory.BENEFICIAL, 0x8FB8FF, e -> {});
    // resentful barrier (manas reborn envy): soaks hits by spending your magicules instead of health
    public static final DeferredHolder<MobEffect, MobEffect> RESENTFUL_BARRIER = reg("resentful_barrier", MobEffectCategory.BENEFICIAL, 0x2F8F4F, e -> {});
    // inspiration / ally boost: +attack and speed for allies
    public static final DeferredHolder<MobEffect, MobEffect> INSPIRATION = reg("inspiration", MobEffectCategory.BENEFICIAL, 0xFFD34A, e -> {
        e.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("inspiration"), 3, ADD);
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("inspiration"), 0.1, MULT);
        e.addAttributeModifier(Attributes.ARMOR, id("inspiration"), 2, ADD);
    });

    // ------------------------------------------------------------ debuffs

    // fragility: takes 20% more damage a level
    public static final DeferredHolder<MobEffect, MobEffect> FRAGILITY = reg("fragility", MobEffectCategory.HARMFUL, 0x7A6A9A, e -> {});
    // movement interference: -10% speed, attack speed and jump a level
    public static final DeferredHolder<MobEffect, MobEffect> MOVEMENT_INTERFERENCE = reg("movement_interference", MobEffectCategory.HARMFUL, 0x5A5A7A, e -> {
        e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("movement_interference"), 0.1, ADD);
        e.addAttributeModifier(Attributes.ATTACK_SPEED, id("movement_interference"), -0.1, MULT);
        e.addAttributeModifier(Attributes.JUMP_STRENGTH, id("movement_interference"), -0.1, MULT);
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("movement_interference"), -0.1, MULT);
    });
    // burden: heavier (gravity +50%), falls hurt 50% more
    public static final DeferredHolder<MobEffect, MobEffect> BURDEN = reg("burden", MobEffectCategory.HARMFUL, 0x4A3A2A, e -> {
        e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("burden"), 0.1, ADD);
        e.addAttributeModifier(Attributes.GRAVITY, id("burden"), 0.5, MULT);
        e.addAttributeModifier(Attributes.FALL_DAMAGE_MULTIPLIER, id("burden"), 0.5, MULT);
    });
    // chill: -20% speed / attack speed, and frozen at least 20 << level ticks
    public static final DeferredHolder<MobEffect, MobEffect> CHILL = EFFECTS.register("chill", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9FE6FF) {
        { addAttributeModifier(Attributes.MOVEMENT_SPEED, id("chill"), -0.2, MULT); addAttributeModifier(Attributes.ATTACK_SPEED, id("chill"), -0.2, MULT); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d > 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            if (!e.canFreeze()) return false;
            int min = a > 3 ? 180 : 20 << a;
            if (e.getTicksFrozen() < min) e.setTicksFrozen(min);
            return true;
        }
    });
    // silence: magic takes longer to chant, 10% a level (see LoreEngine.castNeed)
    public static final DeferredHolder<MobEffect, MobEffect> SILENCE = reg("silence", MobEffectCategory.HARMFUL, 0x6A6A6A, e -> {});
    // sleep: can't do anything until it wears off or you get hit
    public static final DeferredHolder<MobEffect, MobEffect> SLEEP = EFFECTS.register("sleep", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A3A6A) {
        {
            for (var at : java.util.List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.FLYING_SPEED, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH,
                    Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE, Attributes.FOLLOW_RANGE))
                addAttributeModifier(at, id("sleep"), -1, MULT);
        }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 10 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            if (e instanceof Mob m) m.setTarget(null);
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 0, 0, 0, 0, 0);
            return true;
        }
    });
    // infinite imprisonment: stopped completely, nothing regenerates
    public static final DeferredHolder<MobEffect, MobEffect> INFINITE_IMPRISONMENT = EFFECTS.register("infinite_imprisonment", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x1A1A3A) {
        {
            for (var at : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.JUMP_STRENGTH,
                    Attributes.BLOCK_INTERACTION_RANGE, Attributes.FOLLOW_RANGE))
                addAttributeModifier(at, id("infinite_imprisonment"), -1, MULT);
        }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 5 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0); if (e instanceof Mob m) m.setTarget(null); return true; }
    });
    // oppression: -95% speed and jump
    public static final DeferredHolder<MobEffect, MobEffect> OPPRESSION = reg("oppression", MobEffectCategory.HARMFUL, 0x3A0A5A, e -> {
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("oppression"), -0.95, MULT);
        e.addAttributeModifier(Attributes.JUMP_STRENGTH, id("oppression"), -0.95, MULT);
    });
    // webbed: stuck (-99% speed, half jump and reach)
    public static final DeferredHolder<MobEffect, MobEffect> WEBBED = reg("webbed", MobEffectCategory.HARMFUL, 0xE8E8E8, e -> {
        e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("webbed"), -0.99, MULT);
        e.addAttributeModifier(Attributes.JUMP_STRENGTH, id("webbed"), -0.5, MULT);
        e.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, id("webbed"), -0.5, MULT);
        e.addAttributeModifier(Attributes.FOLLOW_RANGE, id("webbed"), -0.8, MULT);
    });
    // petrification: -25% movement a level, and it hurts as it spreads
    public static final DeferredHolder<MobEffect, MobEffect> PETRIFICATION = EFFECTS.register("petrification", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8A8A8A) {
        { addAttributeModifier(Attributes.MOVEMENT_SPEED, id("petrification"), -0.25, MULT); addAttributeModifier(Attributes.JUMP_STRENGTH, id("petrification"), -0.25, MULT); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 40 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { if (a >= 3) e.hurtServer(l, l.damageSources().magic(), 2f * (a - 2)); return true; }
    });
    // insanity: from level 2, spiritual damage every second (2 + 2 a level), and players hear things
    public static final DeferredHolder<MobEffect, MobEffect> INSANITY = EFFECTS.register("insanity", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8A2A6A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            if (a > 0) e.hurtServer(l, l.damageSources().magic(), 2f + a * 2);
            if (e instanceof ServerPlayer p && p.getRandom().nextInt(10) == 3)
                p.level().playSound(p, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.5f, 1);
            return true;
        }
    });
    // fatal poison: 2 a level every second, can't be dodged
    public static final DeferredHolder<MobEffect, MobEffect> FATAL_POISON = EFFECTS.register("fatal_poison", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A8A1A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { e.hurtServer(l, l.damageSources().magic(), 2f * (a + 1)); return true; }
    });
    // corrosion: 2 a level every second and it eats armour (15 durability a level)
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION = EFFECTS.register("corrosion", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6A1EA0) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            e.hurtServer(l, l.damageSources().wither(), 2f * (a + 1));
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = e.getItemBySlot(s);
                if (!st.isEmpty() && st.isDamageableItem()) st.hurtAndBreak(15 * (a + 1), e, s);
            }
            return true;
        }
    });
    // curse: -20% max health a level, and at level 4+ it's lethal
    public static final DeferredHolder<MobEffect, MobEffect> CURSE = EFFECTS.register("curse", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x2A0A2A) {
        { addAttributeModifier(Attributes.MAX_HEALTH, id("curse"), -0.2, MULT); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return a >= 3 && d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) { if (a >= 4) e.hurtServer(l, l.damageSources().magic(), e.getMaxHealth()); return true; }
    });
    // black burn: black flame that keeps burning, 4 a level every second, through fire immunity
    public static final DeferredHolder<MobEffect, MobEffect> BLACK_BURN = EFFECTS.register("black_burn", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x1A0A2A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            e.hurtServer(l, l.damageSources().magic(), 4f * (a + 1));
            var spr = com.brightwebs.brightcore.fx.BrightParticles.ALL.get("fire_black");
            if (spr != null) l.sendParticles(spr.get(), e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
            return true;
        }
    });
    // magic interference: no flying
    public static final DeferredHolder<MobEffect, MobEffect> MAGIC_INTERFERENCE = EFFECTS.register("magic_interference", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x5A3A8A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 10 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            if (e instanceof ServerPlayer p && p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) { p.getAbilities().mayfly = false; p.getAbilities().flying = false; p.onUpdateAbilities(); }
            return true;
        }
    });
    // energy blockade: your magicules leak away instead of coming back (20% of regen a level)
    public static final DeferredHolder<MobEffect, MobEffect> ENERGY_BLOCKADE = EFFECTS.register("energy_blockade", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A5A8A) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            var d = EnergyAPI.get(e);
            EnergyAPI.set(e, d.withMana(Math.max(0, d.mana() - d.maxMana() * 0.002 * (a + 1))));
            return true;
        }
    });
    // anti skill: can't use skills at all while it lasts
    public static final DeferredHolder<MobEffect, MobEffect> ANTI_SKILL = reg("anti_skill", MobEffectCategory.HARMFUL, 0x9A2A2A, e -> {});
    // mind control: a mob under it fights for whoever controls it (Bonds owner), then goes back
    public static final DeferredHolder<MobEffect, MobEffect> MIND_CONTROL = EFFECTS.register("mind_control", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xC040C0) {
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 20 == 0 || d == 1; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            if (!(e instanceof Mob m)) return true;
            MobEffectInstance i = m.getEffect(LoreEffects.MIND_CONTROL);
            if (i != null && !i.isInfiniteDuration() && i.getDuration() <= 1) { m.setData(Bonds.OWNER, ""); m.setTarget(null); }
            return true;
        }
    });


    // imperial ascension (manas reborn pride): double health, can't be knocked around (the doubled magicules are handled in Sins)
    public static final DeferredHolder<MobEffect, MobEffect> IMPERIAL_ASCENSION = reg("imperial_ascension", MobEffectCategory.BENEFICIAL, 0xFFD34A, e -> {
        e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("imperial_ascension"), 1, ADD);
        e.addAttributeModifier(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, id("imperial_ascension"), 1, ADD);
        e.addAttributeModifier(Attributes.MAX_HEALTH, id("imperial_ascension"), 1, MULT);
    });
    // final yawn (manas reborn sloth's lullaby): +20% damage, +10% speed, 5% less damage taken a level. you fall asleep when it ends
    public static final DeferredHolder<MobEffect, MobEffect> FINAL_YAWN = EFFECTS.register("final_yawn", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xC8B8F8) {
        { addAttributeModifier(Attributes.ATTACK_DAMAGE, id("final_yawn"), 0.2, MULT); addAttributeModifier(Attributes.MOVEMENT_SPEED, id("final_yawn"), 0.1, MULT); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 10 == 0 || d == 1; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            MobEffectInstance i = e.getEffect(LoreEffects.FINAL_YAWN);
            if (i != null && i.getDuration() <= 1) e.addEffect(new MobEffectInstance(LoreEffects.SLEEP, 1200, 0, true, false, true));
            else if (e.getHealth() < e.getMaxHealth()) e.heal(0.5f * (a + 1));
            return true;
        }
    });
    // drowsiness (tensura sloth): -25% speed and jump, -4 attack, slower swings, and 20% more damage taken a level
    public static final DeferredHolder<MobEffect, MobEffect> DROWSINESS = EFFECTS.register("drowsiness", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xB8A8E8) {
        {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("drowsiness"), -0.25, MULT);
            addAttributeModifier(Attributes.ATTACK_DAMAGE, id("drowsiness"), -4, ADD);
            addAttributeModifier(Attributes.ATTACK_SPEED, id("drowsiness"), -0.1, MULT);
            addAttributeModifier(Attributes.JUMP_STRENGTH, id("drowsiness"), -0.25, MULT);
        }
        @Override public void addAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap map, int amp) { super.addAttributeModifiers(map, 0); }
        @Override public boolean shouldApplyEffectTickThisTick(int d, int a) { return d % 40 == 0; }
        @Override public boolean applyEffectTick(ServerLevel l, LivingEntity e, int a) {
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 1, 0.2, 0, 0.2, 0);
            return true;
        }
    });

    public static void init(IEventBus bus) { EFFECTS.register(bus); }

    // ------------------------------------------------------------ hooks

    static void conceal(ServerLevel l, LivingEntity e, double r) {
        for (Mob m : l.getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(r), m -> m.getTarget() == e)) m.setTarget(null);
    }

    static boolean has(LivingEntity e, Holder<MobEffect> h) { return e.hasEffect(h); }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        Safe.run("LoreEffects.onIncoming", () -> {
            LivingEntity e = event.getEntity();
            float amount = event.getAmount();
            // fragility: +20% a level
            MobEffectInstance f = e.getEffect(FRAGILITY);
            if (f != null) amount *= 1 + 0.2f * (f.getAmplifier() + 1);
            MobEffectInstance dz = e.getEffect(DROWSINESS);
            if (dz != null) amount *= 1 + 0.2f * (dz.getAmplifier() + 1);
            // sleep breaks when you're hit
            if (e.hasEffect(SLEEP)) e.removeEffect(SLEEP);
            // barriers: one level blocks one hit of their kind
            boolean magic = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
            Holder<MobEffect> barrier = magic ? MAGIC_BARRIER : PHYSICAL_BARRIER;
            MobEffectInstance b = e.getEffect(barrier);
            if (b != null && amount > 0) {
                event.setCanceled(true);
                int left = b.getAmplifier() - 1, dur = b.getDuration();
                e.removeEffect(barrier);
                if (left >= 0) e.addEffect(new MobEffectInstance(barrier, dur, left, b.isAmbient(), b.isVisible()));
                e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1, magic ? 1.4f : 0.9f);
                return;
            }
            // resentful barrier: magicules take the hit (10 per point of damage), health only once you run dry
            if (e.hasEffect(RESENTFUL_BARRIER) && amount > 0) {
                double cost = amount * 10;
                if (EnergyAPI.spendMana(e, cost)) { event.setCanceled(true); return; }
            }
            event.setAmount(amount);
        });
    }

    // anti skill stops skill use
    @SubscribeEvent
    public static void onActivate(AbilityEvent.Activate event) {
        if (event.getPlayer().hasEffect(ANTI_SKILL)) event.setCanceled(true);
    }
}

package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.api.skill.DamageKind;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.brightwebs.jaycee.skill.SkillHelper.*;

// the beta's ten skills, working like they do in tensura 2.0.1.4 (except raphael)
//
//   unique    great sage, predator         (you get one at random when you're reborn)
//   ultimate  raphael                      (great sage evolves into it: mastered + 1M EP)
//   extra     magic sense
//   common    self regeneration
//   intrinsic flame breath (lizardman and lesser daemon lines), body armor
//   resist    flame attack resistance -> flame attack nullification (learned by taking fire)
//   magic     fireball                     (aspectual / fire magic)
public final class JayceeSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, JayceeMod.ID);

    // unique

    // tensura modes: analytical appraisal, analysis, refine. the bonuses are always on while you have it
    public static final DeferredHolder<Ability, Ability> GREAT_SAGE = SKILLS.register("great_sage", () ->
            new Ability(new Ability.Properties().type(SkillType.UNIQUE).category(AbilityCategory.SUPPORT).switchable()
                    .mode("analytical_appraisal", 0, 0, 0)
                    .mode("analysis", 0, 0, 0)
                    .mode("refine", 0, 0, 0)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    return switch (mode) {
                        case 0 -> GreatSage.appraisal(player, this);
                        case 1 -> GreatSage.analysis(player, this);
                        default -> { Refining.openSage(player); yield true; }
                    };
                }
                @Override public void tick(ServerPlayer p, ServerLevel l) { GreatSage.tick(p, this); }
                @Override public void onForgotten(ServerPlayer p) { GreatSage.forgotten(p); }
                @Override public void onSwitch(ServerPlayer p, boolean on) { if (!on) GreatSage.switchedOff(p); }
            });

    // tensura modes: predation, analysis, stomach, mimicry, isolation
    public static final DeferredHolder<Ability, Ability> PREDATOR = SKILLS.register("predator", () ->
            new Ability(new Ability.Properties().type(SkillType.UNIQUE).category(AbilityCategory.OFFENSE)
                    .mode("predation", 0, 0, 0)
                    .mode("analysis", 0, 0, 0)
                    .mode("stomach", 0, 0, 0)
                    .mode("mimicry", 0, 0, 0)
                    .mode("isolation", 0, 0, 0)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    return switch (mode) {
                        case 0 -> Predation.predation(player, this);
                        case 1 -> { Refining.openStomachRefining(player); yield true; }
                        case 2 -> { Stomach.open(player); yield true; }
                        case 3 -> { player.sendOverlayMessage(Component.translatable("jaycee.message.coming_soon")); yield false; }
                        default -> Predation.isolation(player, this);
                    };
                }
                @Override public boolean ignoresCooldown(ServerPlayer player, int mode) { return mode == 0 && player.isShiftKeyDown(); }
            });

    // ultimate

    // not in tensura so it's our own design (great sage's bonuses still apply)
    public static final DeferredHolder<Ability, Ability> RAPHAEL = SKILLS.register("raphael", () ->
            new Ability(new Ability.Properties().type(SkillType.ULTIMATE).category(AbilityCategory.SUPPORT).switchable()
                    .mode("thought_acceleration", 60, 0, 25)
                    .mode("analytical_appraisal", 0, 0, 0)
                    .mode("future_attack_prediction", 80, 0, 60)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    AbilityAPI.addMasteryPoint(player, this);
                    return switch (mode) {
                        case 0 -> { SkillState.accelerate(player, 24, 15, 5, 1); yield true; }
                        case 1 -> GreatSage.appraisal(player, this);
                        default -> {
                            SkillState.foresight(player, 30);
                            player.sendOverlayMessage(Component.translatable("jaycee.skill.future_attack_prediction.on"));
                            yield true;
                        }
                    };
                }
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (GreatSage.sage() == null || !AbilityAPI.isSwitchedOn(p, GreatSage.sage())) GreatSage.tick(p, this);
                }
                @Override public void onForgotten(ServerPlayer p) { GreatSage.forgotten(p); }
                @Override public void onSwitch(ServerPlayer p, boolean on) { if (!on && !GreatSage.hasSage(p)) GreatSage.switchedOff(p); }
            });

    // extra

    // tensura's magic sense: a toggle, 5 magicules a second, living things glow through walls for you
    public static final DeferredHolder<Ability, Ability> MAGIC_PERCEPTION = SKILLS.register("magic_perception", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle().manaCost(5)) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { MagicSense.tick(p, this); }
                @Override public void onToggle(ServerPlayer p, boolean on) { MagicSense.toggled(p, on); }
            });

    // tensura's self regen: a toggle, on once you learn it. while you're hurt it heals 2 HP a second
    // (4 mastered) for 100 magicules every 5 seconds, and turns off when you run out
    public static final DeferredHolder<Ability, Ability> SELF_REGENERATION = SKILLS.register("self_regeneration", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.DEFENSE).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (Regeneration.overridden(p, "ultraspeed_regeneration") || Regeneration.overridden(p, "infinite_regeneration")) return;
                    boolean hurt = p.getHealth() < p.getMaxHealth();
                    if (p.tickCount % Tk.SKILL_TICK == 0 && hurt) {
                        if (!Tk.spend(p, this, 100)) {
                            p.sendSystemMessage(Component.translatable("jaycee.skill.lack_magicule.toggled_off", name()).withStyle(net.minecraft.ChatFormatting.RED));
                            AbilityAPI.setToggled(p, this, false);
                            return;
                        }
                        REGEN_UNTIL.put(p.getUUID(), p.tickCount + 240L);
                        Tk.activatedTime(p, this);
                    }
                    if (hurt && p.tickCount % 20 == 0 && REGEN_UNTIL.getOrDefault(p.getUUID(), 0L) > p.tickCount) {
                        boolean m = Tk.mastered(p, this);
                        p.heal(m ? 4 : 2);
                        if (m) Regeneration.restoreSpirit(p, 8);  // tensura: mastered also heals spiritual health
                        com.brightwebs.brightcore.fx.Fx.aura(l, com.brightwebs.brightcore.fx.BrightParticles.HEAL, p, 4);
                    }
                }
                @Override public void onLearned(ServerPlayer p) { AbilityAPI.setToggled(p, this, true); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) REGEN_UNTIL.remove(p.getUUID()); }
            });
    private static final java.util.Map<java.util.UUID, Long> REGEN_UNTIL = new java.util.HashMap<>();

    // tensura: instant regen, all missing health at once for 100 magicules per HP (60 mastered)
    public static final DeferredHolder<Ability, Ability> ULTRASPEED_REGENERATION = SKILLS.register("ultraspeed_regeneration", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.DEFENSE).passive().switchable()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { Regeneration.instant(p, this, false); }
            });

    // tensura: the top of the chain, instant regen that heals spiritual health too
    public static final DeferredHolder<Ability, Ability> INFINITE_REGENERATION = SKILLS.register("infinite_regeneration", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.DEFENSE).passive().switchable()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { Regeneration.instant(p, this, true); }
            });

    // intrinsic

    // tensura: hold to breathe fire, 8 damage a second (16 mastered), 30 magicules a second
    public static final DeferredHolder<Ability, Ability> FLAME_BREATH = SKILLS.register("flame_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) { return FireSkills.breathHeld(p, this, held); }
            });

    // tensura: press for scale armor in every empty armor slot (50 magicules), press again to shed it
    public static final DeferredHolder<Ability, Ability> BODY_ARMOR = SKILLS.register("body_armor", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.DEFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return BodyArmor.press(p, this); }
                @Override public void tick(ServerPlayer p, ServerLevel l) { BodyArmor.tick(p, this); }
            });

    // magic

    // tensura: hold to chant (3s, 1s mastered), let go to fire. 30,000 magicules (50,000 charged)
    public static final DeferredHolder<Ability, Ability> FIREBALL = SKILLS.register("fireball", () ->
            new Ability(new Ability.Properties().magic("aspectual", "fire", 3f, 40).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) { return FireSkills.fireballHeld(p, this, held); }
                @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) { FireSkills.fireballRelease(p, this, held); }
            });

    static {
        BrightSkills.register(SKILLS, DamageKind.FLAME, "flame_attack_resistance", "flame_attack_nullification");
    }


    // ---------------- pressure (haki). see Pressure.java ----------------

    // tensura's coercion: a roar that scares and weakens anything way weaker than you
    public static final DeferredHolder<Ability, Ability> COERCION = SKILLS.register("coercion", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return Pressure.coercion(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.isShiftKeyDown(); }
            });

    // tensura's haki: hold it to let your aura out and crush everything weaker around you
    public static final DeferredHolder<Ability, Ability> HAKI = SKILLS.register("haki", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return Pressure.hakiStart(p, this); }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return Pressure.hakiHeld(p, this, held, Pressure.Variant.DEFAULT, Pressure.HAKI_MULT, Pressure.HAKI_COST);
                }
                @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) { Pressure.hakiReleased(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.isShiftKeyDown(); }
            });

    // tensura's demon lord haki: stronger purple haki, plus a coat mode that wraps your attacks in it
    public static final DeferredHolder<Ability, Ability> DEMON_LORD_HAKI = SKILLS.register("demon_lord_haki", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.OFFENSE).held()
                    .mode("release", 0, 0, 0).mode("coat", 0, 0, 0)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l, int mode) {
                    return mode == 1 ? Pressure.toggleCoat(p, this) && false : Pressure.hakiStart(p, this);
                }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return mode == 0 && Pressure.hakiHeld(p, this, held, Pressure.Variant.DEMON_LORD, Pressure.DEMON_LORD_MULT, Pressure.DEMON_LORD_COST);
                }
                @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) { if (mode == 0) Pressure.hakiReleased(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return mode == 1 || p.isShiftKeyDown(); }
            });

    // tensura's hero haki: the holy version, golden
    public static final DeferredHolder<Ability, Ability> HERO_HAKI = SKILLS.register("hero_haki", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return Pressure.hakiStart(p, this); }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return Pressure.hakiHeld(p, this, held, Pressure.Variant.HERO, Pressure.HERO_MULT, Pressure.HERO_COST);
                }
                @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) { Pressure.hakiReleased(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.isShiftKeyDown(); }
            });

    // tensura's mortal fear: your haki pours out of all your subordinates and pumps them up
    public static final DeferredHolder<Ability, Ability> MORTAL_FEAR = SKILLS.register("mortal_fear", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return Pressure.hakiStart(p, this); }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) { return Pressure.mortalFearHeld(p, this, held); }
                @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) { Pressure.hakiReleased(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.isShiftKeyDown(); }
            });

    // ---------------- more tensura skills. see MoreSkills.java ----------------

    // strength: +6 attack for a minute (2 mastered, +12 mastered)
    public static final DeferredHolder<Ability, Ability> STRENGTH = SKILLS.register("strength", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.SUPPORT)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.strengthen(p, this, 30, 1200, 2400, 1, 2, 3); }
                @Override public void tick(ServerPlayer p, ServerLevel l) { MoreSkills.strengthTick(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.hasEffect(com.brightwebs.jaycee.effect.JayceeEffects.STRENGTHEN); }
            });

    // steel strength: the stronger one, +12 attack (+18 mastered) for 30s (90s mastered)
    public static final DeferredHolder<Ability, Ability> STEEL_STRENGTH = SKILLS.register("steel_strength", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.strengthen(p, this, 30, 600, 1800, 2, 3, 3); }
                @Override public void tick(ServerPlayer p, ServerLevel l) { MoreSkills.strengthTick(p, this); }
                @Override public boolean ignoresCooldown(ServerPlayer p, int mode) { return p.hasEffect(com.brightwebs.jaycee.effect.JayceeEffects.STRENGTHEN); }
            });

    // strengthen body: always on, +5 armor and you take 80% damage
    public static final DeferredHolder<Ability, Ability> STRENGTHEN_BODY = SKILLS.register("strengthen_body", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.DEFENSE).passive().switchable()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { if (p.tickCount % 20 == 0) MoreSkills.strengthenBody(p, true); }
                @Override public void onSwitch(ServerPlayer p, boolean on) { MoreSkills.strengthenBody(p, on); }
                @Override public void onForgotten(ServerPlayer p) { MoreSkills.strengthenBody(p, false); }
            });

    // thought acceleration on its own (great sage and raphael already have it built in)
    public static final DeferredHolder<Ability, Ability> THOUGHT_ACCELERATION = SKILLS.register("thought_acceleration", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).passive().switchable()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { if (p.tickCount % 20 == 0) MoreSkills.thought(p, this, true); }
                @Override public void onSwitch(ServerPlayer p, boolean on) { MoreSkills.thought(p, this, on); }
                @Override public void onForgotten(ServerPlayer p) { MoreSkills.thought(p, this, false); }
            });

    // paralysis: your hits paralyze while it's on your bar
    public static final DeferredHolder<Ability, Ability> PARALYSIS = SKILLS.register("paralysis", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public void onHit(ServerPlayer p, LivingEntity t, float dmg) {
                    MoreSkills.touch(p, this, t, e -> e.addEffect(new MobEffectInstance(com.brightwebs.jaycee.effect.JayceeEffects.PARALYSIS, 200, Tk.mastered(p, this) ? 1 : 0), p),
                            com.brightwebs.brightcore.fx.BrightParticles.SPARK);
                }
            });

    // poison: your hits poison while it's on your bar
    public static final DeferredHolder<Ability, Ability> POISON = SKILLS.register("poison", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public void onHit(ServerPlayer p, LivingEntity t, float dmg) {
                    MoreSkills.touch(p, this, t, e -> e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, Tk.mastered(p, this) ? 2 : 0), p),
                            com.brightwebs.brightcore.fx.BrightParticles.ACID);
                }
            });

    // corrosion: your hits wither, and pressing it rots raw meat into rotten flesh
    public static final DeferredHolder<Ability, Ability> CORROSION = SKILLS.register("corrosion", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.rotMeat(p, this); }
                @Override public void onHit(ServerPlayer p, LivingEntity t, float dmg) {
                    MoreSkills.touch(p, this, t, e -> e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, Tk.mastered(p, this) ? 1 : 0), p),
                            com.brightwebs.brightcore.fx.BrightParticles.VOID);
                }
            });

    // ice breath: hold, 8 damage a second (16 mastered), freezes
    public static final DeferredHolder<Ability, Ability> ICE_BREATH = SKILLS.register("ice_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return MoreSkills.breath(p, this, held, 30, 8, 16, 8, com.brightwebs.brightcore.fx.BrightParticles.FROST,
                            com.brightwebs.brightcore.fx.BrightParticles.SHARD, net.minecraft.sounds.SoundEvents.POWDER_SNOW_STEP, MoreSkills::iceHit);
                }
            });

    // thunder breath: hold, 10 damage a second (20 mastered)
    public static final DeferredHolder<Ability, Ability> THUNDER_BREATH = SKILLS.register("thunder_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return MoreSkills.breath(p, this, held, 50, 10, 20, 8, com.brightwebs.brightcore.fx.BrightParticles.SPARK,
                            com.brightwebs.brightcore.fx.BrightParticles.DARK_SPARK, net.minecraft.sounds.SoundEvents.BEE_LOOP, MoreSkills::thunderHit);
                }
            });

    // poisonous breath: hold, 15 damage a second (30 mastered), poisons
    public static final DeferredHolder<Ability, Ability> POISONOUS_BREATH = SKILLS.register("poisonous_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return MoreSkills.breath(p, this, held, 100, 15, 30, 8, com.brightwebs.brightcore.fx.BrightParticles.ACID,
                            com.brightwebs.brightcore.fx.BrightParticles.MIST, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, MoreSkills::poisonHit);
                }
            });

    // paralysing breath: hold, 2 damage a second, paralysis III. costs half once mastered
    public static final DeferredHolder<Ability, Ability> PARALYSING_BREATH = SKILLS.register("paralysing_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
                    return MoreSkills.breath(p, this, held, Tk.mastered(p, this) ? 25 : 50, 2, 2, 8, com.brightwebs.brightcore.fx.BrightParticles.SPARK,
                            com.brightwebs.brightcore.fx.BrightParticles.BUBBLE, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, MoreSkills::paralysisHit);
                }
            });

    // ultrasonic waves: a sonic boom line, 8 damage, 3s cooldown
    public static final DeferredHolder<Ability, Ability> ULTRASONIC_WAVES = SKILLS.register("ultrasonic_waves", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.ultrasonic(p, this); }
            });

    // voice cannon: shout a blast, 20 damage (30 mastered)
    public static final DeferredHolder<Ability, Ability> VOICE_CANNON = SKILLS.register("voice_cannon", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.voiceCannon(p, this); }
            });

    // water blade: a slicing disc of water, 40 damage
    public static final DeferredHolder<Ability, Ability> WATER_BLADE = SKILLS.register("water_blade", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.waterBlade(p, this); }
            });

    // hydraulic propulsion: shoot yourself forward through water or rain
    public static final DeferredHolder<Ability, Ability> HYDRAULIC_PROPULSION = SKILLS.register("hydraulic_propulsion", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.MOVEMENT)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return MoreSkills.hydraulic(p, this); }
            });

    // gravity flight: a toggle, fly for 5 magicules a second
    public static final DeferredHolder<Ability, Ability> GRAVITY_FLIGHT = SKILLS.register("gravity_flight", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.MOVEMENT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { MoreSkills.gravityFlight(p, this); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) MoreSkills.gravityFlightOff(p); }
                @Override public void onForgotten(ServerPlayer p) { MoreSkills.gravityFlightOff(p); }
            });

    // sense heat source: warm things glow for you in 20 blocks (free, like tensura)
    public static final DeferredHolder<Ability, Ability> SENSE_HEAT_SOURCE = SKILLS.register("sense_heat_source", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { MoreSkills.sense(p, this, 0, 20); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) MagicSense.clear(p); }
            });

    // universal perception: magic sense + heat + sound rolled into one, 30 blocks, 25 magicules a second
    public static final DeferredHolder<Ability, Ability> UNIVERSAL_PERCEPTION = SKILLS.register("universal_perception", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { MoreSkills.sense(p, this, 25, 30); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) MagicSense.clear(p); }
            });

    private JayceeSkills() {}

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, path); }

    public static void init(IEventBus modBus) {
        SKILLS.register(modBus);
        BrightSkills.addStartingUnique(id("great_sage"));
        BrightSkills.addStartingUnique(id("predator"));
    }
}

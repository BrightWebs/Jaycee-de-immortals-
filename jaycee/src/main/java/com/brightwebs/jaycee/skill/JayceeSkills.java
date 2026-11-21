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

    private JayceeSkills() {}

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, path); }

    public static void init(IEventBus modBus) {
        SKILLS.register(modBus);
        BrightSkills.addStartingUnique(id("great_sage"));
        BrightSkills.addStartingUnique(id("predator"));
    }
}

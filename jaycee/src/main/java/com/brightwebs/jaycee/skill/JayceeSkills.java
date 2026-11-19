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

/**
 * The beta's ten abilities, working as they do in Tensura: Reincarnated 2.0.1.4 (Raphael excepted).
 *
 *  Unique    Great Sage, Predator          (one is granted at random on rebirth)
 *  Ultimate  Raphael                       (Great Sage evolves into it: mastered + 1M EP)
 *  Extra     Magic Sense
 *  Common    Self-Regeneration
 *  Intrinsic Flame Breath (Lizardman and Lesser Daemon lines), Body Armor
 *  Resist    Flame Attack Resistance -> Flame Attack Nullification (learned by enduring fire)
 *  Magic     Fireball                      (Aspectual / Fire Magic)
 */
public final class JayceeSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, JayceeMod.ID);

    // ---------------- UNIQUE ----------------

    /** Tensura modes: Analytical Appraisal, Analysis, Refine. The bonuses are always on while owned. */
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

    /** Tensura modes: Predation, Analysis, Stomach, Mimicry, Isolation. */
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

    // ---------------- ULTIMATE ----------------

    /** Not part of Tensura: Reincarnated, so it keeps its own design (Great Sage's bonuses apply too). */
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

    // ---------------- EXTRA ----------------

    /** Tensura's Magic Sense: a toggle; 5 magicules a second; living things glow through walls for you. */
    public static final DeferredHolder<Ability, Ability> MAGIC_PERCEPTION = SKILLS.register("magic_perception", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle().manaCost(5)) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { MagicSense.tick(p, this); }
                @Override public void onToggle(ServerPlayer p, boolean on) { MagicSense.toggled(p, on); }
            });

    /**
     * Tensura's Self-Regeneration: a toggle, on once learned. While hurt it regenerates 2 HP a second
     * (4 mastered) for 100 magicules every 5 seconds, and switches off when magicules run out.
     */
    public static final DeferredHolder<Ability, Ability> SELF_REGENERATION = SKILLS.register("self_regeneration", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.DEFENSE).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
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
                        p.heal(Tk.mastered(p, this) ? 4 : 2);
                        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 3, 0.35, 0.5, 0.35, 0);
                    }
                }
                @Override public void onLearned(ServerPlayer p) { AbilityAPI.setToggled(p, this, true); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) REGEN_UNTIL.remove(p.getUUID()); }
            });
    private static final java.util.Map<java.util.UUID, Long> REGEN_UNTIL = new java.util.HashMap<>();

    // ---------------- INTRINSIC ----------------

    /** Tensura: hold to breathe fire, 8 damage a second (16 mastered), 30 magicules a second. */
    public static final DeferredHolder<Ability, Ability> FLAME_BREATH = SKILLS.register("flame_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).held()) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return true; }
                @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) { return FireSkills.breathHeld(p, this, held); }
            });

    /** Tensura: press for scale armor in every empty armor slot (50 magicules); press again to shed it. */
    public static final DeferredHolder<Ability, Ability> BODY_ARMOR = SKILLS.register("body_armor", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.DEFENSE)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) { return BodyArmor.press(p, this); }
                @Override public void tick(ServerPlayer p, ServerLevel l) { BodyArmor.tick(p, this); }
            });

    // ---------------- MAGIC ----------------

    /** Tensura: hold to chant (3 s, 1 s mastered), release to fire; 30,000 magicules (50,000 charged). */
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

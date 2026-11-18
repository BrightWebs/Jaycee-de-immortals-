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
 * The beta's ten abilities: one or two from every rank.
 *
 *  Unique    Great Sage, Predator          (one is granted at random on rebirth)
 *  Ultimate  Raphael                       (Great Sage evolves into it: mastered + 1M EP)
 *  Extra     Magic Perception, Self-Regeneration
 *  Intrinsic Flame Breath                  (Lizardman and Lesser Daemon lines)
 *  Common    Body Armor
 *  Resist    Flame Attack Resistance -> Flame Attack Nullification (learned by enduring fire)
 *  Magic     Fireball                      (Aspectual / Fire Magic)
 */
public final class JayceeSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, JayceeMod.ID);

    // ---------------- UNIQUE ----------------

    public static final DeferredHolder<Ability, Ability> GREAT_SAGE = SKILLS.register("great_sage", () ->
            new Ability(new Ability.Properties().type(SkillType.UNIQUE).category(AbilityCategory.SUPPORT)
                    .mode("analytical_appraisal", 5, 0, 1)
                    .mode("thought_acceleration", 40, 0, 30)
                    .mode("chant_annulment", 20, 0, 40)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    return switch (mode) {
                        case 0 -> Appraisal.appraise(player, false);
                        case 1 -> { SkillState.accelerate(player, 16, 10, 3, 0); yield true; }
                        default -> {
                            SkillState.chantAnnulment(player, 30);
                            player.sendOverlayMessage(Component.translatable("jaycee.skill.chant_annulment.on"));
                            yield true;
                        }
                    };
                }
            });

    public static final DeferredHolder<Ability, Ability> PREDATOR = SKILLS.register("predator", () ->
            new Ability(new Ability.Properties().type(SkillType.UNIQUE).category(AbilityCategory.OFFENSE)
                    .mode("predation", 15, 0, 1.5f)
                    .mode("analysis", 5, 0, 1)
                    .mode("stomach", 0, 0, 0.5f)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    return switch (mode) {
                        case 0 -> Predation.devour(player, level, this);
                        case 1 -> Appraisal.appraise(player, true);
                        default -> { Stomach.open(player); yield true; }
                    };
                }
            });

    // ---------------- ULTIMATE ----------------

    public static final DeferredHolder<Ability, Ability> RAPHAEL = SKILLS.register("raphael", () ->
            new Ability(new Ability.Properties().type(SkillType.ULTIMATE).category(AbilityCategory.SUPPORT)
                    .mode("thought_acceleration", 60, 0, 25)
                    .mode("analytical_appraisal", 0, 0, 0.5f)
                    .mode("future_attack_prediction", 80, 0, 60)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    return switch (mode) {
                        case 0 -> { SkillState.accelerate(player, 24, 15, 5, 1); yield true; }
                        case 1 -> Appraisal.appraise(player, true);
                        default -> {
                            SkillState.foresight(player, 30);
                            player.sendOverlayMessage(Component.translatable("jaycee.skill.future_attack_prediction.on"));
                            yield true;
                        }
                    };
                }
            });

    // ---------------- EXTRA ----------------

    public static final DeferredHolder<Ability, Ability> MAGIC_PERCEPTION = SKILLS.register("magic_perception", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    keep(p, MobEffects.NIGHT_VISION, 0);
                    if (p.tickCount % 20 != 0) return;
                    for (LivingEntity e : around(p, 20)) give(e, MobEffects.GLOWING, 2, 0);
                    if (!p.isCreative() && !EnergyAPI.spendMana(p, 1)) AbilityAPI.setToggled(p, this, false);
                }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) p.removeEffect(MobEffects.NIGHT_VISION); }
            });

    public static final DeferredHolder<Ability, Ability> SELF_REGENERATION = SKILLS.register("self_regeneration", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (p.tickCount % 40 != 0 || p.getHealth() >= p.getMaxHealth() || p.hurtTime > 0) return;
                    p.heal(1f + AbilityAPI.level(p, this) * 0.2f);
                    if (p.getRandom().nextInt(4) == 0) AbilityAPI.addMastery(p, this, 1);
                }
            });

    // ---------------- INTRINSIC ----------------

    public static final DeferredHolder<Ability, Ability> FLAME_BREATH = SKILLS.register("flame_breath", () ->
            new Ability(new Ability.Properties().type(SkillType.INTRINSIC).category(AbilityCategory.OFFENSE).manaCost(20).cooldownSeconds(6)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) {
                    Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                    for (int i = 1; i <= 7; i++) {
                        Vec3 at = eye.add(look.scale(i));
                        l.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6 + i * 2, 0.12 * i, 0.12 * i, 0.12 * i, 0.02);
                    }
                    float dmg = 3f * power(p, this);
                    for (LivingEntity t : around(p, 8)) {
                        Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
                        if (to.length() > 8 || to.normalize().dot(look) < 0.85) continue;
                        t.igniteForTicks(100);
                        t.hurtServer(l, p.damageSources().playerAttack(p), dmg);
                    }
                    l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1, 0.8f);
                    return true;
                }
            });

    // ---------------- COMMON ----------------

    public static final DeferredHolder<Ability, Ability> BODY_ARMOR = SKILLS.register("body_armor", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.DEFENSE).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    keep(p, MobEffects.RESISTANCE, 0);
                    if (p.tickCount % 20 == 0 && !p.isCreative() && !EnergyAPI.spendAura(p, 2)) AbilityAPI.setToggled(p, this, false);
                }
                @Override public void onToggle(ServerPlayer p, boolean on) {
                    if (!on) p.removeEffect(MobEffects.RESISTANCE);
                    else p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1, 1);
                }
            });

    // ---------------- MAGIC ----------------

    public static final DeferredHolder<Ability, Ability> FIREBALL = SKILLS.register("fireball", () ->
            new Ability(new Ability.Properties().magic("aspectual", "fire", 0.4f, 40).category(AbilityCategory.OFFENSE).manaCost(30).cooldownSeconds(1.5f)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) {
                    int shots = AbilityAPI.level(p, this) >= 7 ? 3 : AbilityAPI.level(p, this) >= 4 ? 2 : 1;
                    SkillState.cast(p, this, () -> {
                        for (int i = 0; i < shots; i++) {
                            Vec3 look = p.getLookAngle();
                            Vec3 dir = look.add((i - (shots - 1) / 2.0) * 0.12 * -look.z, 0, (i - (shots - 1) / 2.0) * 0.12 * look.x);
                            SmallFireball ball = new SmallFireball(l, p, dir.normalize().scale(1.4));
                            ball.setPos(p.getX() + look.x, p.getEyeY() - 0.1, p.getZ() + look.z);
                            l.addFreshEntity(ball);
                        }
                        l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1, 1.2f);
                    });
                    return true;
                }
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

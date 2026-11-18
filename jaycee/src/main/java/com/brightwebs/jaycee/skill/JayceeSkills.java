package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.brightwebs.jaycee.skill.SkillHelper.*;

/**
 * Jaycee's skills, in Tensura's ranks: Common, Extra, Unique and each race line's Intrinsic.
 * (Resistances and Nullifications come from Bright Core and are learned by enduring damage.)
 *
 * Every Unique has modes: Shift + its key switches mode. Damage grows with mastery, up to x2.
 */
public final class JayceeSkills {
    private static final DeferredRegister<Ability> SKILLS = DeferredRegister.create(AbilityRegistry.REGISTRY, JayceeMod.ID);

    private static Ability.Properties unique(AbilityCategory c) {
        return new Ability.Properties().type(SkillType.UNIQUE).category(c);
    }

    // =====================================================================================
    // UNIQUE: one is granted at random when you're reborn (choose a race).
    // =====================================================================================

    /** Devour what you defeat. */
    public static final DeferredHolder<Ability, Ability> GLUTTON = SKILLS.register("glutton", () ->
            new Ability(unique(AbilityCategory.OFFENSE)
                    .mode("devour", 40, 0, 4)
                    .mode("analyze", 5, 0, 1)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    LivingEntity t = target(player, mode == 0 ? 5 : 24);
                    if (t == null) return false;
                    if (mode == 1) {
                        player.sendSystemMessage(Component.translatable("jaycee.skill.analyze",
                                t.getDisplayName(), Math.round(t.getHealth()), Math.round(t.getMaxHealth()),
                                EnergyAPI.format(EnergyAPI.get(t).ep())));
                        return true;
                    }
                    t.hurtServer(level, player.damageSources().indirectMagic(player, player), 6 * power(player, this));
                    puff(level, ParticleTypes.SQUID_INK, t, 30, 0.4);
                    level.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1, 0.6f);
                    if (!t.isAlive()) {
                        // Devoured: the Glutton keeps an extra share of what it ate.
                        EnergyAPI.absorbKill(player, t, com.brightwebs.brightcore.api.race.Alignment.NEUTRAL);
                        player.heal(4);
                    }
                    return true;
                }
            });

    /** Strike unseen. */
    public static final DeferredHolder<Ability, Ability> PHANTOM = SKILLS.register("phantom", () ->
            new Ability(unique(AbilityCategory.MOVEMENT)
                    .mode("veil", 30, 0, 20)
                    .mode("shadow_strike", 25, 10, 5)
                    .mode("mark", 5, 0, 2)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    if (mode == 0) {
                        give(player, MobEffects.INVISIBILITY, 15, 0);
                        puff(level, ParticleTypes.LARGE_SMOKE, player, 25, 0.4);
                        return true;
                    }
                    LivingEntity t = target(player, mode == 1 ? 16 : 32);
                    if (t == null) return false;
                    if (mode == 2) {
                        give(t, MobEffects.GLOWING, 20, 0);
                        return true;
                    }
                    Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1.5, 0, 1.5));
                    player.teleportTo(behind.x, t.getY(), behind.z);
                    player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
                    t.hurtServer(level, player.damageSources().playerAttack(player), 8 * power(player, this));
                    puff(level, ParticleTypes.SMOKE, t, 20, 0.3);
                    return true;
                }
            });

    /** The sky answers. */
    public static final DeferredHolder<Ability, Ability> STORMBORN = SKILLS.register("stormborn", () ->
            new Ability(unique(AbilityCategory.OFFENSE)
                    .mode("thunderbolt", 30, 0, 3)
                    .mode("chain_lightning", 80, 20, 12)
                    .mode("gale", 15, 0, 2)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    if (mode == 0) {
                        BlockHitResult hit = block(player, 32);
                        if (hit == null) return false;
                        bolt(level, hit.getLocation());
                        return true;
                    }
                    if (mode == 1) {
                        var targets = around(player, 12);
                        if (targets.isEmpty()) {
                            player.sendOverlayMessage(Component.translatable("jaycee.message.no_target"));
                            return false;
                        }
                        int n = 0;
                        for (LivingEntity t : targets) {
                            if (n++ >= 3 + Math.round(power(player, this) * 2)) break;
                            bolt(level, t.position());
                        }
                        return true;
                    }
                    for (LivingEntity t : around(player, 6)) {
                        Vec3 away = t.position().subtract(player.position()).normalize();
                        t.knockback(1.5 * power(player, this), -away.x, -away.z, player.damageSources().playerAttack(player), 1f);
                    }
                    level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 1, player.getZ(), 6, 2, 0.5, 2, 0);
                    level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1, 1);
                    return true;
                }
            });

    /** Fury made flesh. */
    public static final DeferredHolder<Ability, Ability> WRATH = SKILLS.register("wrath", () ->
            new Ability(unique(AbilityCategory.OFFENSE)
                    .mode("berserk", 0, 60, 30)
                    .mode("eruption", 60, 40, 10)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    if (mode == 0) {
                        give(player, MobEffects.STRENGTH, 20, 1);
                        give(player, MobEffects.SPEED, 20, 1);
                        give(player, MobEffects.RESISTANCE, 20, 0);
                        puff(level, ParticleTypes.ANGRY_VILLAGER, player, 12, 0.6);
                        return true;
                    }
                    level.explode(player, player.getX(), player.getY(), player.getZ(),
                            2.5f * power(player, this), Level.ExplosionInteraction.NONE);
                    return true;
                }
            });

    /** Nothing gets through. */
    public static final DeferredHolder<Ability, Ability> BASTION = SKILLS.register("bastion", () ->
            new Ability(unique(AbilityCategory.DEFENSE)
                    .mode("fortress", 30, 20, 25)
                    .mode("repel", 20, 10, 6)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    if (mode == 0) {
                        give(player, MobEffects.ABSORPTION, 20, 2 + Math.round(power(player, this)));
                        give(player, MobEffects.RESISTANCE, 10, 1);
                        puff(level, ParticleTypes.ENCHANTED_HIT, player, 30, 0.6);
                        return true;
                    }
                    for (LivingEntity t : around(player, 5)) {
                        Vec3 away = t.position().subtract(player.position()).normalize();
                        t.knockback(2.0, -away.x, -away.z, player.damageSources().playerAttack(player), 1f);
                        give(t, MobEffects.SLOWNESS, 3, 1);
                    }
                    level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1, 0.8f);
                    return true;
                }
            });

    /** Thought outruns time. */
    public static final DeferredHolder<Ability, Ability> SAGE = SKILLS.register("sage", () ->
            new Ability(unique(AbilityCategory.SUPPORT)
                    .mode("foresight", 40, 0, 20)
                    .mode("appraise", 5, 0, 1)
                    .mode("mend", 30, 0, 8)) {
                @Override
                public boolean activate(ServerPlayer player, ServerLevel level, int mode) {
                    if (mode == 0) {
                        for (LivingEntity t : around(player, 16)) give(t, MobEffects.SLOWNESS, 6, 2);
                        give(player, MobEffects.SPEED, 6, 1);
                        return true;
                    }
                    if (mode == 1) {
                        var energy = EnergyAPI.get(player);
                        player.sendSystemMessage(Component.translatable("jaycee.skill.appraise",
                                RaceAPI.race(player).map(r -> r.name()).orElse(Component.translatable("brightcore.race.none")),
                                EnergyAPI.format(energy.ep()), EnergyAPI.format(energy.aura()), EnergyAPI.format(energy.mana())));
                        return true;
                    }
                    if (player.getHealth() >= player.getMaxHealth()) {
                        player.sendOverlayMessage(Component.translatable("jaycee.message.full_health"));
                        return false;
                    }
                    player.heal(player.getMaxHealth() * 0.25f * power(player, this));
                    puff(level, ParticleTypes.HAPPY_VILLAGER, player, 15, 0.5);
                    return true;
                }
            });

    // =====================================================================================
    // EXTRA: stronger learned skills.
    // =====================================================================================

    public static final DeferredHolder<Ability, Ability> THOUGHT_ACCELERATION = SKILLS.register("thought_acceleration", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.SUPPORT).toggle().manaCost(20)) {
                @Override public void tick(ServerPlayer player, ServerLevel level) {
                    if (player.tickCount % 20 != 0) return;
                    for (LivingEntity t : around(player, 10)) give(t, MobEffects.SLOWNESS, 2, 0);
                    if (!EnergyAPI.spendMana(player, 2)) com.brightwebs.brightcore.api.AbilityAPI.setToggled(player, this, false);
                }
            });

    public static final DeferredHolder<Ability, Ability> HELLFIRE = SKILLS.register("hellfire", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.OFFENSE).manaCost(45).cooldownSeconds(6)) {
                @Override public boolean activate(ServerPlayer player, ServerLevel level) {
                    LivingEntity t = target(player, 20);
                    if (t == null) return false;
                    t.igniteForTicks(160);
                    t.hurtServer(level, player.damageSources().indirectMagic(player, player), 7 * power(player, this));
                    puff(level, ParticleTypes.SOUL_FIRE_FLAME, t, 40, 0.5);
                    return true;
                }
            });

    public static final DeferredHolder<Ability, Ability> SPATIAL_STEP = SKILLS.register("spatial_step", () ->
            new Ability(new Ability.Properties().type(SkillType.EXTRA).category(AbilityCategory.MOVEMENT).manaCost(22).cooldownSeconds(2.5f)) {
                @Override public boolean activate(ServerPlayer player, ServerLevel level) {
                    BlockHitResult hit = block(player, 24);
                    if (hit == null) return false;
                    BlockPos to = hit.getBlockPos().relative(hit.getDirection());
                    if (!level.getBlockState(to).getCollisionShape(level, to).isEmpty()
                            || !level.getBlockState(to.above()).getCollisionShape(level, to.above()).isEmpty()) {
                        player.sendOverlayMessage(Component.translatable("jaycee.message.no_room"));
                        return false;
                    }
                    puff(level, ParticleTypes.PORTAL, player, 30, 0.4);
                    player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                    return true;
                }
            });

    // =====================================================================================
    // COMMON: basic skills anyone can learn.
    // =====================================================================================

    public static final DeferredHolder<Ability, Ability> MAGIC_SENSE = SKILLS.register("magic_sense", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.SUPPORT).toggle()) {
                @Override public void tick(ServerPlayer player, ServerLevel level) { keep(player, MobEffects.NIGHT_VISION, 0); }
                @Override public void onToggle(ServerPlayer player, boolean on) { if (!on) player.removeEffect(MobEffects.NIGHT_VISION); }
            });

    public static final DeferredHolder<Ability, Ability> BODY_REINFORCEMENT = SKILLS.register("body_reinforcement", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.DEFENSE).auraCost(25).cooldownSeconds(20)) {
                @Override public boolean activate(ServerPlayer player, ServerLevel level) {
                    give(player, MobEffects.RESISTANCE, 20, 0);
                    give(player, MobEffects.STRENGTH, 20, 0);
                    return true;
                }
            });

    public static final DeferredHolder<Ability, Ability> MANA_BOLT = SKILLS.register("mana_bolt", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.OFFENSE).manaCost(10).cooldownSeconds(1)) {
                @Override public boolean activate(ServerPlayer player, ServerLevel level) {
                    LivingEntity t = target(player, 20);
                    if (t == null) return false;
                    t.hurtServer(level, player.damageSources().indirectMagic(player, player), 3 * power(player, this));
                    puff(level, ParticleTypes.WITCH, t, 15, 0.3);
                    return true;
                }
            });

    public static final DeferredHolder<Ability, Ability> MANA_DASH = SKILLS.register("mana_dash", () ->
            new Ability(new Ability.Properties().type(SkillType.COMMON).category(AbilityCategory.MOVEMENT).manaCost(12).cooldownSeconds(1.5f)) {
                @Override public boolean activate(ServerPlayer player, ServerLevel level) {
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(player.getDeltaMovement().add(look.x * 1.3, Math.max(0.2, look.y * 0.55), look.z * 1.3));
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    return true;
                }
            });

    // =====================================================================================
    // INTRINSIC: every member of a race line is born with its line's skill.
    // =====================================================================================

    private static Ability.Properties intrinsic(AbilityCategory c) {
        return new Ability.Properties().type(SkillType.INTRINSIC).category(c);
    }

    /** human: heals faster when close to death. */
    public static final DeferredHolder<Ability, Ability> TENACITY = SKILLS.register("tenacity", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (p.getHealth() < p.getMaxHealth() * 0.3f) keep(p, MobEffects.REGENERATION, 0);
                }
            });

    /** elf: sees in the dark. */
    public static final DeferredHolder<Ability, Ability> KEEN_SENSES = SKILLS.register("keen_senses", () ->
            new Ability(intrinsic(AbilityCategory.SUPPORT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { keep(p, MobEffects.NIGHT_VISION, 0); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) p.removeEffect(MobEffects.NIGHT_VISION); }
            });

    /** dwarf: digs like stone is soft. */
    public static final DeferredHolder<Ability, Ability> STONEBLOOD = SKILLS.register("stoneblood", () ->
            new Ability(intrinsic(AbilityCategory.SUPPORT).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { keep(p, MobEffects.HASTE, 0); }
            });

    /** beastfolk: runs like the wind. */
    public static final DeferredHolder<Ability, Ability> BEAST_SPRINT = SKILLS.register("beast_sprint", () ->
            new Ability(intrinsic(AbilityCategory.MOVEMENT).toggle()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { keep(p, MobEffects.SPEED, 1); }
                @Override public void onToggle(ServerPlayer p, boolean on) { if (!on) p.removeEffect(MobEffects.SPEED); }
            });

    /** goblin: finds more. */
    public static final DeferredHolder<Ability, Ability> SCAVENGER = SKILLS.register("scavenger", () ->
            new Ability(intrinsic(AbilityCategory.SUPPORT).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { keep(p, MobEffects.LUCK, 0); }
            });

    /** orc: shrugs off poison and hunger. */
    public static final DeferredHolder<Ability, Ability> IRON_GUT = SKILLS.register("iron_gut", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (p.tickCount % 10 != 0) return;
                    p.removeEffect(MobEffects.POISON);
                    p.removeEffect(MobEffects.HUNGER);
                }
            });

    /** ogre: a roar that saps enemies' strength. */
    public static final DeferredHolder<Ability, Ability> WAR_ROAR = SKILLS.register("war_roar", () ->
            new Ability(intrinsic(AbilityCategory.OFFENSE).auraCost(30).cooldownSeconds(15)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) {
                    for (LivingEntity t : around(p, 8)) give(t, MobEffects.WEAKNESS, 10, 1);
                    l.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1, 1);
                    return true;
                }
            });

    /** slime: no fall can hurt a body with no bones. */
    public static final DeferredHolder<Ability, Ability> GELATINOUS_BODY = SKILLS.register("gelatinous_body", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { p.resetFallDistance(); }
            });

    /** lizardfolk: breathes water. */
    public static final DeferredHolder<Ability, Ability> SCALED_HIDE = SKILLS.register("scaled_hide", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (p.isInWater()) keep(p, MobEffects.WATER_BREATHING, 0);
                }
            });

    /** giant: a stomp that throws everything back. */
    public static final DeferredHolder<Ability, Ability> EARTHSHAKER = SKILLS.register("earthshaker", () ->
            new Ability(intrinsic(AbilityCategory.OFFENSE).auraCost(40).cooldownSeconds(10)) {
                @Override public boolean activate(ServerPlayer p, ServerLevel l) {
                    for (LivingEntity t : around(p, 6)) {
                        Vec3 away = t.position().subtract(p.position()).normalize();
                        t.knockback(1.2, -away.x, -away.z, p.damageSources().playerAttack(p), 1f);
                        t.hurtServer(l, p.damageSources().playerAttack(p), 5 * power(p, this));
                    }
                    l.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 4, 2, 0.2, 2, 0);
                    l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 0.6f);
                    return true;
                }
            });

    /** lesser daemon: fire is home. */
    public static final DeferredHolder<Ability, Ability> INFERNAL_BLOOD = SKILLS.register("infernal_blood", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) { keep(p, MobEffects.FIRE_RESISTANCE, 0); }
            });

    /** wight: the dead don't rot twice. */
    public static final DeferredHolder<Ability, Ability> DEATHLESS_FLESH = SKILLS.register("deathless_flesh", () ->
            new Ability(intrinsic(AbilityCategory.DEFENSE).passive()) {
                @Override public void tick(ServerPlayer p, ServerLevel l) {
                    if (p.tickCount % 10 != 0) return;
                    p.removeEffect(MobEffects.WITHER);
                    p.removeEffect(MobEffects.POISON);
                }
            });

    private JayceeSkills() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(JayceeMod.ID, path);
    }

    public static void init(IEventBus modBus) {
        SKILLS.register(modBus);
        for (String u : new String[]{"glutton", "phantom", "stormborn", "wrath", "bastion", "sage"}) {
            BrightSkills.addStartingUnique(id(u));
        }
    }
}

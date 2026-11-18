package com.brightwebs.brightcore.api.race;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.brightwebs.brightcore.data.RaceData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Optional;

/** Choosing, evolving and applying races. Server side unless noted. */
public final class RaceAPI {
    private RaceAPI() {}

    // ---- reading (client too) --------------------------------------------------

    public static RaceData data(Player player) {
        return player.getData(BrightAttachments.RACE);
    }

    public static Optional<Race> race(Player player) {
        return data(player).raceId().map(RaceRegistry::get);
    }

    /** What stands between the player and an evolution, or empty if they can take it now. */
    public static Optional<Component> blocker(Player player, Race.Evolution evo) {
        Race target = RaceRegistry.get(evo.target());
        if (target == null) return Optional.of(Component.translatable("brightcore.race.unknown"));
        double ep = EnergyAPI.get(player).ep();
        if (ep < evo.epNeeded()) {
            return Optional.of(Component.translatable("brightcore.race.need_ep",
                    EnergyAPI.format(evo.epNeeded()), EnergyAPI.format(ep)));
        }
        if (!player.isCreative() && player.experienceLevel < evo.xpLevels()) {
            return Optional.of(Component.translatable("brightcore.race.need_xp", evo.xpLevels(), player.experienceLevel));
        }
        return Optional.empty();
    }

    // ---- changing ----------------------------------------------------------------

    /** First-join choice. Ignored if the player already chose. */
    public static boolean choose(ServerPlayer player, Race race) {
        // A saved race that no longer exists (an older version of the mod) counts as no race.
        if ((data(player).chosen() && race(player).isPresent()) || !race.isStarting()) return false;
        become(player, race, true);
        grantStartingUnique(player);
        return true;
    }

    /** Admin/command: force a race and roll a fresh body for it. */
    public static void set(ServerPlayer player, Race race) {
        become(player, race, true);
    }

    /** Evolves into one of the current race's listed evolutions, if every requirement is met. */
    public static boolean evolve(ServerPlayer player, Identifier targetId) {
        Optional<Race> current = race(player);
        if (current.isEmpty()) return false;
        for (Race.Evolution evo : current.get().evolutions()) {
            if (!evo.target().equals(targetId)) continue;
            Optional<Component> why = blocker(player, evo);
            if (why.isPresent()) {
                player.sendOverlayMessage(why.get());
                return false;
            }
            Race target = RaceRegistry.get(targetId);
            if (NeoForge.EVENT_BUS.post(new RaceEvent.Evolve(player, current.get(), target)).isCanceled()) return false;
            if (!player.isCreative()) player.giveExperienceLevels(-evo.xpLevels());
            become(player, target, false);
            celebrate(player, target);
            return true;
        }
        return false;
    }

    private static void become(ServerPlayer player, Race race, boolean freshBody) {
        player.setData(BrightAttachments.RACE, new RaceData(race.id().toString(), true));
        if (freshBody) EnergyAPI.rollFor(player, race);
        else EnergyAPI.raiseFor(player, race);
        applyBody(player, race);
        player.setHealth(player.getMaxHealth());
        for (Identifier skill : race.intrinsics()) {
            Ability a = AbilityRegistry.get(skill);
            if (a != null && AbilityAPI.learn(player, a)) {
                player.sendSystemMessage(Component.translatable("brightcore.message.acquired", a.name()));
            }
        }
    }

    /** Reincarnation's gift: one random Unique skill from the pool content mods fill. */
    private static void grantStartingUnique(ServerPlayer player) {
        List<Identifier> pool = BrightSkills.startingUniques();
        if (pool.isEmpty()) return;
        Ability a = AbilityRegistry.get(pool.get(player.getRandom().nextInt(pool.size())));
        if (a != null && AbilityAPI.learn(player, a)) {
            player.sendSystemMessage(Component.translatable("brightcore.message.unique_born", a.name()));
        }
    }

    // ---- the body ----------------------------------------------------------------

    private static final Identifier HEALTH = BrightCore.id("race_health");
    private static final Identifier ATTACK = BrightCore.id("race_attack");
    private static final Identifier ATTACK_SPEED = BrightCore.id("race_attack_speed");
    private static final Identifier SPEED = BrightCore.id("race_speed");
    private static final Identifier KNOCKBACK = BrightCore.id("race_knockback");
    private static final Identifier SIZE = BrightCore.id("race_size");

    /** Sets the player's body stats to the race's, as modifiers on top of vanilla's defaults. */
    public static void applyBody(Player player, Race race) {
        modify(player, Attributes.MAX_HEALTH, HEALTH, race.health() - 20);
        modify(player, Attributes.ATTACK_DAMAGE, ATTACK, race.attack() - 1);
        modify(player, Attributes.ATTACK_SPEED, ATTACK_SPEED, race.attackSpeed() - 4);
        modify(player, Attributes.MOVEMENT_SPEED, SPEED, race.speed() - 0.1);
        modify(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, race.knockbackResistance());
        modify(player, Attributes.SCALE, SIZE, race.size() - 1);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void modify(Player player, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void celebrate(ServerPlayer player, Race race) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(),
                80, 0.6, 1.0, 0.6, 0.35);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(),
                40, 0.4, 1.2, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.sendSystemMessage(Component.translatable("brightcore.race.evolved", race.name()));
    }

    /** All races in a line, for menus: the forms this race can become next. */
    public static List<Race.Evolution> next(Player player) {
        return race(player).map(Race::evolutions).orElse(List.of());
    }
}

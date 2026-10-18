package com.brightwebs.jaycee.gameplay;

import com.brightwebs.jaycee.gameplay.divine.DivinityService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Server-authoritative, intangible Essence rewards and additive growth. */
public final class EssenceGrowth {
    private EssenceGrowth() {}
    private static final ResourceLocation HEALTH_BOOST = ResourceLocation.fromNamespaceAndPath("jaycee", "growth_health");
    private static final ResourceLocation POWER_BOOST = ResourceLocation.fromNamespaceAndPath("jaycee", "growth_strength");
    static void register() { NeoForge.EVENT_BUS.addListener(EssenceGrowth::onDeath); }

    /** Provisional vanilla balance; modded creatures use a conservative fallback. */
    static int value(LivingEntity mob) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return switch (id) {
            case "minecraft:chicken", "minecraft:rabbit" -> 2;
            case "minecraft:cow", "minecraft:sheep", "minecraft:pig" -> 4;
            case "minecraft:zombie", "minecraft:skeleton" -> 12;
            case "minecraft:spider" -> 14;
            case "minecraft:creeper" -> 18;
            case "minecraft:enderman" -> 35;
            case "minecraft:blaze" -> 40;
            case "minecraft:witch" -> 45;
            case "minecraft:wither_skeleton" -> 55;
            case "minecraft:ravager" -> 120;
            case "minecraft:elder_guardian" -> 300;
            case "minecraft:warden" -> 500;
            case "minecraft:wither" -> 1500;
            case "minecraft:ender_dragon" -> 3000;
            default -> mob instanceof Enemy ? 12 : 3;
        };
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getEntity() instanceof net.minecraft.world.entity.player.Player
                || player.isSpectator() || !player.isAlive()
                || !PlayerState.get(player).unlocked()) return;
        int reward = value(event.getEntity());
        var data = PlayerState.read(player);
        // 60-second budget persisted in the player's tag. Do not pay twice for one death.
        long tick = player.server.getTickCount();
        long start = data.getLong("EssenceHuntWindow");
        int used = Math.max(0, data.getInt("EssenceHuntUsed"));
        if (tick < start || tick - start >= 1200) { start = tick; used = 0; }
        int amount = Math.min(reward, Math.max(0, 600 - used));
        data.putLong("EssenceHuntWindow", start);
        data.putInt("EssenceHuntUsed", used + amount);
        PlayerState.save(player, data);
        if (amount <= 0) return;
        long credited = DivinityService.creditEssence(player, amount);
        if (credited > 0) {
            var progress = DivinityService.model(player).progression(player.getUUID());
            player.displayClientMessage(Component.literal("+" + credited + " Essence  |  Total: "
                + DivinityService.model(player).profile(player.getUUID()).essence()
                + "  |  Lv " + progress.level() + ": " + progress.intoLevel()
                + "/" + progress.nextCost()), true);
            // CreditEssence synchronizes the actual ledger. Apply level resource growth immediately too.
            grow(player);
            GameplayNetwork.sync(player);
        }
    }

    /** First patron-specific skill-tree pass: server-owned temporary divine blessings.
     * All effects require the Blessed lineage AND at least one invested Divine Affinity rank.
     * These are small passive manifestations, not automatic ownership of god-exclusive spells. */
    static void applyPatronAffinity(ServerPlayer player) {
        var state = PlayerState.get(player);
        if (!state.unlocked()) return;
        int rank = Math.clamp(PlayerState.read(player).getInt("GrowthRank_patron"), 0, 100);
        if (rank < 1) return;
        var lineage = DivinityService.model(player).profile(player.getUUID()).npcPatrons();
        if (lineage.isEmpty()) return;
        String patron = lineage.stream().sorted().findFirst().orElse("");
        var effect = switch (patron) {
            case "zeus", "hermes" -> net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED;
            case "poseidon" -> net.minecraft.world.effect.MobEffects.WATER_BREATHING;
            case "hades", "hephaestus" -> net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE;
            case "athena", "hera" -> net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE;
            case "ares" -> net.minecraft.world.effect.MobEffects.DAMAGE_BOOST;
            case "apollo" -> net.minecraft.world.effect.MobEffects.NIGHT_VISION;
            case "artemis" -> net.minecraft.world.effect.MobEffects.JUMP;
            case "aphrodite", "hestia" -> net.minecraft.world.effect.MobEffects.REGENERATION;
            case "demeter" -> net.minecraft.world.effect.MobEffects.SATURATION;
            case "dionysus" -> net.minecraft.world.effect.MobEffects.LUCK;
            default -> null;
        };
        if (effect == null) return;
        // Cap amplifier to keep early blessings meaningful without becoming invulnerability.
        int amplifier = rank >= 12 ? 1 : 0;
        var active = player.getEffect(effect);
        if (active == null || active.getDuration() < 30 || active.getAmplifier() < amplifier)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 65, amplifier, true, false, true));
    }

    /** Called once per second. Apply only NEW level deltas, preserving configured pools. */
    static void grow(ServerPlayer player) {
        applyAttributes(player);
        applyPatronAffinity(player);
        var state = PlayerState.get(player);
        if (!state.unlocked()) return;
        int level = Math.min(10000, DivinityService.model(player).progression(player.getUUID()).level());
        CompoundTag data = PlayerState.read(player);
        int old = data.contains("EssenceGrowthLevel") ? data.getInt("EssenceGrowthLevel") : 1;
        if (level <= old) return;
        int delta = level - old;
        double soulBoost = Math.min(1_000_000 - state.maxSoul(), delta * 10.0);
        double manaBoost = Math.min(1_000_000 - state.maxMana(), delta * 5.0);
        data.putDouble("MaxSoul", state.maxSoul() + soulBoost);
        data.putDouble("Soul", Math.min(state.maxSoul() + soulBoost, state.soul() + soulBoost));
        data.putDouble("MaxMana", state.maxMana() + manaBoost);
        data.putDouble("Mana", Math.min(state.maxMana() + manaBoost, state.mana() + manaBoost));
        data.putInt("EssenceGrowthLevel", level);
        // Unspent points are recomputed from earned levels and purchased ranks.
        PlayerState.save(player, data);
        player.sendSystemMessage(Component.literal("Divine level " + level + "! Mana +" + (int)manaBoost
            + ", Spiritual Health +" + (int)soulBoost + ". Skill points available: " + points(player)));
    }

    static void applyAttributes(ServerPlayer player) {
        var data = PlayerState.read(player);
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double bonus = Math.min(200, Math.max(0, data.getInt("GrowthRank_health")) * 2.0);
            var old = health.getModifier(HEALTH_BOOST);
            if ((old == null && bonus > 0) || (old != null && old.amount() != bonus)) {
                health.removeModifier(HEALTH_BOOST);
                if (bonus > 0) health.addTransientModifier(new AttributeModifier(HEALTH_BOOST, bonus, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        var damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            double bonus = Math.min(25, Math.max(0, data.getInt("GrowthRank_strength")) * 0.5);
            var old = damage.getModifier(POWER_BOOST);
            if ((old == null && bonus > 0) || (old != null && old.amount() != bonus)) {
                damage.removeModifier(POWER_BOOST);
                if (bonus > 0) damage.addTransientModifier(new AttributeModifier(POWER_BOOST, bonus, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    static int points(ServerPlayer player) {
        var data = PlayerState.read(player);
        int earned = Math.max(0, DivinityService.model(player).progression(player.getUUID()).level() - 1);
        return Math.max(0, earned - Math.max(0, data.getInt("EssenceSpentPoints")));
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> commands() {
        var root = Commands.literal("growth").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            c.getSource().sendSuccess(() -> Component.literal("Growth points: " + points(p)
                + ". /jc growth invest soul|mana|regeneration <points>"), false);
            return 1;
        });
        for (String branch : java.util.List.of("health", "soul", "mana", "regeneration", "resistance", "strength", "patron"))
            root.then(Commands.literal("invest").then(Commands.literal(branch)
                .then(Commands.argument("points", IntegerArgumentType.integer(1, 100))
                    .executes(c -> invest(c.getSource().getPlayerOrException(), branch,
                        IntegerArgumentType.getInteger(c, "points"))))));
        return root;
    }

    static int invest(ServerPlayer player, String branch, int requested) {
        if (requested < 1 || requested > 100) return 0;
        if (!java.util.Set.of("health", "soul", "mana", "regeneration", "resistance", "strength", "patron").contains(branch)) return 0;
        var profile = DivinityService.model(player).profile(player.getUUID());
        if ("patron".equals(branch) && profile.npcPatrons().isEmpty()) return 0;
        if (!PlayerState.get(player).unlocked() || requested > points(player)) {
            player.sendSystemMessage(Component.literal("Growth investment rejected: insufficient earned skill points or no awakening."));
            return 0;
        }
        var data = PlayerState.read(player);
        int spent = Math.max(0, data.getInt("EssenceSpentPoints"));
        if (spent > Integer.MAX_VALUE - requested) return 0;
        var state = PlayerState.get(player);
        int oldRank = Math.max(0, data.getInt("GrowthRank_" + branch));
        if (oldRank > 100 - requested) return 0;
        switch (branch) {
            case "health", "strength" -> { /* Vanilla attribute modifiers are refreshed server-side. */ }
            case "soul" -> {
                double gain = Math.min(1_000_000 - state.maxSoul(), requested * 20.0);
                if (gain <= 0) return 0;
                data.putDouble("MaxSoul", state.maxSoul() + gain);
                data.putDouble("Soul", state.soul() + gain);
            }
            case "mana" -> {
                double gain = Math.min(1_000_000 - state.maxMana(), requested * 15.0);
                if (gain <= 0) return 0;
                data.putDouble("MaxMana", state.maxMana() + gain);
                data.putDouble("Mana", state.mana() + gain);
            }
            case "regeneration" -> data.putDouble("ManaRegen", Math.min(10000, state.manaRegen() + requested * 0.25));
            case "resistance" -> data.putDouble("SoulResistance", Math.min(0.75, state.soulResistance() + requested * 0.025));
            case "patron" -> data.putDouble("ManaRegen", Math.min(10000, state.manaRegen() + requested * 0.15));
            default -> { return 0; }
        }
        data.putInt("GrowthRank_" + branch, oldRank + requested);
        data.putInt("EssenceSpentPoints", spent + requested);
        PlayerState.save(player, data);
        applyAttributes(player);
        if (branch.equals("health")) player.heal(requested * 2.0f);
        GameplayNetwork.sync(player);
        player.sendSystemMessage(Component.literal("Invested " + requested + " point(s) in " + branch + ". Remaining: " + points(player)));
        return 1;
    }
}

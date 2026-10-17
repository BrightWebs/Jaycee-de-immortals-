package com.brightwebs.jaycee.gameplay;

import com.brightwebs.jaycee.gameplay.divine.DivinityService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
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
                || player.isCreative() || player.isSpectator() || !player.isAlive()
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
        if (credited > 0)
            player.displayClientMessage(Component.literal("+" + credited + " Essence"), true);
    }

    /** Called once per second. Apply only NEW level deltas, preserving configured pools. */
    static void grow(ServerPlayer player) {
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
        for (String branch : java.util.List.of("soul", "mana", "regeneration"))
            root.then(Commands.literal("invest").then(Commands.literal(branch)
                .then(Commands.argument("points", IntegerArgumentType.integer(1, 100))
                    .executes(c -> invest(c.getSource().getPlayerOrException(), branch,
                        IntegerArgumentType.getInteger(c, "points"))))));
        return root;
    }

    private static int invest(ServerPlayer player, String branch, int requested) {
        if (!PlayerState.get(player).unlocked() || requested > points(player)) {
            player.sendSystemMessage(Component.literal("Growth investment rejected: insufficient earned skill points or no awakening."));
            return 0;
        }
        var data = PlayerState.read(player);
        int spent = Math.max(0, data.getInt("EssenceSpentPoints"));
        if (spent > Integer.MAX_VALUE - requested) return 0;
        var state = PlayerState.get(player);
        switch (branch) {
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
            default -> { return 0; }
        }
        data.putInt("EssenceSpentPoints", spent + requested);
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
        player.sendSystemMessage(Component.literal("Invested " + requested + " point(s) in " + branch + ". Remaining: " + points(player)));
        return 1;
    }
}

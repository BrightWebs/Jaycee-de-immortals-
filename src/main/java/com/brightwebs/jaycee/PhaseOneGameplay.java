package com.brightwebs.jaycee;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Phase 1 server-authoritative spell vertical slice. */
public final class PhaseOneGameplay {
    private static final String ROOT = "JayceePhaseOne";
    private static final String PROGRESS = "EarnedEssence";
    private static final String BALANCE = "TransferableEssence";
    private static final String MANA = "Mana";
    private static final int MAX_MANA = 100;
    private PhaseOneGameplay() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PhaseOneGameplay::registerCommands);
        NeoForge.EVENT_BUS.addListener(PhaseOneGameplay::tick);
    }

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(ROOT)) {
            CompoundTag first = new CompoundTag();
            first.putInt(MANA, MAX_MANA);
            persistent.put(ROOT, first);
        }
        return persistent.getCompound(ROOT);
    }

    public static void earn(ServerPlayer p, long amount) {
        if (amount <= 0) return;
        CompoundTag d = data(p);
        d.putLong(BALANCE, saturatingAdd(d.getLong(BALANCE), amount));
        d.putLong(PROGRESS, saturatingAdd(d.getLong(PROGRESS), amount));
    }

    private static long saturatingAdd(long a, long b) {
        return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b;
    }

    private static int level(long total) {
        int level = 1;
        long rest = Math.max(0, total);
        while (level < 10000) {
            long required = 100L * (level + 1) * (level + 1);
            if (rest < required) break;
            rest -= required;
            level++;
        }
        return level;
    }

    private static String grade(int level) {
        if (level >= 111) return "SSS";
        if (level >= 96) return "SS";
        if (level >= 81) return "S";
        if (level >= 66) return "A";
        if (level >= 51) return "B";
        if (level >= 36) return "C";
        if (level >= 21) return "D";
        if (level >= 11) return "E";
        return "F";
    }

    private static void registerCommands(RegisterCommandsEvent e) {
        var root = Commands.literal("jc")
            .then(Commands.literal("status").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                CompoundTag d = data(p);
                int lv = level(d.getLong(PROGRESS));
                c.getSource().sendSuccess(() -> Component.literal("Divine Lv." + lv + " [" + grade(lv) + "]  Essence " + d.getLong(BALANCE) + "  Mana " + d.getInt(MANA) + "/" + MAX_MANA), false);
                return 1;
            }))
            .then(Commands.literal("essence").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                c.getSource().sendSuccess(() -> Component.literal("Transferable essence: " + data(p).getLong(BALANCE)), false);
                return 1;
            }))
            .then(Commands.literal("grade").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                int lv = level(data(p).getLong(PROGRESS));
                c.getSource().sendSuccess(() -> Component.literal("Divine grade: " + grade(lv) + " (level " + lv + ")"), false);
                return 1;
            }))
            .then(Commands.literal("cast")
                .then(Commands.literal("lightning").executes(c -> cast(c.getSource().getPlayerOrException(), "lightning")))
                .then(Commands.literal("heal").executes(c -> cast(c.getSource().getPlayerOrException(), "heal")))
                .then(Commands.literal("dash").executes(c -> cast(c.getSource().getPlayerOrException(), "dash")))
                .then(Commands.literal("blink").executes(c -> cast(c.getSource().getPlayerOrException(), "blink")))
                .then(Commands.literal("ward").executes(c -> cast(c.getSource().getPlayerOrException(), "ward"))))
            .then(Commands.literal("edit").requires(s -> s.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.literal("essence")
                        .then(Commands.literal("add")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000000))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "target");
                                    int amount = IntegerArgumentType.getInteger(c, "amount");
                                    earn(p, amount);
                                    c.getSource().sendSuccess(() -> Component.literal("Granted " + amount + " essence to " + p.getName().getString()), true);
                                    return 1;
                                }))))));
        e.getDispatcher().register(root);
        e.getDispatcher().register(Commands.literal("jaycee").redirect(e.getDispatcher().getRoot().getChild("jc")));
    }

    private static int cast(ServerPlayer p, String kind) {
        CompoundTag d = data(p);
        if (!JayceeMod.unlocked(p)) {
            p.sendSystemMessage(Component.literal("Absorb three Jaycee essences before using divine abilities."));
            return 0;
        }
        int cost = switch (kind) {
            case "lightning" -> 30;
            case "blink" -> 22;
            case "ward" -> 18;
            case "heal" -> 20;
            default -> 12;
        };
        int cooldown = switch (kind) {
            case "lightning" -> 60;
            case "blink" -> 50;
            case "heal" -> 80;
            default -> 30;
        };
        long now = p.serverLevel().getGameTime();
        String key = "Cooldown_" + kind;
        if (now < d.getLong(key)) {
            p.sendSystemMessage(Component.literal("Cooldown: " + ((d.getLong(key) - now + 19) / 20) + "s"));
            return 0;
        }
        if (d.getInt(MANA) < cost) {
            p.sendSystemMessage(Component.literal("Not enough mana (requires " + cost + ")."));
            return 0;
        }
        HitResult hit = (kind.equals("lightning") || kind.equals("blink")) ? p.pick(24.0, 0.0F, false) : null;
        if (hit != null && hit.getType() != HitResult.Type.BLOCK) {
            p.sendSystemMessage(Component.literal("Look at a block within 24 blocks."));
            return 0;
        }
        if (kind.equals("blink")) {
            BlockPos target = ((BlockHitResult) hit).getBlockPos().relative(((BlockHitResult) hit).getDirection());
            if (!p.serverLevel().hasChunkAt(target) ||
                !p.serverLevel().getBlockState(target).getCollisionShape(p.serverLevel(), target).isEmpty()) {
                p.sendSystemMessage(Component.literal("Target destination must be an empty loaded block."));
                return 0;
            }
        }
        d.putInt(MANA, d.getInt(MANA) - cost);
        d.putLong(key, now + cooldown);
        switch (kind) {
            case "lightning" -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(p.serverLevel());
                if (bolt != null) {
                    Vec3 point = hit.getLocation();
                    bolt.moveTo(point.x, point.y, point.z);
                    p.serverLevel().addFreshEntity(bolt);
                }
            }
            case "heal" -> p.heal(8.0F);
            case "dash" -> {
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(p.getDeltaMovement().add(look.x * 1.3, Math.max(0.20, look.y * 0.55), look.z * 1.3));
                p.hurtMarked = true;
            }
            case "blink" -> {
                BlockPos target = ((BlockHitResult) hit).getBlockPos().relative(((BlockHitResult) hit).getDirection());
                p.teleportTo(target.getX() + .5, target.getY(), target.getZ() + .5);
            }
            case "ward" -> p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 15, 1));
            default -> { return 0; }
        }
        p.displayClientMessage(Component.literal("Cast " + kind + "  (-" + cost + " mana)"), true);
        return 1;
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0) return;
        CompoundTag d = data(p);
        d.putInt(MANA, Math.min(MAX_MANA, d.getInt(MANA) + 3));
    }
}

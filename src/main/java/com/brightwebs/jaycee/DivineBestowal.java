package com.brightwebs.jaycee;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * First playable essence bestowal backend. M/N UI is a future frontend to this
 * server-controlled transaction path. Gifts do not create subordinate bonds.
 */
public final class DivineBestowal {
    private static final int MAX_RANGE_SQUARED = 12 * 12;
    private static final int OFFER_LIFETIME_TICKS = 20 * 60;
    private static final String MOB_ESSENCE = "JayceeReceivedEssence";
    private static final String DIVINE_NAME = "JayceeDivineName";
    private static final Map<UUID, Pending> offers = new HashMap<>();

    private record Pending(UUID sender, UUID recipient, int amount, String name, long expiresAt) {}
    private DivineBestowal() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DivineBestowal::commands);
    }

    private static void commands(RegisterCommandsEvent e) {
        var bestow = Commands.literal("bestow")
            .then(Commands.argument("target", EntityArgument.entity())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000_000))
                    .executes(c -> begin(c.getSource().getPlayerOrException(),
                        EntityArgument.getEntity(c, "target"),
                        IntegerArgumentType.getInteger(c, "amount"), null))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(c -> begin(c.getSource().getPlayerOrException(),
                            EntityArgument.getEntity(c, "target"),
                            IntegerArgumentType.getInteger(c, "amount"),
                            StringArgumentType.getString(c, "name"))))));
        // Independent root also permits /jc bestow through redirect when registered
        e.getDispatcher().register(Commands.literal("jc")
            .then(bestow)
            .then(Commands.literal("offer")
                .then(Commands.literal("accept").executes(c -> answer(c.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("decline").executes(c -> answer(c.getSource().getPlayerOrException(), false)))));
    }

    private static int begin(ServerPlayer giver, Entity recipient, int amount, String proposed) {
        if (!JayceeMod.unlocked(giver)) {
            giver.sendSystemMessage(Component.literal("Divine essence access has not been unlocked."));
            return 0;
        }
        if (!(recipient instanceof LivingEntity) || recipient == giver || recipient.isRemoved()
                || giver.level() != recipient.level() || giver.distanceToSqr(recipient) > MAX_RANGE_SQUARED) {
            giver.sendSystemMessage(Component.literal("Select another living player or mob within 12 blocks."));
            return 0;
        }
        if (PhaseOneGameplay.essenceBalance(giver) < amount) {
            giver.sendSystemMessage(Component.literal("Not enough transferable essence."));
            return 0;
        }
        String name = proposed == null ? null : proposed.trim();
        if (name != null && (name.isEmpty() || name.length() > 40 || name.chars().anyMatch(Character::isISOControl))) {
            giver.sendSystemMessage(Component.literal("Divine name must be 1-40 printable characters."));
            return 0;
        }
        if (recipient instanceof ServerPlayer other) {
            long expires = giver.serverLevel().getGameTime() + OFFER_LIFETIME_TICKS;
            offers.put(other.getUUID(), new Pending(giver.getUUID(), other.getUUID(), amount, name, expires));
            other.sendSystemMessage(Component.literal(giver.getName().getString() + " offers " + amount
                + " essence" + (name == null ? "" : " and divine name \"" + name + "\"")
                + ". Enter /jc offer accept or /jc offer decline within 60 seconds."));
            giver.sendSystemMessage(Component.literal("Offer sent to " + other.getName().getString()
                + "; no essence has been deducted."));
            return 1;
        }
        // Direct gifts to eligible mobs are immediate and persistent.
        if (!PhaseOneGameplay.spendEssence(giver, amount)) return 0;
        var data = recipient.getPersistentData();
        long before = Math.max(0, data.getLong(MOB_ESSENCE));
        data.putLong(MOB_ESSENCE, PhaseOneGameplay.safeAdd(before, amount));
        if (name != null) {
            data.putString(DIVINE_NAME, name);
            recipient.setCustomName(Component.literal(name));
            recipient.setCustomNameVisible(true);
        }
        giver.sendSystemMessage(Component.literal("Bestowed " + amount + " essence upon "
            + recipient.getName().getString() + ". This essence now belongs to the creature."));
        return 1;
    }

    private static int answer(ServerPlayer recipient, boolean accept) {
        Pending pending = offers.remove(recipient.getUUID());
        if (pending == null) {
            recipient.sendSystemMessage(Component.literal("No pending divine essence offer."));
            return 0;
        }
        ServerPlayer giver = recipient.server.getPlayerList().getPlayer(pending.sender());
        if (!accept) {
            recipient.sendSystemMessage(Component.literal("Divine essence offer declined."));
            if (giver != null) giver.sendSystemMessage(Component.literal(recipient.getName().getString() + " declined the offer."));
            return 1;
        }
        if (giver == null || giver.level() != recipient.level()
                || giver.distanceToSqr(recipient) > MAX_RANGE_SQUARED
                || recipient.serverLevel().getGameTime() > pending.expiresAt()
                || !PhaseOneGameplay.spendEssence(giver, pending.amount())) {
            recipient.sendSystemMessage(Component.literal("Essence offer expired, giver is out of range, or balance is insufficient."));
            return 0;
        }
        PhaseOneGameplay.creditEssence(recipient, pending.amount());
        if (pending.name() != null) recipient.getPersistentData().putString(DIVINE_NAME, pending.name());
        recipient.sendSystemMessage(Component.literal("Received " + pending.amount() + " essence from "
            + giver.getName().getString() + "."));
        giver.sendSystemMessage(Component.literal("Bestowal completed. " + pending.amount()
            + " essence is now owned by " + recipient.getName().getString() + "."));
        return 1;
    }
}

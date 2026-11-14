package com.brightwebs.jaycee.ichor;

import com.brightwebs.jaycee.JayceeMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /ichor for testing: get, set, add, pray (stand-in until the temple exists), reroll (draws a new Birthright).
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class IchorCommand {
    private IchorCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ichor")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            IchorData d = IchorAPI.get(p);
                            c.getSource().sendSuccess(() -> Component.translatable("jaycee.command.ichor.get", p.getDisplayName(),
                                    String.format("%,d", d.ichor()), d.grade().label(), d.birthright().label()), false);
                            return d.ichor();
                        })))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, Grade.MAX_ICHOR))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    IchorAPI.set(p, IntegerArgumentType.getInteger(c, "amount"));
                                    return confirm(c.getSource(), p);
                                }))))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-Grade.MAX_ICHOR, Grade.MAX_ICHOR))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    IchorAPI.add(p, IntegerArgumentType.getInteger(c, "amount"));
                                    return confirm(c.getSource(), p);
                                }))))
                .then(Commands.literal("pray").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            IchorAPI.pray(EntityArgument.getPlayer(c, "player"));
                            return 1;
                        })))
                .then(Commands.literal("reroll").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            IchorAPI.drawBirthright(p);
                            return confirm(c.getSource(), p);
                        }))));
    }

    private static int confirm(net.minecraft.commands.CommandSourceStack source, ServerPlayer p) {
        IchorData d = IchorAPI.get(p);
        source.sendSuccess(() -> Component.translatable("jaycee.command.ichor.get", p.getDisplayName(),
                String.format("%,d", d.ichor()), d.grade().label(), d.birthright().label()), true);
        return 1;
    }
}

package com.brightwebs.brightcore.command;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.data.RaceData;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /race get | set | evolve | reset and /ep get | add | set, for testing
@EventBusSubscriber(modid = BrightCore.ID)
public final class RaceCommand {
    private static final DynamicCommandExceptionType UNKNOWN =
            new DynamicCommandExceptionType(id -> Component.translatable("brightcore.race.unknown_id", id));

    private RaceCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(Commands.literal(BrightCore.commandRoot()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("race")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            Component name = RaceAPI.race(p).map(Race::name).orElse(Component.translatable("brightcore.race.none"));
                            c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.race.get", p.getDisplayName(), name), false);
                            return 1;
                        })))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("race", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(RaceRegistry.REGISTRY.keySet(), b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Identifier id = IdentifierArgument.getId(c, "race");
                                    Race race = RaceRegistry.get(id);
                                    if (race == null) throw UNKNOWN.create(id);
                                    RaceAPI.set(p, race);
                                    c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.race.set", p.getDisplayName(), race.name()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("evolve").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("race", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(RaceRegistry.REGISTRY.keySet(), b))
                                .executes(c -> RaceAPI.evolve(EntityArgument.getPlayer(c, "player"), IdentifierArgument.getId(c, "race")) ? 1 : 0))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            // clears the race so the race menu opens again
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            p.setData(BrightAttachments.RACE, RaceData.EMPTY);
                            p.setData(BrightAttachments.ENERGY, EnergyData.EMPTY);
                            c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.race.reset", p.getDisplayName()), true);
                            return 1;
                        })))));

        d.register(Commands.literal(BrightCore.commandRoot()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("ep")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            EnergyData e = EnergyAPI.get(p);
                            c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.ep.get", p.getDisplayName(),
                                    EnergyAPI.format(e.ep()), EnergyAPI.format(e.maxAura()), EnergyAPI.format(e.maxMana())), false);
                            return (int) Math.min(Integer.MAX_VALUE, e.ep());
                        })))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, EnergyData.MAX_POOL))
                                .executes(c -> {
                                    // split like a kill, 80% aura 20% mana
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    double amount = DoubleArgumentType.getDouble(c, "amount");
                                    EnergyData e = EnergyAPI.get(p);
                                    EnergyAPI.set(p, e.withMax(e.maxAura() + amount * 0.8, e.maxMana() + amount * 0.2, e.maxSpirit()).refilled());
                                    c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.ep.set", p.getDisplayName(),
                                            EnergyAPI.format(EnergyAPI.get(p).ep())), true);
                                    return 1;
                                }))))));
    }
}

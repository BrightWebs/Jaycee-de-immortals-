package com.brightwebs.brightcore.command;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.data.AbilityData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
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

/** /ability learn | learnall | forget | slot | list. For testing and admins. */
@EventBusSubscriber(modid = BrightCore.ID)
public final class AbilityCommand {
    private static final DynamicCommandExceptionType UNKNOWN =
            new DynamicCommandExceptionType(id -> Component.translatable("brightcore.command.unknown", id));

    private AbilityCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal(BrightCore.commandRoot()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("ability")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("learn").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("ability", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(AbilityRegistry.REGISTRY.keySet(), b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Ability a = ability(c);
                                    boolean ok = AbilityAPI.learn(p, a);
                                    c.getSource().sendSuccess(() -> Component.translatable(
                                            ok ? "brightcore.command.learned" : "brightcore.command.already", p.getDisplayName(), a.name()), true);
                                    return ok ? 1 : 0;
                                }))))
                .then(Commands.literal("learnall").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            int n = 0;
                            for (Ability a : AbilityRegistry.REGISTRY) if (AbilityAPI.learn(p, a)) n++;
                            int learned = n;
                            c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.learned_all", p.getDisplayName(), learned), true);
                            return learned;
                        })))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("ability", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(AbilityRegistry.REGISTRY.keySet(), b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Ability a = ability(c);
                                    boolean ok = AbilityAPI.forget(p, a);
                                    c.getSource().sendSuccess(() -> Component.translatable("brightcore.command.forgot", p.getDisplayName(), a.name()), true);
                                    return ok ? 1 : 0;
                                }))))
                .then(Commands.literal("slot").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, AbilityData.SLOTS_PER_PAGE))
                                .then(Commands.argument("ability", IdentifierArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(AbilityRegistry.REGISTRY.keySet(), b))
                                        .executes(c -> {
                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                            int slot = IntegerArgumentType.getInteger(c, "slot");
                                            int index = AbilityAPI.get(p).page() * AbilityData.SLOTS_PER_PAGE + slot - 1;
                                            boolean ok = AbilityAPI.assign(p, index, ability(c));
                                            c.getSource().sendSuccess(() -> Component.translatable(
                                                    ok ? "brightcore.command.slot_set" : "brightcore.command.slot_failed", slot), true);
                                            return ok ? 1 : 0;
                                        })))))
                .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            AbilityData data = AbilityAPI.get(p);
                            c.getSource().sendSuccess(() -> Component.literal(p.getName().getString()
                                    + ": page " + (data.page() + 1)), false);
                            for (Identifier id : data.owned()) {
                                c.getSource().sendSuccess(() -> Component.literal(" - " + id), false);
                            }
                            return data.owned().size();
                        })))
                ));
    }

    private static Ability ability(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(c, "ability");
        Ability a = AbilityRegistry.get(id);
        if (a == null) throw UNKNOWN.create(id);
        return a;
    }
}

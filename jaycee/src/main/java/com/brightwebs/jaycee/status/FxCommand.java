package com.brightwebs.jaycee.status;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.fx.FxRecipes;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /jaycee fx <ability>: plays any ability's particle recipe where you're looking, to preview it. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class FxCommand {
    private FxCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(BrightCore.commandRoot())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("fx").then(Commands.argument("ability", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(FxRecipes.ids(), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(c, "ability");
                            var r = FxRecipes.get(id);
                            if (r == null) {
                                c.getSource().sendFailure(Component.translatable("jaycee.command.fx.unknown", id));
                                return 0;
                            }
                            FxRecipes.play((ServerLevel) p.level(), id, p, p.getEyePosition().add(p.getLookAngle().scale(6)));
                            c.getSource().sendSuccess(() -> Component.translatable("jaycee.command.fx.played", id, r.primary(), r.accent(), r.shape()), false);
                            return 1;
                        }))));
    }
}

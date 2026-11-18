package com.brightwebs.jaycee.status;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Status-screen extras: the player's Title, their Souls (taken from soul-bearing beings), and
 * their Rank, which is read from EP.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class StatusData {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    public static final Supplier<AttachmentType<String>> TITLE = ATTACHMENTS.register("title",
            () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("title"))
                    .copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final Supplier<AttachmentType<Integer>> SOULS = ATTACHMENTS.register("souls",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("souls"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_INT)
                    .build());

    private StatusData() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static String title(Player p) { return p.getData(TITLE); }
    public static int souls(Player p) { return p.getData(SOULS); }

    /** EP ranks, weakest first. */
    private static final double[] RANK_EP = {0, 1_000, 5_000, 20_000, 60_000, 150_000, 400_000, 1_000_000, 5_000_000};
    private static final String[] RANKS = {"F", "E", "D", "C", "B", "A", "Special A", "S", "Special S"};

    public static String rank(double ep) {
        int r = 0;
        for (int i = 0; i < RANK_EP.length; i++) if (ep >= RANK_EP[i]) r = i;
        return RANKS[r];
    }

    private static final java.util.Set<String> SOULFUL = java.util.Set.of(
            "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner",
            "witch", "piglin", "piglin_brute");

    /** Beings with a soul worth taking. */
    public static boolean hasSoul(LivingEntity e) {
        if (e instanceof Player) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return SOULFUL.contains(id);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()
                && hasSoul(event.getEntity())) {
            killer.setData(SOULS, souls(killer) + 1);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(BrightCore.commandRoot())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("title")
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("title", StringArgumentType.greedyString()).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    String t = StringArgumentType.getString(c, "title");
                                    p.setData(TITLE, t);
                                    c.getSource().sendSuccess(() -> Component.translatable("jaycee.command.title.set", p.getDisplayName(), t), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            p.setData(TITLE, "");
                            c.getSource().sendSuccess(() -> Component.translatable("jaycee.command.title.clear", p.getDisplayName()), true);
                            return 1;
                        }))))
                .then(Commands.literal("souls")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer()).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    int n = Math.max(0, souls(p) + IntegerArgumentType.getInteger(c, "amount"));
                                    p.setData(SOULS, n);
                                    c.getSource().sendSuccess(() -> Component.translatable("jaycee.command.souls", p.getDisplayName(), n), true);
                                    return n;
                                }))))));
    }
}

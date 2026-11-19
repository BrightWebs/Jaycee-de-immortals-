package com.brightwebs.jaycee.status;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Status extras: titles, souls, rank (read from EP) and progress counters used to unlock things. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class StatusData {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    /** The equipped title's id ("" for none). Synced to everyone so it can show above the name. */
    public static final Supplier<AttachmentType<String>> TITLE = ATTACHMENTS.register("equipped_title",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("title")).copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8).build());
    public static final Supplier<AttachmentType<List<String>>> TITLES = ATTACHMENTS.register("titles",
            () -> AttachmentType.<List<String>>builder(() -> List.of()).serialize(Codec.STRING.listOf().fieldOf("owned")).copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());
    public static final Supplier<AttachmentType<Boolean>> TITLE_PUBLIC = ATTACHMENTS.register("title_public",
            () -> AttachmentType.builder(() -> true).serialize(Codec.BOOL.fieldOf("public")).copyOnDeath()
                    .sync(ByteBufCodecs.BOOL).build());
    public static final Supplier<AttachmentType<Integer>> SOULS = ATTACHMENTS.register("souls",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("souls")).copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_INT).build());
    /** Progress counters (mana spent, damage endured, monsters killed...). Server only. */
    public static final Supplier<AttachmentType<Map<String, Double>>> COUNTERS = ATTACHMENTS.register("counters",
            () -> AttachmentType.<Map<String, Double>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("counters")).copyOnDeath().build());

    private StatusData() {}

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    public static String title(Player p) { return p.getData(TITLE); }
    public static List<String> titles(Player p) { return p.getData(TITLES); }
    public static boolean titlePublic(Player p) { return p.getData(TITLE_PUBLIC); }
    public static int souls(Player p) { return p.getData(SOULS); }

    public static double counter(Player p, String key) { return p.getData(COUNTERS).getOrDefault(key, 0.0); }

    public static double addCounter(ServerPlayer p, String key, double amount) {
        Map<String, Double> m = new HashMap<>(p.getData(COUNTERS));
        double v = m.getOrDefault(key, 0.0) + amount;
        m.put(key, v);
        p.setData(COUNTERS, Map.copyOf(m));
        return v;
    }

    /** Ranks need a Guild Card; until the guild exists the rank row stays hidden. */
    public static boolean hasGuildCard(Player p) { return false; }

    private static final double[] RANK_EP = {0, 1_000, 5_000, 20_000, 60_000, 150_000, 400_000, 1_000_000, 5_000_000};
    private static final String[] RANKS = {"F", "E", "D", "C", "B", "A", "Special A", "S", "Special S"};

    public static String rank(double ep) {
        int r = 0;
        for (int i = 0; i < RANK_EP.length; i++) if (ep >= RANK_EP[i]) r = i;
        return RANKS[r];
    }

    private static final Set<String> SOULFUL = Set.of(
            "villager", "wandering_trader", "pillager", "vindicator", "evoker", "illusioner", "witch", "piglin", "piglin_brute");

    public static boolean hasSoul(LivingEntity e) {
        return e instanceof Player || SOULFUL.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("StatusData.onDeath", () -> {
            if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == event.getEntity()) return;
            if (hasSoul(event.getEntity())) killer.setData(SOULS, souls(killer) + 1);
            if (event.getEntity() instanceof Enemy) addCounter(killer, "monsters_killed", 1);
            String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath();
            if (type.contains("slime")) addCounter(killer, "slimes_killed", 1);
        });
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(BrightCore.commandRoot())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("title")
                        .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("title", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Titles.ALL.keySet(), b))
                                        .executes(c -> {
                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                            String t = StringArgumentType.getString(c, "title");
                                            if (!Titles.ALL.containsKey(t)) return 0;
                                            Titles.grant(p, t);
                                            return 1;
                                        }))))
                        .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            Titles.equip(p, "");
                            p.setData(TITLES, List.of());
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

    static List<String> withAdded(List<String> list, String id) {
        List<String> l = new ArrayList<>(list);
        if (!l.contains(id)) l.add(id);
        return List.copyOf(l);
    }
}

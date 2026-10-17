package com.brightwebs.jaycee.gameplay;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.gameplay.divine.DivinityModel;
import com.brightwebs.jaycee.gameplay.divine.DivinityService;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Physical Divine Totem opens a spiritual invitation; Essence itself is never an item. */
public final class DivineTotemService {
    private DivineTotemService() {}
    public static final class Totem extends Item {
        public Totem(Properties props) { super(props); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                  InteractionHand hand) {
            if (!level.isClientSide && player instanceof ServerPlayer server) show(server);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
    }

    static void register() { NeoForge.EVENT_BUS.addListener(DivineTotemService::trades); }
    private static void trades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CLERIC) return;
        event.getTrades().get(2).add((trader, random) ->
            new MerchantOffer(new ItemCost(Items.EMERALD, 16), new ItemStack(JayceeMod.DIVINE_TOTEM.get()), 4, 10, 0.05f));
    }

    private static final List<String> OLYMPIANS = List.of("zeus", "poseidon", "hades", "athena", "ares",
        "apollo", "artemis", "hermes", "hephaestus", "aphrodite", "demeter", "dionysus", "hera", "hestia");

    private static void show(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("DIVINE TOTEM - Select an Olympian. Higher gods are harder to convince."));
        for (String id : OLYMPIANS) {
            int chance = chance(player, id);
            player.sendSystemMessage(Component.literal("[" + title(id) + "] " + (chance / 10.0) + "%")
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jc patron seek " + id))));
        }
        player.sendSystemMessage(Component.literal("Completed raids and Luck Points improve your chances. Selecting consumes the Totem."));
    }

    private static String title(String id) { return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1); }

    static int luck(ServerPlayer player) {
        int wins = Math.min(100, Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.RAID_WIN))));
        var data = PlayerState.read(player);
        int extra = Math.clamp(data.getInt("DivineLuckBonus"), 0, 300);
        return Math.min(500, wins * 10 + extra);
    }

    static int chance(ServerPlayer player, String id) {
        int base = switch (id) {
            case "zeus", "poseidon", "hades" -> 60;
            case "hera", "athena", "apollo", "artemis" -> 130;
            case "ares", "hephaestus", "demeter" -> 210;
            default -> 300;
        };
        int raids = Math.min(100, Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.RAID_WIN))));
        return Math.clamp(base + luck(player) * 2 + (raids > 0 ? 80 : 0), 10, 900);
    }

    private static int seek(ServerPlayer player, String id) {
        if (!OLYMPIANS.contains(id) || !DivinityModel.NPC_PATRONS.contains(id)) return 0;
        if (DivinityService.model(player).profile(player.getUUID()).rank() != DivinityModel.Rank.MORTAL
            || !DivinityService.model(player).profile(player.getUUID()).npcPatrons().isEmpty()) {
            player.sendSystemMessage(Component.literal("You already have a divine lineage."));
            return 0;
        }
        InteractionHand hand = player.getMainHandItem().is(JayceeMod.DIVINE_TOTEM.get())
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(JayceeMod.DIVINE_TOTEM.get())) {
            player.sendSystemMessage(Component.literal("Hold a Divine Totem to request a blessing."));
            return 0;
        }
        var data = PlayerState.read(player);
        long now = System.currentTimeMillis();
        if (now < data.getLong("DivineTotemNextAttempt")) {
            player.sendSystemMessage(Component.literal("The gods are not ready to hear another request yet."));
            return 0;
        }
        stack.shrink(1);
        data.putLong("DivineTotemNextAttempt", now + 30_000L);
        PlayerState.save(player, data);
        if (player.getRandom().nextInt(1000) >= chance(player, id)) {
            player.sendSystemMessage(Component.literal(title(id) + " declined your request. Complete raids and improve Luck to increase your chance."));
            return 1;
        }
        DivinityService.olympianBless(player, id);
        player.sendSystemMessage(Component.literal(title(id) + " accepted you! You received your blessing and 250 intangible Essence. Press M."));
        return 1;
    }

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> commands() {
        var root = Commands.literal("seek");
        for (String god : OLYMPIANS) root.then(Commands.literal(god)
            .executes(c -> seek(c.getSource().getPlayerOrException(), god)));
        return root;
    }
}

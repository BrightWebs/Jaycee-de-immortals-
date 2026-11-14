package com.brightwebs.jaycee.ichor;

import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Everything that reads or changes a player's Ichor goes through here. */
public final class IchorAPI {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    public static final Supplier<AttachmentType<IchorData>> ICHOR = ATTACHMENTS.register("ichor",
            () -> AttachmentType.builder(() -> IchorData.EMPTY)
                    .serialize(IchorData.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(IchorData.STREAM_CODEC)
                    .build());

    private IchorAPI() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static IchorData get(Player player) {
        return player.getData(ICHOR);
    }

    public static void set(ServerPlayer player, int ichor) {
        player.setData(ICHOR, get(player).withIchor(ichor));
    }

    public static void add(ServerPlayer player, int amount) {
        long next = (long) get(player).ichor() + amount;
        set(player, (int) Math.max(0, Math.min(Grade.MAX_ICHOR, next)));
    }

    /** First join: draw the Birthright, set starting Ichor, and show the card. */
    public static void drawBirthright(ServerPlayer player) {
        Birthright b = Birthright.draw(player.getRandom());
        player.setData(ICHOR, get(player).withBirthright(b).withIchor(b.rollIchor(player.getRandom())));
        reveal(player, b);
    }

    /** Shows the Birthright as a title card and a chat line. */
    public static void reveal(ServerPlayer player, Birthright b) {
        String key = "jaycee.birthright." + b.getSerializedName();
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(key)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(key + ".subtitle")));
        player.sendSystemMessage(Component.translatable(key + ".chat"));
    }

    /** Praying at a temple: the gods tell you your Ichor, and the menu remembers it. */
    public static void pray(ServerPlayer player) {
        IchorData data = get(player);
        player.setData(ICHOR, data.withRevealed(data.ichor()));
        player.sendSystemMessage(Component.translatable("jaycee.prayer.answer",
                String.format("%,d", data.ichor()), data.grade().label()));
    }
}

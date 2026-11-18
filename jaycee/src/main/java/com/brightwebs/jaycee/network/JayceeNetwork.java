package com.brightwebs.jaycee.network;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.Titles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Menu actions for Jaycee's own systems. The server checks everything. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class JayceeNetwork {
    private JayceeNetwork() {}

    /** Equip a title you own ("" to take it off). */
    public record EquipTitle(String title) implements CustomPacketPayload {
        public static final Type<EquipTitle> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "equip_title"));
        public static final StreamCodec<ByteBuf, EquipTitle> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, EquipTitle::title, EquipTitle::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Show or hide your title above your name. */
    public record TitlePublic(boolean on) implements CustomPacketPayload {
        public static final Type<TitlePublic> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "title_public"));
        public static final StreamCodec<ByteBuf, TitlePublic> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, TitlePublic::on, TitlePublic::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(EquipTitle.TYPE, EquipTitle.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.equip(sp, p.title()); });
        r.playToServer(TitlePublic.TYPE, TitlePublic.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.setPublic(sp, p.on()); });
    }
}

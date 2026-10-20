package com.brightwebs.jaycee.gameplay;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** All blessing odds and random draws stay on the server. */
public final class BlessingNetwork {
    private BlessingNetwork() {}
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "blessing_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());
        public Type<Open> type() { return TYPE; }
    }
    public record Choose(String god) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "blessing_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC = new StreamCodec<>() {
            public Choose decode(RegistryFriendlyByteBuf b) { return new Choose(b.readUtf(32)); }
            public void encode(RegistryFriendlyByteBuf b, Choose p) { b.writeUtf(p.god(), 32); }
        };
        public Type<Choose> type() { return TYPE; }
    }
    public record Result(String god, boolean accepted) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "blessing_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = new StreamCodec<>() {
            public Result decode(RegistryFriendlyByteBuf b) { return new Result(b.readUtf(32), b.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf b, Result p) { b.writeUtf(p.god(), 32); b.writeBoolean(p.accepted()); }
        };
        public Type<Result> type() { return TYPE; }
    }
    public static void registerOn(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (packet, context) ->
            context.enqueueWork(() -> com.brightwebs.jaycee.client.BlessingScreen.open()));
        r.playToClient(Result.TYPE, Result.CODEC, (packet, context) ->
            context.enqueueWork(() -> com.brightwebs.jaycee.client.BlessingScreen.result(packet.god(), packet.accepted())));
        r.playToServer(Choose.TYPE, Choose.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player)
                DivineTotemService.seek(player, packet.god());
        });
    }
}

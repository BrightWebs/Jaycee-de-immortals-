package com.brightwebs.jaycee.gameplay.divine;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DivinityNetwork {
    public static final UUID NONE = new UUID(0, 0);
    public record Request(String operation, UUID target, UUID nonce, long amount, String ability,
                          String name, boolean bind) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "divinity"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf b) {
                return new Request(b.readUtf(24), b.readUUID(), b.readUUID(), b.readLong(), b.readUtf(128), b.readUtf(48), b.readBoolean());
            }
            public void encode(RegistryFriendlyByteBuf b, Request r) {
                b.writeUtf(r.operation, 24); b.writeUUID(r.target); b.writeUUID(r.nonce); b.writeLong(r.amount);
                b.writeUtf(r.ability, 128); b.writeUtf(r.name, 48); b.writeBoolean(r.bind);
            }
        };
        @Override public Type<Request> type() { return TYPE; }
    }
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) DivinityService.handle(player, payload);
        });
    }
    public static void send(String op, UUID target, UUID nonce, long amount, String ability, String name, boolean bind) {
        PacketDistributor.sendToServer(new Request(op, target == null ? NONE : target, nonce == null ? NONE : nonce,
            amount, ability, name, bind));
    }
    public static void request(String operation) { send(operation, null, null, 0, "", "", false); }
    private DivinityNetwork() {}
}

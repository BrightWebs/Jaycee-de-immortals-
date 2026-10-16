package com.brightwebs.jaycee;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Server authoritative privilege synchronization (UI is never the authority). */
public final class JayceeRoleSync {
    public record Role(boolean jaycee, int consumedCores) implements CustomPacketPayload {
        public static final Type<Role> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "role"));
        public static final StreamCodec<ByteBuf,Role> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL,Role::jaycee,ByteBufCodecs.VAR_INT,Role::consumedCores,Role::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Role.TYPE, Role.STREAM_CODEC,
            (payload,context) -> com.brightwebs.jaycee.client.JayceeClient.setServerRole(payload.jaycee(), payload.consumedCores()));
    }

    public static void attach() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if(event.getEntity() instanceof ServerPlayer sp) {
                // Refresh persisted state when a player joins this server.
                if(JayceeAuthority.cores(sp)>=3) JayceeAuthority.afterCore(sp);
                send(sp);
            }
        });
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new Role(JayceeAuthority.isJaycee(player), JayceeAuthority.cores(player)));
    }
}

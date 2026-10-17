package com.brightwebs.jaycee.gameplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GameplayNetwork {
    private GameplayNetwork() {}

    public record State(CompoundTag data) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf buf) {
                CompoundTag tag = buf.readNbt();
                return new State(tag == null ? new CompoundTag() : tag);
            }
            public void encode(RegistryFriendlyByteBuf buf, State value) { buf.writeNbt(value.data()); }
        };
        public Type<State> type() { return TYPE; }
    }

    public record Action(String action, int first, int second, String value) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = new StreamCodec<>() {
            public Action decode(RegistryFriendlyByteBuf buf) {
                return new Action(buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128));
            }
            public void encode(RegistryFriendlyByteBuf buf, Action value) {
                buf.writeUtf(value.action(), 16);
                buf.writeVarInt(value.first());
                buf.writeVarInt(value.second());
                buf.writeUtf(value.value(), 128);
            }
        };
        public Type<Action> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        com.brightwebs.jaycee.gameplay.divine.DivinityNetwork.register(registrar);
        BlessingNetwork.registerOn(registrar);
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) ->
            PlayerState.save(context.player(), payload.data()));
        registrar.playToServer(Action.TYPE, Action.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) Gameplay.handle(player, payload);
        });
    }

    public static void sync(ServerPlayer player) {
        CompoundTag snapshot = PlayerState.read(player);
        snapshot.putLong("ServerTick", player.serverLevel().getGameTime());
        var resolved = ManaBackends.resolve(player);
        snapshot.putString("EffectiveBackend", resolved.id());
        snapshot.putDouble("Mana", PlayerState.bounded(resolved.mana().getCurrent(), resolved.mana().getMax()));
        snapshot.putDouble("MaxMana", PlayerState.maximum(resolved.mana().getMax()));
        snapshot.putDouble("ManaRegen", PlayerState.bounded(resolved.mana().getRegen(), 10000));
        snapshot.put("Divinity", com.brightwebs.jaycee.gameplay.divine.DivinityService.snapshot(player));
        PacketDistributor.sendToPlayer(player, new State(snapshot));
    }
}

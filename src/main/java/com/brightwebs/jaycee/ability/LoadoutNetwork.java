package com.brightwebs.jaycee.ability;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class LoadoutNetwork {
    public static final int SELECT_PAGE = 0, EQUIP = 1, CLEAR = 2, ACTIVATE = 3, CYCLE_MODE = 4, REQUEST = 5;

    public record Action(int action, int page, int slot, int ability) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "loadout_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = new StreamCodec<>() {
            public Action decode(RegistryFriendlyByteBuf buf) {
                return new Action(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            }
            public void encode(RegistryFriendlyByteBuf buf, Action value) {
                buf.writeInt(value.action); buf.writeInt(value.page); buf.writeInt(value.slot); buf.writeInt(value.ability);
            }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(LoadoutClientState.Snapshot snapshot) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee", "loadout_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf buf) {
                int page = buf.readInt(), mana = buf.readInt();
                boolean accessible = buf.readBoolean();
                List<Integer> abilities = readSlots(buf), modes = readSlots(buf), cooldowns = readSlots(buf), active = readSlots(buf);
                List<Boolean> unlocked = new ArrayList<>(9);
                for (int i = 0; i < 9; i++) unlocked.add(buf.readBoolean());
                return new State(new LoadoutClientState.Snapshot(page, abilities, modes, cooldowns, active, mana, accessible, unlocked));
            }
            public void encode(RegistryFriendlyByteBuf buf, State value) {
                var s = value.snapshot;
                buf.writeInt(s.page()); buf.writeInt(s.mana()); buf.writeBoolean(s.accessible());
                for (var list : List.of(s.abilities(), s.modes(), s.cooldowns(), s.activeTicks()))
                    for (int i = 0; i < 40; i++) buf.writeInt(list.get(i));
                for (int i = 0; i < 9; i++) buf.writeBoolean(s.unlocked().get(i));
            }
            private List<Integer> readSlots(RegistryFriendlyByteBuf buf) {
                List<Integer> result = new ArrayList<>(40);
                for (int i = 0; i < 40; i++) result.add(buf.readInt());
                return result;
            }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Action.TYPE, Action.CODEC, (payload, context) ->
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) LoadoutService.action(player, payload);
            }));
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) ->
            context.enqueueWork(() -> LoadoutClientState.accept(payload.snapshot())));
    }

    public static void sendAction(int action, int page, int slot, int ability) {
        PacketDistributor.sendToServer(new Action(action, page, slot, ability));
    }

    static void sync(ServerPlayer player, LoadoutClientState.Snapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new State(snapshot));
    }

    private LoadoutNetwork() {}
}

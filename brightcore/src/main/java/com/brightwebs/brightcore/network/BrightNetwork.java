package com.brightwebs.brightcore.network;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.data.AbilityData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client-to-server packets. The client only ever sends intentions; the server checks everything.
 * Server-to-client syncing needs no packets: the data attachment syncs itself.
 */
@EventBusSubscriber(modid = BrightCore.ID)
public final class BrightNetwork {
    private BrightNetwork() {}

    /** "I pressed ability key N" (0-4). */
    public record ActivateSlot(int slot) implements CustomPacketPayload {
        public static final Type<ActivateSlot> TYPE = new Type<>(BrightCore.id("activate_slot"));
        public static final StreamCodec<ByteBuf, ActivateSlot> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ActivateSlot::slot, ActivateSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** "Show page N of my ability bar" (0-9). */
    public record SetPage(int page) implements CustomPacketPayload {
        public static final Type<SetPage> TYPE = new Type<>(BrightCore.id("set_page"));
        public static final StreamCodec<ByteBuf, SetPage> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetPage::page, SetPage::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** "Put this ability in slot index N (0-49)". An empty id clears the slot. */
    public record AssignSlot(int index, String ability) implements CustomPacketPayload {
        public static final Type<AssignSlot> TYPE = new Type<>(BrightCore.id("assign_slot"));
        public static final StreamCodec<ByteBuf, AssignSlot> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AssignSlot::index,
                ByteBufCodecs.STRING_UTF8, AssignSlot::ability,
                AssignSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // Bump this whenever a packet's format changes.
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ActivateSlot.TYPE, ActivateSlot.CODEC, BrightNetwork::onActivate);
        registrar.playToServer(SetPage.TYPE, SetPage.CODEC, BrightNetwork::onSetPage);
        registrar.playToServer(AssignSlot.TYPE, AssignSlot.CODEC, BrightNetwork::onAssign);
    }

    private static void onActivate(ActivateSlot payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && payload.slot() >= 0 && payload.slot() < AbilityData.SLOTS_PER_PAGE) {
            AbilityAPI.activateSlot(player, payload.slot());
        }
    }

    private static void onSetPage(SetPage payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AbilityAPI.setPage(player, payload.page());
        }
    }

    private static void onAssign(AssignSlot payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (payload.ability().isEmpty()) {
            AbilityAPI.assign(player, payload.index(), null);
            return;
        }
        var ability = AbilityRegistry.get(Identifier.tryParse(payload.ability()));
        if (ability != null) AbilityAPI.assign(player, payload.index(), ability);
    }
}

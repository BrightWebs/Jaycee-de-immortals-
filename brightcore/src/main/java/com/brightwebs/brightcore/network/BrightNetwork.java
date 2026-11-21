package com.brightwebs.brightcore.network;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
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

// client to server packets. the client only says what it wants, the server checks everything
// server to client doesn't need packets, the attachments sync themselves
@EventBusSubscriber(modid = BrightCore.ID)
public final class BrightNetwork {
    private BrightNetwork() {}

    // "i pressed skill key N" (0-4)
    public record ActivateSlot(int slot) implements CustomPacketPayload {
        public static final Type<ActivateSlot> TYPE = new Type<>(BrightCore.id("activate_slot"));
        public static final StreamCodec<ByteBuf, ActivateSlot> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ActivateSlot::slot, ActivateSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "show page N of my bar" (0-9)
    public record SetPage(int page) implements CustomPacketPayload {
        public static final Type<SetPage> TYPE = new Type<>(BrightCore.id("set_page"));
        public static final StreamCodec<ByteBuf, SetPage> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetPage::page, SetPage::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "put this skill in slot N" (0-49). empty id clears it
    public record AssignSlot(int index, String ability) implements CustomPacketPayload {
        public static final Type<AssignSlot> TYPE = new Type<>(BrightCore.id("assign_slot"));
        public static final StreamCodec<ByteBuf, AssignSlot> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AssignSlot::index,
                ByteBufCodecs.STRING_UTF8, AssignSlot::ability,
                AssignSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "next mode for the skill in slot N" (alt + skill key)
    public record CycleMode(int slot) implements CustomPacketPayload {
        public static final Type<CycleMode> TYPE = new Type<>(BrightCore.id("cycle_mode"));
        public static final StreamCodec<ByteBuf, CycleMode> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, CycleMode::slot, CycleMode::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "set this skill to mode N" (menu)
    public record SetMode(String ability, int mode) implements CustomPacketPayload {
        public static final Type<SetMode> TYPE = new Type<>(BrightCore.id("set_mode"));
        public static final StreamCodec<ByteBuf, SetMode> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SetMode::ability, ByteBufCodecs.VAR_INT, SetMode::mode, SetMode::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "i pick this race" from the first join menu. "random" picks a starting race
    // the on/off switch in the skill menu
    public record FlipSwitch(String ability) implements CustomPacketPayload {
        public static final Type<FlipSwitch> TYPE = new Type<>(BrightCore.id("flip_switch"));
        public static final StreamCodec<ByteBuf, FlipSwitch> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, FlipSwitch::ability, FlipSwitch::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // race key (R) went down or up
    public record RaceKey(boolean down) implements CustomPacketPayload {
        public static final Type<RaceKey> TYPE = new Type<>(BrightCore.id("race_key"));
        public static final StreamCodec<ByteBuf, RaceKey> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, RaceKey::down, RaceKey::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // a slot key was let go (ends held skills)
    public record ReleaseSlot(int slot) implements CustomPacketPayload {
        public static final Type<ReleaseSlot> TYPE = new Type<>(BrightCore.id("release_slot"));
        public static final StreamCodec<ByteBuf, ReleaseSlot> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ReleaseSlot::slot, ReleaseSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChooseRace(String race) implements CustomPacketPayload {
        public static final Type<ChooseRace> TYPE = new Type<>(BrightCore.id("choose_race"));
        public static final StreamCodec<ByteBuf, ChooseRace> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ChooseRace::race, ChooseRace::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // "evolve me into this". server checks all the requirements
    public record Evolve(String target) implements CustomPacketPayload {
        public static final Type<Evolve> TYPE = new Type<>(BrightCore.id("evolve"));
        public static final StreamCodec<ByteBuf, Evolve> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Evolve::target, Evolve::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // bump this whenever a packet changes
        PayloadRegistrar registrar = event.registrar("7");
        registrar.playToServer(ActivateSlot.TYPE, ActivateSlot.CODEC, BrightNetwork::onActivate);
        registrar.playToServer(ReleaseSlot.TYPE, ReleaseSlot.CODEC, (payload, context) -> com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.release", () -> {
            if (context.player() instanceof ServerPlayer player
                    && payload.slot() >= 0 && payload.slot() < AbilityData.SLOTS_PER_PAGE) {
                AbilityAPI.releaseSlot(player, payload.slot());
            }
        }));
        registrar.playToServer(RaceKey.TYPE, RaceKey.CODEC, (payload, context) -> com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.raceKey", () -> {
            if (context.player() instanceof ServerPlayer player) com.brightwebs.brightcore.api.race.RaceAbilities.key(player, payload.down());
        }));
        registrar.playToServer(FlipSwitch.TYPE, FlipSwitch.CODEC, (payload, context) -> com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.flipSwitch", () -> {
            if (context.player() instanceof ServerPlayer player) {
                var a = AbilityRegistry.get(Identifier.tryParse(payload.ability()));
                if (a != null) AbilityAPI.flipSwitch(player, a);
            }
        }));
        registrar.playToServer(SetPage.TYPE, SetPage.CODEC, BrightNetwork::onSetPage);
        registrar.playToServer(AssignSlot.TYPE, AssignSlot.CODEC, BrightNetwork::onAssign);
        registrar.playToServer(CycleMode.TYPE, CycleMode.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && payload.slot() >= 0 && payload.slot() < AbilityData.SLOTS_PER_PAGE) {
                AbilityAPI.cycleMode(player, payload.slot());
            }
        });
        registrar.playToServer(SetMode.TYPE, SetMode.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                var a = AbilityRegistry.get(Identifier.tryParse(payload.ability()));
                if (a != null) AbilityAPI.setMode(player, a, payload.mode());
            }
        });
        registrar.playToServer(ChooseRace.TYPE, ChooseRace.CODEC, BrightNetwork::onChooseRace);
        registrar.playToServer(Evolve.TYPE, Evolve.CODEC, BrightNetwork::onEvolve);
    }

    private static void onActivate(ActivateSlot payload, IPayloadContext context) {

        com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.onActivate", () -> {
            if (context.player() instanceof ServerPlayer player
                    && payload.slot() >= 0 && payload.slot() < AbilityData.SLOTS_PER_PAGE) {
                AbilityAPI.activateSlot(player, payload.slot());
            }
        });

    }

    private static void onSetPage(SetPage payload, IPayloadContext context) {

        com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.onSetPage", () -> {
            if (context.player() instanceof ServerPlayer player) {
                AbilityAPI.setPage(player, payload.page());
            }
        });

    }

    private static void onChooseRace(ChooseRace payload, IPayloadContext context) {

        com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.onChooseRace", () -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var options = RaceRegistry.starting();
            if (options.isEmpty()) return;
            Race race = payload.race().equals("random")
                    ? options.get(player.getRandom().nextInt(options.size()))
                    : RaceRegistry.get(Identifier.tryParse(payload.race()));
            boolean ok = race != null && RaceAPI.choose(player, race);
            com.mojang.logging.LogUtils.getLogger().info("[BrightCore] {} chose race {} -> {}", player.getName().getString(), payload.race(), ok ? "accepted" : "rejected");
        });

    }

    private static void onEvolve(Evolve payload, IPayloadContext context) {

        com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.onEvolve", () -> {
            if (context.player() instanceof ServerPlayer player) {
                Identifier id = Identifier.tryParse(payload.target());
                if (id != null) RaceAPI.evolve(player, id);
            }
        });

    }

    private static void onAssign(AssignSlot payload, IPayloadContext context) {

        com.brightwebs.brightcore.api.util.Safe.run("BrightNetwork.onAssign", () -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.ability().isEmpty()) {
                AbilityAPI.assign(player, payload.index(), null);
                return;
            }
            var ability = AbilityRegistry.get(Identifier.tryParse(payload.ability()));
            if (ability != null) AbilityAPI.assign(player, payload.index(), ability);
        });

    }
}

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

    /** Analytical Appraisal's reading of the creature you're looking at (entity -1 clears it). */
    public record AppraisalInfo(int entity, String name, String race, double ep, float hp, float maxHp,
                                float spirit, float maxSpirit, float mana, float maxMana, float aura, float maxAura,
                                int armor, java.util.List<String> skills, int radius, int scanMode) implements CustomPacketPayload {
        public static final Type<AppraisalInfo> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "appraisal"));
        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, AppraisalInfo> CODEC = StreamCodec.of(
                (b, v) -> {
                    b.writeVarInt(v.entity()); b.writeUtf(v.name()); b.writeUtf(v.race()); b.writeDouble(v.ep());
                    b.writeFloat(v.hp()); b.writeFloat(v.maxHp()); b.writeFloat(v.spirit()); b.writeFloat(v.maxSpirit());
                    b.writeFloat(v.mana()); b.writeFloat(v.maxMana()); b.writeFloat(v.aura()); b.writeFloat(v.maxAura());
                    b.writeVarInt(v.armor()); b.writeVarInt(v.skills().size()); for (String s : v.skills()) b.writeUtf(s);
                    b.writeVarInt(v.radius()); b.writeVarInt(v.scanMode());
                },
                b -> {
                    int e = b.readVarInt(); String n = b.readUtf(), r = b.readUtf(); double ep = b.readDouble();
                    float hp = b.readFloat(), mh = b.readFloat(), sp = b.readFloat(), ms = b.readFloat();
                    float mp = b.readFloat(), mm = b.readFloat(), ap = b.readFloat(), ma = b.readFloat();
                    int armor = b.readVarInt(), count = Math.min(b.readVarInt(), 32);
                    java.util.List<String> skills = new java.util.ArrayList<>();
                    for (int i = 0; i < count; i++) skills.add(b.readUtf());
                    int radius = b.readVarInt(), scanMode = b.readVarInt();
                    return new AppraisalInfo(e, n, r, ep, hp, mh, sp, ms, mp, mm, ap, ma, armor, skills, radius, scanMode);
                });
        /** Scan on and no creature in sight: the client reads blocks itself within radius. scanMode 0 both, 1 creatures, 2 blocks. */
        public static AppraisalInfo idle(int radius, int scanMode) {
            return new AppraisalInfo(-2, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, java.util.List.of(), radius, scanMode);
        }
        /** Scan off. */
        public static final AppraisalInfo NONE = new AppraisalInfo(-1, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, java.util.List.of(), 0, 0);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** N pressed (shift: toggle your nameable status). */
    public record NamingKey(boolean shift) implements CustomPacketPayload {
        public static final Type<NamingKey> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "naming_key"));
        public static final StreamCodec<ByteBuf, NamingKey> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, NamingKey::shift, NamingKey::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: open the naming pop-up for this creature, with what each choice gives and costs. */
    public record OpenNaming(int entity, String name, String race, double ep, float hp,
                             double gain0, double gain1, double gain2, double cost0, double cost1, double cost2) implements CustomPacketPayload {
        public static final Type<OpenNaming> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "open_naming"));
        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, OpenNaming> CODEC = StreamCodec.of(
                (b, v) -> { b.writeVarInt(v.entity()); b.writeUtf(v.name()); b.writeUtf(v.race()); b.writeDouble(v.ep()); b.writeFloat(v.hp());
                    b.writeDouble(v.gain0()); b.writeDouble(v.gain1()); b.writeDouble(v.gain2()); b.writeDouble(v.cost0()); b.writeDouble(v.cost1()); b.writeDouble(v.cost2()); },
                b -> new OpenNaming(b.readVarInt(), b.readUtf(), b.readUtf(), b.readDouble(), b.readFloat(),
                        b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The pop-up's Name button. type 0 Subdue, 1 Evolve, 2 Endow. */
    public record NameRequest(int entity, String name, int namingType) implements CustomPacketPayload {
        public static final Type<NameRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "name_request"));
        public static final StreamCodec<ByteBuf, NameRequest> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, NameRequest::entity,
                ByteBufCodecs.STRING_UTF8, NameRequest::name, ByteBufCodecs.VAR_INT, NameRequest::namingType, NameRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Bonds page: follow / stay / release a subordinate. */
    public record SubCommand(String uuid, String command) implements CustomPacketPayload {
        public static final Type<SubCommand> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "sub_command"));
        public static final StreamCodec<ByteBuf, SubCommand> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SubCommand::uuid,
                ByteBufCodecs.STRING_UTF8, SubCommand::command, SubCommand::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Set on the client by OpenNaming; the client opens the pop-up. */
    public static volatile java.util.function.Consumer<OpenNaming> openNaming = p -> {};

    /** The latest reading on this client, and when it arrived (read by the HUD). */
    public static volatile AppraisalInfo lastAppraisal = AppraisalInfo.NONE;
    public static volatile long lastAppraisalAt;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("3");
        r.playToClient(AppraisalInfo.TYPE, AppraisalInfo.CODEC, (p, c) -> { lastAppraisal = p; lastAppraisalAt = System.currentTimeMillis(); });
        r.playToClient(OpenNaming.TYPE, OpenNaming.CODEC, (p, c) -> openNaming.accept(p));
        r.playToServer(NamingKey.TYPE, NamingKey.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) com.brightwebs.brightcore.api.util.Safe.run("NamingKey", () -> com.brightwebs.jaycee.bonds.Naming.key(sp, p.shift())); });
        r.playToServer(NameRequest.TYPE, NameRequest.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) com.brightwebs.brightcore.api.util.Safe.run("NameRequest", () -> com.brightwebs.jaycee.bonds.Naming.name(sp, p.entity(), p.name(), p.namingType())); });
        r.playToServer(SubCommand.TYPE, SubCommand.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) com.brightwebs.brightcore.api.util.Safe.run("SubCommand", () -> com.brightwebs.jaycee.bonds.Bonds.command(sp, p.uuid(), p.command())); });
        r.playToServer(EquipTitle.TYPE, EquipTitle.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.equip(sp, p.title()); });
        r.playToServer(TitlePublic.TYPE, TitlePublic.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.setPublic(sp, p.on()); });
    }
}

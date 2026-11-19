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
                                int armor, java.util.List<String> skills) implements CustomPacketPayload {
        public static final Type<AppraisalInfo> TYPE = new Type<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "appraisal"));
        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, AppraisalInfo> CODEC = StreamCodec.of(
                (b, v) -> {
                    b.writeVarInt(v.entity()); b.writeUtf(v.name()); b.writeUtf(v.race()); b.writeDouble(v.ep());
                    b.writeFloat(v.hp()); b.writeFloat(v.maxHp()); b.writeFloat(v.spirit()); b.writeFloat(v.maxSpirit());
                    b.writeFloat(v.mana()); b.writeFloat(v.maxMana()); b.writeFloat(v.aura()); b.writeFloat(v.maxAura());
                    b.writeVarInt(v.armor()); b.writeVarInt(v.skills().size()); for (String s : v.skills()) b.writeUtf(s);
                },
                b -> {
                    int e = b.readVarInt(); String n = b.readUtf(), r = b.readUtf(); double ep = b.readDouble();
                    float hp = b.readFloat(), mh = b.readFloat(), sp = b.readFloat(), ms = b.readFloat();
                    float mp = b.readFloat(), mm = b.readFloat(), ap = b.readFloat(), ma = b.readFloat();
                    int armor = b.readVarInt(), count = Math.min(b.readVarInt(), 32);
                    java.util.List<String> skills = new java.util.ArrayList<>();
                    for (int i = 0; i < count; i++) skills.add(b.readUtf());
                    return new AppraisalInfo(e, n, r, ep, hp, mh, sp, ms, mp, mm, ap, ma, armor, skills);
                });
        /** Scan on, nothing in sight. */
        public static final AppraisalInfo IDLE = new AppraisalInfo(-2, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, java.util.List.of());
        public static final AppraisalInfo NONE = new AppraisalInfo(-1, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, java.util.List.of());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The latest reading on this client, and when it arrived (read by the HUD). */
    public static volatile AppraisalInfo lastAppraisal = AppraisalInfo.NONE;
    public static volatile long lastAppraisalAt;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("2");
        r.playToClient(AppraisalInfo.TYPE, AppraisalInfo.CODEC, (p, c) -> { lastAppraisal = p; lastAppraisalAt = System.currentTimeMillis(); });
        r.playToServer(EquipTitle.TYPE, EquipTitle.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.equip(sp, p.title()); });
        r.playToServer(TitlePublic.TYPE, TitlePublic.CODEC, (p, c) -> { if (c.player() instanceof ServerPlayer sp) Titles.setPublic(sp, p.on()); });
    }
}

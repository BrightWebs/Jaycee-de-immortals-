package com.brightwebs.jaycee.bonds;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// subordinates. in tensura if you name a monster it serves you
// the owner keeps a list for the bonds page, each subordinate keeps its owner and its order (follow or stay)
// they never target their owner or each other, they protect the owner and jump into the owner's fights
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Bonds {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    // one subordinate like it shows in the owner's list
    public record Sub(String uuid, String name, String type, double ep, String order) {
        public static final Codec<Sub> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("uuid").forGetter(Sub::uuid),
                Codec.STRING.fieldOf("name").forGetter(Sub::name),
                Codec.STRING.fieldOf("type").forGetter(Sub::type),
                Codec.DOUBLE.fieldOf("ep").forGetter(Sub::ep),
                Codec.STRING.fieldOf("order").forGetter(Sub::order)).apply(i, Sub::new));
        public static final StreamCodec<ByteBuf, Sub> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Sub::uuid, ByteBufCodecs.STRING_UTF8, Sub::name, ByteBufCodecs.STRING_UTF8, Sub::type,
                ByteBufCodecs.DOUBLE, Sub::ep, ByteBufCodecs.STRING_UTF8, Sub::order, Sub::new);
        Sub withOrder(String o) { return new Sub(uuid, name, type, ep, o); }
        Sub withEp(double e) { return new Sub(uuid, name, type, e, order); }
    }

    // the owner's subordinates, synced to the owner for the bonds page
    public static final Supplier<AttachmentType<List<Sub>>> SUBORDINATES = ATTACHMENTS.register("subordinates",
            () -> AttachmentType.<List<Sub>>builder(() -> List.of()).serialize(Sub.CODEC.listOf().fieldOf("subs")).copyOnDeath()
                    .sync((holder, to) -> holder == to, Sub.STREAM.apply(ByteBufCodecs.list())).build());
    // on something that got named: the name ("" if none). synced so everyone sees it
    public static final Supplier<AttachmentType<String>> NAME = ATTACHMENTS.register("given_name",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("name")).copyOnDeath().sync(ByteBufCodecs.STRING_UTF8).build());
    // on a subordinate: its owner's UUID ("" if none)
    public static final Supplier<AttachmentType<String>> OWNER = ATTACHMENTS.register("owner",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("owner")).copyOnDeath().sync(ByteBufCodecs.STRING_UTF8).build());
    // on a subordinate: "follow" or "stay"
    public static final Supplier<AttachmentType<String>> ORDER = ATTACHMENTS.register("order",
            () -> AttachmentType.builder(() -> "follow").serialize(Codec.STRING.fieldOf("order")).copyOnDeath().build());
    // on a player: can other people name them. shift + N flips it (tensura's nameable status)
    public static final Supplier<AttachmentType<Boolean>> NAMEABLE = ATTACHMENTS.register("nameable",
            () -> AttachmentType.builder(() -> true).serialize(Codec.BOOL.fieldOf("nameable")).copyOnDeath().build());

    private Bonds() {}

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    public static String owner(Entity e) { return e.getData(OWNER); }
    public static List<Sub> subs(Player p) { return p.getData(SUBORDINATES); }

    public static boolean isSubordinateOf(Entity e, Player owner) {
        return !owner(e).isEmpty() && owner(e).equals(owner.getStringUUID());
    }

    static void add(ServerPlayer owner, LivingEntity sub, String name) {
        List<Sub> l = new ArrayList<>(subs(owner));
        l.removeIf(s -> s.uuid().equals(sub.getStringUUID()));
        l.add(new Sub(sub.getStringUUID(), name, BuiltInRegistries.ENTITY_TYPE.getKey(sub.getType()).toString(), EnergyAPI.ep(sub), "follow"));
        owner.setData(SUBORDINATES, List.copyOf(l));
    }

    // follow / stay / release from the bonds page
    public static void command(ServerPlayer owner, String uuid, String cmd) {
        List<Sub> l = new ArrayList<>(subs(owner));
        for (int i = 0; i < l.size(); i++) {
            Sub s = l.get(i);
            if (!s.uuid().equals(uuid)) continue;
            Entity e = find(owner, uuid);
            if (cmd.equals("release")) {
                l.remove(i);
                if (e != null) { e.setData(OWNER, ""); if (e instanceof Mob m) m.setTarget(null); }
            } else if (cmd.equals("follow") || cmd.equals("stay")) {
                l.set(i, s.withOrder(cmd));
                if (e != null) {
                    e.setData(ORDER, cmd);
                    if (cmd.equals("stay") && e instanceof Mob m) m.getNavigation().stop();
                }
            }
            break;
        }
        owner.setData(SUBORDINATES, List.copyOf(l));
    }

    static Entity find(ServerPlayer owner, String uuid) {
        try {
            return ((ServerLevel) owner.level()).getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // behaviour

    // never go after your owner or another subordinate
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Bonds.onTarget", () -> {
            String own = owner(event.getEntity());
            LivingEntity t = event.getNewAboutToBeSetTarget();
            if (own.isEmpty() || t == null) return;
            if (t.getStringUUID().equals(own) || own.equals(owner(t))) event.setCanceled(true);
        });
    }

    // subordinates protect their owner and help in the owner's fights
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Bonds.onHurt", () -> {
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
            LivingEntity victim = event.getEntity();
            if (victim instanceof ServerPlayer owner && !isSubordinateOf(attacker, owner)) rally(owner, attacker);
            if (attacker instanceof ServerPlayer owner && !isSubordinateOf(victim, owner) && victim != owner) rally(owner, victim);
        });
    }

    private static void rally(ServerPlayer owner, LivingEntity enemy) {
        for (Sub s : subs(owner)) {
            if (find(owner, s.uuid()) instanceof Mob m && m != enemy && m.distanceToSqr(owner) < 32 * 32) m.setTarget(enemy);
        }
    }

    // followers stay close: walk back if they're past 10 blocks, teleport over if past 24
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || owner.tickCount % 20 != 0) return;
        com.brightwebs.brightcore.api.util.Safe.run("Bonds.onTick", () -> {
            List<Sub> l = subs(owner);
            if (l.isEmpty()) return;
            List<Sub> updated = new ArrayList<>(l);
            boolean changed = false;
            for (int i = 0; i < l.size(); i++) {
                Sub s = l.get(i);
                Entity e = find(owner, s.uuid());
                if (!(e instanceof LivingEntity le)) continue;
                double ep = EnergyAPI.ep(le);
                if (Math.abs(ep - s.ep()) >= 1) { updated.set(i, s.withEp(ep)); changed = true; }
                if (!(e instanceof Mob m) || !"follow".equals(e.getData(ORDER))) continue;
                if (m.getTarget() != null && m.getTarget().isAlive() && m.distanceToSqr(owner) < 24 * 24) continue;
                double d = m.distanceToSqr(owner);
                if (d > 24 * 24) m.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                else if (d > 10 * 10) m.getNavigation().moveTo(owner, 1.2);
            }
            if (changed) owner.setData(SUBORDINATES, List.copyOf(updated));
        });
    }
}

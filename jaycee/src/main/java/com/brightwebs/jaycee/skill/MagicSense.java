package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.util.Safe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Magic Sense, as in Tensura: press to toggle. While on it costs 5 magicules a second and grants
 * Presence Sense: living things nearby glow through walls, but only for you. Mastered, the sense
 * reaches twice as far.
 */
final class MagicSense {
    static final double COST_PER_SECOND = 5, PRESENCE = 0.5, PRESENCE_MASTERED = 1.0;
    private static final Map<UUID, Set<Integer>> SHOWN = new HashMap<>();
    private static EntityDataAccessor<Byte> FLAGS;

    private MagicSense() {}

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> flags() {
        if (FLAGS == null) {
            try {
                Field f = Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
                f.setAccessible(true);
                FLAGS = (EntityDataAccessor<Byte>) f.get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Magic Sense can't reach the glowing flag", e);
            }
        }
        return FLAGS;
    }

    static void tick(ServerPlayer p, Ability a) {
        if (p.tickCount % Tk.SKILL_TICK == 0) {
            if (!Tk.spend(p, a, COST_PER_SECOND * 5)) {
                p.sendSystemMessage(Component.translatable("jaycee.skill.lack_magicule.toggled_off", a.name()).withStyle(ChatFormatting.RED));
                AbilityAPI.setToggled(p, a, false);
                return;
            }
            Tk.activatedTime(p, a);
        }
        if (p.tickCount % 10 == 0) Safe.run("MagicSense.glow", () -> glow(p, a));
    }

    /** Sends the glowing flag for nearby creatures to this one player only. */
    private static void glow(ServerPlayer p, Ability a) {
        double radius = 16 * (Tk.mastered(p, a) ? PRESENCE_MASTERED : PRESENCE) * 2;
        ServerLevel level = (ServerLevel) p.level();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> e != p && e.isAlive());
        Set<Integer> now = new HashSet<>();
        EntityDataAccessor<Byte> acc = flags();
        for (LivingEntity e : near) {
            now.add(e.getId());
            byte f = e.getEntityData().get(acc);
            p.connection.send(new ClientboundSetEntityDataPacket(e.getId(), List.of(SynchedEntityData.DataValue.create(acc, (byte) (f | 0x40)))));
        }
        Set<Integer> before = SHOWN.put(p.getUUID(), now);
        if (before != null) for (int id : before) if (!now.contains(id)) restore(p, level.getEntity(id));
    }

    private static void restore(ServerPlayer p, Entity e) {
        if (e == null) return;
        Safe.run("MagicSense.restore", () -> {
            EntityDataAccessor<Byte> acc = flags();
            p.connection.send(new ClientboundSetEntityDataPacket(e.getId(), List.of(SynchedEntityData.DataValue.create(acc, e.getEntityData().get(acc)))));
        });
    }

    static void toggled(ServerPlayer p, boolean on) {
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.6, p.getZ(), 20, 0.4, 0.3, 0.4, 0.1);
        if (!on) {
            Set<Integer> shown = SHOWN.remove(p.getUUID());
            if (shown != null) for (int id : shown) restore(p, level.getEntity(id));
        }
    }
}

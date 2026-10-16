package com.brightwebs.jaycee.gameplay;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft bindings for working abilities, not registrations for unresolved catalog concepts. */
final class AbilityExecutors {
    static final AbilityEngine.Definition<Cast> SOUL_RESTORE = new AbilityEngine.Definition<>(
        "jaycee:soul_restore", List.of(
            new AbilityEngine.Mode<>("Restore", 20, 200,
                cast -> cast.state.soul() < cast.state.maxSoul(),
                cast -> cast.data.putDouble("Soul", Math.min(cast.state.maxSoul(), cast.state.soul() + 30))),
            new AbilityEngine.Mode<>("Fortify", 20, 200,
                cast -> cast.state.barrier() < cast.state.maxBarrier(),
                cast -> cast.data.putDouble("Barrier", Math.min(cast.state.maxBarrier(), cast.state.barrier() + 30)))
        ));
    private static final AbilityEngine<Cast> ENGINE = new AbilityEngine<>(List.of(SOUL_RESTORE));

    private AbilityExecutors() {}

    static AbilityEngine.Result activate(ServerPlayer player, PlayerState state, String id, int mode, long now) {
        return ENGINE.activate(id, mode, now, new Cast(player, state, now));
    }

    static final class Cast implements AbilityEngine.Context {
        private final ServerPlayer player;
        private final PlayerState state;
        private final long now;
        private CompoundTag data;

        private Cast(ServerPlayer player, PlayerState state, long now) {
            this.player = player;
            this.state = state;
            this.now = now;
        }

        public boolean permitted() {
            return state.unlocked() && player.isAlive() && !player.isSpectator() && state.soul() > 0;
        }

        public long cooldownExpiry(String abilityId) { return state.cooldownExpiry(abilityId); }

        public boolean consumeMana(double amount) {
            var mana = ManaBackends.resolve(player).mana();
            try {
                if (mana.getCurrent() < amount || !mana.consume(amount)) return false;
            } catch (RuntimeException ignored) { return false; }
            // Standalone consumption changes saved NBT; never overwrite it with the pre-cast snapshot.
            data = PlayerState.read(player);
            return true;
        }

        public void complete(String abilityId, int mode, long cooldownExpiry) {
            var cooldowns = data.getCompound("Cooldowns");
            cooldowns.putLong(abilityId, cooldownExpiry);
            data.put("Cooldowns", cooldowns);
            data.putString("LastActivationId", abilityId);
            data.putLong("LastActivationTick", now);
            data.putInt("LastActivationMode", mode);
            PlayerState.save(player, data);
            GameplayNetwork.sync(player);
        }
    }
}

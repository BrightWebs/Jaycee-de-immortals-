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
    private static AbilityEngine<Cast> engine() {
        var definitions = new java.util.ArrayList<AbilityEngine.Definition<Cast>>();
        definitions.add(SOUL_RESTORE);
        for (var skill : SkillDefinitions.ALL) {
            if (!skill.intrinsic()) continue;
            var modes = new java.util.ArrayList<AbilityEngine.Mode<Cast>>();
            for (int index = 0; index < skill.modes().size(); index++) {
                final int mode = index;
                modes.add(new AbilityEngine.Mode<>(skill.modes().get(index), skill.activationMana(), 20,
                    cast -> SkillRuntime.canActivate(cast.player, cast.state, skill.id(), mode),
                    cast -> {
                        SkillRuntime.setEnabled(cast.data, skill.id(), true);
                        SkillRuntime.configureMode(cast.data, skill.id(), mode);
                    }));
            }
            definitions.add(new AbilityEngine.Definition<>(skill.id(), modes));
        }
        return new AbilityEngine<>(definitions);
    }

    private AbilityExecutors() {}

    static AbilityEngine.Result activate(ServerPlayer player, PlayerState state, String id, int mode, long now) {
        if (SkillDefinitions.intrinsic(id) && state.skillEnabled(id)) {
            var data = PlayerState.read(player);
            SkillRuntime.setEnabled(data, id, false);
            PlayerState.save(player, data);
            SkillRuntime.stop(player, id);
            GameplayNetwork.sync(player);
            return AbilityEngine.Result.ACTIVATED;
        }
        return engine().activate(id, mode, now, new Cast(player, state, now));
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
            if (amount == 0) {
                data = PlayerState.read(player);
                return true;
            }
            amount *= 1.0 - EssenceGrowth.efficiency(player);
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
            SkillRuntime.effectiveCast(player, abilityId);
            GameplayNetwork.sync(player);
        }
    }
}

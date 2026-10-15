package com.brightwebs.jaycee.gameplay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional integrations register real adapters during common setup; no optional mod classes are linked here. */
public final class ManaBackends {
    public static final String AUTO = "auto";
    public static final String STANDALONE = "standalone";
    public static final String IRON = "irons_spellbooks";
    public static final String ARS = "ars_nouveau";
    private record Adapter(Predicate<ServerPlayer> available, Function<ServerPlayer, ManaBackend> factory) {}
    public record Resolved(String id, ManaBackend mana) {}
    private static final Map<String, Adapter> ADAPTERS = new LinkedHashMap<>();
    private static final java.util.Set<String> WARNED = new java.util.HashSet<>();

    private ManaBackends() {}

    public static synchronized void register(String id, Predicate<ServerPlayer> available,
                                             Function<ServerPlayer, ManaBackend> factory) {
        if (id == null || !id.matches("[a-z0-9_.:-]{1,64}") || STANDALONE.equals(id) || AUTO.equals(id)
                || available == null || factory == null || ADAPTERS.containsKey(id)) {
            throw new IllegalArgumentException("Invalid or duplicate mana backend: " + id);
        }
        ADAPTERS.put(id, new Adapter(available, factory));
    }

    public static synchronized List<String> available(ServerPlayer player) {
        var result = new java.util.ArrayList<String>();
        result.add(AUTO);
        result.add(STANDALONE);
        ADAPTERS.forEach((id, adapter) -> {
            try { if (adapter.available().test(player)) result.add(id); }
            catch (RuntimeException | LinkageError ignored) { /* A missing optional capability leaves standalone usable. */ }
        });
        return List.copyOf(result);
    }

    public static synchronized Resolved resolve(ServerPlayer player) {
        probeOptionalMods();
        String selected = PlayerState.get(player).backend();
        if (AUTO.equals(selected)) {
            for (String id : List.of(IRON, ARS)) {
                Resolved resolved = external(player, id);
                if (resolved != null) return resolved;
            }
        } else {
            Resolved resolved = external(player, selected);
            if (resolved != null) return resolved;
        }
        return new Resolved(STANDALONE, new Standalone(player));
    }

    private static Resolved external(ServerPlayer player, String selected) {
        Adapter adapter = ADAPTERS.get(selected);
        if (adapter != null) {
            try {
                if (adapter.available().test(player)) {
                    ManaBackend mana = adapter.factory().apply(player);
                    if (mana != null) {
                        double current = mana.getCurrent();
                        double maximum = mana.getMax();
                        double regen = mana.getRegen();
                        if (Double.isFinite(current) && Double.isFinite(maximum) && current >= 0
                                && maximum > 0 && Double.isFinite(regen) && regen >= 0) {
                            return new Resolved(selected, new Snapshot(mana, Math.min(current, maximum), maximum, regen));
                        }
                    }
                }
            } catch (RuntimeException | LinkageError ignored) { /* Guard third-party adapters, including temporarily absent capabilities. */ }
        }
        return null;
    }

    /** Stable readings keep third-party getters from throwing between resolve and synchronization. */
    private record Snapshot(ManaBackend source, double current, double max, double regen) implements ManaBackend {
        public double getCurrent() { return current; }
        public double getMax() { return max; }
        public double getRegen() { return regen; }
        public boolean consume(double amount) {
            if (!Double.isFinite(amount) || amount < 0 || amount > current) return false;
            try { return source.consume(amount); }
            catch (RuntimeException | LinkageError ignored) { return false; }
        }
    }

    /** Presence is not integration: only a registered, available adapter can own mana. */
    public static synchronized void probeOptionalMods() {
        if (ModList.get().isLoaded(IRON) && !ADAPTERS.containsKey(IRON) && WARNED.add(IRON)) {
            LogUtils.getLogger().warn("Jaycee: Iron's Spells is installed but its mana adapter is disabled: "
                + "no optional API dependency/adapter is bundled. Verified upstream 1.21.1-3.16.3 "
                + "(e4056af90302d37eb1739f5ff05020b020e6e252) targets NeoForge 21.1.200, "
                + "while Jaycee targets 21.1.172. Register a compatible ManaBackends adapter; using fallback.");
        }
        if (ModList.get().isLoaded(ARS) && !ADAPTERS.containsKey(ARS) && WARNED.add(ARS)) {
            LogUtils.getLogger().warn("Jaycee: Ars Nouveau is installed but its mana adapter is disabled: "
                + "no optional API dependency/adapter is bundled. Verified upstream 5.13.3 "
                + "(fd8c9520722bb96a3e75febaf770d7078db5ac43) targets NeoForge 21.1.228, "
                + "while Jaycee targets 21.1.172. Register a compatible ManaBackends adapter; using fallback.");
        }
    }

    private record Standalone(ServerPlayer player) implements ManaBackend {
        public double getCurrent() { return PlayerState.get(player).mana(); }
        public double getMax() { return PlayerState.get(player).maxMana(); }
        public double getRegen() { return PlayerState.get(player).manaRegen(); }
        public boolean consume(double amount) {
            if (!Double.isFinite(amount) || amount < 0 || amount > getCurrent()) return false;
            var data = PlayerState.read(player);
            data.putDouble("Mana", getCurrent() - amount);
            PlayerState.save(player, data);
            return true;
        }
    }
}

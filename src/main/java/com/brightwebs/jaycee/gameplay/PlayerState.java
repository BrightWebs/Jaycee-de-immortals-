package com.brightwebs.jaycee.gameplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** Immutable snapshot. Only the logical server changes the authoritative tag. */
public final class PlayerState {
    public static final String TAG = "JayceeGameplay";
    public static final int PAGES = 10;
    public static final int SLOTS = 4;
    private final CompoundTag data;

    private PlayerState(CompoundTag data) {
        this.data = data.copy();
    }

    public static PlayerState get(Player player) {
        return new PlayerState(read(player));
    }

    static CompoundTag read(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound(TAG).copy();
        if (!data.contains("MaxSoul", Tag.TAG_ANY_NUMERIC)) data.putDouble("MaxSoul", 100);
        if (!data.contains("Soul", Tag.TAG_ANY_NUMERIC)) data.putDouble("Soul", 100);
        if (!data.contains("MaxMana", Tag.TAG_ANY_NUMERIC)) data.putDouble("MaxMana", 100);
        if (!data.contains("Mana", Tag.TAG_ANY_NUMERIC)) data.putDouble("Mana", 100);
        if (!data.contains("ManaRegen", Tag.TAG_ANY_NUMERIC)) data.putDouble("ManaRegen", 2);
        if (!data.contains("Portrait3d")) data.putBoolean("Portrait3d", true);
        if (!data.contains("Backend")) data.putString("Backend", ManaBackends.AUTO);
        if (!data.contains("Consumed")) data.putInt("Consumed", Math.clamp(root.getInt("JayceeConsumed"), 0, 3));
        return data;
    }

    static void save(Player player, CompoundTag data) {
        player.getPersistentData().put(TAG, data.copy());
        player.getPersistentData().remove("JayceeConsumed");
    }

    public double soul() { return bounded(data.getDouble("Soul"), maxSoul()); }
    public double maxSoul() { return maximum(data.getDouble("MaxSoul")); }
    public double barrier() { return bounded(data.getDouble("Barrier"), maxBarrier()); }
    public double maxBarrier() { return maxSoul(); }
    /** Fraction of explicit spiritual damage resisted, from 0 through 1. */
    public double soulResistance() { return bounded(data.getDouble("SoulResistance"), 1); }
    public double mana() { return bounded(data.getDouble("Mana"), maxMana()); }
    public double maxMana() { return maximum(data.getDouble("MaxMana")); }
    /** Mana regenerated per second, not per tick. */
    public double manaRegen() { return bounded(data.getDouble("ManaRegen"), 10000); }
    public String backend() { return data.getString("Backend"); }
    public String effectiveBackend() { return data.contains("EffectiveBackend") ? data.getString("EffectiveBackend") : backend(); }
    public int page() { return Math.clamp(data.getInt("Page"), 0, PAGES - 1); }
    public boolean portrait3d() { return data.getBoolean("Portrait3d"); }
    public int consumed() { return Math.clamp(data.getInt("Consumed"), 0, 3); }
    public boolean unlocked() { return consumed() >= 3; }
    /** Server world time when this snapshot was sent; client world time advances the displayed countdown. */
    public long serverTick() { return data.getLong("ServerTick"); }
    public String lastActivationId() { return data.getString("LastActivationId"); }
    public long lastActivationTick() { return data.getLong("LastActivationTick"); }
    public int lastActivationMode() { return data.getInt("LastActivationMode"); }
    public String ability(int page, int slot) {
        return validSlot(page, slot) ? data.getCompound("Abilities").getString(key(page, slot)) : "";
    }
    public int mode(int page, int slot) {
        if (!validSlot(page, slot)) return 0;
        var art = AbilityCatalog.find(ability(page, slot));
        if (art == null || art.modes().isEmpty()) return 0;
        return Math.clamp(data.getCompound("Modes").getInt(key(page, slot)), 0, art.modes().size() - 1);
    }
    public long cooldownRemaining(String id, long gameTime) {
        long expiry = cooldownExpiry(id);
        return expiry > gameTime ? expiry - gameTime : 0;
    }
    public long cooldownExpiry(String id) { return data.getCompound("Cooldowns").getLong(id); }
    static String key(int page, int slot) { return page + "_" + slot; }
    static boolean validSlot(int page, int slot) { return page >= 0 && page < PAGES && slot >= 0 && slot < SLOTS; }
    static double maximum(double value) { return Double.isFinite(value) ? Math.clamp(value, 1, 1_000_000) : 100; }
    static double bounded(double value, double max) { return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0; }
}

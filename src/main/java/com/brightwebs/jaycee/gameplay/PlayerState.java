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
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            data.put("Divinity", com.brightwebs.jaycee.gameplay.divine.DivinityService.ownMirror(serverPlayer));
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
    /** Operator-granted test identity, not a completed race evolution or EP qualification. */
    public boolean jayceeTest() { return data.getBoolean("JayceeTest"); }
    public boolean playerMenuPreview() { return data.getBoolean("PlayerMenuPreview"); }
    public boolean unlocked() { return consumed() >= 3 || jayceeTest() || supreme()
        || !divinity().getCompound("Grants").isEmpty() || (!divinity().getString("Rank").isEmpty()
            && !divinity().getString("Rank").equals("MORTAL")); }
    public CompoundTag divinity() { return data.getCompound("Divinity").copy(); }
    public boolean supreme() { return data.getCompound("Divinity").getBoolean("Supreme"); }
    public boolean abilityOwned(String id) {
        var art = AbilityCatalog.find(id);
        return art != null && art.implemented() && (supreme() || jayceeTest()
            || data.getCompound("Divinity").getCompound("Grants").contains(id)
            || (!data.getBoolean("DivinityRestoreMigrated") && SkillDefinitions.RESTORE.equals(id) && consumed() >= 3)
            || (!data.getBoolean("DivinityMigrated") && SkillDefinitions.intrinsic(id) && skillExplicitGrant(id)));
    }
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
        if (SkillDefinitions.intrinsic(art.id())) return skillMode(art.id());
        return Math.clamp(data.getCompound("Modes").getInt(key(page, slot)), 0, art.modes().size() - 1);
    }
    public long cooldownRemaining(String id, long gameTime) {
        long expiry = cooldownExpiry(id);
        return expiry > gameTime ? expiry - gameTime : 0;
    }
    public long cooldownExpiry(String id) { return data.getCompound("Cooldowns").getLong(id); }
    public long skillXp(String id) { return SkillMath.capXp(skill(id).getLong("Xp"), skillMaxLevel(), skillXpPerLevel()); }
    public int skillLevel(String id) {
        return SkillMath.level(skillXp(id), skillMaxLevel(), skillXpPerLevel());
    }
    public int skillMaxLevel() { return Math.clamp(data.getInt("SkillMaxLevel") == 0 ? 10 : data.getInt("SkillMaxLevel"), 1, 100); }
    public int skillXpPerLevel() { return Math.clamp(data.getInt("SkillXpPerLevel") == 0 ? 100 : data.getInt("SkillXpPerLevel"), 1, 1000000); }
    public int skillModeLevelStep() { return Math.clamp(data.getInt("SkillModeLevelStep") == 0 ? 3 : data.getInt("SkillModeLevelStep"), 1, 100); }
    public boolean skillExplicitGrant(String id) { return SkillDefinitions.intrinsic(id) && skill(id).getBoolean("Granted"); }
    public boolean skillGranted(String id) {
        var definition = SkillDefinitions.find(id);
        return definition != null && (SkillDefinitions.PHYSICAL.equals(id) || abilityOwned(id));
    }
    public boolean skillEnabled(String id) { return skillGranted(id) && skill(id).getBoolean("Enabled"); }
    public int skillOutput(String id) {
        var value = skill(id);
        return value.contains("Output") ? Math.clamp(value.getInt("Output"), 0, 100) : 100;
    }
    public int skillMode(String id) {
        var d = SkillDefinitions.find(id);
        int mode = skill(id).getInt("Mode");
        return d != null && mode >= 0 && mode < d.modes().size()
            && (!SkillDefinitions.PERCEPTION.equals(id)
                || SkillMath.modeUnlocked(mode, d.modes().size(), skillLevel(id), skillModeLevelStep())) ? mode : 0;
    }
    public String skillStyle(String id) {
        var d = SkillDefinitions.find(id);
        int index = skill(id).getInt("Style");
        return d == null || d.styles().isEmpty() ? "" : d.styles().get(Math.clamp(index, 0, d.styles().size() - 1));
    }
    public double activeDrain() { return bounded(data.getDouble("SkillDrain"), 10000); }
    public double skillActivationMana(String id) {
        if (SkillDefinitions.REGEN.equals(id)) return 0;
        return SkillDefinitions.intrinsic(id) ? tuning("Activation", 2) : SkillDefinitions.RESTORE.equals(id) ? 20 : 0;
    }
    public double skillUpkeepMana(String id) {
        return switch (id) {
            case SkillDefinitions.SPEED -> tuning("SpeedDrain", 2);
            case SkillDefinitions.FLIGHT -> tuning("FlightDrain", 4);
            case SkillDefinitions.PERCEPTION -> tuning("PerceptionDrain", 2);
            case SkillDefinitions.REGEN -> tuning("RegenDrain", 3);
            case SkillDefinitions.RESISTANCE -> tuning("ResistanceDrain", 3);
            default -> 0;
        };
    }
    public String skillEffectSummary(String id) {
        return switch (id) {
            case SkillDefinitions.SPEED -> "Up to +" + Math.round(tuning("SpeedBonus", 1) * 100) + "% movement speed";
            case SkillDefinitions.FLIGHT -> "Flying speed up to " + tuning("FlightSpeed", 0.08) + "; Hover / forward Cruise";
            case SkillDefinitions.PERCEPTION -> "Visible targets within " + tuning("Range", 24) + " blocks; Living mode at level " + skillModeLevelStep();
            case SkillDefinitions.REGEN -> "Up to " + tuning("Heal", 1) + " missing health / second";
            case SkillDefinitions.RESISTANCE -> "Up to " + Math.round(tuning("Resistance", 0.25) * 100) + "% damage reduction";
            case SkillDefinitions.RESTORE -> "20 mana, 200 ticks; +30 SP or barrier";
            default -> "Sprint distance / effective hostile hits";
        };
    }
    private double tuning(String key, double fallback) {
        var tuning = data.getCompound("SkillTuning");
        return tuning.contains(key, Tag.TAG_ANY_NUMERIC) ? bounded(tuning.getDouble(key), 10000) : fallback;
    }
    private CompoundTag skill(String id) { return data.getCompound("Skills").getCompound(id); }
    static String key(int page, int slot) { return page + "_" + slot; }
    static boolean validSlot(int page, int slot) { return page >= 0 && page < PAGES && slot >= 0 && slot < SLOTS; }
    static double maximum(double value) { return Double.isFinite(value) ? Math.clamp(value, 1, 1_000_000) : 100; }
    static double bounded(double value, double max) { return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0; }
}

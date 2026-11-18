package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.brightwebs.brightcore.data.EnergyData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Aura, Mana, Spirit and EP for every living being.
 *
 * Kills give the killer a share of the victim's power, added to max Aura and max Mana:
 * 10% when the victim is at least as strong, falling with the gap to a floor of 1%
 * (Manas Reborn-style diminishing returns, so weak prey stops mattering).
 */
public final class EnergyAPI {
    public static final double BASE_GAIN = 0.01, MAX_GAIN = 0.10;

    /** Starting energy for creatures that aren't players: {aura, mana}. Content mods add entries. */
    private static final Map<EntityType<?>, double[]> CREATURE_ENERGY = new HashMap<>();
    /** Fallback for creatures with no entry, from their max health. */
    private static Function<LivingEntity, double[]> fallback = e -> {
        double hp = e.getMaxHealth();
        return new double[]{hp * 40, hp * 10};
    };

    private EnergyAPI() {}

    /** Sets a creature type's starting Aura and Mana. */
    public static void setCreatureEnergy(EntityType<?> type, double aura, double mana) {
        CREATURE_ENERGY.put(type, new double[]{aura, mana});
    }

    public static void setFallback(Function<LivingEntity, double[]> f) {
        fallback = f;
    }

    // ---- reading ---------------------------------------------------------------

    /** The being's energy, rolling a creature's starting values the first time (server side). */
    public static EnergyData get(LivingEntity entity) {
        EnergyData data = entity.getData(BrightAttachments.ENERGY);
        if (!data.initialized() && !(entity instanceof Player) && !entity.level().isClientSide()) {
            double[] base = CREATURE_ENERGY.getOrDefault(entity.getType(), null);
            if (base == null) base = fallback.apply(entity);
            // +-15% so no two wolves are identical
            double roll = 0.85 + entity.getRandom().nextDouble() * 0.3;
            double spirit = Math.max(10, entity.getMaxHealth() * 5);
            data = EnergyData.EMPTY.withMax(base[0] * roll, base[1] * roll, spirit).refilled();
            entity.setData(BrightAttachments.ENERGY, data);
        }
        return data;
    }

    public static double ep(LivingEntity entity) {
        return get(entity).ep();
    }

    public static void set(LivingEntity entity, EnergyData data) {
        entity.setData(BrightAttachments.ENERGY, data);
    }

    // ---- changing --------------------------------------------------------------

    public static boolean spendMana(LivingEntity entity, double amount) {
        if (amount <= 0) return true;
        EnergyData d = get(entity);
        if (d.mana() < amount) return false;
        set(entity, d.withMana(d.mana() - amount));
        return true;
    }

    public static boolean spendAura(LivingEntity entity, double amount) {
        if (amount <= 0) return true;
        EnergyData d = get(entity);
        if (d.aura() < amount) return false;
        set(entity, d.withAura(d.aura() - amount));
        return true;
    }

    /** Rolls a fresh body for a new race: Aura and Mana within its ranges, full pools. */
    public static void rollFor(LivingEntity entity, Race race) {
        var r = entity.getRandom();
        double aura = race.minAura() + r.nextDouble() * (race.maxAura() - race.minAura());
        double mana = race.minMana() + r.nextDouble() * (race.maxMana() - race.minMana());
        set(entity, EnergyData.EMPTY.withMax(aura, mana, race.spirit()).refilled());
    }

    /** On evolving: pools rise to at least the new race's floor, never fall, then refill. */
    public static void raiseFor(LivingEntity entity, Race race) {
        EnergyData d = get(entity);
        var r = entity.getRandom();
        double aura = Math.max(d.maxAura(), race.minAura() + r.nextDouble() * (race.maxAura() - race.minAura()));
        double mana = Math.max(d.maxMana(), race.minMana() + r.nextDouble() * (race.maxMana() - race.minMana()));
        set(entity, d.withMax(aura, mana, Math.max(d.maxSpirit(), race.spirit())).refilled());
    }

    /**
     * The killer takes a share of the victim's power.
     * @return the EP gained
     */
    public static double absorbKill(LivingEntity killer, LivingEntity victim, Alignment alignment) {
        EnergyData k = get(killer), v = get(victim);
        double ratio = v.ep() / Math.max(1, k.ep());
        double share = Math.clamp(MAX_GAIN * ratio, BASE_GAIN, MAX_GAIN) * alignment.gainMultiplier();
        double gainAura = v.maxAura() * share, gainMana = v.maxMana() * share;
        EnergyData grown = k.withMax(k.maxAura() + gainAura, k.maxMana() + gainMana, k.maxSpirit());
        set(killer, grown.withAura(grown.aura() + gainAura).withMana(grown.mana() + gainMana));
        return gainAura + gainMana;
    }

    // ---- display ---------------------------------------------------------------

    /** 950, 12.4k, 3.10M, 1.00B */
    public static String format(double v) {
        if (v < 1_000) return String.valueOf(Math.round(v));
        if (v < 1_000_000) return String.format(Locale.ROOT, "%.1fk", v / 1_000);
        if (v < 1_000_000_000) return String.format(Locale.ROOT, "%.2fM", v / 1_000_000);
        return String.format(Locale.ROOT, "%.2fB", v / 1_000_000_000);
    }
}

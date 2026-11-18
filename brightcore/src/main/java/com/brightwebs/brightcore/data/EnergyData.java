package com.brightwebs.brightcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A being's energy. EP (Existence Points) = max Aura + max Mana.
 *
 * Aura is life-force (it backs physical power), Mana is magical energy (it pays for abilities),
 * and Spirit is spiritual health, a second health pool for attacks on the soul.
 *
 * Immutable: changes go through EnergyAPI so they save, and sync to the player who owns them.
 *
 * @param initialized false until the being's starting values are rolled
 */
public record EnergyData(double aura, double maxAura,
                         double mana, double maxMana,
                         double spirit, double maxSpirit,
                         boolean initialized) {

    /** Every being's EP is clamped between these. */
    public static final double MIN_POOL = 10, MAX_POOL = 1.0E9;

    public static final EnergyData EMPTY = new EnergyData(0, 0, 0, 0, 0, 0, false);

    public static final Codec<EnergyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("aura", 0.0).forGetter(EnergyData::aura),
            Codec.DOUBLE.optionalFieldOf("max_aura", 0.0).forGetter(EnergyData::maxAura),
            Codec.DOUBLE.optionalFieldOf("mana", 0.0).forGetter(EnergyData::mana),
            Codec.DOUBLE.optionalFieldOf("max_mana", 0.0).forGetter(EnergyData::maxMana),
            Codec.DOUBLE.optionalFieldOf("spirit", 0.0).forGetter(EnergyData::spirit),
            Codec.DOUBLE.optionalFieldOf("max_spirit", 0.0).forGetter(EnergyData::maxSpirit),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(EnergyData::initialized)
    ).apply(i, EnergyData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public EnergyData {
        maxAura = Math.clamp(maxAura, 0, MAX_POOL);
        maxMana = Math.clamp(maxMana, 0, MAX_POOL);
        maxSpirit = Math.max(0, maxSpirit);
        aura = Math.clamp(aura, 0, maxAura);
        mana = Math.clamp(mana, 0, maxMana);
        spirit = Math.clamp(spirit, 0, maxSpirit);
    }

    /** Existence Points: the being's total power. */
    public double ep() { return maxAura + maxMana; }

    public EnergyData withAura(double v) { return new EnergyData(v, maxAura, mana, maxMana, spirit, maxSpirit, initialized); }
    public EnergyData withMana(double v) { return new EnergyData(aura, maxAura, v, maxMana, spirit, maxSpirit, initialized); }
    public EnergyData withSpirit(double v) { return new EnergyData(aura, maxAura, mana, maxMana, v, maxSpirit, initialized); }

    /** Sets the maximums; current values are kept (and clamped). */
    public EnergyData withMax(double maxAura, double maxMana, double maxSpirit) {
        return new EnergyData(aura, maxAura, mana, maxMana, spirit, maxSpirit, true);
    }

    /** Everything full. */
    public EnergyData refilled() { return new EnergyData(maxAura, maxAura, maxMana, maxMana, maxSpirit, maxSpirit, initialized); }
}

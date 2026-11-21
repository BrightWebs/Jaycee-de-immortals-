package com.brightwebs.brightcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// energy. EP = max aura + max mana
// aura is life force (physical power), mana is magic energy (pays for skills),
// spirit is spiritual health, a second health bar for attacks on the soul
// go through EnergyAPI to change it so it saves and syncs
// initialized is false until the starting numbers get rolled
public record EnergyData(double aura, double maxAura,
                         double mana, double maxMana,
                         double spirit, double maxSpirit,
                         boolean initialized) {

    // EP always stays between these
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

    // EP, your total power
    public double ep() { return maxAura + maxMana; }

    public EnergyData withAura(double v) { return new EnergyData(v, maxAura, mana, maxMana, spirit, maxSpirit, initialized); }
    public EnergyData withMana(double v) { return new EnergyData(aura, maxAura, v, maxMana, spirit, maxSpirit, initialized); }
    public EnergyData withSpirit(double v) { return new EnergyData(aura, maxAura, mana, maxMana, v, maxSpirit, initialized); }

    // sets the maxes, keeps the current values (clamped)
    public EnergyData withMax(double maxAura, double maxMana, double maxSpirit) {
        return new EnergyData(aura, maxAura, mana, maxMana, spirit, maxSpirit, true);
    }

    // everything full
    public EnergyData refilled() { return new EnergyData(maxAura, maxAura, maxMana, maxMana, maxSpirit, maxSpirit, initialized); }
}

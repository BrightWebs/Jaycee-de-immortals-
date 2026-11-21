package com.brightwebs.brightcore.fx;

import com.brightwebs.brightcore.BrightCore;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

// bright core's particles, one set per element or effect. every skill's visuals use these (see Fx)
// textures are in assets/brightcore/textures/particle, how they move is in FxParticle
public final class BrightParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, BrightCore.ID);
    public static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> ALL = new LinkedHashMap<>();

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType>
            FLAME = reg("flame"), BLACK_FLAME = reg("black_flame"), EMBER = reg("ember"), FROST = reg("frost"),
            SPARK = reg("spark"), DARK_SPARK = reg("dark_spark"), SHOCKWAVE = reg("shockwave"), SOUL = reg("soul"),
            SPIRIT = reg("spirit"), MIST = reg("mist"), HEAL = reg("heal"), RUNE = reg("rune"), AURA = reg("aura"),
            HOLY = reg("holy"), VOID = reg("void"), BUBBLE = reg("bubble"), GUST = reg("gust"), PETAL = reg("petal"),
            SHARD = reg("shard"), BLOOD = reg("blood"), DROP = reg("drop"), ACID = reg("acid"), SLASH = reg("slash"),
            PREDATOR_MIST = reg("predator_mist");

    private BrightParticles() {}

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> reg(String name) {
        DeferredHolder<ParticleType<?>, SimpleParticleType> h = TYPES.register(name, () -> new SimpleParticleType(false));
        ALL.put(name, h);
        return h;
    }

    public static void init(IEventBus modBus) { TYPES.register(modBus); }
}

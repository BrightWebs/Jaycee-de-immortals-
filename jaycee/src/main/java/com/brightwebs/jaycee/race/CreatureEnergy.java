package com.brightwebs.jaycee.race;

import com.brightwebs.brightcore.api.EnergyAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Starting Aura and Mana for creatures that would be undervalued by the default
 * (max health x 40 Aura, x 10 Mana): bosses, and creatures known for magic.
 * A fresh player starts around 1,000 EP; a zombie is about the same.
 */
public final class CreatureEnergy {
    private CreatureEnergy() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, e -> e.enqueueWork(CreatureEnergy::apply));
    }

    private static void apply() {
        set("witch", 1_200, 2_500);
        set("evoker", 4_000, 9_000);
        set("enderman", 4_000, 2_000);
        set("blaze", 3_000, 3_000);
        set("ravager", 20_000, 2_000);
        set("piglin_brute", 12_000, 1_000);
        set("wither_skeleton", 8_000, 2_000);
        set("elder_guardian", 80_000, 40_000);
        set("warden", 600_000, 150_000);
        set("wither", 900_000, 600_000);
        set("ender_dragon", 2_500_000, 1_500_000);
    }

    private static void set(String vanillaId, double aura, double mana) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(vanillaId))
                .ifPresent(type -> EnergyAPI.setCreatureEnergy(type, aura, mana));
    }
}

package com.brightwebs.brightcore.api.race;

import com.brightwebs.brightcore.BrightCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The race registry. Races get ids like "jaycee:human". Synced so the client can show the race menu. */
public final class RaceRegistry {
    public static final ResourceKey<Registry<Race>> KEY = ResourceKey.createRegistryKey(BrightCore.id("races"));
    public static final Registry<Race> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();

    private RaceRegistry() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> event.register(REGISTRY));
    }

    public static Race get(Identifier id) {
        return id == null ? null : REGISTRY.getValue(id);
    }

    /** Races offered on first join, in a stable order. */
    public static List<Race> starting() {
        List<Race> list = new ArrayList<>();
        for (Race r : REGISTRY) if (r.isStarting()) list.add(r);
        list.sort(Comparator.comparing(r -> r.id().toString()));
        return list;
    }
}

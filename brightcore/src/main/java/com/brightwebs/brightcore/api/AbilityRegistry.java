package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.BrightCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The ability registry. Abilities get ids like "jaycee:thunderbolt", just like blocks and items.
 *
 * In a content mod:
 * <pre>
 * DeferredRegister&lt;Ability&gt; ABILITIES = DeferredRegister.create(AbilityRegistry.REGISTRY, "jaycee");
 * DeferredHolder&lt;Ability, Ability&gt; THUNDERBOLT = ABILITIES.register("thunderbolt", Thunderbolt::new);
 * </pre>
 */
public final class AbilityRegistry {
    public static final ResourceKey<Registry<Ability>> KEY = ResourceKey.createRegistryKey(BrightCore.id("abilities"));

    // sync(true) keeps numeric ids identical on client and server.
    public static final Registry<Ability> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();

    private AbilityRegistry() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> event.register(REGISTRY));
    }

    /** Null if no mod registered it (for example after an addon was removed). */
    public static Ability get(Identifier id) {
        return id == null ? null : REGISTRY.getValue(id);
    }
}

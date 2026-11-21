package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.BrightCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

// the skill registry. ids look like "jaycee:thunderbolt", same as blocks and items
//
// in a content mod:
//   DeferredRegister<Ability> ABILITIES = DeferredRegister.create(AbilityRegistry.REGISTRY, "jaycee");
//   DeferredHolder<Ability, Ability> THUNDERBOLT = ABILITIES.register("thunderbolt", Thunderbolt::new);
public final class AbilityRegistry {
    public static final ResourceKey<Registry<Ability>> KEY = ResourceKey.createRegistryKey(BrightCore.id("abilities"));

    // sync(true) so the number ids match on client and server
    public static final Registry<Ability> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();

    private AbilityRegistry() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> event.register(REGISTRY));
    }

    // null if nothing registered it (like if an addon got removed)
    public static Ability get(Identifier id) {
        return id == null ? null : REGISTRY.getValue(id);
    }
}

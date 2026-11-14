package com.brightwebs.brightcore;

import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Bright Core: the framework Jaycee is built on.
 *
 * It owns the systems every content mod needs and nothing content-specific:
 *   api/      Ability, the registry, AbilityAPI (all rules) and events
 *   data/     per-player ability data: owned abilities, 10 pages x 5 slots, cooldowns, mana
 *   network/  the client only ever says "I pressed slot N" or "switch page"; the server decides
 *   client/   ability keys and the bottom-right ability bar (its look can be replaced)
 *   command/  /ability for testing and admins
 */
@Mod(BrightCore.ID)
public final class BrightCore {
    public static final String ID = "brightcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrightCore(IEventBus modBus) {
        AbilityRegistry.init(modBus);
        BrightAttachments.init(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}

package com.brightwebs.brightcore;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Bright Core: the framework Jaycee is built on.
 *
 * It owns the systems every content mod needs and nothing content-specific:
 * the ability registry, per-player ability data, syncing, activation and the ability bar.
 */
@Mod(BrightCore.ID)
public final class BrightCore {
    public static final String ID = "brightcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrightCore(IEventBus modBus) {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}

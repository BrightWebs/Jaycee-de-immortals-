package com.brightwebs.jaycee;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Jaycee: Di Immortales.
 *
 * Only the interface remains for now (see the client package). Gameplay was
 * cleared out so the Ichor system can be built on a clean base.
 */
@Mod(JayceeMod.ID)
public final class JayceeMod {
    public static final String ID = "jaycee";

    public JayceeMod(IEventBus bus) {
    }
}

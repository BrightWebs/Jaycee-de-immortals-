package com.brightwebs.jaycee;

import com.brightwebs.jaycee.ability.TestAbilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Jaycee: Di Immortales. Built on Bright Core.
 *
 * Bright Core supplies the ability system and ability bar; this mod supplies the content:
 * the menu, the ability bar's look, and (step by step) the Ichor system, realms and beings.
 */
@Mod(JayceeMod.ID)
public final class JayceeMod {
    public static final String ID = "jaycee";

    public JayceeMod(IEventBus bus) {
        TestAbilities.init(bus);
    }
}

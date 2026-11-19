package com.brightwebs.brightcore;

import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.race.RaceRegistry;
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
 *   api/race/ races: forms, evolutions, alignment, body stats
 *   data/     abilities (10 pages x 5 slots), energy (Aura, Mana, Spirit = EP) and race, per entity
 *   network/  the client only ever says "I pressed slot N" or "switch page"; the server decides
 *   client/   ability keys and the bottom-right ability bar (its look can be replaced)
 *   command/  /<root> ability | race | ep, for testing and admins (root set by the content mod)
 */
@Mod(BrightCore.ID)
public final class BrightCore {
    public static final String ID = "brightcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrightCore(IEventBus modBus) {
        AbilityRegistry.init(modBus);
        com.brightwebs.brightcore.fx.BrightParticles.init(modBus);
        RaceRegistry.init(modBus);
        BrightAttachments.init(modBus);
        com.brightwebs.brightcore.api.skill.BrightSkills.init(modBus);
    }

    private static String commandRoot = ID;

    /** The root for Bright Core's commands: /<root> ability|race|ep. Content mods set their own. */
    public static String commandRoot() { return commandRoot; }

    /** Call from your mod's constructor, before commands are registered. */
    public static void setCommandRoot(String root) { commandRoot = root; }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}

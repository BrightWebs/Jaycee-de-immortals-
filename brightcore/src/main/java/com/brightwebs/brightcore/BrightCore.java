package com.brightwebs.brightcore;

import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// Bright Core, the base that Jaycee runs on.
// It has all the general systems (skills, races, energy) but no actual content:
//   api/      skills, the registry, AbilityAPI and events
//   api/race/ races, evolutions, alignment, body stats
//   data/     what each entity has: skills (10 pages of 5), energy (aura + mana = EP), race
//   network/  client only says "i pressed slot N" or "next page", server decides the rest
//   client/   the skill keys and the default skill bar
//   command/  /<root> ability | race | ep for testing
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

    // command root, so it's /<root> ability|race|ep. the content mod picks the name
    public static String commandRoot() { return commandRoot; }

    // call this from the mod constructor before commands get registered
    public static void setCommandRoot(String root) { commandRoot = root; }

    // the basic test commands (ability, race, ep). a content mod with its own commands turns these off
    private static boolean legacyCommands = true;
    public static boolean legacyCommands() { return legacyCommands; }
    public static void setLegacyCommands(boolean on) { legacyCommands = on; }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}

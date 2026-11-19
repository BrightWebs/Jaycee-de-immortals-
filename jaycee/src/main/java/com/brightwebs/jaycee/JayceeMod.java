package com.brightwebs.jaycee;

import com.brightwebs.jaycee.skill.JayceeSkills;
import com.brightwebs.jaycee.ichor.IchorAPI;
import com.brightwebs.jaycee.race.JayceeRaces;
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
        // Every cheat command lives under /jaycee.
        com.brightwebs.brightcore.BrightCore.setCommandRoot(ID);
        JayceeSkills.init(bus);
        IchorAPI.init(bus);
        JayceeRaces.init(bus);
        com.brightwebs.jaycee.race.RaceSkills.init();
        com.brightwebs.jaycee.race.TensuraRequirements.registerChecks();
        com.brightwebs.jaycee.status.StatusData.init(bus);
        com.brightwebs.jaycee.skill.Stomach.init(bus);
        com.brightwebs.jaycee.skill.Refining.init(bus);
        com.brightwebs.jaycee.training.Training.init(bus);
        com.brightwebs.jaycee.race.CreatureEnergy.init(bus);
        com.brightwebs.jaycee.bonds.Bonds.init(bus);
    }
}

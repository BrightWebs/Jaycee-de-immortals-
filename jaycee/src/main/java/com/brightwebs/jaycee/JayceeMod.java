package com.brightwebs.jaycee;

import com.brightwebs.jaycee.skill.JayceeSkills;
import com.brightwebs.jaycee.ichor.IchorAPI;
import com.brightwebs.jaycee.race.JayceeRaces;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// Jaycee: Di Immortales, built on Bright Core
// bright core does the skill system and skill bar, this mod is the actual content:
// the menu, how the bar looks, and slowly the ichor system, realms and beings
@Mod(JayceeMod.ID)
public final class JayceeMod {
    public static final String ID = "jaycee";

    public JayceeMod(IEventBus bus) {
        // all the cheat commands go under /jaycee
        com.brightwebs.brightcore.BrightCore.setCommandRoot(ID);
        com.brightwebs.brightcore.BrightCore.setLegacyCommands(false);  // we use the tensura style ones in command/JayceeCommands
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
        com.brightwebs.jaycee.appearance.Appearance.init(bus);
    }
}

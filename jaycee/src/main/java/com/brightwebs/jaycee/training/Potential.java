package com.brightwebs.jaycee.training;

import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.jaycee.race.JayceeRaces;
import net.minecraft.world.entity.player.Player;

/**
 * Racial potential: how talented a race is at each attribute (a "talent" of 1-9, shown as a
 * growth grade F..SSS), the base level cap that talent gives, and how evolution raises that cap.
 */
public final class Potential {
    private static final String[] GRADES = {"F", "F", "E", "D", "C", "B", "A", "S", "SS", "SSS"};

    private Potential() {}

    public static int talent(String line, Attr a) {
        JayceeRaces.Info i = JayceeRaces.INFO.get(line);
        if (i == null) return 3;
        int str = i.strength(), spd = i.speed(), mag = i.magic(), aura = i.aura();
        boolean aquatic = line.equals("merfolk"), reptile = line.equals("lizardman");
        boolean undead = i.group().equals("undead"), spirit = i.group().equals("spiritual");
        int t = switch (a) {
            case PHYSICAL_STRENGTH -> str;
            case SPEED -> spd;
            case AGILITY -> line.equals("giant") || line.equals("zombie") ? spd - 1 : spd;
            case ENDURANCE -> (str + aura + 1) / 2;
            case VITALITY -> Math.round(2 + 3 * i.health());
            case LUNG_CAPACITY -> aquatic || undead ? 9 : reptile ? 6 : 3;
            case SWIMMING_SPEED -> aquatic ? 9 : reptile ? 6 : 3;
            case PERCEPTION -> (spd + mag) / 2 + (line.equals("elf") || line.equals("beastfolk") || line.equals("direwolf") ? 2 : 0);
            case TARGETING -> (spd + mag) / 2 + (line.equals("elf") ? 2 : 0);
            case ARCHERY -> line.equals("elf") ? 7 : line.equals("halfling") ? 5 : (spd + 1) / 2;
            case COMBAT_MASTERY -> (str + spd) / 2 + (line.equals("ogre") || line.equals("lizardman") ? 1 : 0);
            case MAGIC_CONTROL -> mag;
            case AURA_CONTROL -> aura;
            case MENTAL_FORTITUDE -> (mag + aura) / 2 + (spirit ? 2 : 0);
            case REGENERATION -> Math.round(1 + 3 * i.health()) + (line.equals("vampire") || line.equals("slime") ? 3 : 0);
        };
        return Math.max(1, Math.min(9, t));
    }

    public static String grade(int talent) { return GRADES[Math.max(0, Math.min(9, talent))]; }

    /** XP multiplier from talent: F-grade trains at 55%, SSS at 175%. */
    public static double growth(int talent) { return 0.4 + 0.15 * talent; }

    public static int baseLimit(int talent) { return 10 + 8 * talent; }

    /** Evolution raises the cap; divine forms raise it further. */
    public static int limit(int talent, int tier, boolean divine) {
        return (int) Math.round(baseLimit(talent) * (1 + 0.6 * (tier - 1)) * (divine ? 1.5 : 1));
    }

    public static String line(Player p) {
        return RaceAPI.race(p).map(r -> JayceeRaces.lineOf(r.id().getPath())).orElse("human");
    }

    public static int limit(Player p, Attr a) {
        int t = talent(line(p), a);
        Race r = RaceAPI.race(p).orElse(null);
        return r == null ? baseLimit(t) : limit(t, r.tier(), r.isDivine());
    }

    /** EXP needed to go from this level to the next. */
    public static long need(int level) { return Math.round(60 * Math.pow(level + 1, 1.6)); }
}

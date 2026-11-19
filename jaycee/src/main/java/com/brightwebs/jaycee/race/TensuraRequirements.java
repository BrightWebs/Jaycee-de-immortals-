package com.brightwebs.jaycee.race;

import com.brightwebs.brightcore.api.race.Race.Requirement;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.jaycee.status.DemonLord;
import com.brightwebs.jaycee.status.StatusData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evolution requirements taken from Tensura: Reincarnated 2.0.1.4 (each race's
 * getEvolutionRequirements and its config numbers). Parts are weighted; 100% opens the evolution.
 * Tensura's own drop items don't exist here, so each is stood in for by a fitting Minecraft item:
 *   Dragon Essence -> Dragon's Breath, Elemental Essence -> Breeze Rod, Daemon Essence -> Ghast Tear,
 *   Royal Blood -> Golden Apple, Zane Blood -> Wither Rose, Magic Ore -> Amethyst Shard.
 * Bosses are the Wither, Ender Dragon, Elder Guardian and Warden. "Colossus" (the Ogre trial) is the Warden.
 * Our own lines that Tensura doesn't have keep their EP gates.
 */
public final class TensuraRequirements {
    private TensuraRequirements() {}

    static Requirement ep(double v, float w) { return new Requirement("ep", "", v, w); }
    static Requirement boss(int n, float w) { return new Requirement("boss", "", n, w); }
    static Requirement eat(String item, int n) { return new Requirement("item_consume", item, n, 100); }

    public static final Map<String, List<Requirement>> BY_TARGET = new HashMap<>();
    static {
        for (String r : List.of("enlightened_human", "enlightened_elf", "enlightened_dwarf", "enlightened_hobgoblin", "enlightened_ogre", "enlightened_merfolk", "beast_lord"))
            BY_TARGET.put(r, List.of(ep(100_000, 100)));
        for (String r : List.of("human_saint", "elf_saint", "dwarf_saint", "merfolk_saint", "hobgoblin_saint", "spirit_beast"))
            BY_TARGET.put(r, List.of(ep(400_000, 50), boss(4, 50)));
        for (String r : List.of("divine_human", "divine_elf", "divine_dwarf", "divine_fish", "divine_oni", "divine_fighter", "divine_boar",
                "divine_beast", "divine_bird", "divine_dragon", "divine_vampire", "divine_skeleton", "divine_giant", "god_slime"))
            BY_TARGET.put(r, List.of(ep(2_000_000, 100)));
        BY_TARGET.put("hobgoblin", List.of(ep(2_000, 100)));
        BY_TARGET.put("high_orc", List.of(ep(5_000, 100)));
        BY_TARGET.put("greater_daemon", List.of(ep(20_000, 100)));
        BY_TARGET.put("arch_daemon", List.of(ep(140_000, 100)));
        BY_TARGET.put("daemon_lord", List.of(new Requirement("awaken", "", 1, 50), new Requirement("named", "", 1, 50), new Requirement("physical", "", 1, 50)));
        BY_TARGET.put("devil_lord", List.of(new Requirement("awaken", "", 1, 50), new Requirement("named", "", 1, 25), new Requirement("physical", "", 1, 25)));
        BY_TARGET.put("orc_lord", List.of(eat("minecraft:golden_apple", 10)));
        BY_TARGET.put("orc_disaster", List.of(ep(200_000, 50), boss(1, 50))); // Tensura: + the Starved skill
        for (String r : List.of("spirit_boar", "vampire_lord", "spirit_oni", "death_oni")) BY_TARGET.put(r, List.of(ep(400_000, 100)));
        BY_TARGET.put("vampire_overcomer", List.of(ep(150_000, 100)));
        BY_TARGET.put("wight_king", List.of(ep(200_000, 100)));
        BY_TARGET.put("harpy_queen", List.of(ep(200_000, 100)));
        for (String r : List.of("spirit_skeleton", "spirit_bird", "true_dragonewt")) BY_TARGET.put(r, List.of(ep(800_000, 100)));
        BY_TARGET.put("dragonewt", List.of(eat("minecraft:dragon_breath", 10)));
        BY_TARGET.put("metal_slime", List.of(eat("minecraft:amethyst_shard", 100)));
        BY_TARGET.put("demon_slime", List.of(new Requirement("awaken", "", 1, 100)));
        BY_TARGET.put("ancient_giant", List.of(ep(300_000, 50), new Requirement("item_carry", "minecraft:ancient_debris", 20, 50)));
        BY_TARGET.put("mystic_oni", List.of(eat("minecraft:breeze_rod", 10)));
        BY_TARGET.put("wicked_oni", List.of(eat("minecraft:ghast_tear", 10)));
        BY_TARGET.put("vampire", List.of(eat("minecraft:wither_rose", 1)));
        BY_TARGET.put("ogre", List.of(new Requirement("colossus", "", 1, 100)));
        BY_TARGET.put("kijin", List.of(new Requirement("named", "", 1, 100)));
    }

    /** Requirement types Jaycee answers for. */
    public static void registerChecks() {
        RaceAPI.registerRequirement("boss", (p, r) -> (float) (StatusData.counter(p, "bosses_killed") / Math.max(1, r.amount())));
        RaceAPI.registerRequirement("awaken", (p, r) -> DemonLord.isTrue(p) ? 1 : 0);
        RaceAPI.registerRequirement("named", (p, r) -> StatusData.title(p).isEmpty() ? 0 : 1);
        RaceAPI.registerRequirement("physical", (p, r) -> 1);
        RaceAPI.registerRequirement("colossus", (p, r) -> StatusData.counter(p, "warden_killed") > 0 ? 1 : 0);
    }
}

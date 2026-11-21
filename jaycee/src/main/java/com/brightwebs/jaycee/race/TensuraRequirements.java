package com.brightwebs.jaycee.race;

import com.brightwebs.brightcore.api.race.Race.Requirement;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.jaycee.status.DemonLord;
import com.brightwebs.jaycee.status.StatusData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// evolution requirements from tensura 2.0.1.4 (each race's getEvolutionRequirements and its config)
// parts are weighted, 100% means you can evolve
// tensura's own drop items don't exist here so each one uses a minecraft item instead:
//   dragon essence -> dragon's breath, elemental essence -> breeze rod, daemon essence -> ghast tear,
//   royal blood -> golden apple, zane blood -> wither rose, magic ore -> amethyst shard
// bosses are the wither, ender dragon, elder guardian and warden. "colossus" (the ogre trial) is the warden
// our own lines tensura doesn't have just keep their EP gates
public final class TensuraRequirements {
    private TensuraRequirements() {}

    static Requirement ep(double v, float w) { return new Requirement("ep", "", v, w); }
    static Requirement boss(int n, float w) { return new Requirement("boss", "", n, w); }
    static Requirement eat(String item, int n) { return new Requirement("item_consume", item, n, 100); }

    public static final Map<String, List<Requirement>> BY_TARGET = new HashMap<>();
    // tensura's getAwakeningEvolution: where a true demon lord / true hero awakening sends you
    public static final Map<String, String> AWAKENING = map("spirit_boar", "divine_boar", "orc", "spirit_boar", "human_saint", "divine_human", "human", "human_saint", "lesser_daemon", "arch_daemon", "death_oni", "divine_fighter", "ogre", "spirit_oni", "spirit_oni", "divine_oni", "ghoul", "vampire_lord", "vampire_lord", "divine_vampire", "wight", "spirit_skeleton", "spirit_skeleton", "divine_skeleton", "goblin", "hobgoblin_saint", "hobgoblin_saint", "divine_oni", "elf_saint", "divine_elf", "elf", "elf_saint", "harpy", "spirit_bird", "spirit_bird", "divine_bird", "dwarf", "dwarf_saint", "dwarf_saint", "divine_dwarf", "slime", "demon_slime", "spirit_beast", "divine_beast", "beastfolk", "spirit_beast", "giant", "ancient_giant", "ancient_giant", "divine_giant", "merfolk_saint", "divine_fish", "merfolk", "merfolk_saint", "lizardman", "true_dragonewt");
    // tensura's getHarvestFestivalEvolution: what a harvest festival gift or evolve/endow naming gives you
    public static final Map<String, String> HARVEST = map("orc_lord", "orc_disaster", "orc", "high_orc", "human", "enlightened_human", "greater_daemon", "arch_daemon", "lesser_daemon", "greater_daemon", "ogre", "kijin", "kijin", "mystic_oni", "enlightened_ogre", "mystic_oni", "ghoul", "vampire", "wight", "wight_king", "hobgoblin", "enlightened_hobgoblin", "goblin", "hobgoblin", "elf", "enlightened_elf", "harpy", "harpy_queen", "dwarf", "enlightened_dwarf", "beastfolk", "beast_lord", "merfolk", "enlightened_merfolk", "lizardman", "dragonewt");

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return Map.copyOf(m);
    }

    // requirements that depend on where you're coming from ("from>to"), checked before BY_TARGET
    public static final Map<String, List<Requirement>> BY_EDGE = new HashMap<>();
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
        BY_TARGET.put("orc_disaster", List.of(ep(200_000, 50), boss(1, 50)));  // tensura also wants the starved skill
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
        BY_TARGET.put("vampire", List.of(eat("minecraft:wither_rose", 1)));  // tensura: 1 zane blood from a ghoul
        BY_EDGE.put("human>vampire", List.of(eat("minecraft:wither_rose", 5)));  // tensura: 5 zane blood from a human
        // tensura: a wight gets cured back to human like a zombie villager (weakness, resistance,
        // fire resistance, regen II+, absorption IV+, 20% each)
        BY_EDGE.put("wight>human", List.of(new Requirement("cure", "", 1, 100)));
        BY_TARGET.put("ogre", List.of(new Requirement("colossus", "", 1, 100)));
        BY_TARGET.put("kijin", List.of(new Requirement("named", "", 1, 100)));
    }

    // requirement types jaycee handles
    public static void registerChecks() {
        RaceAPI.registerRequirement("boss", (p, r) -> (float) (StatusData.counter(p, "bosses_killed") / Math.max(1, r.amount())));
        RaceAPI.registerRequirement("awaken", (p, r) -> DemonLord.isTrue(p) ? 1 : 0);
        // tensura's NamedRequirement: somebody gave you a name (naming, N)
        RaceAPI.registerRequirement("named", (p, r) -> p.getData(com.brightwebs.jaycee.bonds.Bonds.NAME).isEmpty() ? 0 : 1);
        RaceAPI.registerRequirement("cure", (p, r) -> {
            float c = 0;
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) c += 0.2f;
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE)) c += 0.2f;
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) c += 0.2f;
            var regen = p.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            if (regen != null && regen.getAmplifier() >= 1) c += 0.2f;
            var abs = p.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
            if (abs != null && abs.getAmplifier() >= 3) c += 0.2f;
            return c;
        });
        RaceAPI.registerRequirement("physical", (p, r) -> 1);
        RaceAPI.registerRequirement("colossus", (p, r) -> StatusData.counter(p, "warden_killed") > 0 ? 1 : 0);
    }
}

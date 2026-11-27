package com.brightwebs.jaycee.race;

import java.util.List;
import java.util.Map;

// extra natural (intrinsic) skills per race line, on top of what the generated race table gives.
// kept out of JayceeRaces since that file gets regenerated
public final class RaceExtras {
    private RaceExtras() {}

    private static final Map<String, List<String>> BY_LINE = Map.of(
            "ogre", List.of("ogre_berserker"),
            "vampire", List.of("drain"),
            "lizardman", List.of("scale_armor"),
            "merfolk", List.of("water_breathing"));

    // dragon forms get dragon skin and thunder breath on top of the lizardman stuff
    private static final Map<String, List<String>> BY_RACE = Map.of(
            "dragonewt", List.of("dragon_skin"),
            "true_dragonewt", List.of("dragon_skin", "thunder_breath"),
            "divine_dragon", List.of("dragon_skin", "thunder_breath"));

    public static List<String> intrinsics(String race) {
        List<String> out = new java.util.ArrayList<>(BY_LINE.getOrDefault(JayceeRaces.lineOf(race), List.of()));
        out.addAll(BY_RACE.getOrDefault(race, List.of()));
        return out;
    }
}

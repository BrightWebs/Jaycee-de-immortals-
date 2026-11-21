package com.brightwebs.jaycee.appearance;

import java.util.function.Predicate;

import static com.brightwebs.jaycee.appearance.Features.*;

// race features: ears, tails, horns, wings and the rest. each one only shows for the races
// (and evolutions) that actually have it. stuff a race can't hide yet goes in require()
final class RaceParts {
    private RaceParts() {}

    static final Predicate<String> ELF = line("elf"), DWARF = line("dwarf"), GOBLIN = line("goblin"), OGRE = line("ogre"),
            ORC = line("orc"), LIZARD = line("lizardman"), MERFOLK = line("merfolk"), DAEMON = line("lesser_daemon"),
            ANGEL = line("lesser_angel"), PIXIE = line("pixie"), VAMPIRE = line("vampire"), INSECT = line("insectoid"),
            DIREWOLF = line("direwolf"), ELEMENTAL = line("elemental");
    static final Predicate<String> BEAST_LORDS = races("beast_lord", "spirit_beast", "divine_beast");
    static final Predicate<String> WOLF = races("beastfolk", "wolfkin").or(BEAST_LORDS).or(DIREWOLF);
    static final Predicate<String> FOX = races("foxkin").or(BEAST_LORDS);
    static final Predicate<String> CAT = races("catkin").or(BEAST_LORDS);
    static final Predicate<String> LION = races("lionkin").or(BEAST_LORDS);
    static final Predicate<String> RABBIT = races("rabbitkin").or(BEAST_LORDS);
    static final Predicate<String> HARPY = races("harpy", "harpy_queen", "spirit_bird", "divine_bird");
    static final Predicate<String> DRAGON = races("dragonewt", "true_dragonewt", "divine_dragon");
    static final Predicate<String> HIGH_ANGEL = races("angel", "power", "dominion", "cherub", "seraph");
    static final Predicate<String> DRYAD = races("dryad");

    static void register() {
        // ears
        add("ears", "none", View.HEAD, ANY, null, null, b().done());
        add("ears", "elf", View.FACE, ELF, "#E8C4A8", "#D9A88C", b()
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -10, -22, 0, -0.8f, -0.5f, 3.4f, 1.8f, 1.0f, PRIMARY)
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -10, -22, 3.0f, -1.6f, -0.5f, 1.4f, 0.9f, 1.0f, PRIMARY)
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -10, -22, 0.4f, -0.4f, -0.6f, 2.4f, 1.0f, 0.2f, SECONDARY).mirror().done());
        add("ears", "elf_long", View.FACE, ELF, "#E8C4A8", "#D9A88C", b()
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -12, -32, 0, -0.8f, -0.5f, 5.2f, 1.6f, 1.0f, PRIMARY)
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -12, -32, 4.8f, -1.4f, -0.5f, 1.8f, 0.8f, 1.0f, PRIMARY).mirror().done());
        add("ears", "elf_droop", View.FACE, ELF, "#E8C4A8", "#D9A88C", b()
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -8, 18, 0, -0.8f, -0.5f, 4.4f, 1.6f, 1.0f, PRIMARY).mirror().done());
        add("ears", "goblin", View.FACE, GOBLIN, "#7FA35A", "#5E7E40", b()
                .rot(HEAD, 4.0f, -4.8f, 0.4f, 0, -20, -8, 0, -1.4f, -0.5f, 4.6f, 2.8f, 1.0f, PRIMARY)
                .rot(HEAD, 4.0f, -4.8f, 0.4f, 0, -20, -8, 0.6f, -0.9f, -0.6f, 3.0f, 1.6f, 0.2f, SECONDARY).mirror().done());
        add("ears", "wolf", View.HEAD, WOLF, "#6E6A72", "#E9D9D0", b()
                .box(HEAD, 1.4f, -11.2f, -1.2f, 2.6f, 2.4f, 1.4f, PRIMARY).box(HEAD, 1.9f, -12.6f, -1.2f, 1.6f, 1.4f, 1.4f, PRIMARY)
                .box(HEAD, 1.8f, -10.9f, -1.35f, 1.8f, 1.8f, 0.3f, SECONDARY).mirror().done());
        add("ears", "fox", View.HEAD, FOX, "#E07A2E", "#F4E8DC", b()
                .box(HEAD, 1.2f, -11.6f, -1.2f, 3.2f, 2.8f, 1.4f, PRIMARY).box(HEAD, 1.8f, -13.2f, -1.2f, 2.0f, 1.6f, 1.4f, PRIMARY)
                .box(HEAD, 2.2f, -14.0f, -1.2f, 1.2f, 0.8f, 1.4f, DARK)
                .box(HEAD, 1.7f, -11.3f, -1.35f, 2.2f, 2.2f, 0.3f, SECONDARY).mirror().done());
        add("ears", "cat", View.HEAD, CAT, "#3B3540", "#F2B8C6", b()
                .box(HEAD, 1.4f, -10.4f, -1.0f, 2.6f, 1.6f, 1.2f, PRIMARY).box(HEAD, 1.9f, -11.4f, -1.0f, 1.6f, 1.0f, 1.2f, PRIMARY)
                .box(HEAD, 1.9f, -10.2f, -1.15f, 1.6f, 1.2f, 0.3f, SECONDARY).mirror().done());
        add("ears", "lion", View.HEAD, LION, "#C8943E", "#F0D9A8", b()
                .box(HEAD, 2.4f, -10.0f, -0.4f, 2.2f, 1.6f, 1.2f, PRIMARY).box(HEAD, 2.8f, -9.8f, -0.55f, 1.4f, 1.0f, 0.3f, SECONDARY).mirror().done());
        add("ears", "rabbit", View.HEAD, RABBIT, "#F2EEE8", "#F2B8C6", b()
                .rot(HEAD, 1.8f, -8.6f, 0, -12, 0, 8, -0.9f, -7.0f, -0.6f, 1.8f, 7.0f, 1.2f, PRIMARY)
                .rot(HEAD, 1.8f, -8.6f, 0, -12, 0, 8, -0.5f, -6.4f, -0.75f, 1.0f, 5.6f, 0.3f, SECONDARY).mirror().done());
        add("ears", "rabbit_lop", View.HEAD, RABBIT, "#C9A27A", "#F2B8C6", b()
                .rot(HEAD, 3.8f, -8.2f, 0, 0, 0, 165, -0.9f, -6.0f, -0.6f, 1.8f, 6.0f, 1.2f, PRIMARY).mirror().done());
        add("ears", "pig", View.FACE, ORC, "#7C9A5A", "#5E7A40", b()
                .rot(HEAD, 3.4f, -7.8f, -1.0f, -10, 0, 35, 0, 0, 0, 2.8f, 2.2f, 0.8f, PRIMARY).mirror().done());
        add("ears", "fin", View.FACE, MERFOLK, "#4FB7D9", "#2A7FA8", b()
                .rot(HEAD, 4.2f, -4.6f, 0.0f, 0, 25, -15, 0, -2.6f, -0.2f, 0.4f, 4.2f, 3.6f, PRIMARY)
                .rot(HEAD, 4.2f, -4.6f, 0.0f, 0, 25, -15, 0.1f, -2.2f, 0.2f, 0.4f, 0.4f, 3.4f, SECONDARY)
                .rot(HEAD, 4.2f, -4.6f, 0.0f, 0, 25, -15, 0.1f, -0.6f, 0.2f, 0.4f, 0.4f, 3.0f, SECONDARY).mirror().done());
        add("ears", "antennae", View.HEAD, INSECT, "#3A3F2A", "#C7E05A", b()
                .chain(HEAD, 1.4f, -8.6f, -2.0f, new float[][]{{3, 0.7f, 70, 15, PRIMARY}, {3, 0.6f, 40, 25, PRIMARY}, {0.9f, 1.2f, 20, 25, SECONDARY}}).mirror().done());
        add("ears", "daemon", View.FACE, DAEMON, "#B7A0A8", "#7A5A66", b()
                .rot(HEAD, 4.0f, -4.6f, 0.4f, 0, -15, -30, 0, -0.6f, -0.5f, 3.6f, 1.2f, 0.8f, PRIMARY).mirror().done());

        // tails
        add("tail", "none", View.BACK, ANY, null, null, b().done());
        add("tail", "wolf", View.BACK, WOLF, "#6E6A72", "#E9D9D0", b().chain(BODY, 0, 10.5f, 1.8f, new float[][]{
                {3, 2.4f, -40, 0, PRIMARY}, {4, 3.2f, -50, 0, PRIMARY}, {3, 2.6f, -65, 0, PRIMARY}, {1.6f, 1.6f, -70, 0, SECONDARY}}).done());
        add("tail", "fox", View.BACK, FOX, "#E07A2E", "#F4E8DC", b().chain(BODY, 0, 10.5f, 1.8f, new float[][]{
                {3, 2.8f, -20, 0, PRIMARY}, {4, 4.2f, -35, 0, PRIMARY}, {4, 4.0f, -50, 0, PRIMARY}, {2.4f, 3.0f, -60, 0, SECONDARY}}).done());
        add("tail", "fox_twin", View.BACK, races("foxkin", "spirit_beast", "divine_beast"), "#E07A2E", "#F4E8DC", b().chain(BODY, 1.2f, 10.5f, 1.8f, new float[][]{
                {3, 2.6f, -20, 25, PRIMARY}, {4, 3.6f, -35, 30, PRIMARY}, {3.4f, 3.2f, -50, 30, PRIMARY}, {2, 2.4f, -60, 30, SECONDARY}}).mirror().done());
        add("tail", "cat", View.BACK, CAT, "#3B3540", "#F2B8C6", b().chain(BODY, 0, 10.5f, 1.8f, new float[][]{
                {3, 1.2f, -50, 0, PRIMARY}, {3, 1.2f, -20, 0, PRIMARY}, {3, 1.2f, 20, 0, PRIMARY}, {2.4f, 1.2f, 60, 0, PRIMARY}}).done());
        add("tail", "lion", View.BACK, LION, "#C8943E", "#6A3E1E", b().chain(BODY, 0, 10.5f, 1.8f, new float[][]{
                {3, 1.2f, -45, 0, PRIMARY}, {3, 1.2f, -60, 0, PRIMARY}, {3, 1.2f, -40, 0, PRIMARY}, {2.2f, 2.4f, -20, 0, SECONDARY}}).done());
        add("tail", "rabbit", View.BACK, RABBIT, "#F2EEE8", "#F2EEE8", b().box(BODY, -1.5f, 9.2f, 1.8f, 3, 3, 2.4f, PRIMARY).done());
        add("tail", "reptile", View.BACK, LIZARD, "#4E8A4A", "#C9D98A", b().chain(BODY, 0, 10.0f, 1.6f, new float[][]{
                {4, 3.4f, -35, 0, PRIMARY}, {4, 2.8f, -30, 0, PRIMARY}, {4, 2.2f, -20, 0, PRIMARY}, {3.6f, 1.4f, -10, 0, PRIMARY}}).done());
        add("tail", "dragon", View.BACK, DRAGON, "#3A4E8C", "#C8B07A", b().chain(BODY, 0, 10.0f, 1.6f, new float[][]{
                {4, 3.6f, -30, 0, PRIMARY}, {4.4f, 3.0f, -25, 0, PRIMARY}, {4.4f, 2.4f, -15, 0, PRIMARY}, {4, 1.6f, -5, 0, PRIMARY}, {1.6f, 2.6f, 0, 0, SECONDARY}})
                .rot(BODY, 0, 10.0f, 1.6f, -30, 0, 0, -0.4f, -2.8f, 1.0f, 0.8f, 1.2f, 1.6f, SECONDARY)
                .rot(BODY, 0, 10.0f, 1.6f, -30, 0, 0, -0.4f, -2.8f, 3.0f, 0.8f, 1.0f, 1.4f, SECONDARY).done());
        add("tail", "daemon", View.BACK, DAEMON, "#2A1A2E", "#B0203A", b().chain(BODY, 0, 10.5f, 1.8f, new float[][]{
                {3, 1.0f, -40, 0, PRIMARY}, {3, 1.0f, -15, 0, PRIMARY}, {3, 1.0f, 10, 0, PRIMARY}, {3, 1.0f, 30, 0, PRIMARY}, {1.8f, 2.6f, 30, 0, SECONDARY}}).done());
        add("tail", "fish", View.BODY, MERFOLK, "#2EA8C8", "#7FE0F0", b()
                .box(BODY, -4.3f, 11.4f, -2.3f, 8.6f, 7.0f, 4.6f, PRIMARY)
                .box(BODY, -3.6f, 18.4f, -1.9f, 7.2f, 5.0f, 3.8f, PRIMARY)
                .box(BODY, -2.6f, 23.4f, -1.4f, 5.2f, 2.4f, 2.8f, PRIMARY)
                .box(BODY, -4.4f, 13.0f, -2.4f, 8.8f, 0.8f, 0.2f, SECONDARY).box(BODY, -3.7f, 19.6f, -2.0f, 7.4f, 0.8f, 0.2f, SECONDARY)
                .rot(BODY, 0, 25.4f, 0, 0, 0, 40, -0.4f, 0, -1.2f, 6.0f, 1.0f, 2.4f, SECONDARY)
                .rot(BODY, 0, 25.4f, 0, 0, 0, 140, -0.4f, -1.0f, -1.2f, 6.0f, 1.0f, 2.4f, SECONDARY).done());
        require("merfolk", "tail", "fish");

        // horns
        add("horns", "none", View.HEAD, ANY, null, null, b().done());
        add("horns", "oni_single", View.FACE, OGRE, "#F2E6D0", "#C8B48C", b()
                .box(HEAD, -0.7f, -10.6f, -3.4f, 1.4f, 2.0f, 1.4f, PRIMARY).box(HEAD, -0.45f, -11.8f, -3.2f, 0.9f, 1.2f, 0.9f, SECONDARY).done());
        add("horns", "oni_twin", View.FACE, OGRE, "#F2E6D0", "#C8B48C", b()
                .chain(HEAD, 2.2f, -8.4f, -2.6f, new float[][]{{2, 1.4f, 80, 15, PRIMARY}, {1.4f, 0.9f, 75, 20, SECONDARY}}).mirror().done());
        add("horns", "tusks", View.FACE, OGRE.or(ORC), "#F2E6D0", "#C8B48C", b()
                .box(HEAD, -2.6f, -2.0f, -4.4f, 0.8f, 1.4f, 0.8f, PRIMARY).box(HEAD, 1.8f, -2.0f, -4.4f, 0.8f, 1.4f, 0.8f, PRIMARY)
                .box(HEAD, -2.6f, -2.7f, -4.4f, 0.8f, 0.7f, 0.8f, SECONDARY).box(HEAD, 1.8f, -2.7f, -4.4f, 0.8f, 0.7f, 0.8f, SECONDARY).done());
        add("horns", "demon_curved", View.FACE, DAEMON, "#2A1A2E", "#B0203A", b()
                .chain(HEAD, 2.4f, -8.2f, -1.6f, new float[][]{{2.2f, 1.6f, 60, 20, PRIMARY}, {2.2f, 1.2f, 20, 25, PRIMARY}, {1.8f, 0.8f, -10, 25, SECONDARY}}).mirror().done());
        add("horns", "ram", View.FACE, DAEMON.or(races("greater_daemon", "arch_daemon")), "#5A4A44", "#2A1E1A", b()
                .chain(HEAD, 3.6f, -7.6f, -0.6f, new float[][]{{2, 1.8f, 70, 40, PRIMARY}, {2, 1.6f, 0, 50, PRIMARY}, {2, 1.4f, -70, 30, PRIMARY}, {1.4f, 1.0f, -130, 0, SECONDARY}}).mirror().done());
        add("horns", "dragon", View.FACE, DRAGON, "#2C2A3A", "#C8B07A", b()
                .chain(HEAD, 2.6f, -7.6f, -0.8f, new float[][]{{3, 1.6f, 35, 12, PRIMARY}, {3, 1.2f, 20, 12, PRIMARY}, {2.4f, 0.8f, 10, 10, SECONDARY}}).mirror().done());
        add("horns", "lizard_crest", View.HEAD, LIZARD, "#4E8A4A", "#C9D98A", b()
                .box(HEAD, -0.5f, -9.6f, -3f, 1, 1.2f, 2, PRIMARY).box(HEAD, -0.5f, -9.8f, 0, 1, 1.4f, 2, PRIMARY).box(HEAD, -0.5f, -9.4f, 3, 1, 1.0f, 1.8f, PRIMARY).done());

        // wings
        add("wings", "none", View.BACK, ANY, null, null, b().done());
        add("wings", "angel", View.BACK, ANGEL, "#F8F6F0", "#E2DCD0", feathered(10, 7, 6));
        add("wings", "angel_grand", View.BACK, HIGH_ANGEL, "#F8F6F0", "#F0D890", feathered(13, 10, 8));
        add("wings", "harpy", View.BACK, HARPY, "#8A6A4A", "#D8C4A0", feathered(11, 8, 6));
        add("wings", "bat", View.BACK, DAEMON.or(VAMPIRE), "#2A1A2E", "#5A2A44", membrane(9, 7));
        add("wings", "dragon", View.BACK, DRAGON, "#3A4E8C", "#7A8AC0", membrane(12, 9));
        add("wings", "fairy", View.BACK, PIXIE, "#9FE8FF", "#E8B8FF", b()
                .rot(BODY, 1.0f, 3.0f, 2.2f, 0, -20, -25, 0, -5, 0, 7, 6, 0.2f, GLOW)
                .rot(BODY, 1.0f, 3.0f, 2.2f, 0, -20, -25, 0.6f, -4.4f, -0.05f, 5.8f, 4.8f, 0.3f, SECONDARY)
                .rot(BODY, 1.0f, 5.0f, 2.2f, 0, -20, 20, 0, 0, 0, 5, 4, 0.2f, GLOW).mirror().done());
        require("harpy", "wings", "harpy");

        // other race stuff
        add("race_extra", "none", View.HEAD, ANY, null, null, b().done());
        add("race_extra", "halo", View.HEAD, HIGH_ANGEL, "#FFE27A", "#FFF6C8", halo());
        add("race_extra", "beard_short", View.FACE, DWARF, "#8A5A2E", "#B07A44", b()
                .box(HEAD, -4.2f, -2.2f, -4.6f, 8.4f, 2.8f, 1.0f, PRIMARY).box(HEAD, -4.3f, -4.5f, -4.4f, 1.2f, 2.0f, 4.4f, PRIMARY).box(HEAD, 3.1f, -4.5f, -4.4f, 1.2f, 2.0f, 4.4f, PRIMARY).done());
        add("race_extra", "beard_long", View.FACE, DWARF, "#8A5A2E", "#B07A44", b()
                .box(HEAD, -4.2f, -3.2f, -4.6f, 8.4f, 4.0f, 1.0f, PRIMARY).box(HEAD, -3.4f, 0.8f, -4.8f, 6.8f, 3.6f, 1.2f, PRIMARY)
                .box(HEAD, -2.2f, 4.4f, -4.9f, 4.4f, 2.4f, 1.2f, SECONDARY).box(HEAD, -4.3f, -4.5f, -4.4f, 1.2f, 2.4f, 4.4f, PRIMARY).box(HEAD, 3.1f, -4.5f, -4.4f, 1.2f, 2.4f, 4.4f, PRIMARY).done());
        add("race_extra", "beard_braided", View.FACE, DWARF, "#B0482A", "#E0C060", b()
                .box(HEAD, -4.2f, -3.2f, -4.6f, 8.4f, 3.4f, 1.0f, PRIMARY)
                .box(HEAD, -2.6f, 0.2f, -4.8f, 1.8f, 4, 1.2f, PRIMARY).box(HEAD, 0.8f, 0.2f, -4.8f, 1.8f, 4, 1.2f, PRIMARY)
                .box(HEAD, -2.5f, 2.0f, -4.9f, 1.6f, 0.8f, 1.3f, GOLD).box(HEAD, 0.9f, 2.0f, -4.9f, 1.6f, 0.8f, 1.3f, GOLD).done());
        add("race_extra", "snout", View.FACE, ORC, "#7C9A5A", "#4A3A2E", b()
                .box(HEAD, -1.8f, -4.0f, -5.4f, 3.6f, 2.4f, 1.4f, PRIMARY).box(HEAD, -1.2f, -3.2f, -5.5f, 0.8f, 0.8f, 0.2f, SECONDARY).box(HEAD, 0.4f, -3.2f, -5.5f, 0.8f, 0.8f, 0.2f, SECONDARY).done());
        add("race_extra", "fangs", View.FACE, VAMPIRE.or(DIREWOLF), "#F8F6F0", "#F8F6F0", b()
                .box(HEAD, -1.6f, -2.0f, -4.15f, 0.5f, 0.9f, 0.2f, PRIMARY).box(HEAD, 1.1f, -2.0f, -4.15f, 0.5f, 0.9f, 0.2f, PRIMARY).done());
        add("race_extra", "gills", View.FACE, MERFOLK, "#2A7FA8", "#4FB7D9", b()
                .box(HEAD, 4.0f, -2.6f, -1.6f, 0.2f, 0.4f, 2.4f, PRIMARY).box(HEAD, 4.0f, -1.8f, -1.6f, 0.2f, 0.4f, 2.4f, PRIMARY).box(HEAD, 4.0f, -1.0f, -1.6f, 0.2f, 0.4f, 2.4f, PRIMARY).mirror().done());
        add("race_extra", "mane", View.HEAD, LION, "#9A5A22", "#C8943E", b()
                .box(HEAD, -5.2f, -9.2f, -0.6f, 10.4f, 10.6f, 5.6f, PRIMARY).box(HEAD, -5.6f, -2.6f, -2.0f, 1.2f, 4.0f, 4.0f, SECONDARY).box(HEAD, 4.4f, -2.6f, -2.0f, 1.2f, 4.0f, 4.0f, SECONDARY).done());
        add("race_extra", "scales", View.FACE, LIZARD.or(DRAGON), "#4E8A4A", "#C9D98A", b()
                .box(HEAD, -4.1f, -3.6f, -4.1f, 1.4f, 0.8f, 0.15f, PRIMARY).box(HEAD, -3.6f, -2.8f, -4.1f, 1.0f, 0.8f, 0.15f, PRIMARY)
                .box(HEAD, 2.7f, -3.6f, -4.1f, 1.4f, 0.8f, 0.15f, PRIMARY).box(HEAD, 2.6f, -2.8f, -4.1f, 1.0f, 0.8f, 0.15f, PRIMARY)
                .box(BODY, -4.1f, 3.0f, -2.1f, 1.6f, 1.0f, 0.15f, SECONDARY).box(BODY, 2.5f, 5.0f, -2.1f, 1.6f, 1.0f, 0.15f, SECONDARY).done());
        add("race_extra", "leaves", View.HEAD, DRYAD, "#4FA83A", "#E87AA8", b()
                .rot(HEAD, 0, -8.8f, -2, -30, 0, -20, -1.6f, -2.4f, 0, 3.2f, 2.4f, 0.4f, PRIMARY)
                .rot(HEAD, 0, -8.8f, 0, -10, 0, 25, -1.6f, -2.6f, 0, 3.2f, 2.6f, 0.4f, PRIMARY)
                .rot(HEAD, -3, -8.6f, 1, 0, 30, -35, -1.4f, -2.2f, 0, 2.8f, 2.2f, 0.4f, PRIMARY)
                .rot(HEAD, 3, -8.6f, 1, 0, -30, 35, -1.4f, -2.2f, 0, 2.8f, 2.2f, 0.4f, PRIMARY)
                .box(HEAD, 3.6f, -7.8f, -2.6f, 1.6f, 1.6f, 1.6f, SECONDARY).box(HEAD, 3.9f, -7.5f, -2.8f, 1.0f, 1.0f, 0.3f, GOLD)
                .box(BODY, -4.2f, 0.5f, -2.3f, 0.8f, 10, 0.4f, PRIMARY).box(BODY, -3.6f, 4f, -2.3f, 1.6f, 0.6f, 0.4f, PRIMARY).done());
        add("race_extra", "flowers", View.HEAD, DRYAD.or(PIXIE).or(ELF), "#E87AA8", "#FFE27A", b()
                .box(HEAD, 3.4f, -8.4f, -2.8f, 1.8f, 1.8f, 1.8f, PRIMARY).box(HEAD, 3.8f, -8.0f, -3.0f, 1.0f, 1.0f, 0.3f, SECONDARY)
                .box(HEAD, -5.0f, -7.6f, -1.0f, 1.6f, 1.6f, 1.6f, PRIMARY).box(HEAD, -5.1f, -7.2f, -0.6f, 0.3f, 0.8f, 0.8f, SECONDARY).done());
        add("race_extra", "pixie_glow", View.BODY, PIXIE, "#9FE8FF", "#E8B8FF", b()
                .box(HEAD, -0.6f, -11.6f, -0.6f, 1.2f, 1.2f, 1.2f, GLOW).box(BODY, -0.5f, 5.5f, -2.4f, 1, 1, 0.3f, GLOW).done());
    }

    // feathered wings: an arm going back and up with feathers hanging off it
    static java.util.List<Box> feathered(float span, float drop, int feathers) {
        B b = b();
        b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -28, -18, 0, -0.6f, -0.6f, span, 1.4f, 1.2f, PRIMARY);
        for (int i = 0; i < feathers; i++) {
            float x = span * i / feathers, len = drop * (1 - 0.55f * i / feathers) + 1;
            b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -28, -18, x, 0.4f, -0.4f, span / feathers + 0.2f, len, 0.7f, i % 2 == 0 ? PRIMARY : SECONDARY);
        }
        b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -28, -18, span * 0.7f, -1.6f, -0.5f, span * 0.35f, 1.2f, 1.0f, SECONDARY);
        return b.mirror().done();
    }

    // bat and dragon wings: finger bones with thin skin between them
    static java.util.List<Box> membrane(float span, float drop) {
        B b = b();
        b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -25, -22, 0, -0.6f, -0.5f, span, 1.2f, 1.0f, PRIMARY);
        b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -25, -22, span - 0.6f, -1.8f, -0.5f, 1.0f, 1.6f, 1.0f, PRIMARY);
        for (int i = 1; i <= 3; i++) {
            float x = span * i / 4f;
            b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -25, -22, x, 0.4f, -0.4f, 0.6f, drop * (1 - 0.18f * i), 0.6f, PRIMARY);
        }
        b.rot(BODY, 1.5f, 2.6f, 2.2f, 0, -25, -22, 0.2f, 0.4f, -0.2f, span - 0.4f, drop * 0.75f, 0.2f, SECONDARY);
        return b.mirror().done();
    }

    static java.util.List<Box> halo() {
        B b = b();
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            b.box(HEAD, (float) (Math.cos(a) * 3.6) - 0.6f, -11.2f, (float) (Math.sin(a) * 3.6) - 0.6f, 1.2f, 0.5f, 1.2f, GLOW);
        }
        return b.done();
    }
}

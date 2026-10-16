package com.brightwebs.jaycee.client;

import java.util.ArrayList;
import java.util.List;

public final class SkillMagicBrowserModelTest {
    private static int checks;
    public static void main(String[] args) {
        var m = new SkillMagicBrowserModel();
        List<SkillMagicBrowserModel.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 25; i++) entries.add(new SkillMagicBrowserModel.Entry(
            "skill" + i, "Skill " + i, i % 2 == 0 ? List.of("Movement", "Divine") : List.of("Healing"),
            i % 3 != 0, i % 5 == 0));
        m.entries(entries);
        check(m.count("") == 25, "All counts unique registered definitions");
        check(m.count("Movement") == 13, "Category membership counted");
        check(m.count("Unknown") == 0, "Empty categories remain empty");
        check(m.selected().equals("skill0"), "Initial selection");
        m.select("skill19"); check(m.selected().equals("skill19"), "Select visible entry");
        m.select("invented"); check(m.selected().equals("skill19"), "Reject missing selection");
        m.scroll(999); check(m.offset() == 19, "Bottom clamps to complete last page");
        m.scroll(-999); check(m.offset() == 0, "Top clamps");
        m.category("Divine"); check(m.visible().size() == 13 && m.offset() == 0, "Category filters and resets");
        check(m.selected().equals("skill0"), "Filtered selection reconciles");
        m.query("  SKILL 2 "); check(m.visible().size() == 4, "Case insensitive trimmed search within category");
        check(m.count("") == 25, "Search does not alter registry counts");
        m.detailScroll(999, 100); check(m.detailOffset() == 85, "Long details fully reachable");
        m.detailScroll(-999, 100); check(m.detailOffset() == 0, "Detail top clamp");
        m.detailScroll(9, 2); check(m.detailOffset() == 0, "Short details do not scroll");
        m.cycleFilter(); check(m.filter() == SkillMagicBrowserModel.Filter.UNLOCKED, "Unlocked filter");
        check(m.visible().stream().allMatch(SkillMagicBrowserModel.Entry::unlocked), "Only actual grants");
        m.cycleFilter(); check(m.visible().stream().noneMatch(SkillMagicBrowserModel.Entry::unlocked), "Locked filter");
        m.cycleFilter(); check(m.visible().stream().allMatch(e -> e.unlocked() && e.mastered()), "Mastered requires actual grant");
        m.cycleFilter(); check(m.filter() == SkillMagicBrowserModel.Filter.ALL, "Filter wraps");
        m.query("missing"); check(m.visible().isEmpty() && m.selected() == null, "No false learned entries");
        m.category(""); m.query(""); m.select("skill8");
        m.entries(entries); check(m.selected().equals("skill8"), "Snapshots preserve visible selection");
        m.entries(List.of()); check(m.selected() == null && m.offset() == 0, "Revoked registry safely clears selection");
        m.entries(entries); m.select("skill20"); m.detailScroll(10, 40);
        m.select("skill20"); check(m.detailOffset() == 10, "Same selection retains detail scroll");
        m.select("skill21"); check(m.detailOffset() == 0, "New selection resets details");
        m.entries(List.of(new SkillMagicBrowserModel.Entry("preview", "Old Art", List.of(), false, false)));
        check(m.count("") == 1 && m.count("Divine") == 0, "Unresolved legacy mapping not guessed");
        m.entries(List.of(entries.getFirst(), entries.getFirst()));
        check(m.count("") == 1, "Registry counts unique IDs, not duplicate references");
        m.cycleFilter();
        check(m.visible().isEmpty(), "Unlocked filter hides a missing grant");
        m.entries(List.of(new SkillMagicBrowserModel.Entry("skill0", "Skill 0", List.of("Divine"), true, false)));
        check(m.visible().size() == 1 && m.selected().equals("skill0"), "Server grant refreshes selection and results");
        m.entries(List.of(entries.getFirst()));
        check(m.visible().isEmpty() && m.selected() == null, "Server revocation immediately removes unlocked result");
        m.cycleFilter();
        check(m.visible().size() == 1, "Revoked skill remains inspectable in locked filter");
        m.cycleFilter();
        m.entries(List.of(new SkillMagicBrowserModel.Entry("skill0", "Skill 0", List.of("Divine"), true, true)));
        check(m.visible().size() == 1, "Actual mastery refreshes mastered filter");
        m.entries(List.of(new SkillMagicBrowserModel.Entry("skill0", "Skill 0", List.of("Divine"), false, true)));
        check(m.visible().isEmpty(), "Stored mastery does not bypass revoked grant");
        m.cycleFilter(); m.entries(entries); m.scroll(100);
        m.entries(List.of(entries.getFirst()));
        check(m.offset() == 0, "Registry shrink clamps an old bottom-page offset");
        m.detailScroll(100, 120); m.detailScroll(0, 2);
        check(m.detailOffset() == 0, "Shorter server-backed detail body clamps old scroll");
        var registered = List.of("divine_speed", "divine_flight", "divine_perception",
            "regeneration", "divine_resistances", "physical_training", "soul_restore");
        var actual = new ArrayList<SkillMagicBrowserModel.Entry>();
        for (String name : registered) {
            String primary = name.equals("physical_training") ? "Physical" : name.equals("soul_restore") ? "Magic" : "Intrinsic";
            String id = "jaycee:" + name;
            actual.add(new SkillMagicBrowserModel.Entry(id, name, SkillMagicBrowserModel.skillCategories(id, primary), false, false));
        }
        m.entries(actual);
        check(m.count("") == 7, "Only seven actual definitions; no fabricated magical-mastery skill");
        check(m.count("Divine Mastery") == 5, "Intrinsic shorthand maps to Divine Mastery");
        check(m.count("Physical Mastery") == 1, "Physical Training uses actual Physical Mastery");
        check(m.count("Magical Mastery") == 1, "Only real Soul Restore mastery in Magical Mastery");
        check(m.count("Movement") == 2, "Speed and flight classified by implemented movement");
        check(m.count("Perception") == 1, "Perception has real registered detection");
        check(m.count("Resistances") == 1, "Plural resistance ID maps to real resistance filter");
        check(m.count("Patron Disciplines") == 0, "No guessed patron origin");
        check(m.count("Elemental Affinities") == 0, "No guessed affinities");
        check(SkillMagicBrowserModel.skillCategories("jaycee:divine_speed", "Divine Mastery").equals(
            SkillMagicBrowserModel.skillCategories("jaycee:divine_speed", "Intrinsic")), "Expanded and shorthand metadata equivalent");
        check(SkillMagicBrowserModel.skillCategories("jaycee:divine_speed", "Movement").equals(List.of("Movement")),
            "Repeated primary and secondary membership counted once");
        String longDetail = "Complete effect description ".repeat(200);
        String narration = SkillMagicBrowserModel.detailNarration("Divine Speed", "Locked",
            List.of(longDetail, "Requires explicit server grant", "No patron origin registered"));
        check(narration.startsWith("Divine Speed. Locked."), "Narration identifies selected entry and actual state");
        check(narration.contains(longDetail), "Narration includes all long details without truncation");
        check(narration.contains("Requires explicit server grant"), "Narration includes prerequisites");
        check(narration.contains("No patron origin registered"), "Narration includes origin limitations");
        check(narration.endsWith("Page Up and Page Down scroll the full details."), "Narration includes keyboard scrolling");
        System.out.println("SkillMagicBrowserModelTest: " + checks + " checks passed");
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}

package com.brightwebs.jaycee.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** No Minecraft or testing-library dependencies: run with java -ea. */
public final class AbilityBrowserModelTest {
    private static AbilityBrowserModel.Variant variant(int index, String name, String family, String text) {
        return new AbilityBrowserModel.Variant(index, "source_" + index + ".", name, family, text,
                Map.of("gameplay_concept", text, "mana_cost", "null"));
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var variants = new ArrayList<AbilityBrowserModel.Variant>();
        variants.add(variant(1, "Shared Name", "Family A", "Push enemies"));
        variants.add(variant(2, "Shared Name", "Family B", "Energize allies"));
        variants.add(variant(3, "Other", "Family A", "Heal companions"));
        for (int i = 4; i <= 292; i++)
            variants.add(variant(i, "Name " + i, "Family " + i % 21, "Concept " + i));
        // Three repeated names, 292 source records -> 289 distinct canonical entries.
        variants.set(290, variant(291, "Other", "Family B", "Different variant"));
        variants.set(291, variant(292, "Name 4", "Family B", "Another variant"));
        var model = new AbilityBrowserModel(variants, List.of("Legacy A", "Legacy B", "Legacy C", "Legacy D", "Legacy E", "Legacy F"));
        check(model.count(AbilityBrowserModel.ALL) == 295, "All counts unique canonical names plus six legacy entries");
        check(model.count(AbilityBrowserModel.LEGACY) == 6, "Legacy remains accessible");
        check(model.count("Family A") == 2, "Family membership counts unique names");
        check(model.count("Family B") == 3, "Cross-family duplicate retained");
        var shared = model.entries().getFirst();
        check(shared.variants().size() == 2, "Both descriptions and variants retained");
        check(shared.variants().getFirst().sourceId().equals("source_1."), "Original extraction punctuation retained");
        check(shared.variants().getFirst().browsingId().equals("design:source_1/1"), "Only internal browsing ID is cleaned");
        model.search("ENERGIZE");
        check(model.results().size() == 1, "Case insensitive concept search");
        model.filter("Family A");
        check(model.results().size() == 1, "Search within active family includes all variant concepts");
        model.select(shared);
        model.search("Heal");
        check(model.selected() == null, "Hidden selection cleared to prevent stale equip");
        model.search("x".repeat(200));
        check(model.search().length() == 128, "Search length capped");
        check(model.results().isEmpty(), "Empty results safe");
        model.moveSelection(1, 7);
        check(model.selected() == null, "Empty navigation safe");
        model.search("");
        model.filter(AbilityBrowserModel.ALL);
        model.scrollList(Integer.MAX_VALUE, 7);
        check(model.listOffset() == 288, "List end bound");
        model.scrollList(Integer.MIN_VALUE, 7);
        check(model.listOffset() == 0, "List start bound");
        model.moveSelection(-1, 7);
        check(model.selected().legacyIndex() == 5 && model.listOffset() == 288, "Keyboard reaches final legacy entry");
        model.moveSelection(Integer.MAX_VALUE, 7);
        check(model.selected().legacyIndex() == 5, "Navigation overflow safe");
        model.scrollFilters(Integer.MAX_VALUE, 11);
        check(model.filterOffset() == model.families().size() - 11, "Filter scroll bound");
        model.scrollDetails(Integer.MAX_VALUE, 200, 18);
        check(model.detailOffset() == 182, "Full detail text reachable");
        model.scrollDetails(0, 2, 18);
        check(model.detailOffset() == 0, "Detail bound adjusts on content change");
        model.filter(AbilityBrowserModel.LEGACY);
        check(model.selected() != null, "Visible legacy selection retained across filter");
        model.filter("Family A");
        check(model.selected() == null, "Legacy selection cleared when filtered out");
        check(AbilityBrowserModel.offset(0, 9, 2, 7) == 0, "Short list never scrolls");
        System.out.println("AbilityBrowserModelTest: all checks passed");
    }
}

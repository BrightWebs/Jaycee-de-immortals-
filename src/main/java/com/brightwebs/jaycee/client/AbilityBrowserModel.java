package com.brightwebs.jaycee.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only design index. A source variant is never an executable ability registration. */
public final class AbilityBrowserModel {
    public static final String ALL = "All Abilities";
    public static final String LEGACY = "Legacy (unmapped)";
    public static final int MAX_SEARCH = 128;
    public record Variant(int index, String sourceId, String name, String family,
                          String concept, Map<String, String> fields) {
        public Variant { fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields)); }
        public String browsingId() {
            return "design:" + sourceId.replaceAll("[^a-zA-Z0-9_/-]", "").toLowerCase(Locale.ROOT) + "/" + index;
        }
    }
    public record Entry(String name, List<Variant> variants, int legacyIndex) {
        public Entry { variants = List.copyOf(variants); }
        public boolean legacy() { return legacyIndex >= 0; }
        public boolean belongs(String family) {
            return ALL.equals(family) || (legacy() ? LEGACY.equals(family)
                    : variants.stream().anyMatch(v -> v.family().equals(family)));
        }
        public boolean matches(String query) {
            return (name + " " + String.join(" ", variants.stream().map(Variant::concept).toList()))
                    .toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
        }
    }
    private final List<Entry> entries;
    private final List<String> families;
    private String filter = ALL, search = "";
    private Entry selected;
    private int filterOffset, listOffset, detailOffset;

    public AbilityBrowserModel(List<Variant> variants, List<String> legacyNames) {
        Map<String, List<Variant>> grouped = new LinkedHashMap<>();
        List<String> groups = new ArrayList<>(List.of(ALL));
        for (Variant variant : variants) {
            grouped.computeIfAbsent(variant.name(), ignored -> new ArrayList<>()).add(variant);
            if (!groups.contains(variant.family())) groups.add(variant.family());
        }
        List<Entry> built = new ArrayList<>();
        grouped.forEach((name, records) -> built.add(new Entry(name, records, -1)));
        for (int i = 0; i < legacyNames.size(); i++) built.add(new Entry(legacyNames.get(i), List.of(), i));
        groups.add(LEGACY);
        entries = List.copyOf(built);
        families = List.copyOf(groups);
    }
    public List<String> families() { return families; }
    public List<Entry> entries() { return entries; }
    public List<Entry> results() { return entries.stream().filter(e -> e.belongs(filter) && e.matches(search)).toList(); }
    public int count(String family) { return (int) entries.stream().filter(e -> e.belongs(family)).count(); }
    public String filter() { return filter; }
    public String search() { return search; }
    public Entry selected() { return selected; }
    public int filterOffset() { return filterOffset; }
    public int listOffset() { return listOffset; }
    public int detailOffset() { return detailOffset; }
    public void filter(String value) {
        if (!families.contains(value)) throw new IllegalArgumentException(value);
        filter = value; resetQuery();
    }
    public void search(String value) {
        search = value.substring(0, Math.min(MAX_SEARCH, value.length()));
        resetQuery();
    }
    private void resetQuery() {
        listOffset = 0;
        if (selected != null && !results().contains(selected)) selected = null;
        detailOffset = 0;
    }
    public void select(Entry entry) {
        if (entry != null && !results().contains(entry)) throw new IllegalArgumentException("Hidden entry");
        selected = entry; detailOffset = 0;
    }
    public static int offset(int current, int delta, int total, int visible) {
        return (int) Math.max(0L, Math.min(Math.max(0, total - visible), (long) current + delta));
    }
    public void scrollFilters(int delta, int visible) { filterOffset = offset(filterOffset, delta, families.size(), visible); }
    public void scrollList(int delta, int visible) { listOffset = offset(listOffset, delta, results().size(), visible); }
    public void scrollDetails(int delta, int lines, int visible) { detailOffset = offset(detailOffset, delta, lines, visible); }
    public void moveSelection(int delta, int visible) {
        List<Entry> found = results();
        if (found.isEmpty()) { selected = null; return; }
        int index = selected == null ? (delta < 0 ? found.size() - 1 : 0)
                : (int) Math.max(0L, Math.min(found.size() - 1L, (long) found.indexOf(selected) + delta));
        select(found.get(index));
        if (index < listOffset) listOffset = index;
        if (index >= listOffset + visible) listOffset = index - visible + 1;
    }
}

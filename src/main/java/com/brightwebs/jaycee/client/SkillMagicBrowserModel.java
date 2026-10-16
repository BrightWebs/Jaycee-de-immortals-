package com.brightwebs.jaycee.client;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;

/** Dependency-free filtering and bounded scrolling; no saved gameplay state. */
final class SkillMagicBrowserModel {
    static final String SETTING_TIMING = "Nonzero output, mode and style changes take effect at the next paid upkeep. Zero or off stops this power immediately.";
    enum Filter { ALL, UNLOCKED, LOCKED, MASTERED }
    record Entry(String id, String name, List<String> categories, boolean unlocked, boolean mastered) {}
    private List<Entry> entries = List.of();
    private String category = "", query = "", selected;
    private Filter filter = Filter.ALL;
    private int offset, detailOffset;
    static String detailNarration(String name, String status, List<String> lines) {
        return name + ". " + status + ". " + String.join(". ", lines) +
            ". Page Up and Page Down scroll the full details.";
    }
    void entries(List<Entry> values) {
        var unique = new LinkedHashMap<String, Entry>();
        for (var entry : values) unique.putIfAbsent(entry.id(), entry);
        entries = List.copyOf(unique.values());
        reconcile();
    }
    List<Entry> visible() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return entries.stream().filter(e -> category.isEmpty() || e.categories().contains(category))
            .filter(e -> e.name().toLowerCase(Locale.ROOT).contains(needle))
            .filter(e -> switch (filter) {
                case ALL -> true; case UNLOCKED -> e.unlocked();
                case LOCKED -> !e.unlocked(); case MASTERED -> e.unlocked() && e.mastered();
            }).toList();
    }
    int count(String value) { return (int) entries.stream().filter(e -> value.isEmpty() || e.categories().contains(value)).count(); }
    void category(String value) { category = value; reset(); }
    String category() { return category; }
    void query(String value) { query = value; reset(); }
    void cycleFilter() { filter = Filter.values()[(filter.ordinal() + 1) % Filter.values().length]; reset(); }
    Filter filter() { return filter; }
    String selected() { return selected; }
    void select(String id) {
        if (visible().stream().anyMatch(e -> e.id().equals(id))) {
            if (!id.equals(selected)) detailOffset = 0;
            selected = id;
        }
    }
    int offset() { return offset; }
    void scroll(int amount) { offset = clamp(offset + amount, Math.max(0, visible().size() - SkillMagicLayout.VISIBLE_ROWS)); }
    int detailOffset() { return detailOffset; }
    void detailScroll(int amount, int lines) {
        detailOffset = clamp(detailOffset + amount, Math.max(0, lines - SkillMagicLayout.DETAIL_LINES));
    }
    private void reset() { offset = 0; detailOffset = 0; reconcile(); }
    private void reconcile() {
        var matches = visible();
        if (matches.stream().noneMatch(e -> e.id().equals(selected))) {
            selected = matches.isEmpty() ? null : matches.getFirst().id();
            detailOffset = 0;
        }
        offset = clamp(offset, Math.max(0, matches.size() - SkillMagicLayout.VISIBLE_ROWS));
    }
    private static int clamp(int value, int max) { return Math.max(0, Math.min(value, max)); }
}

package com.brightwebs.jaycee.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Status views use only saved server facts; presentation never grants abilities or affinities. */
public final class StatusData {
    private static final String PRESENTATION = "StatusPresentation";
    private static final String AFFINITIES = "Affinities";

    /** A zero required value means no progress requirement is supplied. Icons may be empty. */
    public record Entry(String id, String name, String description, String icon,
                        int level, long progress, long required) {}

    private StatusData() {}

    public static List<Entry> traits(PlayerState state) {
        var entries = new ArrayList<Entry>();
        var grants = state.divinity().getCompound("Grants");
        for (var definition : SkillDefinitions.ALL) {
            String id = definition.id();
            if (!definition.intrinsic() || !state.skillGranted(id)) continue;
            // Only actual divine grants or authority count, never test access or legacy training.
            if (!state.supreme() && !grants.contains(id)) continue;
            int level = state.skillLevel(id);
            long required = level >= state.skillMaxLevel() ? 0
                : (long) (level + 1) * (level + 1) * state.skillXpPerLevel();
            entries.add(new Entry(id, definition.name(), definition.description(),
                definition.icon().toString(), level, state.skillXp(id), required));
        }
        return List.copyOf(entries);
    }

    public static List<Entry> affinities(PlayerState state) {
        var stored = state.statusPresentation().getCompound(AFFINITIES);
        var entries = new ArrayList<Entry>();
        for (String id : stored.getAllKeys().stream().sorted().toList()) {
            if (ResourceLocation.tryParse(id) == null || !stored.contains(id, Tag.TAG_COMPOUND)) continue;
            var value = stored.getCompound(id);
            String name = value.getString("Name");
            String description = value.getString("Description");
            if (!validText(name, 128, false) || !validText(description, 2048, true)
                    || !value.contains("Level", Tag.TAG_INT)
                    || !validNumber(value, "Progress", Tag.TAG_LONG)
                    || !validNumber(value, "Required", Tag.TAG_LONG)) continue;
            int level = value.getInt("Level");
            long progress = value.getLong("Progress");
            long required = value.getLong("Required");
            if (!validProgress(level, progress, required)) continue;
            String icon = value.getString("Icon");
            entries.add(new Entry(id, name, description,
                ResourceLocation.tryParse(icon) == null ? "" : icon, level, progress, required));
        }
        return List.copyOf(entries);
    }

    public static String title(PlayerState state) {
        String title = state.statusPresentation().getString("Title");
        return validText(title, 128, true) ? title : "";
    }

    /** Retained NPC lineage IDs only; neither historical grants nor NPC offering support is implied. */
    public static List<String> patrons(PlayerState state) {
        return state.divinity().getList("Npc", Tag.TAG_STRING).stream()
            .map(Tag::getAsString).filter(id -> !id.isBlank()).distinct().sorted().toList();
    }

    /** Server gameplay hooks only; no client action routes to these setters. Empty clears the title. */
    public static void setTitle(ServerPlayer player, String title) {
        if (!validText(title, 128, true)) throw new IllegalArgumentException("Invalid status title");
        var data = PlayerState.read(player);
        var presentation = data.getCompound(PRESENTATION);
        if (title.isEmpty()) presentation.remove("Title");
        else presentation.putString("Title", title);
        data.put(PRESENTATION, presentation);
        save(player, data);
    }

    /** Stores an actual server-awarded affinity. Null icon and zero required omit optional metadata. */
    public static void setAffinity(ServerPlayer player, String id, String name, String description,
                                   ResourceLocation icon, int level, long progress, long required) {
        String key = affinityId(id);
        if (!validText(name, 128, false) || !validText(description, 2048, true)
                || !validProgress(level, progress, required))
            throw new IllegalArgumentException("Invalid affinity metadata or progress");
        var data = PlayerState.read(player);
        var presentation = data.getCompound(PRESENTATION);
        var affinities = presentation.getCompound(AFFINITIES);
        var value = new CompoundTag();
        value.putString("Name", name);
        value.putString("Description", description);
        if (icon != null) value.putString("Icon", icon.toString());
        value.putInt("Level", level);
        value.putLong("Progress", progress);
        value.putLong("Required", required);
        affinities.put(key, value);
        presentation.put(AFFINITIES, affinities);
        data.put(PRESENTATION, presentation);
        save(player, data);
    }

    public static void removeAffinity(ServerPlayer player, String id) {
        String key = affinityId(id);
        var data = PlayerState.read(player);
        var presentation = data.getCompound(PRESENTATION);
        var affinities = presentation.getCompound(AFFINITIES);
        affinities.remove(key);
        presentation.put(AFFINITIES, affinities);
        data.put(PRESENTATION, presentation);
        save(player, data);
    }

    private static String affinityId(String id) {
        var parsed = id == null || id.length() > 128 ? null : ResourceLocation.tryParse(id);
        if (parsed == null) throw new IllegalArgumentException("Invalid affinity resource ID");
        return parsed.toString();
    }

    private static boolean validText(String value, int limit, boolean emptyAllowed) {
        return value != null && value.length() <= limit && (emptyAllowed || !value.isBlank())
            && value.chars().noneMatch(Character::isISOControl);
    }

    private static boolean validNumber(CompoundTag value, String key, int type) {
        return !value.contains(key) || value.contains(key, type);
    }

    private static boolean validProgress(int level, long progress, long required) {
        return level >= 0 && progress >= 0 && required >= 0 && (required == 0 || progress <= required);
    }

    private static void save(ServerPlayer player, CompoundTag data) {
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
    }
}

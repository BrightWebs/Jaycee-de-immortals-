package com.brightwebs.jaycee.gameplay;

import java.lang.reflect.Constructor;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** Standalone snapshot regression checks, following the existing main-method test convention. */
public final class StatusDataTest {
    public static void main(String[] args) throws Exception {
        var data = new CompoundTag();
        data.putBoolean("JayceeTest", true);
        check(StatusData.traits(snapshot(data)).isEmpty(), "test identity must not unlock traits");
        check(StatusData.affinities(snapshot(data)).isEmpty(), "test identity must not infer affinities");
        check(StatusData.title(snapshot(data)).isEmpty(), "test identity must not infer a title");

        var divinity = new CompoundTag();
        var grants = new CompoundTag();
        grants.putString(SkillDefinitions.SPEED, "operator");
        grants.putString(SkillDefinitions.PHYSICAL, "operator");
        grants.putString(SkillDefinitions.RESTORE, "operator");
        divinity.put("Grants", grants);
        var patrons = new ListTag();
        patrons.add(StringTag.valueOf("zeus"));
        patrons.add(StringTag.valueOf("athena"));
        patrons.add(StringTag.valueOf("zeus"));
        divinity.put("Npc", patrons);
        data.put("Divinity", divinity);
        check(StatusData.traits(snapshot(data)).stream().map(StatusData.Entry::id).toList()
            .equals(List.of(SkillDefinitions.SPEED)), "only explicitly granted intrinsic traits");
        check(StatusData.affinities(snapshot(data)).isEmpty(), "patrons and grants must not infer affinities");
        check(StatusData.patrons(snapshot(data)).equals(List.of("athena", "zeus")),
            "patrons must be retained, distinct, and sorted");

        divinity.putBoolean("Supreme", true);
        data.put("Divinity", divinity);
        check(StatusData.traits(snapshot(data)).size()
            == SkillDefinitions.ALL.stream().filter(SkillDefinition::intrinsic).count(),
            "authority owns all intrinsic traits, never training or Restore");

        var presentation = new CompoundTag();
        presentation.putString("Title", "A saved title");
        var affinities = new CompoundTag();
        var affinity = new CompoundTag();
        affinity.putString("Name", "Stored affinity");
        affinity.putString("Description", "Explicit server metadata");
        affinity.putString("Icon", "invalid icon");
        affinity.putInt("Level", 2);
        affinity.putLong("Progress", 3);
        affinity.putLong("Required", 10);
        affinities.put("jaycee:stored", affinity);
        presentation.put("Affinities", affinities);
        data.put("StatusPresentation", presentation);
        var state = snapshot(data);
        check(StatusData.title(state).equals("A saved title"), "title must come from storage");
        check(StatusData.affinities(state).equals(List.of(new StatusData.Entry("jaycee:stored",
            "Stored affinity", "Explicit server metadata", "", 2, 3, 10))),
            "stored affinity metadata must survive; invalid icon must be safe");
        affinity.putLong("Progress", -1);
        affinities.put("jaycee:stored", affinity);
        presentation.put("Affinities", affinities);
        data.put("StatusPresentation", presentation);
        check(StatusData.affinities(snapshot(data)).isEmpty(), "negative progress must be rejected");
        check(StatusData.affinities(state).size() == 1, "snapshots must remain independent");
        affinity.putLong("Progress", 3);
        affinity.remove("Level");
        affinities.put("jaycee:stored", affinity);
        presentation.put("Affinities", affinities);
        data.put("StatusPresentation", presentation);
        check(StatusData.affinities(snapshot(data)).isEmpty(), "missing level must not fabricate level zero");
        affinity.putInt("Level", 2);
        affinity.remove("Progress");
        affinity.remove("Required");
        affinities.put("jaycee:stored", affinity);
        presentation.put("Affinities", affinities);
        data.put("StatusPresentation", presentation);
        check(StatusData.affinities(snapshot(data)).equals(List.of(new StatusData.Entry("jaycee:stored",
            "Stored affinity", "Explicit server metadata", "", 2, 0, 0))),
            "stored level without optional progress must remain visible without a requirement");
        System.out.println("StatusData snapshot checks passed");
    }

    private static PlayerState snapshot(CompoundTag data) throws Exception {
        Constructor<PlayerState> constructor = PlayerState.class.getDeclaredConstructor(CompoundTag.class);
        constructor.setAccessible(true);
        return constructor.newInstance(data);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

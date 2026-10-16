package com.brightwebs.jaycee.gameplay.divine;

import java.util.*;

/** NBT codec checks only; entity hooks require a real Minecraft server. */
public final class MobDivinityTagsTest {
    public static void main(String[] args) {
        int assertions = 0;
        for (String status : List.of("active", "unloaded", "deceased", "removed")) {
            var p = new DivinityModel.MobProfile(UUID.randomUUID(), "minecraft:wolf", "minecraft:the_nether",
                "entity.minecraft.wolf", "Current Name", "Divine Name", 7, 20, 12, 123, 456,
                UUID.randomUUID(), Map.of("jaycee:divine_speed", UUID.randomUUID().toString()), status);
            var tag = MobDivinityTags.write(p);
            if (!MobDivinityTags.read(tag).equals(p)) throw new AssertionError("Mob NBT round trip: " + status);
            assertions++;
            if (!tag.getString("Rank").equals("Unawakened") || !tag.getBoolean("Subordinate"))
                throw new AssertionError("Honest rank and binding mirror");
            assertions++;
        }
        var p = new DivinityModel.MobProfile(UUID.randomUUID(), "minecraft:cow", "minecraft:overworld",
            "entity.minecraft.cow", "", "", 10, 10, 0, 0, 0, null, Map.of(), "active");
        var t = MobDivinityTags.write(p);
        if (t.hasUUID("Patron") || t.getBoolean("Subordinate") || !MobDivinityTags.read(t).equals(p))
            throw new AssertionError("Unbound mob codec");
        assertions++;
        System.out.println("MobDivinityTagsTest: " + assertions + " assertions passed");
    }
}

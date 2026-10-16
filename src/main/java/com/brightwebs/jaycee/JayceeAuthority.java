package com.brightwebs.jaycee;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** Unique Jaycee role is bound to a player UUID and persisted with the world. */
public final class JayceeAuthority extends SavedData {
    private UUID holder;

    private static JayceeAuthority load(CompoundTag tag, HolderLookup.Provider registries) {
        JayceeAuthority data = new JayceeAuthority();
        if (tag.hasUUID("Holder")) data.holder = tag.getUUID("Holder");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (holder != null) tag.putUUID("Holder", holder);
        return tag;
    }

    private static JayceeAuthority state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(JayceeAuthority::new, JayceeAuthority::load), "jaycee_unique_authority");
    }

    /** Optional dedicated server property: -Djaycee.owner_uuid=<uuid>. */
    private static UUID configuredOwner() {
        String value = System.getProperty("jaycee.owner_uuid", "").trim();
        if (value.isEmpty()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ex) { return null; }
    }

    public static boolean isJaycee(ServerPlayer player) {
        UUID owner = configuredOwner();
        if (owner != null && owner.equals(player.getUUID())) return cores(player) >= 3 && hasAdvancement(player);
        return state(player.server).holder != null
            && state(player.server).holder.equals(player.getUUID())
            && cores(player) >= 3 && hasAdvancement(player);
    }

    public static int cores(ServerPlayer player) {
        return player.getPersistentData().getInt("JayceeConsumed");
    }

    public static boolean hasAdvancement(ServerPlayer player) {
        var entry = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("jaycee", "jaycee"));
        return entry != null && player.getAdvancements().getOrStartProgress(entry).isDone();
    }

    /** After the third consumed core, the earliest eligible player gets the world role. */
    public static void afterCore(ServerPlayer player) {
        if (cores(player) < 3) return;
        UUID owner = configuredOwner();
        JayceeAuthority data = state(player.server);
        boolean eligible = owner != null && owner.equals(player.getUUID());
        if (data.holder == null) {
            data.holder = player.getUUID();
            data.setDirty();
            eligible = true;
        } else if (data.holder.equals(player.getUUID())) {
            eligible = true;
        }
        if (eligible) grantAdvancement(player);
    }

    private static void grantAdvancement(ServerPlayer player) {
        var entry = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("jaycee", "jaycee"));
        if (entry != null) player.getAdvancements().award(entry, "claimed");
    }
}

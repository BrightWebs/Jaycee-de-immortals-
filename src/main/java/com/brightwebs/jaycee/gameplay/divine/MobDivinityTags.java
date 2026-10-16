package com.brightwebs.jaycee.gameplay.divine;

import java.util.*;
import net.minecraft.nbt.*;

/** Same schema for the authoritative world ledger and its non-authoritative entity mirror. */
public final class MobDivinityTags {
    private MobDivinityTags() {}
    public static CompoundTag write(DivinityModel.MobProfile p) {
        var t = new CompoundTag();
        t.putUUID("Id", p.id()); t.putString("Type", p.type()); t.putString("Dimension", p.dimension());
        t.putString("Original", p.original()); t.putString("CustomName", p.customName());
        t.putString("DivineName", p.divineName()); t.putString("Username", p.divineName().isEmpty() ? p.customName().isEmpty() ? p.original() : p.customName() : p.divineName());
        t.putFloat("Health", p.health()); t.putFloat("MaxHealth", p.maxHealth());
        t.putLong("Ep", p.ep()); t.putLong("Essence", p.essence()); t.putLong("Received", p.received());
        t.putString("Rank", "Unawakened"); t.putString("Status", p.status());
        t.putString("Rules", "Mob evolution, EP conversion and automatic promotion are unavailable; Essence and EP remain separate.");
        t.putBoolean("Subordinate", p.master() != null);
        if (p.master() != null) t.putUUID("Patron", p.master());
        var grants = new CompoundTag(); p.grants().forEach(grants::putString); t.put("Grants", grants);
        return t;
    }
    public static DivinityModel.MobProfile read(CompoundTag t) {
        var grants = new HashMap<String, String>(); var g = t.getCompound("Grants");
        g.getAllKeys().forEach(k -> grants.put(k, g.getString(k)));
        return new DivinityModel.MobProfile(t.getUUID("Id"), t.getString("Type"), t.getString("Dimension"),
            t.getString("Original"), t.getString("CustomName"), t.getString("DivineName"),
            t.getFloat("Health"), t.getFloat("MaxHealth"), t.getLong("Ep"), t.getLong("Essence"),
            t.getLong("Received"), t.hasUUID("Patron") ? t.getUUID("Patron") : null, grants, t.getString("Status"));
    }
}

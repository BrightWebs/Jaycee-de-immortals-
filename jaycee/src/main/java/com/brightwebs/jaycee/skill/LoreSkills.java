package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityCategory;
import com.brightwebs.brightcore.api.SkillType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// every skill from the lore list that doesn't have its own hand made class yet.
// the numbers live in assets/jaycee/lore_skills.json (made by tools/gen_lore_skills.py, tensura's numbers where tensura has the skill)
// and what they actually do is in LoreEngine. hand made skills always win, this only fills the gaps
public final class LoreSkills {
    private LoreSkills() {}
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public record Spec(String id, String name, SkillType type, String rank, String elem, String acc, String arch,
                       int mp, int ap, float dmg, int cd, int cdm, float range, float radius, int cast, int castm,
                       String mclass, int dur, List<String> modes, boolean toggle, boolean tensura) {
        boolean magic() { return type == SkillType.MAGIC; }
        boolean resist() { return arch.equals("resist"); }
        // which arch a mode uses (one mode skills just use their own)
        String archOf(int mode) { return modes.isEmpty() ? arch : modes.get(Math.floorMod(mode, modes.size())); }
    }

    public static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    private static void load() {
        if (!SPECS.isEmpty()) return;
        try (InputStream in = LoreSkills.class.getResourceAsStream("/assets/jaycee/lore_skills.json")) {
            if (in == null) { LOG.error("[Jaycee] lore_skills.json is missing"); return; }
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    List<String> modes = new ArrayList<>();
                    if (o.has("modes")) for (JsonElement m : o.getAsJsonArray("modes")) modes.add(m.getAsString());
                    Spec s = new Spec(str(o, "id", ""), str(o, "name", ""), SkillType.valueOf(str(o, "type", "EXTRA")), str(o, "rank", "extra"),
                            str(o, "elem", "aura"), str(o, "acc", "aura"), str(o, "arch", "aura"),
                            num(o, "mp", 0), num(o, "ap", 0), (float) dbl(o, "dmg", 10), num(o, "cd", 60), num(o, "cdm", 30),
                            (float) dbl(o, "range", 16), (float) dbl(o, "radius", 3), num(o, "cast", 20), num(o, "castm", 10),
                            str(o, "mclass", "other"), num(o, "dur", 600), modes, o.has("toggle") && o.get("toggle").getAsBoolean(),
                            "tensura".equals(str(o, "src", "")));
                    if (!s.id().isEmpty()) SPECS.put(s.id(), s);
                } catch (Exception bad) {
                    LOG.warn("[Jaycee] skipped a broken lore skill entry: {}", bad.toString());
                }
            }
        } catch (Exception ex) {
            LOG.error("[Jaycee] couldn't read lore_skills.json", ex);
        }
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
    private static int num(JsonObject o, String k, int d) { return o.has(k) ? (int) Math.round(o.get(k).getAsDouble()) : d; }
    private static double dbl(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }

    // called by JayceeSkills after the hand made ones are in, so nothing gets registered twice
    public static void register(DeferredRegister<Ability> reg) {
        load();
        Set<String> taken = new HashSet<>();
        reg.getEntries().forEach(h -> taken.add(h.getId().getPath()));
        int n = 0;
        for (Spec s : SPECS.values()) {
            if (taken.contains(s.id())) continue;
            reg.register(s.id(), () -> new LoreAbility(s));
            n++;
        }
        LOG.info("[Jaycee] {} lore skills registered", n);
    }

    public static Spec spec(Ability a) {
        return a instanceof LoreAbility la ? la.spec : null;
    }

    static AbilityCategory category(String arch) {
        return switch (arch) {
            case "guard", "resist" -> AbilityCategory.DEFENSE;
            case "move" -> AbilityCategory.MOVEMENT;
            case "heal", "sense", "summon", "aura", "awaken" -> AbilityCategory.SUPPORT;
            default -> AbilityCategory.OFFENSE;
        };
    }

    private static Ability.Properties props(Spec s) {
        Ability.Properties p = new Ability.Properties().type(s.type()).category(category(s.arch()));
        if (s.resist()) return p.passive().switchable();
        if (s.magic()) p.magic(s.mclass(), s.elem(), s.cast() / 20f, Math.round(s.range())).held();
        // cooldowns are set by the engine itself (they're shorter once mastered), so the modes carry 0
        if (s.modes().isEmpty()) {
            p.mode("cast", s.mp(), s.ap(), 0);
            if (s.toggle()) p.toggle();
        } else {
            for (String m : s.modes()) {
                int mult = m.equals("awaken") ? 2 : 1;
                p.mode(m, s.mp() * mult, s.ap() * mult, 0);
            }
        }
        return p;
    }

    // the one class all lore skills use. everything it does goes through LoreEngine
    static final class LoreAbility extends Ability {
        final Spec spec;

        LoreAbility(Spec s) {
            super(props(s));
            this.spec = s;
        }

        @Override public Component modeName(int index) {
            if (spec.modes().isEmpty()) return name();
            return Component.translatable("jaycee.lore.mode." + spec.archOf(index));
        }

        @Override public boolean activate(ServerPlayer p, ServerLevel l, int mode) {
            if (spec.magic()) return true;  // magic is held: the circle builds up and it goes off when you let go
            return LoreEngine.use(p, this, spec, spec.archOf(mode));
        }

        @Override public boolean onHeld(ServerPlayer p, ServerLevel l, int mode, int held) {
            if (spec.magic()) LoreEngine.casting(p, this, spec, held);
            return true;
        }

        @Override public void onRelease(ServerPlayer p, ServerLevel l, int mode, int held) {
            if (!spec.magic()) return;
            int need = AbilityAPI.isMastered(p, this) ? spec.castm() : spec.cast();
            if (held < need) {
                p.sendOverlayMessage(Component.translatable("jaycee.lore.cast_stopped", name()).withStyle(ChatFormatting.GRAY));
                return;
            }
            LoreEngine.use(p, this, spec, spec.archOf(mode));
        }

        @Override public void tick(ServerPlayer p, ServerLevel l) {
            if (spec.toggle()) LoreEngine.toggledTick(p, this, spec);
        }

        @Override public void onToggle(ServerPlayer p, boolean on) {
            if (!on) LoreEngine.stop(p, spec);
        }

        @Override public void onForgotten(ServerPlayer p) { LoreEngine.stop(p, spec); }

        @Override public void onHit(ServerPlayer p, net.minecraft.world.entity.LivingEntity target, float damage) {
            if (spec.arch().equals("aura") && LoreEngine.isActive(p, spec)) LoreEngine.element(p, target, spec, damage * 0.25f);
        }

        Identifier key() { return id(); }
    }
}

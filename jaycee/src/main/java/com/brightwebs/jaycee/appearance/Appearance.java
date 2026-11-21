package com.brightwebs.jaycee.appearance;

import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.jaycee.JayceeMod;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// a player's look from the character editor, kept separate from race, EP and skills
// keys: slot -> option id (bangs, back, ears, tail...), "<group>.c1/.c2/.c3" -> color RRGGBB,
// "hair.hl" / "hair.tip" -> "1" when highlights / colored tips are on
// synced to everyone so other players see it, saved with the player and kept on death
public final class Appearance {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    public static final Supplier<AttachmentType<Map<String, String>>> LOOK = ATTACHMENTS.register("appearance",
            () -> AttachmentType.<Map<String, String>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("look")).copyOnDeath()
                    .sync(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)).build());

    // slots the renderer draws, back to front
    public static final List<String> WORN = List.of("tail", "wings", "cape", "back", "bangs", "hair_extra", "ears", "horns",
            "race_extra", "eyes", "markings", "acc_head", "acc_face", "acc_neck", "waist");

    private Appearance() {}

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        Features.bootstrap();
    }

    public static Map<String, String> of(Player p) { return p.getData(LOOK); }

    public static String raceId(Player p) {
        return RaceAPI.race(p).map(r -> r.id().getPath()).orElse("human");
    }

    // only keeps what the race is allowed to wear and adds back what it can't take off (young merfolk tail)
    // presets and edits all go through this so you can't hide a natural form to dodge its weakness
    public static Map<String, String> sanitize(Map<String, String> in, String raceId) {
        Map<String, String> out = new LinkedHashMap<>();
        int n = 0;
        for (Map.Entry<String, String> e : in.entrySet()) {
            if (++n > 96) break;
            String k = e.getKey(), v = e.getValue();
            if (k == null || v == null || k.length() > 32 || v.length() > 32) continue;
            if (k.endsWith(".c1") || k.endsWith(".c2") || k.endsWith(".c3")) {
                if (v.matches("[0-9A-Fa-f]{6}")) out.put(k, v.toUpperCase());
            } else if (k.equals("hair.hl") || k.equals("hair.tip")) {
                out.put(k, "1".equals(v) ? "1" : "0");
            } else if (WORN.contains(k)) {
                Features.Option o = Features.get(k, v);
                if (o != null && o.race().test(raceId)) out.put(k, v);
            }
        }
        out.putAll(Features.requiredAll(raceId));
        return out;
    }

    // apply changes button in the editor
    public static void apply(ServerPlayer p, Map<String, String> look) {
        p.setData(LOOK, Map.copyOf(sanitize(look, raceId(p))));
    }

    // after evolving: drop what the new form can't have, add what it has to have
    public static void raceChanged(ServerPlayer p) {
        Map<String, String> now = of(p), clean = sanitize(now, raceId(p));
        if (!clean.equals(now)) p.setData(LOOK, Map.copyOf(clean));
    }

    // colors

    // the color a box with this tint gets, 0xFFRRGGBB
    public static int color(Map<String, String> look, Features.Option o, int tint) {
        String g = Features.colorGroup(o.slot());
        return switch (tint) {
            case Features.PRIMARY, Features.GLOW -> rgb(look, g + ".c1", o.c1(), defaultFor(g, 1));
            case Features.SECONDARY -> rgb(look, g + ".c2", o.c2(), defaultFor(g, 2));
            case Features.HIGHLIGHT -> g.equals("hair") && !"1".equals(look.get("hair.hl"))
                    ? rgb(look, g + ".c1", o.c1(), defaultFor(g, 1)) : rgb(look, g + ".c2", o.c2(), defaultFor(g, 2));
            case Features.TIP -> g.equals("hair") && !"1".equals(look.get("hair.tip"))
                    ? rgb(look, g + ".c1", o.c1(), defaultFor(g, 1)) : rgb(look, g + ".c3", o.c2(), defaultFor(g, 3));
            case Features.WHITE -> 0xFFF4F4F8;
            case Features.DARK -> 0xFF1A1620;
            case Features.GOLD -> 0xFFF0C040;
            default -> 0xFFFFFFFF;
        };
    }

    static int rgb(Map<String, String> look, String key, String optionDefault, String groupDefault) {
        String v = look.get(key);
        if (v == null) v = optionDefault != null ? optionDefault.replace("#", "") : groupDefault;
        try {
            return 0xFF000000 | Integer.parseInt(v, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    public static String defaultFor(String group, int n) {
        return switch (group) {
            case "hair" -> n == 1 ? "3A7BFF" : n == 2 ? "9FE8FF" : "B46CFF";
            default -> n == 1 ? "F4F4F8" : n == 2 ? "B8C8D8" : "9FE8FF";
        };
    }
}

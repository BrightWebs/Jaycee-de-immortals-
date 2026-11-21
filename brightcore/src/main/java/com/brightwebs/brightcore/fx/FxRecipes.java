package com.brightwebs.brightcore.fx;

import com.brightwebs.brightcore.BrightCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// plays a skill's particle recipe from assets/brightcore/fx_recipes.json
// (two particle kinds and a shape for every skill on the tensura list)
// new skills get their look with one line: FxRecipes.play(level, "fire_ball", caster, targetPosition);
public final class FxRecipes {
    public record Recipe(String primary, String accent, String shape) {}
    private static Map<String, Recipe> recipes;

    private FxRecipes() {}

    private static synchronized Map<String, Recipe> all() {
        if (recipes != null) return recipes;
        recipes = new HashMap<>();
        try (InputStream in = FxRecipes.class.getResourceAsStream("/assets/brightcore/fx_recipes.json")) {
            if (in == null) return recipes;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                var fam = o.getAsJsonArray("families");
                recipes.put(e.getKey(), new Recipe(fam.get(0).getAsString(), fam.size() > 1 ? fam.get(1).getAsString() : fam.get(0).getAsString(),
                        o.get("shape").getAsString()));
            }
        } catch (Exception ex) {
            BrightCore.LOGGER.error("[BrightCore] couldn't read fx_recipes.json", ex);
        }
        return recipes;
    }

    public static Recipe get(String abilityId) { return all().get(abilityId); }

    public static java.util.Set<String> ids() { return all().keySet(); }

    private static Supplier<SimpleParticleType> fam(String name) {
        var h = BrightParticles.ALL.get(name);
        return h != null ? h : BrightParticles.AURA;
    }

    // plays the recipe for a skill. target can be null for stuff centred on yourself
    public static void play(ServerLevel l, String abilityId, LivingEntity caster, Vec3 target) {
        com.brightwebs.brightcore.api.util.Safe.run("FxRecipes.play " + abilityId, () -> {
            Recipe r = get(abilityId);
            if (r == null) return;
            var a = fam(r.primary()); var b = fam(r.accent());
            Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle(), feet = caster.position();
            Vec3 at = target != null ? target : eye.add(look.scale(6));
            switch (r.shape()) {
                case "cone" -> { Fx.cone(l, a, eye.add(look.scale(0.6)), look, 0.5, 0.25, 24); Fx.cone(l, b, eye.add(look.scale(0.6)), look, 0.35, 0.3, 8); }
                case "beam" -> { Fx.beam(l, a, eye.add(look.scale(0.5)), at, 0.25); Fx.beam(l, b, eye.add(look.scale(0.5)), at, 0.9); Fx.burst(l, b, at, 16, 0.2); }
                case "projectile" -> { Fx.beam(l, b, eye.add(look.scale(0.5)), at, 0.6); Fx.sphere(l, a, at, 0.4, 14); Fx.shockwave(l, at, 1); Fx.burst(l, a, at, 20, 0.2); }
                case "area" -> { Fx.magicCircle(l, a, at, 3, caster.tickCount); Fx.column(l, a, at, 3, 30, 0.08); Fx.cloud(l, b, at.add(0, 1, 0), 16, 1.5); }
                case "slash" -> {
                    Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
                    for (int i = -4; i <= 4; i++) Fx.move(l, a, eye.add(look.scale(1.6)).add(side.scale(i * 0.25)).add(0, -Math.abs(i) * 0.06, 0), look.scale(0.15));
                    Fx.burst(l, b, eye.add(look.scale(2)), 8, 0.2);
                }
                case "sense" -> { Fx.ring(l, a, feet.add(0, 0.1, 0), 1.0, 14, 0.1); Fx.aura(l, b, caster, 8); }
                case "guard" -> { Fx.sphere(l, a, feet.add(0, caster.getBbHeight() / 2, 0), caster.getBbWidth() + 0.4, 18); Fx.burst(l, b, feet.add(0, 1, 0), 6, 0.1); }
                case "heal" -> { Fx.aura(l, a, caster, 14); Fx.spiral(l, b, feet, 0.8, 2, 2, 16, caster.tickCount * 0.2); }
                case "move" -> { Fx.beam(l, a, feet.add(0, 1, 0), at, 0.4); Fx.burst(l, b, feet.add(0, 1, 0), 10, 0.2); Fx.burst(l, b, at, 10, 0.2); }
                case "summon" -> { Fx.magicCircle(l, a, at, 1.5, caster.tickCount); Fx.spiral(l, a, at, 1.2, 2.5, 2, 30, 0); Fx.burst(l, b, at.add(0, 1, 0), 20, 0.25); }
                case "awaken" -> { Fx.spiral(l, a, feet, 0.9, 2.6, 3, 40, 0); Fx.spiral(l, b, feet, 0.9, 2.6, 3, 40, Math.PI); Fx.shockwave(l, feet.add(0, 1, 0), 1.5); }
                default -> { Fx.aura(l, a, caster, 12); Fx.burst(l, b, feet.add(0, 1, 0), 8, 0.12); }
            }
        });
    }
}

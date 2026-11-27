package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.skill.LoreSkills.Spec;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

// how each lore skill looks. every skill has two sprite kinds (f1, f2), two colours (c1, c2) and a pattern per
// kind of thing it does, picked from tensura's / manas reborn's version of it or the lore (tools/gen_lore_fx.py).
// colours go out as dust, sprites as our own particles, so two skills with the same shape still look different
final class LoreFx {
    private LoreFx() {}

    // ------------------------------------------------------------ little pieces

    static Supplier<SimpleParticleType> sprite(String fam) {
        var h = BrightParticles.ALL.get(fam);
        return h != null ? h : BrightParticles.AURA;
    }

    static void dust(ServerLevel l, int color, Vec3 at, int n, double spread, float size) {
        l.sendParticles(new DustParticleOptions(color, size), at.x, at.y, at.z, n, spread, spread, spread, 0);
    }

    static void fade(ServerLevel l, int from, int to, Vec3 at, int n, double spread, float size) {
        l.sendParticles(new DustColorTransitionOptions(from, to, size), at.x, at.y, at.z, n, spread, spread, spread, 0);
    }

    static void dustLine(ServerLevel l, int color, Vec3 a, Vec3 b, double step, float size) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3 u = d.scale(1 / len);
        for (double t = 0; t <= len; t += step) dust(l, color, a.add(u.scale(t)), 1, 0.02, size);
    }

    static void dustRing(ServerLevel l, int color, Vec3 c, double r, int n, float size, double tilt) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            dust(l, color, c.add(Math.cos(a) * r, Math.sin(a) * r * tilt, Math.sin(a) * r * (1 - Math.abs(tilt))), 1, 0, size);
        }
    }

    // a jagged lightning line from a to b, with a branch or two
    static void zigzag(ServerLevel l, Spec s, Vec3 a, Vec3 b, int segs, double jag, boolean sprites) {
        bolt(l, s.c1(), s.c2(), sprites ? sprite(s.f1()) : null, a, b, segs, jag);
    }

    static void bolt(ServerLevel l, int c1, int c2, Supplier<SimpleParticleType> spr, Vec3 a, Vec3 b, int segs, double jag) {
        Vec3 prev = a;
        java.util.Random r = new java.util.Random();
        for (int i = 1; i <= segs; i++) {
            Vec3 next = a.lerp(b, i / (double) segs);
            if (i < segs) next = next.add((r.nextDouble() - 0.5) * jag, (r.nextDouble() - 0.5) * jag, (r.nextDouble() - 0.5) * jag);
            dustLine(l, c1, prev, next, 0.12, 1.1f);
            if (i % 2 == 0) dustLine(l, c2, prev, next, 0.3, 0.7f);
            if (spr != null && i % 3 == 0) Fx.burst(l, spr, next, 2, 0.05);
            if (i == segs / 2 && segs > 4) {
                Vec3 br = next.add((r.nextDouble() - 0.5) * jag * 3, (r.nextDouble() - 0.5) * jag * 2, (r.nextDouble() - 0.5) * jag * 3);
                dustLine(l, c2, next, br, 0.18, 0.8f);
            }
            prev = next;
        }
    }

    private static Vec3 side(Vec3 look) {
        Vec3 s = look.cross(new Vec3(0, 1, 0));
        return s.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : s.normalize();
    }

    // ------------------------------------------------------------ bolts (shots)

    // drawn every tick while a bolt flies
    static void boltTrail(ServerLevel l, Spec s, Vec3 from, Vec3 to, int age) {
        String p = s.pat("projectile");
        Vec3 dir = to.subtract(from).normalize(), sd = side(dir), up = sd.cross(dir);
        switch (p) {
            case "lance" -> { dustLine(l, s.c1(), from.subtract(dir.scale(1.2)), to, 0.12, 1.2f); Fx.move(l, sprite(s.f1()), to, dir.scale(0.05)); }
            case "star" -> {
                for (int i = 0; i < 4; i++) { double a = age * 0.6 + i * Math.PI / 2; dust(l, s.c2(), to.add(sd.scale(Math.cos(a) * 0.45)).add(up.scale(Math.sin(a) * 0.45)), 1, 0, 1f); }
                dust(l, s.c1(), to, 3, 0.08, 1.4f); Fx.beam(l, sprite(s.f1()), from, to, 0.5);
            }
            case "helix" -> {
                for (double t = 0; t < 1; t += 0.2) {
                    Vec3 at = from.lerp(to, t); double a = (age + t) * 2.2;
                    dust(l, s.c1(), at.add(sd.scale(Math.cos(a) * 0.35)).add(up.scale(Math.sin(a) * 0.35)), 1, 0, 1f);
                    dust(l, s.c2(), at.add(sd.scale(-Math.cos(a) * 0.35)).add(up.scale(-Math.sin(a) * 0.35)), 1, 0, 1f);
                }
                Fx.burst(l, sprite(s.f1()), to, 1, 0.02);
            }
            case "crescent" -> { for (int i = -4; i <= 4; i++) dust(l, i % 2 == 0 ? s.c1() : s.c2(), to.add(sd.scale(i * 0.18)).subtract(dir.scale(Math.abs(i) * 0.06)), 1, 0, 1.1f); }
            case "meteor" -> { fade(l, s.c1(), s.c2(), to, 4, 0.15, 1.6f); Fx.beam(l, sprite(s.f1()), from, to, 0.3); Fx.burst(l, sprite(s.f2()), from, 2, 0.05); }
            default -> { Fx.beam(l, sprite(s.f1()), from, to, 0.35); dust(l, s.c1(), to, 3, 0.12, 1.3f); Fx.burst(l, sprite(s.f2()), to, 1, 0.02); }
        }
    }

    static void boltImpact(ServerLevel l, Spec s, Vec3 at, float r) {
        Fx.sphere(l, sprite(s.f1()), at, Math.max(0.5, r * 0.5), 14);
        fade(l, s.c1(), s.c2(), at, 24, Math.max(0.3, r * 0.4), 1.4f);
        if (s.pat("projectile").equals("star")) for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4; dustLine(l, s.c2(), at, at.add(Math.cos(a) * 1.6, 0.3, Math.sin(a) * 1.6), 0.2, 0.9f);
        }
        dustRing(l, s.c2(), at, Math.max(1, r), 20, 1f, 0);
        Fx.burst(l, sprite(s.f2()), at, 14, 0.18);
        Fx.shockwave(l, at, Math.max(1, r / 2));
    }

    // ------------------------------------------------------------ rays, breaths, storms, strikes

    static void beam(ServerLevel l, Spec s, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize(), sd = side(dir), up = sd.cross(dir);
        double len = to.distanceTo(from);
        switch (s.pat("beam")) {
            case "zigzag" -> { zigzag(l, s, from, to, Math.max(6, (int) (len / 1.5)), 0.9, true); zigzag(l, s, from, to, Math.max(4, (int) (len / 2.5)), 1.4, false); }
            case "helix" -> {
                for (double t = 0; t < len; t += 0.25) {
                    double a = t * 1.6; Vec3 at = from.add(dir.scale(t));
                    dust(l, s.c1(), at.add(sd.scale(Math.cos(a) * 0.45)).add(up.scale(Math.sin(a) * 0.45)), 1, 0, 1f);
                    dust(l, s.c2(), at.add(sd.scale(-Math.cos(a) * 0.45)).add(up.scale(-Math.sin(a) * 0.45)), 1, 0, 1f);
                }
                Fx.beam(l, sprite(s.f1()), from, to, 0.6);
            }
            case "strike" -> {
                Vec3 sky = to.add(0, 14, 0);
                dustLine(l, s.c1(), sky, to, 0.15, 1.8f); dustLine(l, s.c2(), sky.add(0.3, 0, 0), to, 0.3, 0.9f);
                Fx.column(l, sprite(s.f1()), to, 0.6, 30, 0.15);
            }
            case "wide" -> {
                for (int k = -1; k <= 1; k++) dustLine(l, k == 0 ? s.c1() : s.c2(), from.add(sd.scale(k * 0.4)), to.add(sd.scale(k * 0.4)), 0.2, 1.2f);
                for (double t = 1; t < len; t += 3) dustRing(l, s.c2(), from.add(dir.scale(t)), 0.7, 10, 0.8f, 1);
                Fx.beam(l, sprite(s.f1()), from, to, 0.4);
            }
            default -> { dustLine(l, s.c1(), from, to, 0.12, 1.3f); Fx.beam(l, sprite(s.f1()), from, to, 0.35); Fx.beam(l, sprite(s.f2()), from, to, 1.2); }
        }
        fade(l, s.c1(), s.c2(), to, 16, 0.4, 1.3f);
        Fx.burst(l, sprite(s.f2()), to, 12, 0.15);
    }

    // breaths start at the mouth: eye height, a bit forward and lower
    static Vec3 mouth(LivingEntity e) {
        return e.getEyePosition().add(e.getLookAngle().scale(0.35)).add(0, -0.22, 0);
    }

    static void cone(ServerLevel l, Spec s, LivingEntity p, double range) {
        Vec3 m = mouth(p), look = p.getLookAngle(), sd = side(look), up = sd.cross(look);
        switch (s.pat("cone")) {
            case "vortex" -> {
                for (double t = 0.3; t < range; t += 0.35) { double a = t * 2.4, r = t * 0.22; dust(l, t % 0.7 < 0.35 ? s.c1() : s.c2(), m.add(look.scale(t)).add(sd.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r)), 1, 0, 1.2f); }
                Fx.cone(l, sprite(s.f1()), m, look, 0.5, 0.2, 18);
            }
            case "fan" -> {
                for (double t = 1; t < range; t += 1.2) for (int i = -5; i <= 5; i++) dust(l, s.c1(), m.add(look.scale(t)).add(sd.scale(i * t * 0.09)), 1, 0.03, 1f);
                if (s.f1().contains("spark")) for (int i = 0; i < 3; i++) zigzag(l, s, m, m.add(look.scale(range)).add(sd.scale((i - 1) * range * 0.25)), 6, 0.5, false);
                Fx.cone(l, sprite(s.f2()), m, look, 0.5, 0.25, 12);
            }
            case "roar" -> {
                for (double t = 1; t < range; t += 1.5) dustRing(l, t % 3 < 1.5 ? s.c1() : s.c2(), m.add(look.scale(t)), 0.3 + t * 0.25, 14, 1f, 1);
                Fx.cone(l, BrightParticles.SONIC, m, look, 0.5, 0.2, 6);
            }
            default -> {
                // the spray narrows at the mouth and opens up as it goes
                Fx.cone(l, sprite(s.f1()), m, look, 0.6, 0.18, 30);
                Fx.cone(l, sprite(s.f2()), m, look, 0.45, 0.24, 10);
                for (double t = 0.4; t < range; t += 0.5) dust(l, s.c1(), m.add(look.scale(t)), 2, t * 0.12, 1.1f);
            }
        }
    }

    static void area(ServerLevel l, Spec s, Vec3 at, float r, int tick) {
        switch (s.pat("area")) {
            case "pillar" -> {
                for (double y = 0; y < 9; y += 0.3) dustRing(l, y % 0.6 < 0.3 ? s.c1() : s.c2(), at.add(0, y, 0), r * 0.35, 8, 1.2f, 0);
                if (s.f1().contains("spark")) zigzag(l, s, at.add(0, 12, 0), at, 10, 0.8, true);
                Fx.column(l, sprite(s.f1()), at, r * 0.4, 40, 0.2);
            }
            case "rain" -> {
                java.util.Random rnd = new java.util.Random();
                for (int i = 0; i < 40; i++) {
                    Vec3 top = at.add((rnd.nextDouble() - 0.5) * r * 2, 6 + rnd.nextDouble() * 3, (rnd.nextDouble() - 0.5) * r * 2);
                    dustLine(l, i % 3 == 0 ? s.c2() : s.c1(), top, top.add(0, -1.2, 0), 0.3, 1f);
                    if (i % 4 == 0) Fx.move(l, sprite(s.f1()), top, new Vec3(0, -0.6, 0));
                }
            }
            case "nova" -> {
                for (int k = 1; k <= 3; k++) dustRing(l, k == 2 ? s.c2() : s.c1(), at.add(0, 0.3, 0), r * k / 3.0, 16 + k * 8, 1.4f, 0);
                Fx.sphere(l, sprite(s.f1()), at.add(0, 1, 0), r * 0.6, 30);
                Fx.shockwave(l, at, r / 2);
            }
            case "dome" -> {
                for (int i = 0; i < 70; i++) {
                    double u = Math.random(), a = Math.random() * Math.PI * 2, k = Math.sqrt(1 - u * u);
                    dust(l, i % 3 == 0 ? s.c2() : s.c1(), at.add(k * Math.cos(a) * r, u * r, k * Math.sin(a) * r), 1, 0, 1.2f);
                }
                Fx.burst(l, sprite(s.f1()), at.add(0, r / 2, 0), 10, 0.1);
            }
            case "vortex" -> {
                for (int i = 0; i < 60; i++) { double k = i / 60.0, a = k * 14 + tick * 0.3, rr = r * (1 - k); dust(l, i % 2 == 0 ? s.c1() : s.c2(), at.add(Math.cos(a) * rr, k * 4, Math.sin(a) * rr), 1, 0, 1.2f); }
                Fx.spiral(l, sprite(s.f1()), at, r * 0.6, 4, 3, 30, tick * 0.3);
            }
            case "quake" -> {
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2 / 7 + Math.random() * 0.4;
                    Vec3 end = at.add(Math.cos(a) * r * 1.3, 0.1, Math.sin(a) * r * 1.3);
                    dustLine(l, s.c1(), at.add(0, 0.1, 0), end, 0.25, 1.3f);
                    Fx.burst(l, sprite(s.f1()), end, 4, 0.15);
                }
                Fx.shockwave(l, at, r / 2);
            }
            case "meteors" -> {
                java.util.Random rnd = new java.util.Random();
                for (int i = 0; i < 6; i++) {
                    Vec3 hit = at.add((rnd.nextDouble() - 0.5) * r * 2, 0, (rnd.nextDouble() - 0.5) * r * 2);
                    dustLine(l, s.c1(), hit.add(-2, 10, -1), hit, 0.25, 1.6f);
                    Fx.burst(l, sprite(s.f1()), hit, 8, 0.18);
                }
                Fx.shockwave(l, at, r / 2);
            }
            default -> {
                Fx.magicCircle(l, sprite(s.f1()), at.add(0, 0.1, 0), r, tick);
                dustRing(l, s.c1(), at.add(0, 0.1, 0), r, 30, 1.3f, 0);
                for (int i = 0; i < 30; i++) dust(l, s.c2(), at.add((Math.random() - 0.5) * r * 2, Math.random() * 2.5, (Math.random() - 0.5) * r * 2), 1, 0, 1f);
            }
        }
        fade(l, s.c1(), s.c2(), at.add(0, 1, 0), 20, r * 0.4, 1.3f);
    }

    static void slash(ServerLevel l, Spec s, LivingEntity p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), sd = side(look), up = sd.cross(look);
        Vec3 mid = eye.add(look.scale(1.8));
        switch (s.pat("slash")) {
            case "cross" -> { arc(l, s, mid, sd.add(up).normalize(), look, 1.6); arc(l, s, mid, sd.subtract(up).normalize(), look, 1.6); }
            case "flurry" -> { for (int i = 0; i < 4; i++) { double a = Math.random() * Math.PI; arc(l, s, mid.add(look.scale(i * 0.3)), sd.scale(Math.cos(a)).add(up.scale(Math.sin(a))).normalize(), look, 1.2); } }
            case "cleave" -> arc(l, s, mid, up, look, 2.0);
            case "wave" -> { for (int k = 0; k < 4; k++) arc(l, s, eye.add(look.scale(1.5 + k * 1.6)), sd, look, 1.3 + k * 0.2); }
            case "spin" -> { dustRing(l, s.c1(), p.position().add(0, 1, 0), 2.2, 40, 1.4f, 0); dustRing(l, s.c2(), p.position().add(0, 1, 0), 1.7, 28, 1f, 0); Fx.ring(l, sprite(s.f1()), p.position().add(0, 1, 0), 2, 16, 0.2); }
            default -> arc(l, s, mid, sd, look, 1.8);
        }
        Fx.burst(l, sprite(s.f2()), mid, 8, 0.18);
    }

    // one sweeping crescent: across 'axis', bowed toward 'look'
    private static void arc(ServerLevel l, Spec s, Vec3 mid, Vec3 axis, Vec3 look, double half) {
        for (int i = -10; i <= 10; i++) {
            double t = i / 10.0;
            Vec3 at = mid.add(axis.scale(t * half)).subtract(look.scale(t * t * 0.6));
            dust(l, Math.abs(i) < 4 ? s.c2() : s.c1(), at, 1, 0, 1.3f);
            if (i % 3 == 0) Fx.move(l, sprite(s.f1()), at, look.scale(0.2));
        }
    }

    // ------------------------------------------------------------ things that sit on you (aura, guard, sight, awaken)

    // strength: 1 for the start (big), 0 for the every-few-ticks version while it stays on
    static void self(ServerLevel l, Spec s, LivingEntity p, String kind, int tick, boolean start) {
        String pat = s.pat(kind);
        Vec3 feet = p.position(), mid = feet.add(0, p.getBbHeight() / 2, 0), head = feet.add(0, p.getBbHeight() + 0.3, 0);
        int n = start ? 3 : 1;
        float rr = p.getBbWidth();
        switch (pat) {
            case "rise" -> { for (int i = 0; i < 4 * n; i++) dust(l, i % 2 == 0 ? s.c1() : s.c2(), feet.add((Math.random() - 0.5) * 1.2, Math.random() * 2, (Math.random() - 0.5) * 1.2), 1, 0, 1.2f); Fx.aura(l, sprite(s.f1()), p, n * 2); }
            case "orbit" -> { for (int k = 0; k < 2; k++) for (int i = 0; i < 3 * n; i++) { double a = tick * 0.25 + i * 2.1 + k * Math.PI; dust(l, k == 0 ? s.c1() : s.c2(), mid.add(Math.cos(a) * 0.9, (k == 0 ? 0.4 : -0.3) + Math.sin(tick * 0.1) * 0.2, Math.sin(a) * 0.9), 1, 0, 1.1f); } }
            case "halo" -> { dustRing(l, s.c2(), head, 0.4, 10 * n, 0.9f, 0); dust(l, s.c1(), mid, 2 * n, 0.4, 1f); }
            case "crackle" -> {
                // little lightning arcs running over the body
                for (int i = 0; i < n * 2; i++) {
                    Vec3 a = feet.add((Math.random() - 0.5) * rr * 1.4, Math.random() * p.getBbHeight(), (Math.random() - 0.5) * rr * 1.4);
                    zigzag(l, s, a, a.add((Math.random() - 0.5) * 0.9, (Math.random() - 0.3) * 0.9, (Math.random() - 0.5) * 0.9), 4, 0.25, i == 0);
                }
            }
            case "drip" -> { for (int i = 0; i < 3 * n; i++) Fx.move(l, sprite(s.f1()), feet.add((Math.random() - 0.5) * 0.8, 0.8 + Math.random() * 1.2, (Math.random() - 0.5) * 0.8), new Vec3(0, -0.12, 0)); dust(l, s.c1(), mid, 2 * n, 0.3, 1f); }
            case "wings" -> {
                Vec3 back = feet.add(0, 1.35, 0).subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.35));
                Vec3 sd = side(p.getLookAngle());
                double flap = Math.sin(tick * 0.15) * 0.25;
                for (int w = -1; w <= 1; w += 2) for (int i = 1; i <= 7; i++) for (int j = 0; j < i / 2 + 1; j++) {
                    double x = i * 0.22, y = 0.5 - i * 0.07 - j * 0.22 + (w > 0 ? flap : flap) * x;
                    if (!start && (i + j + tick) % 2 == 1) continue;
                    dust(l, j == 0 ? s.c2() : s.c1(), back.add(sd.scale(w * x)).add(0, y, 0), 1, 0, 1f);
                }
            }
            case "mist" -> { fade(l, s.c1(), s.c2(), feet.add(0, 0.3, 0), 5 * n, 0.6, 1.8f); Fx.cloud(l, sprite(s.f1()), feet.add(0, 0.5, 0), n * 2, 0.6); }
            case "runes", "radar" -> {
                double r = 1.4;
                dustRing(l, s.c1(), feet.add(0, 0.05, 0), r, 18 * n, 1f, 0);
                dustRing(l, s.c2(), feet.add(0, 0.05, 0), r * 0.6, 10 * n, 0.9f, 0);
                double a = tick * 0.15; dustLine(l, s.c2(), feet.add(0, 0.06, 0), feet.add(Math.cos(a) * r, 0.06, Math.sin(a) * r), 0.2, 0.8f);
            }
            case "dome" -> { for (int i = 0; i < 12 * n; i++) { double u = Math.random() * 2 - 1, a = Math.random() * Math.PI * 2, k = Math.sqrt(1 - u * u); dust(l, i % 3 == 0 ? s.c2() : s.c1(), mid.add(k * Math.cos(a) * 1.3, u * 1.3, k * Math.sin(a) * 1.3), 1, 0, 1f); } }
            case "hex" -> { for (int i = 0; i < 6; i++) { double a = tick * 0.06 + i * Math.PI / 3; Vec3 c = mid.add(Math.cos(a) * 1.1, 0, Math.sin(a) * 1.1); dustRing(l, i % 2 == 0 ? s.c1() : s.c2(), c, 0.25, 6, 0.9f, 1); } }
            case "shards" -> { for (int i = 0; i < 5; i++) { double a = tick * 0.2 + i * 1.256; Fx.move(l, sprite(s.f1()), mid.add(Math.cos(a) * 1.1, Math.sin(a * 2) * 0.4, Math.sin(a) * 1.1), Vec3.ZERO); } dust(l, s.c1(), mid, n, 0.6, 1f); }
            case "wall" -> { Vec3 f = feet.add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2)), sd = side(p.getLookAngle()); for (int i = 0; i < 10 * n; i++) dust(l, i % 2 == 0 ? s.c1() : s.c2(), f.add(sd.scale((Math.random() - 0.5) * 2.2)).add(0, Math.random() * 2.2, 0), 1, 0, 1.2f); }
            case "cocoon" -> { for (int i = 0; i < 10 * n; i++) { double k = (i + tick % 10) / 10.0, a = k * 12; dust(l, s.c1(), feet.add(Math.cos(a) * 0.7, (k % 1) * 2.2, Math.sin(a) * 0.7), 1, 0, 1f); } }
            case "pillar" -> { for (double y = 0; y < (start ? 12 : 4); y += 0.4) dustRing(l, y % 0.8 < 0.4 ? s.c1() : s.c2(), feet.add(0, y, 0), 0.6, 6, 1.3f, 0); }
            case "explode" -> { if (start) { Fx.shockwave(l, mid, 2); Fx.sphere(l, sprite(s.f1()), mid, 2, 30); } fade(l, s.c1(), s.c2(), mid, 4 * n, 0.6, 1.4f); }
            case "crown" -> { for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4 + tick * 0.03; Vec3 b = head.add(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35); dustLine(l, s.c2(), b, b.add(0, 0.25, 0), 0.1, 0.8f); } dustRing(l, s.c1(), head, 0.35, 12, 0.9f, 0); }
            case "storm" -> { for (int i = 0; i < n; i++) { double a = Math.random() * Math.PI * 2; Vec3 g = feet.add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5); zigzag(l, s, g.add(0, 6, 0), g, 7, 0.6, true); } }
            case "pulse" -> dustRing(l, s.c1(), feet.add(0, 0.1, 0), 1 + (tick % 20) * 0.4, 24, 1f, 0);
            case "eye" -> {
                // an eye that opens above your head
                Vec3 c = head.add(0, 0.4, 0), sd = side(p.getLookAngle());
                for (int i = -6; i <= 6; i++) { double x = i * 0.08, y = 0.22 * (1 - (i / 6.0) * (i / 6.0)); dust(l, s.c1(), c.add(sd.scale(x)).add(0, y, 0), 1, 0, 0.8f); dust(l, s.c1(), c.add(sd.scale(x)).add(0, -y, 0), 1, 0, 0.8f); }
                dust(l, s.c2(), c, 3, 0.04, 1.4f);
            }
            case "threads" -> {
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(14), e -> e != p && e.isAlive())) {
                    if ((tick + e.getId()) % 3 != 0 && !start) continue;
                    dustLine(l, s.c1(), mid, e.position().add(0, e.getBbHeight() / 2, 0), 0.5, 0.6f);
                }
            }
            default -> Fx.aura(l, sprite(s.f1()), p, 2 * n);
        }
        if (start) { Fx.burst(l, sprite(s.f2()), mid, 12, 0.15); fade(l, s.c1(), s.c2(), mid, 12, 0.5, 1.3f); }
    }

    // ------------------------------------------------------------ heals, moves, summons, hits

    static void heal(ServerLevel l, Spec s, LivingEntity p) {
        Vec3 f = p.position();
        switch (s.pat("heal")) {
            case "rain" -> { for (int i = 0; i < 30; i++) Fx.move(l, sprite(s.f1()), f.add((Math.random() - 0.5) * 4, 3 + Math.random(), (Math.random() - 0.5) * 4), new Vec3(0, -0.25, 0)); }
            case "bloom" -> { for (int k = 0; k < 8; k++) { double a = k * Math.PI / 4; dustLine(l, k % 2 == 0 ? s.c1() : s.c2(), f.add(0, 0.1, 0), f.add(Math.cos(a) * 1.6, 0.6, Math.sin(a) * 1.6), 0.15, 1.2f); } Fx.burst(l, BrightParticles.PETAL, f.add(0, 1, 0), 10, 0.15); }
            case "crosses" -> { for (int i = 0; i < 5; i++) { Vec3 c = f.add((Math.random() - 0.5) * 1.6, 0.5 + Math.random() * 1.6, (Math.random() - 0.5) * 1.6); dustLine(l, s.c1(), c.add(0, -0.2, 0), c.add(0, 0.2, 0), 0.1, 1f); dustLine(l, s.c1(), c.add(-0.2, 0, 0), c.add(0.2, 0, 0), 0.1, 1f); } }
            default -> Fx.spiral(l, sprite(s.f1()), f, 0.8, 2.2, 2, 24, 0);
        }
        fade(l, s.c1(), s.c2(), f.add(0, 1, 0), 16, 0.5, 1.2f);
    }

    static void move(ServerLevel l, Spec s, Vec3 from, Vec3 to) {
        switch (s.pat("move")) {
            case "trail" -> { for (double t = 0; t <= 1; t += 0.12) { Vec3 at = from.lerp(to, t); dustLine(l, s.c1(), at, at.add(0, 1.8, 0), 0.25, 1.1f); } }
            case "shadow" -> { fade(l, s.c1(), 0x050208, from.add(0, 0.2, 0), 30, 0.5, 2f); fade(l, s.c1(), 0x050208, to.add(0, 0.2, 0), 30, 0.5, 2f); }
            case "dash" -> { Vec3 sd = side(to.subtract(from).normalize()); for (int k = -2; k <= 2; k++) dustLine(l, k == 0 ? s.c2() : s.c1(), from.add(0, 0.4 + Math.abs(k) * 0.35, 0).add(sd.scale(k * 0.2)), to.add(0, 0.4 + Math.abs(k) * 0.35, 0), 0.3, 0.9f); }
            case "flash" -> zigzag(l, s, from.add(0, 1, 0), to.add(0, 1, 0), 8, 0.6, true);
            default -> { Fx.beam(l, sprite(s.f2()), from.add(0, 1, 0), to.add(0, 1, 0), 0.5); }
        }
        Fx.burst(l, sprite(s.f1()), from.add(0, 1, 0), 16, 0.15);
        Fx.burst(l, sprite(s.f1()), to.add(0, 1, 0), 16, 0.15);
    }

    static void summon(ServerLevel l, Spec s, Vec3 at, Vec3 facing, int tick) {
        switch (s.pat("summon")) {
            case "portal" -> { Vec3 sd = side(facing); for (int i = 0; i < 36; i++) { double a = i * Math.PI / 18; dust(l, i % 2 == 0 ? s.c1() : s.c2(), at.add(0, 1.2, 0).add(sd.scale(Math.cos(a) * 1.1)).add(0, Math.sin(a) * 1.3, 0), 1, 0, 1.3f); } }
            case "call" -> zigzag(l, s, at.add(0, 12, 0), at, 10, 0.8, true);
            default -> { dustRing(l, s.c1(), at.add(0, 0.05, 0), 1.4, 24, 1.3f, 0); for (double y = 0; y < 2.5; y += 0.25) dustRing(l, s.c2(), at.add(0, y, 0), 0.7, 6, 1f, 0); }
        }
        Fx.magicCircle(l, sprite(s.f1()), at.add(0, 0.05, 0), 1.4, tick);
        Fx.spiral(l, sprite(s.f2()), at, 1, 2.4, 2, 24, 0);
    }

    // essence pulled out of a target and flowing into you (drain and devour skills)
    static void essence(ServerLevel l, int c1, int c2, LivingEntity from, LivingEntity to) {
        Vec3 a = from.position().add(0, from.getBbHeight() / 2, 0), b = to.position().add(0, to.getBbHeight() / 2, 0);
        for (int i = 0; i < 10; i++) {
            Vec3 at = a.lerp(b, Math.random()).add((Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4);
            l.sendParticles(new DustColorTransitionOptions(c1, c2, 1.1f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        Vec3 dir = b.subtract(a).normalize();
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, a.x, a.y, a.z, 0, dir.x, dir.y, dir.z, 0.35);
    }
}

package com.brightwebs.brightcore.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

// effect shapes made out of our particles, server side. skills use these for their look
// (burst, ring, beam...) so everything looks consistent
// particles sent with count 0 use dx/dy/dz as velocity
public final class Fx {
    private Fx() {}

    private static ParticleOptions p(Supplier<SimpleParticleType> type) { return type.get(); }

    // one particle moving at this velocity
    public static void move(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 at, Vec3 vel) {
        l.sendParticles(p(type), at.x, at.y, at.z, 0, vel.x, vel.y, vel.z, 1);
    }

    // puff of particles flying out from a point
    public static void burst(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) move(l, type, at, randomDir(l).scale(speed * (0.4 + l.getRandom().nextDouble() * 0.6)));
    }

    // particles scattered in a box around a point, drifting
    public static void cloud(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 at, int count, double spread) {
        l.sendParticles(p(type), at.x, at.y, at.z, count, spread, spread, spread, 0.01);
    }

    // flat ring (on the ground or around a body). outward > 0 makes it grow
    public static void ring(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 c, double radius, int points, double outward) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points, x = Math.cos(a), z = Math.sin(a);
            move(l, type, c.add(x * radius, 0, z * radius), new Vec3(x * outward, 0, z * outward));
        }
    }

    // hollow ball of particles
    public static void sphere(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 c, double radius, int points) {
        for (int i = 0; i < points; i++) move(l, type, c.add(randomDir(l).scale(radius)), Vec3.ZERO);
    }

    // straight beam from one point to another
    public static void beam(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        int n = Math.max(1, (int) (d.length() / step));
        for (int i = 0; i <= n; i++) move(l, type, from.add(d.scale(i / (double) n)), Vec3.ZERO);
    }

    // spiral going up (auras, summons, transformations)
    public static void spiral(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 base, double radius, double height, double turns, int points, double phase) {
        for (int i = 0; i < points; i++) {
            double t = i / (double) points, a = phase + t * turns * Math.PI * 2;
            move(l, type, base.add(Math.cos(a) * radius, t * height, Math.sin(a) * radius), new Vec3(0, 0.02, 0));
        }
    }

    // column coming up from the ground (pillars, healing, ascending)
    public static void column(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 base, double radius, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = l.getRandom().nextDouble() * Math.PI * 2, r = l.getRandom().nextDouble() * radius;
            move(l, type, base.add(Math.cos(a) * r, 0, Math.sin(a) * r), new Vec3(0, speed * (0.5 + l.getRandom().nextDouble()), 0));
        }
    }

    // cone spraying in a direction (breaths, sprays)
    public static void cone(ServerLevel l, Supplier<SimpleParticleType> type, Vec3 from, Vec3 dir, double speed, double spread, int count) {
        Vec3 n = dir.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 v = n.add(randomDir(l).scale(spread)).normalize().scale(speed * (0.6 + l.getRandom().nextDouble() * 0.6));
            move(l, type, from, v);
        }
    }

    // magic circle on the ground, outer ring with runes spinning inside
    public static void magicCircle(ServerLevel l, Supplier<SimpleParticleType> ringType, Vec3 c, double radius, int tick) {
        ring(l, ringType, c.add(0, 0.05, 0), radius, 24, 0);
        for (int i = 0; i < 6; i++) {
            double a = tick * 0.1 + i * Math.PI / 3;
            move(l, BrightParticles.RUNE, c.add(Math.cos(a) * radius * 0.6, 0.1, Math.sin(a) * radius * 0.6), new Vec3(0, 0.01, 0));
        }
    }

    // shockwave ring plus a ground ring spreading out
    public static void shockwave(ServerLevel l, Vec3 c, double power) {
        move(l, BrightParticles.SHOCKWAVE, c, Vec3.ZERO);
        ring(l, BrightParticles.GUST, c, 0.5, 16, 0.25 * power);
    }

    // aura rising around a body
    public static void aura(ServerLevel l, Supplier<SimpleParticleType> type, Entity e, int count) {
        column(l, type, e.position(), e.getBbWidth() * 0.7, count, 0.04);
    }

    private static Vec3 randomDir(ServerLevel l) {
        double u = l.getRandom().nextDouble() * 2 - 1, a = l.getRandom().nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
        return new Vec3(s * Math.cos(a), u, s * Math.sin(a));
    }
}

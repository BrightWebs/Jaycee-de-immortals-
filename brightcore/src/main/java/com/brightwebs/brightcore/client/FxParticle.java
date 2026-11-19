package com.brightwebs.brightcore.client;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.fx.BrightParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.Map;

/** How each particle family moves and fades. */
@EventBusSubscriber(modid = BrightCore.ID, value = Dist.CLIENT)
public final class FxParticle extends SingleQuadParticle {
    /**
     * life in ticks (min, extra random), gravity (negative rises), friction, start size, end size
     * (relative), spin per tick, glow (full bright), animate (frames over life) or pick a random frame.
     */
    public record Style(int life, int lifeRand, float gravity, float friction, float size, float endSize,
                        float spin, boolean glow, boolean animate) {}

    private static final Map<String, Style> STYLES = Map.ofEntries(
            Map.entry("flame", new Style(14, 8, -0.02f, 0.92f, 0.22f, 0.3f, 0, true, true)),
            Map.entry("black_flame", new Style(16, 8, -0.02f, 0.92f, 0.26f, 0.3f, 0, true, true)),
            Map.entry("ember", new Style(20, 12, -0.006f, 0.96f, 0.08f, 0.2f, 0, true, true)),
            Map.entry("frost", new Style(30, 12, 0.012f, 0.96f, 0.14f, 0.7f, 0.12f, true, false)),
            Map.entry("spark", new Style(4, 4, 0, 0.8f, 0.2f, 0.8f, 0, true, false)),
            Map.entry("dark_spark", new Style(4, 4, 0, 0.8f, 0.2f, 0.8f, 0, true, false)),
            Map.entry("shockwave", new Style(10, 0, 0, 1f, 0.6f, 6f, 0, true, true)),
            Map.entry("soul", new Style(30, 10, -0.008f, 0.96f, 0.18f, 0.6f, 0, true, true)),
            Map.entry("spirit", new Style(22, 10, -0.004f, 0.94f, 0.14f, 0.3f, 0, true, true)),
            Map.entry("mist", new Style(26, 10, -0.002f, 0.9f, 0.35f, 2.2f, 0.02f, false, true)),
            Map.entry("heal", new Style(20, 8, -0.02f, 0.94f, 0.13f, 0.5f, 0, true, true)),
            Map.entry("rune", new Style(22, 10, -0.01f, 0.9f, 0.18f, 0.9f, 0.04f, true, false)),
            Map.entry("aura", new Style(18, 8, -0.025f, 0.94f, 0.2f, 0.5f, 0, true, true)),
            Map.entry("holy", new Style(16, 6, -0.004f, 0.92f, 0.2f, 0.4f, 0.08f, true, true)),
            Map.entry("void", new Style(20, 8, 0, 0.88f, 0.2f, 1.6f, 0, false, true)),
            Map.entry("bubble", new Style(30, 10, -0.01f, 0.96f, 0.14f, 1.2f, 0, false, true)),
            Map.entry("gust", new Style(12, 4, 0, 0.98f, 0.4f, 1.2f, 0.05f, false, true)),
            Map.entry("petal", new Style(60, 20, 0.004f, 0.98f, 0.12f, 1f, 0.06f, false, false)),
            Map.entry("shard", new Style(24, 10, 0.06f, 0.98f, 0.14f, 1f, 0.15f, false, false)),
            Map.entry("blood", new Style(28, 10, 0.06f, 0.98f, 0.09f, 1f, 0, false, false)),
            Map.entry("drop", new Style(28, 10, 0.06f, 0.98f, 0.09f, 1f, 0, false, false)),
            Map.entry("acid", new Style(28, 10, 0.06f, 0.98f, 0.1f, 1f, 0, true, false)),
            Map.entry("predator_mist", new Style(12, 6, 0, 0.86f, 0.42f, 1.9f, 0.09f, false, false)),
            Map.entry("slash", new Style(6, 2, 0, 0.9f, 0.8f, 1.3f, 0, true, true)));

    private final SpriteSet sprites;
    private final Style style;
    private final float startSize;
    private float spin;

    FxParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Style style, RandomSource r) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.style = style;
        this.xd = dx; this.yd = dy; this.zd = dz;
        this.gravity = style.gravity();
        this.friction = style.friction();
        this.lifetime = style.life() + (style.lifeRand() > 0 ? r.nextInt(style.lifeRand()) : 0);
        this.startSize = style.size() * (0.8f + r.nextFloat() * 0.4f);
        this.quadSize = startSize;
        this.hasPhysics = style.gravity() > 0.03f;
        this.spin = style.spin() * (r.nextBoolean() ? 1 : -1);
        this.roll = r.nextFloat() * (float) Math.PI * 2 * (style.spin() != 0 ? 1 : 0);
        this.oRoll = roll;
        if (style.animate()) setSpriteFromAge(sprites); else setSprite(sprites.get(r));
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float t = age / (float) lifetime;
        quadSize = startSize * (1 + (style.endSize() - 1) * t);
        alpha = t < 0.7f ? 1 : Math.max(0, 1 - (t - 0.7f) / 0.3f);
        oRoll = roll;
        roll += spin;
        if (style.animate()) setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() { return Layer.TRANSLUCENT; }

    @Override
    protected int getLightCoords(float partial) {
        return style.glow() ? 0xF000F0 : super.getLightCoords(partial);
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        BrightParticles.ALL.forEach((name, holder) -> {
            Style style = STYLES.getOrDefault(name, new Style(20, 8, 0, 0.95f, 0.15f, 1f, 0, false, true));
            event.registerSpriteSet(holder.get(), sprites -> (ParticleProvider<SimpleParticleType>)
                    (type, level, x, y, z, dx, dy, dz, r) -> new FxParticle(level, x, y, z, dx, dy, dz, sprites, style, r));
        });
    }
}

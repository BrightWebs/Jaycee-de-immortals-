package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.effect.JayceeEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

// when you're scared by haki the edges of the screen go dark and close in, worse the higher the fear.
// tensura does a screen overlay for fear too
final class FearOverlay {
    private FearOverlay() {}

    static void render(GuiGraphicsExtractor g, int sw, int sh) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        MobEffectInstance fear = p.getEffect(JayceeEffects.FEAR);
        if (fear == null) return;
        float strength = Math.min(1, (fear.getAmplifier() + 1) / 10f);
        // breathing pulse so it feels like pressure, not just a dark frame
        float pulse = 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        int alpha = (int) (170 * strength * pulse);
        int edge = (int) (Math.min(sw, sh) * (0.12f + 0.18f * strength));
        int dark = (alpha << 24) | 0x05030A;
        g.fillGradient(0, 0, sw, edge, dark, 0x00000000);
        g.fillGradient(0, sh - edge, sw, sh, 0x00000000, dark);
        // sides: draw thin vertical strips getting lighter toward the middle
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            int a = (int) (alpha * (1 - i / (float) steps));
            int w = Math.max(1, edge / steps);
            int c = (a << 24) | 0x05030A;
            g.fill(i * w, 0, (i + 1) * w, sh, c);
            g.fill(sw - (i + 1) * w, 0, sw - i * w, sh, c);
        }
    }
}

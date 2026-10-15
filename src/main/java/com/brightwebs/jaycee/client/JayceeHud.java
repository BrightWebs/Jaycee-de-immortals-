package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.config.HudConfig;
import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.PlayerState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.UUID;
import java.util.function.IntFunction;

@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class JayceeHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JayceeMod.ID, "gameplay_hud");
    private static final int RESOURCE_WIDTH = 188, RESOURCE_HEIGHT = 58;
    private static final int SLOT_WIDTH = 86, SLOT_HEIGHT = 38, STACK_HEIGHT = 158;
    private static HudMotion hp = new HudMotion(), sp = new HudMotion(), mana = new HudMotion();
    private static final long[] lastCooldown = new long[4];
    private static final double[] activation = new double[4], ready = new double[4];
    private static final String[] lastAbility = {"", "", "", ""};
    private static IntFunction<Component> keyLabels = slot -> Component.literal("—");
    private static UUID playerId;
    private static Object levelIdentity;
    private static long lastFrame;
    private static boolean healthBarRendered;

    private JayceeHud() {}

    /** Supply translated labels from the actual rebindable slot KeyMappings. */
    public static void setSlotKeyProvider(IntFunction<Component> provider) {
        keyLabels = java.util.Objects.requireNonNull(provider);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.PLAYER_HEALTH, LAYER, JayceeHud::render);
    }

    private static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return HudConfig.ENABLED.get() && mc.player != null && mc.level != null
                && !mc.player.isSpectator() && !mc.options.hideGui
                && !mc.getDebugOverlay().showDebugScreen() && mc.screen == null;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        healthBarRendered = false;
        if (!visible() || graphics.guiWidth() < 1 || graphics.guiHeight() < 1) {
            lastFrame = 0;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!mc.player.getUUID().equals(playerId) || levelIdentity != mc.level) {
            playerId = mc.player.getUUID();
            levelIdentity = mc.level;
            hp = new HudMotion();
            sp = new HudMotion();
            mana = new HudMotion();
            java.util.Arrays.fill(lastAbility, "");
            java.util.Arrays.fill(lastCooldown, 0);
            java.util.Arrays.fill(activation, 0);
            java.util.Arrays.fill(ready, 0);
            lastFrame = 0;
        }
        long now = System.nanoTime();
        double seconds = lastFrame == 0 || mc.isPaused() ? 0 : Math.max(0, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        PlayerState state = PlayerState.get(mc.player);
        hp.update(mc.player.getHealth() / Math.max(1.0, mc.player.getMaxHealth()), seconds, true);
        sp.update(state.soul() / state.maxSoul(), seconds, false);
        mana.update(state.mana() / state.maxMana(), seconds, false);
        float scale = (float) Math.min(HudConfig.SCALE.get(),
                Math.min(graphics.guiWidth() / (double) RESOURCE_WIDTH, graphics.guiHeight() / (double) STACK_HEIGHT));
        int width = Math.max(1, (int) (graphics.guiWidth() / scale));
        int height = Math.max(1, (int) (graphics.guiHeight() / scale));
        var resources = HudLayout.place(HudConfig.RESOURCE_ANCHOR.get(), HudConfig.RESOURCE_X.get(),
                HudConfig.RESOURCE_Y.get(), RESOURCE_WIDTH, RESOURCE_HEIGHT, width, height);
        var abilities = HudLayout.place(HudConfig.ABILITY_ANCHOR.get(), HudConfig.ABILITY_X.get(),
                HudConfig.ABILITY_Y.get(), SLOT_WIDTH, STACK_HEIGHT, width, height);
        graphics.pose().pushPose();
        try {
            graphics.pose().scale(scale, scale, 1);
            drawResources(graphics, resources.x(), resources.y(), state);
            healthBarRendered = true;
            for (int slot = 0; slot < 4; slot++) {
                drawSlot(graphics, abilities.x(), abilities.y() + slot * 40, slot, state, seconds);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void drawResources(GuiGraphics graphics, int x, int y, PlayerState state) {
        Minecraft mc = Minecraft.getInstance();
        panel(graphics, x, y, RESOURCE_WIDTH, RESOURCE_HEIGHT, 0xFF5D5278);
        PlayerPortrait.render(graphics, mc.player, x + 5, y + 7, 44, state.portrait3d());
        drawBar(graphics, x + 55, y + 5, hp, "HP", mc.player.getHealth(), mc.player.getMaxHealth(), 0xFFD14759, true);
        drawBar(graphics, x + 55, y + 22, sp, "SP", state.soul(), state.maxSoul(), 0xFFA454DC, false);
        drawBar(graphics, x + 55, y + 39, mana, "MP", state.mana(), state.maxMana(), 0xFF398DDE, false);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, HudMotion motion, String label,
                                double current, double maximum, int color, boolean trail) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x, y, color, false);
        String value = (int) Math.ceil(current) + "/" + (int) Math.ceil(maximum);
        graphics.drawString(font, value, x + 126 - font.width(value), y, 0xFFE8E4F0, false);
        int top = y + 10;
        graphics.fill(x, top, x + 126, top + 4, 0xFF211D30);
        if (trail) graphics.fill(x, top, x + (int) (126 * motion.trail()), top + 4, 0xFFDBA4A3);
        graphics.fill(x, top, x + (int) (126 * motion.value()), top + 4, color);
        if (motion.pulse() > 0) {
            int alpha = (int) (motion.pulse() * 90);
            graphics.fill(x, top, x + 126, top + 4, alpha << 24 | 0xFFFFFF);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int slot, PlayerState state, double seconds) {
        Minecraft mc = Minecraft.getInstance();
        String id = state.ability(state.page(), slot);
        AbilityCatalog.Art art = AbilityCatalog.find(id);
        long cooldown = state.cooldownRemaining(id, mc.level.getGameTime());
        if (id.equals(lastAbility[slot])) {
            if (cooldown > lastCooldown[slot]) activation[slot] = 1;
            if (cooldown == 0 && lastCooldown[slot] > 0) ready[slot] = 1;
        } else {
            activation[slot] = ready[slot] = 0;
        }
        lastAbility[slot] = id;
        lastCooldown[slot] = cooldown;
        activation[slot] = Math.max(0, activation[slot] - seconds * 2.5);
        ready[slot] = Math.max(0, ready[slot] - seconds * 1.8);
        int border = blend(0xFF514D6B, 0xFF8CCCB4, ready[slot]);
        border = blend(border, 0xFFA97DDD, activation[slot]);
        panel(graphics, x, y, SLOT_WIDTH, SLOT_HEIGHT, border);
        graphics.drawString(mc.font, keyLabels.apply(slot), x + 4, y + 4, 0xFFE7DFEF, false);
        if (art == null) {
            graphics.fill(x + 5, y + 16, x + 23, y + 17, 0xFF625B78);
            graphics.drawString(mc.font, "Empty", x + 29, y + 15, 0xFF928A9D, false);
            return;
        }
        AbilityIcons.render(graphics, art, x + 5, y + 15, 18, false);
        if (cooldown > 0) {
            double fraction = Math.clamp(cooldown / (double) Math.max(1, art.cooldownTicks()), 0, 1);
            graphics.fill(x + 5, y + 15, x + 23, y + 15 + (int) Math.ceil(18 * fraction), 0xB00B0914);
        }
        String status = !art.implemented() ? "Preview" : cooldown > 0
                ? String.format(java.util.Locale.ROOT, "%.1fs", cooldown / 20.0) : "Ready";
        graphics.drawString(mc.font, status, x + 29, y + 4, cooldown > 0 ? 0xFFC1A7E8 : 0xFF9BCCB8, false);
        String mode = art.modes().isEmpty() ? "—" : art.modes().get(Math.clamp(state.mode(state.page(), slot), 0, art.modes().size() - 1));
        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(mode, SLOT_WIDTH - 31), x + 29, y + 16, 0xFFE0D7EF, false);
        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(art.name(), SLOT_WIDTH - 31), x + 29, y + 27, 0xFF9C94AC, false);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int border) {
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xCE151221);
    }

    private static int blend(int base, int highlight, double amount) {
        int red = (int) (((base >> 16) & 255) * (1 - amount) + ((highlight >> 16) & 255) * amount);
        int green = (int) (((base >> 8) & 255) * (1 - amount) + ((highlight >> 8) & 255) * amount);
        int blue = (int) ((base & 255) * (1 - amount) + (highlight & 255) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
    public static final class LayerEvents {
        private LayerEvents() {}

        @SubscribeEvent(receiveCanceled = true)
        public static void beforeLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS) || event.getName().equals(LAYER)) {
                healthBarRendered = false;
            }
            if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                    && healthBarRendered && visible() && !HudConfig.SHOW_VANILLA_HEARTS.get()) {
                event.setCanceled(true);
            }
        }
    }
}

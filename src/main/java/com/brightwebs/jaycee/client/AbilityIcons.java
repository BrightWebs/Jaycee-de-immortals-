package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.config.HudConfig;
import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared resource-pack icons and tooltip layout for HUD slots and ability menus. */
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AbilityIcons {
    private static final Map<ResourceLocation, Boolean> AVAILABLE = new HashMap<>();

    private AbilityIcons() {}

    public static ResourceLocation texture(String abilityId) {
        ResourceLocation id = ResourceLocation.tryParse(abilityId);
        String path = id == null ? "unknown" : id.getPath().replace("..", "_");
        return ResourceLocation.parse(HudConfig.ICON_DIRECTORY.get() + "/" + path + ".png");
    }

    public static void render(GuiGraphics graphics, String abilityId, int x, int y, int size, boolean selected) {
        render(graphics, texture(abilityId), abilityId, x, y, size, selected);
    }

    public static void render(GuiGraphics graphics, AbilityCatalog.Art art, int x, int y, int size, boolean selected) {
        ResourceLocation location = HudConfig.ICON_DIRECTORY.get().equals("jaycee:textures/ability")
                ? art.icon() : texture(art.id());
        render(graphics, location, art.id(), x, y, size, selected);
    }

    private static void render(GuiGraphics graphics, ResourceLocation texture, String abilityId,
                               int x, int y, int size, boolean selected) {
        boolean available = AVAILABLE.computeIfAbsent(texture, location -> {
            boolean found = Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
            if (!found) LogUtils.getLogger().warn("Missing ability icon {}; using procedural glyph", location);
            return found;
        });
        if (available) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(texture, x, y, 0, 0, size, size, size, size);
            RenderSystem.disableBlend();
        } else {
            int ordinal = AbilityCatalog.ALL.indexOf(AbilityCatalog.find(abilityId));
            DivineGlyph.icon(graphics, x, y, size, Math.max(0, ordinal), selected);
        }
    }

    public static List<Component> tooltip(Component name, Component description, Component cost,
                                           Component mode, long remainingTicks) {
        return List.of(name, description, cost, mode, remainingTicks > 0
                ? Component.literal(String.format(java.util.Locale.ROOT, "Cooldown: %.1fs", remainingTicks / 20.0))
                : Component.literal("Ready"));
    }

    public static List<Component> tooltip(AbilityCatalog.Art art, int mode, long remainingTicks) {
        String selectedMode = art.modes().isEmpty() ? "—"
                : art.modes().get(Math.clamp(mode, 0, art.modes().size() - 1));
        return tooltip(Component.literal(art.name()),
                Component.literal(art.implemented() ? "Equippable ability" : "Preview only — not implemented"),
                Component.literal("Mana cost: " + art.manaCost()),
                Component.literal("Mode: " + selectedMode), remainingTicks);
    }

    public static void renderTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        UiText.tooltip(graphics, lines, mouseX, mouseY);
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> AVAILABLE.clear());
    }
}

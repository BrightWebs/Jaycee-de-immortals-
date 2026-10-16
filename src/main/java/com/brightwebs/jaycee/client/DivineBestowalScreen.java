package com.brightwebs.jaycee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * First playable N-key bestowal frontend. Sends the same server-side /jc
 * transactions used by other command callers; never modifies essence locally.
 */
public final class DivineBestowalScreen extends Screen {
    private final Entity target;
    private EditBox divineName;
    private EditBox essenceAmount;
    private String warning = "";

    public DivineBestowalScreen(Entity target) {
        super(Component.literal("Divine Bestowal"));
        this.target = target;
    }

    public static void openForTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            mc.setScreen(new DivineBestowalScreen(hit.getEntity()));
        } else {
            mc.player.displayClientMessage(Component.literal("Look at a living player or mob to bestow essence."), true);
        }
    }

    @Override
    protected void init() {
        int x = width / 2;
        int y = height / 2;
        divineName = new EditBox(font, x - 98, y - 42, 196, 20, Component.literal("Optional Divine Name"));
        divineName.setMaxLength(40);
        divineName.setValue(target.getCustomName() != null
            ? target.getCustomName().getString() : target.getName().getString());
        addRenderableWidget(divineName);

        essenceAmount = new EditBox(font, x - 98, y + 2, 196, 20, Component.literal("Essence Amount"));
        essenceAmount.setMaxLength(10);
        essenceAmount.setValue("100");
        addRenderableWidget(essenceAmount);

        addRenderableWidget(Button.builder(Component.literal("Bless"), b -> bless())
            .bounds(x - 98, y + 34, 94, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(x + 4, y + 34, 94, 20).build());
        setInitialFocus(divineName);
    }

    private void bless() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        final int amount;
        try {
            amount = Integer.parseInt(essenceAmount.getValue().trim());
        } catch (NumberFormatException ex) {
            warning = "Enter a valid positive whole-number amount.";
            return;
        }
        if (amount <= 0 || amount > 1_000_000_000) {
            warning = "Amount must be between 1 and 1,000,000,000.";
            return;
        }
        // UUID direct entity lookup is supported by the server's entity argument.
        String targetArgument = target.getUUID().toString();
        String name = divineName.getValue().trim();
        String original = target.getCustomName() != null
            ? target.getCustomName().getString() : target.getName().getString();
        String command = "jc bestow " + targetArgument + " " + amount;
        if (!name.isEmpty() && !name.equals(original)) command += " " + name;
        mc.player.connection.sendCommand(command);
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        renderBackground(g, mx, my, dt);
        int x = width / 2, y = height / 2;
        DivineFrame.frame(g, x - 122, y - 104, 244, 184, true);
        g.drawCenteredString(font, "DIVINE BESTOWAL", x, y - 88, 0xFFEEDFA8);
        g.drawCenteredString(font, font.plainSubstrByWidth(target.getName().getString(), 215), x, y - 70, 0xFFB8DFFF);
        g.drawString(font, "Name (keep or edit):", x - 98, y - 54, 0xFFB5D9F4);
        g.drawString(font, "Essence to bestow:", x - 98, y - 10, 0xFFB5D9F4);
        if (!warning.isEmpty()) g.drawCenteredString(font, warning, x, y + 62, 0xFFFF6677);
        super.render(g, mx, my, dt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

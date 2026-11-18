package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.network.BrightNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

/**
 * The reincarnation menu, shown once on first join. Starting races on the left (two columns),
 * the chosen race's description, nature and stats on the right, then Be Reborn or Random.
 * It can't be closed without choosing.
 */
public final class RaceSelectScreen extends Screen {
    private static final int W = 460, H = 280;
    private List<Race> races = List.of();
    private int picked = 0;
    private int left, top;

    public RaceSelectScreen() {
        super(Component.translatable("jaycee.race_menu.title"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        races = RaceRegistry.starting();
        int rows = (races.size() + 1) / 2;
        int rowH = Math.min(24, (H - 72) / Math.max(1, rows));
        for (int i = 0; i < races.size(); i++) {
            final int index = i;
            int col = i % 2, row = i / 2;
            addRenderableWidget(new OrnateButton(races.get(i).name().getString(),
                    left + 12 + col * 98, top + 44 + row * rowH, 94, rowH - 3, () -> picked = index));
        }
        addRenderableWidget(new OrnateButton(Component.translatable("jaycee.race_menu.choose").getString(),
                left + 222, top + H - 38, 112, 26, this::choose));
        addRenderableWidget(new OrnateButton(Component.translatable("jaycee.race_menu.random").getString(),
                left + 338, top + H - 38, 110, 26, () -> send("random")));
    }

    private void choose() {
        if (!races.isEmpty()) send(races.get(picked).id().toString());
    }

    private void send(String race) {
        ClientPacketDistributor.sendToServer(new BrightNetwork.ChooseRace(race));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        extractBackground(g, mx, my, dt);
        DivineFrame.frame(g, left, top, W, H, false);
        DivineFrame.ribbon(g, left + 6, top + 5, W - 12, 30);
        g.centeredText(font, title, left + W / 2, top + 16, 0xFFEEDFA8);

        DivineFrame.frame(g, left + 212, top + 40, W - 222, H - 86, false);
        if (!races.isEmpty()) {
            Race r = races.get(picked);
            int x = left + 224, y = top + 52;
            g.text(font, r.name(), x, y, r.alignment().color(), false);
            g.text(font, Component.literal(r.alignment().label()), x, y + 12, 0xFFB8DDFF, false);
            int line = 0;
            for (var row : font.split(r.description(), W - 250)) {
                if (line < 5) g.text(font, row, x, y + 28 + line * 11, 0xFF9FC2E2, false);
                line++;
            }
            int sy = y + 90;
            stat(g, x, sy, "Health", String.valueOf((int) r.health()));
            stat(g, x, sy + 12, "Aura", EnergyAPI.format(r.minAura()) + " - " + EnergyAPI.format(r.maxAura()));
            stat(g, x, sy + 24, "Mana", EnergyAPI.format(r.minMana()) + " - " + EnergyAPI.format(r.maxMana()));
            stat(g, x, sy + 36, "Spirit", String.valueOf((int) r.spirit()));
            stat(g, x, sy + 48, "Strength", String.format("%.1f", r.attack()));
            stat(g, x, sy + 60, "Speed", String.format("%.0f%%", r.speed() / 0.1 * 100));
            stat(g, x, sy + 72, "Size", String.format("%.0f%%", r.size() * 100));
        }
        super.extractRenderState(g, mx, my, dt);
        // Gold ring around the picked race's button.
        if (!races.isEmpty()) {
            int rows = (races.size() + 1) / 2;
            int rowH = Math.min(24, (H - 72) / Math.max(1, rows));
            int bx = left + 12 + (picked % 2) * 98, by = top + 44 + (picked / 2) * rowH;
            g.fill(bx, by + rowH - 4, bx + 94, by + rowH - 3, 0xFFF3CF82);
        }
    }

    private void stat(GuiGraphicsExtractor g, int x, int y, String label, String value) {
        g.text(font, label, x, y, 0xFF7FA6CF, false);
        g.text(font, value, x + 70, y, 0xFFE4EBFF, false);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }
}

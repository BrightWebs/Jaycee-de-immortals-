package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.network.JayceeNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// the naming pop up (N on something), tensura's naming menu but in our style:
// type a name (or roll one), pick subdue / evolve / endow, then hit name
public class NamingScreen extends Screen {
    private static final float W = 560, H = 360;
    private static final String[] RANDOM = {"Ranga", "Gobta", "Benimaru", "Shuna", "Souei", "Hakurou", "Shion", "Geld",
            "Gabiru", "Diablo", "Testarossa", "Ultima", "Carrera", "Kurobe", "Rigurd", "Kaijin", "Treyni", "Milim"};
    private static final String[] KEYS = {"subdue", "evolve", "endow"};
    private static final int[] COLORS = {0xFFE05A6A, 0xFFF2C14E, 0xFF6BE07A};

    private final JayceeNetwork.OpenNaming info;
    private String name = "";
    private int type = -1;
    private float k, left, top;

    public NamingScreen(JayceeNetwork.OpenNaming info) {
        super(Component.translatable("jaycee.naming.title"));
        this.info = info;
    }

    @Override
    protected void init() {
        // the name box is the whole point of this screen, so typing is on while it is open
        com.brightwebs.brightcore.api.util.Safe.run("NamingScreen.text", () -> UI.textInput(this, true));
        k = Math.min(0.42f, Math.min(width * 0.9f / W, height * 0.9f / H));  // follows GUI scale
        left = (width - W * k) / 2;
        top = (height - H * k) / 2;
    }

    private double fx(double x) { return (x - left) / k; }
    private double fy(double y) { return (y - top) / k; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        com.brightwebs.brightcore.api.util.Safe.run("NamingScreen.render", () -> {
            double x = fx(mx), y = fy(my);
            g.pose().pushMatrix();
            g.pose().translate(left, top);
            g.pose().scale(k, k);
            // frame
            UI.fill(g, 0, 0, W, H, 0xF0050A18);
            UI.rect(g, 0, 0, W, H, 3, 0xFF4FE3FF);
            UI.rect(g, 6, 6, W - 6, H - 6, 1, 0x887A5CFF);
            UI.fill(g, 0, 0, W, 54, 0xFF0E0A28);
            UI.fill(g, 0, 52, W, 54, 0xFFB46CFF);
            UI.text(g, Component.translatable("jaycee.naming.title"), W / 2, 27, 26, 0xFFFFFFFF, 1);

            // target
            UI.semi(g, info.name() + "  •  " + info.race(), W / 2, 76, 17, 0xFFB8E8F8, 1);
            UI.semi(g, Component.translatable("jaycee.naming.target_ep", UI.num(info.ep())), W / 2, 98, 15, 0xFF9FB8C8, 1);

            // name box + dice
            boolean fieldHover = Page.in(x, y, 60, 118, 440, 160);
            UI.fill(g, 60, 118, 440, 160, 0xFF0A1424);
            UI.rect(g, 60, 118, 440, 160, 2, fieldHover ? 0xFF9AF4FF : 0xFF4FE3FF);
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String shown = name.isEmpty() ? "" : name;
            if (name.isEmpty()) UI.semi(g, Component.translatable("jaycee.naming.placeholder"), 76, 139, 17, 0xFF5F7890, 0);
            UI.text(g, shown + (blink ? "_" : ""), 76, 139, 19, 0xFFFFFFFF, 0);
            boolean diceHover = Page.in(x, y, 452, 118, 500, 160);
            UI.fill(g, 452, 118, 500, 160, diceHover ? 0xFF1A2A44 : 0xFF0E1A2E);
            UI.rect(g, 452, 118, 500, 160, 2, 0xFFB46CFF);
            UI.text(g, "⚄", 476, 139, 26, 0xFFFFFFFF, 1);

            // the three choices
            for (int i = 0; i < 3; i++) {
                float bx = 40 + i * 166, by = 178;
                boolean hover = Page.in(x, y, bx, by, bx + 148, by + 96), sel = type == i;
                UI.fill(g, bx, by, bx + 148, by + 96, sel ? 0xFF132A40 : hover ? 0xFF0F2034 : 0xFF0A1424);
                UI.rect(g, bx, by, bx + 148, by + 96, sel ? 3 : 2, sel ? 0xFF9AF4FF : COLORS[i]);
                UI.text(g, Component.translatable("jaycee.naming." + KEYS[i]), bx + 74, by + 20, 19, COLORS[i], 1);
                double gain = i == 0 ? info.gain0() : i == 1 ? info.gain1() : info.gain2();
                double cost = i == 0 ? info.cost0() : i == 1 ? info.cost1() : info.cost2();
                UI.semi(g, Component.translatable("jaycee.naming.gain", UI.shortNum(gain)), bx + 74, by + 48, 14, 0xFFB8E8F8, 1);
                UI.semi(g, Component.translatable("jaycee.naming.cost", UI.shortNum(cost)), bx + 74, by + 68, 14, 0xFFE8B8C0, 1);
            }
            if (type >= 0) UI.semi(g, Component.translatable("jaycee.naming." + KEYS[type] + ".desc"), W / 2, 290, 13, 0xFF9FB8C8, 1);

            // name button
            boolean ready = type >= 0 && !name.isBlank();
            boolean btnHover = Page.in(x, y, 190, 306, 370, 346);
            UI.fill(g, 190, 306, 370, 346, ready ? (btnHover ? 0xFF1E5A6A : 0xFF124050) : 0xFF141C28);
            UI.rect(g, 190, 306, 370, 346, 2, ready ? 0xFF4FE3FF : 0xFF3A4A5A);
            UI.text(g, Component.translatable("jaycee.naming.name"), 280, 326, 20, ready ? 0xFFFFFFFF : 0xFF6A7A8A, 1);
            g.pose().popMatrix();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return com.brightwebs.brightcore.api.util.Safe.call("NamingScreen.click", () -> {
            if (UI.button(event) != 0) return false;
            double x = fx(event.x()), y = fy(event.y());
            if (Page.in(x, y, 452, 118, 500, 160)) {
                name = RANDOM[(int) (Math.random() * RANDOM.length)];
                MenuScreen.click();
                return true;
            }
            for (int i = 0; i < 3; i++) {
                float bx = 40 + i * 166;
                if (Page.in(x, y, bx, 178, bx + 148, 274)) { type = i; MenuScreen.click(); return true; }
            }
            if (Page.in(x, y, 190, 306, 370, 346) && type >= 0 && !name.isBlank()) {
                ClientPacketDistributor.sendToServer(new JayceeNetwork.NameRequest(info.entity(), name.strip(), type));
                MenuScreen.click();
                onClose();
                return true;
            }
            return false;
        }, false);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (name.length() < 24 && cp >= 32 && cp != 127 && Character.isValidCodePoint(cp)) {
            name += new String(Character.toChars(cp));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 259) {  // backspace
            if (!name.isEmpty()) name = name.substring(0, name.offsetByCodePoints(name.length(), -1));
            return true;
        }
        if (event.key() == 257 || event.key() == 335) {  // enter
            if (type >= 0 && !name.isBlank()) {
                ClientPacketDistributor.sendToServer(new JayceeNetwork.NameRequest(info.entity(), name.strip(), type));
                onClose();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static void open(JayceeNetwork.OpenNaming info) {
        Minecraft.getInstance().setScreenAndShow(new NamingScreen(info));
    }

    @Override
    public void removed() {
        com.brightwebs.brightcore.api.util.Safe.run("NamingScreen.removed", () -> UI.textInput(this, false));
        super.removed();
    }
}

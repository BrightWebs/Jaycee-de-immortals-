package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.network.JayceeNetwork;
import com.brightwebs.jaycee.network.JayceeNetwork.AppraisalInfo;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Analytical Appraisal, drawn as UI in the style of the approved mock-up. Like Tensura it only
 * shows while you look at something: a creature (read by the server: race, rank, EP, HP / SHP /
 * MP / AP, armor, skills) or a block (read here: hardness, whether your held tool can mine and
 * drop it, explosion resistance, light, growth). Nothing in sight, nothing on screen.
 * Coordinates are design pixels of the 1672 x 941 mock-up.
 */
final class AppraisalPanel {
    private static final float DESIGN_W = 1672, X = 1175, Y = 120, W = 460;
    private static final int[][] BAR = {{0xFFB81A2A, 0xFFFF5A6A}, {0xFFE07010, 0xFFFFD070}, {0xFF1A5AE0, 0xFF46CCFF}, {0xFFE0A000, 0xFFFFF070}};
    private static final String[] ICON = {"heart", "spirit", "mana", "aura"};
    private static final String[] LABEL = {"hp", "shp", "mp", "ap"};

    private AppraisalPanel() {}

    /** Draws a glow-margin sprite (10 px margin around its shape). */
    private static void piece(GuiGraphicsExtractor g, String tex, float x, float y, float w, float h) {
        UI.tex(g, tex, x - 10, y - 10, w + 20, h + 20);
    }

    static void render(GuiGraphicsExtractor g, int sw, int sh) {
        AppraisalInfo info = JayceeNetwork.lastAppraisal;
        if (info.entity() == -1) return; // scan off
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float k = sw / DESIGN_W;
        boolean fresh = info.entity() >= 0 && System.currentTimeMillis() - JayceeNetwork.lastAppraisalAt < 1200;
        Entity e = fresh ? mc.level.getEntity(info.entity()) : null;
        if (e instanceof LivingEntity le) { creature(g, k, info, le); return; }
        if (info.scanMode() == 1) return; // creatures only
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK
                && bh.getLocation().distanceTo(mc.player.getEyePosition()) <= Math.max(5, info.radius())) {
            block(g, k, bh.getBlockPos());
        }
    }

    private static void header(GuiGraphicsExtractor g) {
        piece(g, "appr_head", 65, -6, 330, 40);
        UI.text(g, Component.translatable("jaycee.appraisal.header"), W / 2, 14, 20, 0xFFF4ECFF, 1);
    }

    // ---- creature -------------------------------------------------------------

    private static void creature(GuiGraphicsExtractor g, float k, AppraisalInfo info, LivingEntity le) {
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        g.pose().translate(X, Y);
        piece(g, "appr_frame_e", 0, 0, 460, 532);
        header(g);
        piece(g, "appr_portrait", 24, 56, 90, 90);
        UI.text(g, info.name(), 132, 74, UI.fitSize(info.name(), 26, 14, 300, true), 0xFFFFFFFF, 0);
        UI.fill(g, 132, 96, 436, 97, 0x5540C8F0);
        Component race = Component.translatable("jaycee.appraisal.race", info.race());
        UI.semi(g, UI.fit(race.getString(), 17, 220, false), 132, 120, 17, 0xFFE6ECF5, 0);
        UI.semi(g, Component.translatable("jaycee.appraisal.rank"), 400, 120, 17, 0xFFE6ECF5, 2);
        UI.text(g, StatusData.rank(info.ep()), 420, 119, 24, 0xFFC86BFF, 1);
        UI.fill(g, 132, 140, 436, 141, 0x5540C8F0);
        piece(g, "appr_pill", 125, 158, 210, 36);
        UI.text(g, Component.translatable("jaycee.appraisal.ep", UI.num(info.ep())), W / 2, 176, 21, 0xFFFFFFFF, 1);
        float[][] v = {{info.hp(), info.maxHp()}, {info.spirit(), info.maxSpirit()}, {info.mana(), info.maxMana()}, {info.aura(), info.maxAura()}};
        for (int i = 0; i < 4; i++) {
            float y = 222 + i * 34;
            piece(g, "appr_row", 28, y - 14, 404, 28);
            UI.tex(g, "appr_i_" + ICON[i], 38, y - 11, 22, 22);
            UI.semi(g, Component.translatable("jaycee.appraisal." + LABEL[i]), 66, y, 16, 0xFFFFFFFF, 0);
            float frac = v[i][1] <= 0 ? 0 : Math.min(1, v[i][0] / v[i][1]);
            UI.fill(g, 118, y - 4, 318, y + 4, 0xFF161A26);
            if (frac > 0) {
                UI.fill(g, 118, y - 4, 118 + 200 * frac, y + 4, BAR[i][0]);
                UI.fill(g, 118, y - 4, 118 + 200 * frac, y - 1, BAR[i][1]);
            }
            String val = EnergyAPI.format(v[i][0]) + " / " + EnergyAPI.format(v[i][1]);
            UI.semi(g, val, 424, y, UI.fitSize(val, 14, 9, 96, false), 0xFFFFFFFF, 2);
        }
        UI.tex(g, "appr_i_armor", 38, 352, 22, 22);
        UI.semi(g, Component.translatable("jaycee.appraisal.armor", info.armor()), 66, 363, 16, 0xFFFFFFFF, 0);
        UI.semi(g, Component.translatable("jaycee.appraisal.skills"), W / 2, 398, 18, 0xFFE6DCFF, 1);
        UI.fill(g, 60, 398, 150, 399, 0x6040C8F0);
        UI.fill(g, 310, 398, 400, 399, 0x60B060FF);
        piece(g, "appr_box", 34, 414, 392, 104);
        skills(g, info);
        g.pose().popMatrix();

        // the creature itself, live in the portrait
        int x1 = Math.round((X + 28) * k), y1 = Math.round((Y + 60) * k), x2 = Math.round((X + 110) * k), y2 = Math.round((Y + 142) * k);
        float tall = Math.max(le.getBbHeight(), le.getBbWidth());
        int size = Math.max(4, Math.round((y2 - y1) * 0.8f / Math.max(0.5f, tall)));
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, size, 0.0625f, (x1 + x2) / 2f, (y1 + y2) / 2f, le);
    }

    private static void skills(GuiGraphicsExtractor g, AppraisalInfo info) {
        if (info.skills().isEmpty()) {
            UI.semi(g, Component.translatable("jaycee.appraisal.no_skills"), W / 2, 466, 16, 0xFF9FB8C8, 1);
            return;
        }
        int shown = Math.min(2, info.skills().size());
        for (int i = 0; i < shown; i++) {
            float y = 442 + i * 46;
            Ability a = AbilityRegistry.get(Identifier.tryParse(info.skills().get(i)));
            String name = a == null ? info.skills().get(i) : a.name().getString();
            if (a != null) UI.abilityIcon(g, a, 70, y, 34);
            String label = i == shown - 1 && info.skills().size() > shown ? name + "  +" + (info.skills().size() - shown) : name;
            UI.semi(g, UI.fit(label, 18, 300, false), 100, y, 18, 0xFFFFFFFF, 0);
            if (i < shown - 1) UI.fill(g, 50, y + 23, 410, y + 24, 0x403FC8F0);
        }
    }

    // ---- block ---------------------------------------------------------------

    private static void block(GuiGraphicsExtractor g, float k, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        BlockState st = mc.level.getBlockState(pos);
        float hardness = st.getBlock().defaultDestroyTime();
        boolean tool = mc.player.hasCorrectToolForDrops(st);
        int light = Math.max(mc.level.getBrightness(LightLayer.BLOCK, pos), mc.level.getBrightness(LightLayer.SKY, pos));
        List<Component[]> rows = new ArrayList<>();
        rows.add(new Component[]{Component.translatable("jaycee.appraisal.hardness"),
                hardness < 0 ? Component.translatable("jaycee.appraisal.unbreakable") : Component.literal(String.format(java.util.Locale.US, "%.1f", hardness))});
        rows.add(new Component[]{Component.translatable("jaycee.appraisal.correct_tool"),
                Component.translatable(tool ? "jaycee.appraisal.yes" : "jaycee.appraisal.no")});
        rows.add(new Component[]{Component.translatable("jaycee.appraisal.explosion"),
                Component.literal(String.format(java.util.Locale.US, "%.1f", st.getBlock().getExplosionResistance()))});
        rows.add(new Component[]{Component.translatable("jaycee.appraisal.light"), Component.literal(String.valueOf(light))});
        for (Property<?> prop : st.getProperties()) {
            if (prop instanceof IntegerProperty ip && prop.getName().equals("age")) {
                int age = st.getValue(ip), max = ip.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(age);
                rows.add(new Component[]{Component.translatable("jaycee.appraisal.growth"), Component.literal(age + " / " + max)});
            }
        }
        float h = 104 + rows.size() * 34;
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        g.pose().translate(X, Y);
        UI.tex(g, "appr_frame_b", -10, -10, 480, h + 20);
        header(g);
        String name = st.getBlock().getName().getString();
        UI.text(g, name, W / 2, 62, UI.fitSize(name, 24, 13, 390, true), 0xFFFFFFFF, 1);
        UI.fill(g, 40, 84, 420, 85, 0x5540C8F0);
        for (int i = 0; i < rows.size(); i++) {
            float y = 112 + i * 34;
            piece(g, "appr_row", 28, y - 14, 404, 28);
            UI.semi(g, rows.get(i)[0], 42, y, 15, 0xFFE6ECF5, 0);
            int col = 0xFFFFFFFF;
            if (i == 0 && hardness < 0) col = 0xFFFF5555;
            if (i == 1) col = tool ? 0xFF5FE07A : 0xFFFF5555;
            UI.text(g, rows.get(i)[1], 420, y, 15, col, 2);
        }
        g.pose().popMatrix();
    }
}

package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.skill.BodyArmor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// the 3d bits of body armor that sit on top of the suit: horns, crest, cheek fins, chest plates, spine spikes,
// shoulder fins, bracers, thigh plates, boot claws, and the claw hand when the gauntlet's out.
// our own design in the armorsaurus style (blue scales, bone horns). everything's baked once and cached
public final class BodyArmorRender {
    private BodyArmorRender() {}

    static final ContextKey<Integer> KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "body_armor"));
    static final Identifier TEX = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/entity/body_armor_extra.png");

    // what's worn, as bits
    static final int HELMET = 1, CHEST = 2, LEGS = 4, BOOTS = 8, GAUNTLET = 16;
    // which part of the texture a box uses
    private static final int SCALE = 0, BONE = 24, DARK = 48;
    private static final int HEAD = 0, BODY = 1, RIGHT_ARM = 2, RIGHT_LEG = 4, MAIN_ARM = 6;

    private record Bit(int piece, int part, ModelPart model, boolean mirrored) {}
    private static List<Bit> BITS;

    // one box: which piece shows it, which body part it rides on, its pivot/rotation and its size.
    // made for the right side, then mirrored to the left when side is true
    private static void box(List<Bit> out, int piece, int part, boolean side, int tex,
                            float px, float py, float pz, float rx, float ry, float rz,
                            float x, float y, float z, float w, float h, float d) {
        out.add(new Bit(piece, part, bake(tex, px, py, pz, rx, ry, rz, x, y, z, w, h, d), false));
        if (side) out.add(new Bit(piece, part, bake(tex, -px, py, pz, rx, -ry, -rz, -x - w, y, z, w, h, d), true));
    }

    private static ModelPart bake(int tex, float px, float py, float pz, float rx, float ry, float rz,
                                  float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("b", CubeListBuilder.create().texOffs(0, tex).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, (float) Math.toRadians(rx), (float) Math.toRadians(ry), (float) Math.toRadians(rz)));
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("b");
    }

    private static List<Bit> bits() {
        if (BITS != null) return BITS;
        List<Bit> b = new ArrayList<>();
        // helmet: two swept-back horns, a crest of spikes, a brow ridge and cheek fins
        box(b, HELMET, HEAD, true, BONE, -3.5f, -8.6f, 0, -40, 0, -14, -1, -5, -1, 2, 5, 2);
        box(b, HELMET, HEAD, true, BONE, -3.5f, -8.6f, 0, -58, 0, -16, -0.5f, -9.2f, -0.5f, 1, 4.5f, 1);
        box(b, HELMET, HEAD, false, BONE, 0, -9, -3, -20, 0, 0, -0.5f, -2, -0.5f, 1, 2, 1);
        box(b, HELMET, HEAD, false, BONE, 0, -9, -0.5f, -25, 0, 0, -0.5f, -2.8f, -0.5f, 1, 2.8f, 1);
        box(b, HELMET, HEAD, false, BONE, 0, -9, 2, -30, 0, 0, -0.5f, -2.2f, -0.5f, 1, 2.2f, 1);
        box(b, HELMET, HEAD, false, DARK, 0, 0, 0, 0, 0, 0, -5.2f, -7.4f, -5.5f, 10.4f, 1.2f, 1);
        box(b, HELMET, HEAD, true, SCALE, -5, -5, -2, 0, -18, -8, -0.6f, -2.5f, 0, 0.6f, 4, 5);
        // chest: layered plates, a collar and a row of spikes down the back
        box(b, CHEST, BODY, false, DARK, 0, 0, 0, 0, 0, 0, -4.8f, -0.6f, -3.3f, 9.6f, 1.4f, 6.6f);
        box(b, CHEST, BODY, false, SCALE, 0, 0, 0, 0, 0, 0, -4.6f, 0.8f, -3.3f, 9.2f, 4.5f, 1);
        box(b, CHEST, BODY, false, SCALE, 0, 0, 0, 0, 0, 0, -4.2f, 5.4f, -3.1f, 8.4f, 3, 0.8f);
        box(b, CHEST, BODY, false, SCALE, 0, 0, 0, 0, 0, 0, -3.8f, 8.5f, -3f, 7.6f, 2.5f, 0.8f);
        float[][] spine = {{1.5f, 2}, {4.5f, 1.8f}, {7.5f, 1.5f}, {10, 1.1f}};
        for (float[] s : spine) box(b, CHEST, BODY, false, BONE, 0, s[0], 3, 40, 0, 0, -0.5f, -0.5f, 0, 1, 1.2f, s[1]);
        // shoulders: a guard, a swept fin with a bone tip, and a bracer down the forearm
        box(b, CHEST, RIGHT_ARM, true, SCALE, 0, 0, 0, 0, 0, 0, -4.4f, -3.3f, -3.3f, 5.6f, 3.4f, 6.6f);
        box(b, CHEST, RIGHT_ARM, true, DARK, 0, 0, 0, 0, 0, 0, -4.6f, -0.9f, -3.1f, 1.4f, 2, 6.2f);
        box(b, CHEST, RIGHT_ARM, true, SCALE, -3.6f, -3, 0, 0, 0, -28, -0.3f, -5, -2.5f, 0.6f, 5, 5);
        box(b, CHEST, RIGHT_ARM, true, BONE, -3.6f, -3, 0, 0, 0, -28, -0.5f, -6.4f, -0.5f, 1, 1.6f, 1);
        box(b, CHEST, RIGHT_ARM, true, SCALE, 0, 0, 0, 0, 0, 0, -4.3f, 5.6f, -3.3f, 6.6f, 3.4f, 6.6f);
        // legs: thigh plates and knee guards
        box(b, LEGS, RIGHT_LEG, true, SCALE, 0, 0, 0, 0, 0, 0, -3.3f, 0.6f, -2.9f, 1, 4.6f, 5.8f);
        box(b, LEGS, RIGHT_LEG, true, SCALE, 0, 0, 0, 0, 0, 0, -2.8f, 5, -3.4f, 5.6f, 2.4f, 1);
        // boots: a dark cuff and three claws at the toes
        box(b, BOOTS, RIGHT_LEG, true, DARK, 0, 0, 0, 0, 0, 0, -2.9f, 8.4f, -2.9f, 5.8f, 1.2f, 5.8f);
        for (float cx : new float[]{-1.8f, -0.5f, 0.8f}) box(b, BOOTS, RIGHT_LEG, true, BONE, 0, 0, 0, 0, 0, 0, cx, 11, -4.2f, 1, 1, 1.6f);
        // gauntlet: a scaled fist with three claws (rides on whichever arm is your main one)
        box(b, GAUNTLET, MAIN_ARM, true, SCALE, 0, 0, 0, 0, 0, 0, -4.2f, 6.8f, -3.2f, 6.4f, 4.6f, 6.4f);
        box(b, GAUNTLET, MAIN_ARM, true, DARK, 0, 0, 0, 0, 0, 0, -4.4f, 5.6f, -3.4f, 6.8f, 1.3f, 6.8f);
        for (float cx : new float[]{-3.2f, -1.4f, 0.4f}) box(b, GAUNTLET, MAIN_ARM, true, BONE, cx + 0.4f, 11.2f, -2.6f, -18, 0, 0, -0.4f, 0, -0.4f, 0.8f, 3.2f, 0.8f);
        for (float cx : new float[]{-3.4f, -1.6f, 0.2f}) box(b, GAUNTLET, MAIN_ARM, true, BONE, 0, 0, 0, 0, 0, 0, cx, 7.2f, -3.9f, 1, 1, 0.8f);
        BITS = List.copyOf(b);
        return BITS;
    }

    // which pieces a player has on, read from the scale pieces' item model
    static int worn(Player p) {
        int m = 0;
        if (is(p.getItemBySlot(EquipmentSlot.HEAD), "helmet")) m |= HELMET;
        if (is(p.getItemBySlot(EquipmentSlot.CHEST), "chestplate")) m |= CHEST;
        if (is(p.getItemBySlot(EquipmentSlot.LEGS), "leggings")) m |= LEGS;
        if (is(p.getItemBySlot(EquipmentSlot.FEET), "boots")) m |= BOOTS;
        if (is(p.getMainHandItem(), "gauntlet")) m |= GAUNTLET;
        return m;
    }

    private static boolean is(ItemStack s, String piece) {
        return !s.isEmpty() && BodyArmor.model(piece).equals(s.get(DataComponents.ITEM_MODEL));
    }

    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            com.brightwebs.brightcore.api.util.Safe.run("BodyArmorRender.submit", () -> {
                Integer worn = state.getRenderData(KEY);
                if (worn == null || worn == 0 || state.isInvisible) return;
                int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);
                PlayerModel m = getParentModel();
                boolean leftHanded = state.mainArm == HumanoidArm.LEFT;
                var type = RenderTypes.entityCutout(TEX);
                for (Bit bit : bits()) {
                    if ((worn & bit.piece) == 0) continue;
                    // the gauntlet has a right and a left copy, only the main hand's one shows
                    if (bit.part == MAIN_ARM && bit.mirrored != leftHanded) continue;
                    ModelPart host = switch (bit.part) {
                        case HEAD -> m.head;
                        case RIGHT_ARM, MAIN_ARM -> bit.mirrored ? m.leftArm : m.rightArm;
                        case RIGHT_LEG -> bit.mirrored ? m.leftLeg : m.rightLeg;
                        default -> m.body;
                    };
                    if (!host.visible) continue;
                    pose.pushPose();
                    m.root().translateAndRotate(pose);
                    host.translateAndRotate(pose);
                    collector.submitModelPart(bit.model, pose, type, light, overlay, null, -1, 0);
                    pose.popPose();
                }
            });
        }
    }
}

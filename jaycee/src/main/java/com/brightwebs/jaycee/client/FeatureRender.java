package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.appearance.Appearance;
import com.brightwebs.jaycee.appearance.Features;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws Character Editor features (hair, ears, tails, horns, wings...) on every player, attached to
 * the model's head, body, arms and legs so they move with Minecraft's animations. A fish tail
 * replaces the legs. Everyone sees them: the look is synced data on the player.
 */
@EventBusSubscriber(modid = JayceeMod.ID, value = Dist.CLIENT)
public final class FeatureRender {
    public record Look(Map<String, String> map) {}

    public static final ContextKey<Look> KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(JayceeMod.ID, "look"));
    static final Identifier TEX = Identifier.fromNamespaceAndPath(JayceeMod.ID, "textures/entity/feature.png");
    private static final Map<Features.Box, ModelPart> PARTS = new HashMap<>();

    private FeatureRender() {}

    @SubscribeEvent
    public static void modifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
                com.brightwebs.brightcore.api.util.Safe.run("FeatureRender.modifier", () -> {
                    if (entity instanceof Player p) apply(state, worn(p, Appearance.of(p)));
                });
            }
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getSkins()) {
            var renderer = event.getPlayerRenderer(type);
            if (renderer != null) renderer.addLayer(new Layer(renderer));
        }
    }

    /**
     * What the player shows right now. A Merfolk past its first form adapts: the tail shows only in
     * water and legs return on land. A young Merfolk always has its tail.
     */
    static Map<String, String> worn(Player p, Map<String, String> look) {
        if (!"fish".equals(look.get("tail"))) return look;
        String race = Appearance.raceId(p);
        if (!race.equals("merfolk") && !p.isInWater()) {
            Map<String, String> m = new HashMap<>(look);
            m.remove("tail");
            return m;
        }
        return look;
    }

    /** Puts a look on a render state (also used by the editor's live preview). */
    static void apply(AvatarRenderState state, Map<String, String> look) {
        state.setRenderData(KEY, new Look(look));
        if ("fish".equals(look.get("tail"))) {
            state.overrideModelPartVisibility("right_leg", false);
            state.overrideModelPartVisibility("left_leg", false);
        }
    }

    static ModelPart part(Features.Box b) {
        return PARTS.computeIfAbsent(b, k -> {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("c", CubeListBuilder.create().addBox(k.x(), k.y(), k.z(), k.w(), k.h(), k.d()),
                    PartPose.offsetAndRotation(k.px(), k.py(), k.pz(),
                            (float) Math.toRadians(k.rx()), (float) Math.toRadians(k.ry()), (float) Math.toRadians(k.rz())));
            return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("c");
        });
    }

    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            com.brightwebs.brightcore.api.util.Safe.run("FeatureRender.submit", () -> {
                Look look = state.getRenderData(KEY);
                if (look == null || look.map().isEmpty() || state.isInvisible) return;
                int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);
                PlayerModel m = getParentModel();
                for (String slot : Appearance.WORN) {
                    String id = look.map().get(slot);
                    if (id == null || id.equals("none")) continue;
                    Features.Option o = Features.get(slot, id);
                    if (o == null) continue;
                    for (Features.Box b : o.boxes()) {
                        ModelPart host = switch (b.part()) {
                            case Features.HEAD -> m.head;
                            case Features.RIGHT_ARM -> m.rightArm;
                            case Features.LEFT_ARM -> m.leftArm;
                            case Features.RIGHT_LEG -> m.rightLeg;
                            case Features.LEFT_LEG -> m.leftLeg;
                            default -> m.body;
                        };
                        if (!host.visible && b.part() >= Features.RIGHT_LEG) continue;
                        pose.pushPose();
                        m.root().translateAndRotate(pose);
                        host.translateAndRotate(pose);
                        boolean glow = b.tint() == Features.GLOW;
                        collector.submitModelPart(part(b), pose, glow ? RenderTypes.eyes(TEX) : RenderTypes.entityCutout(TEX),
                                glow ? 0xF000F0 : light, overlay, null, Appearance.color(look.map(), o, b.tint()), 0);
                        pose.popPose();
                    }
                }
            });
        }
    }
}

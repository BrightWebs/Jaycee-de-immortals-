package com.brightwebs.jaycee.gameplay.divine;

import com.brightwebs.jaycee.gameplay.GameplayNetwork;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Existing world blocks provide self-appraisal; no foreign player statistics are broadcast. */
public final class DivineAppraisal {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.ConfigValue<String> BLOCK;
    private static final ModConfigSpec.LongValue COOLDOWN, KILL_REWARD, REWARD_BUDGET, REWARD_WINDOW;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Sneak-use the configured existing block to appraise your own divine progression.")
            .define("appraisalEnabled", true);
        BLOCK = b.define("appraisalBlock", "minecraft:enchanting_table",
            value -> value instanceof String s && net.minecraft.resources.ResourceLocation.tryParse(s) != null);
        COOLDOWN = b.defineInRange("appraisalCooldownTicks", 100L, 1L, 72_000L);
        KILL_REWARD = b.comment("Implementation tuning: direct player/projectile hostile kills only, once per victim. Zero disables.")
            .defineInRange("hostileKillEssence", 10L, 0L, 1_000_000L);
        REWARD_BUDGET = b.defineInRange("combatEssencePerWindow", 100L, 0L, 1_000_000L);
        REWARD_WINDOW = b.defineInRange("combatRewardWindowTicks", 1200L, 20L, 72_000L);
        SPEC = b.build();
    }
    private DivineAppraisal() {}

    public static void register() {
        ModList.get().getModContainerById("jaycee").orElseThrow()
            .registerConfig(ModConfig.Type.SERVER, SPEC, "jaycee-progression-server.toml");
        NeoForge.EVENT_BUS.addListener(DivineAppraisal::appraise);
        NeoForge.EVENT_BUS.addListener(DivineAppraisal::reward);
        NeoForge.EVENT_BUS.addListener(DivineAppraisal::spawn);
        NeoForge.EVENT_BUS.addListener(DivineAppraisal::validateBlock);
    }

    private static void validateBlock(ServerStartedEvent event) {
        if (ENABLED.get() && !BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.parse(BLOCK.get())))
            LogUtils.getLogger().warn("Jaycee appraisal block '{}' is not registered; fix appraisalBlock in jaycee-progression-server.toml.", BLOCK.get());
    }

    private static void appraise(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !ENABLED.get()
                || event.getHand() != InteractionHand.MAIN_HAND || !p.isShiftKeyDown() || !p.isAlive() || p.isSpectator()
                || !BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(event.getPos()).getBlock()).toString().equals(BLOCK.get()))
            return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        var root = p.getPersistentData().getCompound(PlayerState.TAG);
        long now = p.server.overworld().getGameTime(), last = root.getLong("LastDivineAppraisal");
        if (root.contains("LastDivineAppraisal") && (now < last || now - last < COOLDOWN.get())) {
            p.displayClientMessage(Component.literal("The appraisal sigil is recovering."), true);
            return;
        }
        root.putLong("LastDivineAppraisal", now);
        p.getPersistentData().put(PlayerState.TAG, root);
        p.sendSystemMessage(Component.literal(describe(p)));
        GameplayNetwork.sync(p);
    }

    public static String describe(ServerPlayer p) {
        var model = DivinityService.model(p);
        var progress = model.progression(p.getUUID());
        var profile = model.profile(p.getUUID());
        return "Divine Appraisal · " + profile.displayName() + " · Lv. " + progress.level()
            + " [" + progress.grade() + "] · Rank " + profile.rank()
            + (model.supreme(p.getUUID()) ? " · Supreme Existence" : "")
            + " · Essence progress " + progress.intoLevel() + " / " + progress.nextCost()
            + " · Total earned " + progress.earned() + " · Transferable " + profile.essence()
            + " · Permanent EP " + profile.ep() + ". Grade does not grant divine rank or abilities.";
    }

    private static void reward(LivingDropsEvent event) {
        var victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()
                || p.isCreative() || p instanceof net.neoforged.neoforge.common.util.FakePlayer
                || !(victim instanceof Enemy) || victim instanceof OwnableEntity owned && owned.getOwnerUUID() != null
                || victim.getPersistentData().getBoolean("JayceeEssenceRewarded")
                || victim.getPersistentData().getBoolean("JayceeArtificialSpawn") || KILL_REWARD.get() == 0)
            return;
        // Drops occur after an uncancelled death, unlike the cancellable living-death hook.
        victim.getPersistentData().putBoolean("JayceeEssenceRewarded", true);
        var root = p.getPersistentData().getCompound(PlayerState.TAG);
        var previous = new EssenceRewardBudget.State(root.getLong("EssenceRewardStart"), root.getLong("EssenceRewardUsed"));
        var award = EssenceRewardBudget.claim(previous, p.server.overworld().getGameTime(),
            KILL_REWARD.get(), REWARD_BUDGET.get(), REWARD_WINDOW.get());
        root.putLong("EssenceRewardStart", award.state().start());
        root.putLong("EssenceRewardUsed", award.state().used());
        p.getPersistentData().put(PlayerState.TAG, root);
        if (award.amount() > 0) {
            long actual = DivinityService.creditEssence(p, award.amount());
            if (actual > 0) p.displayClientMessage(Component.literal("+" + actual + " earned Essence"), true);
        }
    }
    private static void spawn(FinalizeSpawnEvent event) {
        event.getEntity().getPersistentData().putBoolean("JayceeArtificialSpawn",
            !java.util.Set.of("NATURAL", "CHUNK_GENERATION", "PATROL").contains(event.getSpawnType().name()));
    }
}

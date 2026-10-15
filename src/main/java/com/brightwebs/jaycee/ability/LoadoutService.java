package com.brightwebs.jaycee.ability;

import com.brightwebs.jaycee.JayceeMod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** All mutable gameplay state lives in player NBT, not in client packets or static player caches. */
public final class LoadoutService {
    public static final int PAGES = 10, SLOTS_PER_PAGE = 4, MAX_MANA = 80;
    private static final String KEY = "JayceeLoadout";
    private static final int MAX_TIMER = 1200;

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(KEY, 10)) {
            CompoundTag initial = new CompoundTag();
            int[] slots = new int[40];
            Arrays.fill(slots, -1);
            initial.putIntArray("Abilities", slots);
            initial.putInt("Mana", MAX_MANA);
            root.put(KEY, initial);
        }
        CompoundTag tag = root.getCompound(KEY);
        tag.putInt("Page", clamp(tag.getInt("Page"), 0, 9));
        tag.putInt("Mana", clamp(tag.getInt("Mana"), 0, MAX_MANA));
        int[] slots = normalized(tag, "Abilities", 40, -1, -1, 8);
        int[] modes = normalized(tag, "Modes", 40, 0, 0, 100);
        for (int i = 0; i < slots.length; i++) {
            var ability = AbilityCatalog.get(slots[i]);
            if (ability == null || modes[i] >= ability.modes().size()) modes[i] = 0;
        }
        tag.putIntArray("Modes", modes);
        normalized(tag, "Cooldowns", 9, 0, 0, MAX_TIMER);
        normalized(tag, "Active", 9, 0, 0, MAX_TIMER);
        return tag;
    }

    private static int[] normalized(CompoundTag tag, String key, int length, int fallback, int min, int max) {
        int[] old = tag.getIntArray(key), result = new int[length];
        Arrays.fill(result, fallback);
        for (int i = 0; i < Math.min(old.length, length); i++) result[i] = clamp(old[i], min, max);
        tag.putIntArray(key, result);
        return result;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    public static boolean accessible(Player player) {
        // Essence mastery remains the explicit fallback until advancement integration is ready.
        return LoadoutConfig.BETA_HUD.get() || JayceeMod.unlocked(player);
    }

    public static boolean unlocked(Player player, int id) {
        var ability = AbilityCatalog.get(id);
        return ability != null && clamp(player.getPersistentData().getInt("JayceeConsumed"), 0, 3) >= ability.requiredEssences();
    }

    static void action(ServerPlayer player, LoadoutNetwork.Action request) {
        if (request.action() < 0 || request.action() > LoadoutNetwork.REQUEST
            || request.page() < 0 || request.page() >= PAGES
            || request.slot() < 0 || request.slot() >= SLOTS_PER_PAGE
            || (request.action() == LoadoutNetwork.EQUIP && AbilityCatalog.get(request.ability()) == null)) return;
        CompoundTag tag = data(player);
        long now = player.serverLevel().getServer().overworld().getGameTime();
        // Bound both mutation and REQUEST snapshot traffic; this timestamp is server-written only.
        if (tag.contains("LastAction") && now >= tag.getLong("LastAction") && now - tag.getLong("LastAction") < 2) return;
        tag.putLong("LastAction", now);
        if (request.action() == LoadoutNetwork.REQUEST) { sync(player); return; }
        if (!accessible(player) || !player.isAlive() || player.isSpectator()) { sync(player); return; }
        int[] slots = tag.getIntArray("Abilities"), modes = tag.getIntArray("Modes");
        int index = request.page() * SLOTS_PER_PAGE + request.slot();
        switch (request.action()) {
            case LoadoutNetwork.SELECT_PAGE -> tag.putInt("Page", request.page());
            case LoadoutNetwork.EQUIP -> {
                if (!unlocked(player, request.ability())) break;
                slots[index] = request.ability();
                modes[index] = 0;
                tag.putIntArray("Abilities", slots);
                tag.putIntArray("Modes", modes);
            }
            case LoadoutNetwork.CLEAR -> {
                slots[index] = -1;
                modes[index] = 0;
                tag.putIntArray("Abilities", slots);
                tag.putIntArray("Modes", modes);
            }
            case LoadoutNetwork.CYCLE_MODE -> {
                var ability = AbilityCatalog.get(slots[index]);
                if (ability != null && unlocked(player, ability.id())) {
                    modes[index] = (modes[index] + 1) % ability.modes().size();
                    tag.putIntArray("Modes", modes);
                }
            }
            case LoadoutNetwork.ACTIVATE -> {
                if (request.page() == tag.getInt("Page")) activate(player, tag, slots[index], modes[index]);
            }
            default -> { }
        }
        sync(player);
    }

    private static void activate(ServerPlayer player, CompoundTag tag, int id, int mode) {
        var ability = AbilityCatalog.get(id);
        if (ability == null || !unlocked(player, id) || player.isSleeping() || player.isPassenger()) return;
        int[] cooldowns = tag.getIntArray("Cooldowns"), active = tag.getIntArray("Active");
        if (cooldowns[id] > 0 || tag.getInt("Mana") < ability.mana()) return;
        int duration = apply(player, ability, mode);
        if (duration < 0) return;
        tag.putInt("Mana", tag.getInt("Mana") - ability.mana());
        cooldowns[id] = ability.cooldownTicks();
        active[id] = duration;
        tag.putIntArray("Cooldowns", cooldowns);
        tag.putIntArray("Active", active);
    }

    /** -1 means no valid target/destination: no resource is consumed. */
    private static int apply(ServerPlayer player, AbilityCatalog.Ability ability, int mode) {
        int duration;
        switch (ability.id()) {
            case 0, 6 -> {
                LivingEntity target = rayTarget(player, mode == 0 ? ability.range() : ability.range() * 0.75);
                if (target == null || !target.hurt(player.damageSources().indirectMagic(player, player),
                    (ability.id() == 0 ? 12 : 8) * (mode == 0 ? 1 : 0.75F))) return -1;
                return 10;
            }
            case 1 -> {
                duration = mode == 0 ? 100 : 140;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
                return duration;
            }
            case 2 -> {
                int hits = 0;
                double range = mode == 0 ? ability.range() : ability.range() * 0.75;
                for (Monster target : player.serverLevel().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(range))) {
                    if (target.isAlive() && !target.isAlliedTo(player) && target.distanceToSqr(player) <= range * range
                        && player.hasLineOfSight(target) && target.hurt(player.damageSources().indirectMagic(player, player), mode == 0 ? 6 : 4))
                        hits++;
                }
                return hits == 0 ? -1 : 10;
            }
            case 3 -> {
                Vec3 direction = player.getLookAngle();
                double range = mode == 0 ? ability.range() : ability.range() / 2.0;
                Vec3 destination = player.position().add(direction.scale(range));
                // Check the entire swept player volume, not just the destination or an eye ray.
                for (double step = 0.25; step <= range; step += 0.25) {
                    AABB box = player.getBoundingBox().move(direction.scale(step));
                    if (box.minY < player.level().getMinBuildHeight() || box.maxY >= player.level().getMaxBuildHeight()
                        || !player.level().getWorldBorder().isWithinBounds(box)
                        || !loaded(player, box) || !player.level().noCollision(player, box)) return -1;
                }
                player.teleportTo(destination.x, destination.y, destination.z);
                player.fallDistance = 0;
                return 10;
            }
            case 4 -> {
                if (player.getHealth() >= player.getMaxHealth()) return -1;
                player.heal(mode == 0 ? 6 : 4);
                return 10;
            }
            case 5 -> {
                duration = mode == 0 ? 200 : 240;
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
                return duration;
            }
            case 7 -> {
                duration = mode == 0 ? 200 : 240;
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0));
                return duration;
            }
            case 8 -> {
                duration = mode == 0 ? 100 : 140;
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
                for (ServerPlayer ally : player.serverLevel().players()) {
                    if (ally != player && ally.isAlive() && !ally.isSpectator() && player.isAlliedTo(ally)
                        && player.distanceToSqr(ally) <= ability.range() * ability.range() && player.hasLineOfSight(ally))
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
                }
                return duration;
            }
            default -> { return -1; }
        }
    }

    private static boolean loaded(ServerPlayer player, AABB box) {
        for (int x = (int) Math.floor(box.minX) >> 4; x <= (int) Math.floor(box.maxX) >> 4; x++)
            for (int z = (int) Math.floor(box.minZ) >> 4; z <= (int) Math.floor(box.maxZ) >> 4; z++)
                if (!player.serverLevel().hasChunk(x, z)) return false;
        return true;
    }

    private static LivingEntity rayTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(range));
        Vec3 blocked = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity nearest = null;
        double distance = start.distanceToSqr(blocked);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(start, blocked).inflate(1), entity -> entity != player && entity.isAlive() && entity.isAttackable()
                && !entity.isSpectator() && !entity.isAlliedTo(player)
                && (!(entity instanceof Player other) || player.canHarmPlayer(other)))) {
            var intersection = target.getBoundingBox().clip(start, blocked);
            if (intersection.isPresent() && start.distanceToSqr(intersection.get()) < distance) {
                nearest = target;
                distance = start.distanceToSqr(intersection.get());
            }
        }
        return nearest;
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag tag = data(player);
        for (String key : List.of("Cooldowns", "Active")) {
            int[] timers = tag.getIntArray(key);
            for (int i = 0; i < timers.length; i++) if (timers[i] > 0) timers[i]--;
            tag.putIntArray(key, timers);
        }
        int regen = (clamp(tag.getInt("Regen"), 0, 19) + 1) % 20;
        tag.putInt("Regen", regen);
        if (regen == 0 && player.isAlive()) tag.putInt("Mana", Math.min(MAX_MANA, tag.getInt("Mana") + 1));
        if (player.tickCount % 10 == 0) sync(player);
    }

    public static void clone(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        CompoundTag destination = event.getEntity().getPersistentData();
        if (original.contains(KEY, 10)) destination.put(KEY, original.getCompound(KEY).copy());
        destination.putInt("JayceeConsumed", clamp(original.getInt("JayceeConsumed"), 0, 3));
    }

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int[] active = data(player).getIntArray("Active");
            // Vanilla drops buffs on death; restore the persisted remaining duration on the new entity.
            if (active[1] > 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, active[1], 0));
            if (active[5] > 0) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, active[5], 0));
            if (active[7] > 0) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, active[7], 0));
            if (active[8] > 0) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, active[8], 0));
            sync(player);
        }
    }

    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void sync(ServerPlayer player) {
        CompoundTag tag = data(player);
        List<Integer> abilities = new ArrayList<>(40), modes = new ArrayList<>(40),
            cooldowns = new ArrayList<>(40), active = new ArrayList<>(40);
        int[] slots = tag.getIntArray("Abilities"), slotModes = tag.getIntArray("Modes"),
            timers = tag.getIntArray("Cooldowns"), activeTimers = tag.getIntArray("Active");
        for (int i = 0; i < 40; i++) {
            abilities.add(slots[i]); modes.add(slotModes[i]);
            cooldowns.add(slots[i] < 0 ? 0 : timers[slots[i]]);
            active.add(slots[i] < 0 ? 0 : activeTimers[slots[i]]);
        }
        List<Boolean> unlocked = new ArrayList<>(9);
        for (int id = 0; id < 9; id++) unlocked.add(unlocked(player, id));
        LoadoutNetwork.sync(player, new LoadoutClientState.Snapshot(tag.getInt("Page"), abilities, modes, cooldowns,
            active, tag.getInt("Mana"), accessible(player), unlocked));
    }

    private LoadoutService() {}
}

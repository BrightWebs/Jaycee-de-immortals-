package com.brightwebs.jaycee.gameplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/** Logical-server effects. Payment leases last at most twenty ticks and never survive reconnect. */
public final class SkillRuntime {
    private static final ResourceLocation SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("jaycee", "intrinsic_speed");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        SkillMath.Gate gate = new SkillMath.Gate();
        final Set<String> paid = new HashSet<>();
        final Map<String, SkillMath.Lease> leases = new HashMap<>();
        long paidUntil;
        Vec3 position;
        double distance, injury;
        boolean moved;
        boolean flightOwned;
        float oldFlyingSpeed, flightSpeed;
    }
    private SkillRuntime() {}
    static void register() {
        SkillConfig.register();
        NeoForge.EVENT_BUS.addListener(SkillRuntime::incoming);
        NeoForge.EVENT_BUS.addListener(SkillRuntime::damaged);
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) suspend(player);
        });
    }
    private static Session session(ServerPlayer p) {
        return SESSIONS.computeIfAbsent(p.getUUID(), ignored -> {
            var session = new Session();
            var gate = PlayerState.read(p).getCompound("SkillMasteryGate");
            var entries = new java.util.LinkedHashMap<String, Long>();
            var saved = gate.getCompound("Entries");
            saved.getAllKeys().stream().sorted().limit(128).forEach(key -> entries.put(key, saved.getLong(key)));
            session.gate = new SkillMath.Gate(gate.contains("Window") ? gate.getLong("Window") : Long.MIN_VALUE,
                gate.getInt("Awarded"), entries);
            return session;
        });
    }
    static void login(ServerPlayer player) {
        var data = PlayerState.read(player);
        // Recover our vanilla abilities lease after an interrupted server shutdown.
        if (data.getBoolean("SkillFlightOwned")) {
            var runtime = session(player);
            runtime.flightOwned = true;
            runtime.oldFlyingSpeed = data.getFloat("SkillFlightPrevious");
            runtime.flightSpeed = data.getFloat("SkillFlightSpeed");
            releaseFlight(player, runtime);
        }
        data = PlayerState.read(player);
        data.putInt("SkillMaxLevel", SkillConfig.MAX_LEVEL.get());
        data.putInt("SkillXpPerLevel", SkillConfig.XP_PER_LEVEL.get());
        data.putInt("SkillModeLevelStep", SkillConfig.MODE_LEVEL_STEP.get());
        SkillConfig.snapshot(data);
        PlayerState.save(player, data);
        invalidate(player);
    }
    private static boolean eligible(ServerPlayer p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator() && !(p instanceof FakePlayer);
    }
    private static boolean usable(ServerPlayer p, PlayerState s) { return eligible(p) && s.unlocked() && s.soul() > 0; }

    static void edit(CompoundTag data, String id, java.util.function.Consumer<CompoundTag> edit) {
        var skills = data.getCompound("Skills");
        var entry = skills.getCompound(id);
        edit.accept(entry);
        skills.put(id, entry);
        data.put("Skills", skills);
    }

    static void setEnabled(CompoundTag data, String id, boolean enabled) {
        edit(data, id, entry -> entry.putBoolean("Enabled", enabled));
    }
    /** Intrinsics have one configured mode, mirrored into every equipped occurrence. */
    static void configureMode(CompoundTag data, String id, int mode) {
        if (!SkillDefinitions.intrinsic(id)) return;
        edit(data, id, entry -> entry.putInt("Mode", mode));
        var abilities = data.getCompound("Abilities");
        var modes = data.getCompound("Modes");
        for (int page = 0; page < PlayerState.PAGES; page++) {
            for (int slot = 0; slot < PlayerState.SLOTS; slot++) {
                String key = PlayerState.key(page, slot);
                if (id.equals(abilities.getString(key))) modes.putInt(key, mode);
            }
        }
        data.put("Modes", modes);
    }

    static boolean canActivate(ServerPlayer player, PlayerState state, String id, int mode) {
        var d = SkillDefinitions.find(id);
        return usable(player, state) && d != null && d.intrinsic() && state.skillGranted(id)
            && !state.skillEnabled(id) && state.skillOutput(id) > 0
            && (!SkillDefinitions.FLIGHT.equals(id) || !player.getAbilities().mayfly)
            && SkillMath.modeUnlocked(mode, d.modes().size(), state.skillLevel(id), SkillConfig.MODE_LEVEL_STEP.get());
    }
    static boolean modeAllowed(PlayerState state, String id, int mode) {
        var definition = SkillDefinitions.find(id);
        boolean intrinsic = definition != null && definition.intrinsic();
        if (!SkillMath.permission(intrinsic, state.skillGranted(id))) return false;
        return !SkillDefinitions.PERCEPTION.equals(id)
            || SkillMath.modeUnlocked(mode, definition.modes().size(), state.skillLevel(id), SkillConfig.MODE_LEVEL_STEP.get());
    }

    static boolean action(ServerPlayer player, GameplayNetwork.Action action) {
        String id = action.value();
        var d = SkillDefinitions.find(id);
        if (d == null || !d.intrinsic()) return false;
        var s = PlayerState.get(player);
        var data = PlayerState.read(player);
        int value = action.first();
        if ("skill_toggle".equals(action.action())) {
            if (value < 0 || value > 1) return false;
            if (value == 0) {
                setEnabled(data, id, false);
                PlayerState.save(player, data);
                stop(player, id);
            } else {
                if (!s.skillGranted(id)) return false;
                if (!s.skillEnabled(id)) {
                    AbilityExecutors.activate(player, s, id, s.skillMode(id), player.serverLevel().getGameTime());
                    if (!PlayerState.get(player).skillEnabled(id)) return false;
                }
            }
            GameplayNetwork.sync(player);
            return true;
        }
        if (!s.skillGranted(id) || !usable(player, s)) return false;
        switch (action.action()) {
            case "skill_output" -> {
                if (value < 0 || value > 100) return false;
                edit(data, id, entry -> entry.putInt("Output", value));
            }
            case "skill_mode" -> {
                if (!SkillMath.modeUnlocked(value, d.modes().size(), s.skillLevel(id), SkillConfig.MODE_LEVEL_STEP.get())) return false;
                configureMode(data, id, value);
            }
            case "skill_style" -> {
                if (value < 0 || value >= d.styles().size()) return false;
                edit(data, id, entry -> entry.putInt("Style", value));
            }
            default -> { return false; }
        }
        PlayerState.save(player, data);
        configurationChanged(player, id);
        GameplayNetwork.sync(player);
        return true;
    }

    static int grant(ServerPlayer player, String id, boolean granted) {
        if (!SkillDefinitions.intrinsic(id)) return 0;
        var data = PlayerState.read(player);
        edit(data, id, entry -> {
            entry.putBoolean("Granted", granted);
            if (!granted) entry.putBoolean("Enabled", false);
        });
        PlayerState.save(player, data);
        com.brightwebs.jaycee.gameplay.divine.DivinityService.adminGrant(player, id, granted);
        configurationChanged(player, id);
        GameplayNetwork.sync(player);
        player.displayClientMessage(Component.literal((granted ? "Granted " : "Revoked ") + id
            + (PlayerState.get(player).jayceeTest() ? " (test authorization still enabled)" : "")), false);
        return 1;
    }

    /** Clear only our effects, keeping persisted user intent. Used before clone/dimension/logout. */
    static void suspend(ServerPlayer player) {
        invalidate(player);
        SESSIONS.remove(player.getUUID());
    }

    public static void invalidate(ServerPlayer player) {
        var runtime = SESSIONS.get(player.getUUID());
        if (runtime != null) {
            runtime.paid.clear();
            runtime.leases.clear();
            releaseFlight(player, runtime);
        }
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) attribute.removeModifier(SPEED_MODIFIER);
        var data = PlayerState.read(player);
        data.putDouble("SkillDrain", 0);
        PlayerState.save(player, data);
    }
    /** Nonzero parameter edits wait for the next paid boundary; unrelated leases are untouched. */
    static void configurationChanged(ServerPlayer player, String id) {
        var state = PlayerState.get(player);
        if (SkillMath.configurationStops(state.skillEnabled(id), state.skillOutput(id))) stop(player, id);
    }
    static void stop(ServerPlayer player, String id) {
        var runtime = SESSIONS.get(player.getUUID());
        if (runtime != null) {
            runtime.paid.remove(id);
            runtime.leases.remove(id);
            if (SkillDefinitions.FLIGHT.equals(id)) releaseFlight(player, runtime);
        }
        if (SkillDefinitions.SPEED.equals(id)) {
            var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute != null) attribute.removeModifier(SPEED_MODIFIER);
        }
        var data = PlayerState.read(player);
        data.putDouble("SkillDrain", runtime == null ? 0 : currentDrain(player, runtime));
        PlayerState.save(player, data);
    }

    static void testSpCommand(ServerPlayer player) {
        var data = PlayerState.read(player);
        data.putLong("SkillTestSpUntil", player.serverLevel().getGameTime() + 1200);
        PlayerState.save(player, data);
    }

    static void effectiveCast(ServerPlayer player, String id) {
        if (SkillDefinitions.RESTORE.equals(id)
                && PlayerState.read(player).getLong("SkillTestSpUntil") <= player.serverLevel().getGameTime()) {
            award(player, id, "cast:" + id, 200, SkillConfig.CAST_XP.get());
        }
    }

    private static void award(ServerPlayer player, String id, String action, int interval, int xp) {
        if (!eligible(player) || !PlayerState.get(player).unlocked()) return;
        if (!session(player).gate.allow(action, player.serverLevel().getGameTime(), interval, xp, SkillConfig.XP_PER_MINUTE.get())) return;
        var data = PlayerState.read(player);
        edit(data, id, entry -> entry.putLong("Xp", SkillMath.addXp(entry.getLong("Xp"), xp,
            SkillConfig.MAX_LEVEL.get(), SkillConfig.XP_PER_LEVEL.get())));
        data.putInt("SkillMaxLevel", SkillConfig.MAX_LEVEL.get());
        data.putInt("SkillXpPerLevel", SkillConfig.XP_PER_LEVEL.get());
        data.putInt("SkillModeLevelStep", SkillConfig.MODE_LEVEL_STEP.get());
        var gate = new CompoundTag();
        gate.putLong("Window", session(player).gate.window());
        gate.putInt("Awarded", session(player).gate.awarded());
        var entries = new CompoundTag();
        session(player).gate.entries().forEach(entries::putLong);
        gate.put("Entries", entries);
        data.put("SkillMasteryGate", gate);
        PlayerState.save(player, data);
    }

    private static List<LivingEntity> targets(ServerPlayer player, PlayerState state) {
        double range = SkillConfig.SCAN_RANGE.get() * SkillMath.output(state.skillOutput(SkillDefinitions.PERCEPTION));
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
            target -> !(target instanceof Player) && target.isAlive()
                && (state.skillMode(SkillDefinitions.PERCEPTION) == 1 || target instanceof Enemy)
                && player.distanceToSqr(target) <= range * range && player.hasLineOfSight(target))
            .stream().sorted(java.util.Comparator.comparingDouble(player::distanceToSqr)).limit(1).toList();
    }

    private static boolean effective(ServerPlayer player, PlayerState s, String id, List<LivingEntity> targets) {
        if (!s.skillEnabled(id) || s.skillOutput(id) <= 0) return false;
        return switch (id) {
            case SkillDefinitions.SPEED -> player.isSprinting() && player.onGround() && session(player).moved;
            case SkillDefinitions.FLIGHT -> !player.isPassenger() && !player.isFallFlying()
                && (session(player).flightOwned
                    ? player.getAbilities().mayfly && player.getAbilities().getFlyingSpeed() == session(player).flightSpeed
                    : !player.getAbilities().mayfly);
            case SkillDefinitions.PERCEPTION -> !targets.isEmpty();
            case SkillDefinitions.REGEN -> player.getHealth() < player.getMaxHealth();
            case SkillDefinitions.RESISTANCE -> true;
            default -> false;
        };
    }

    static void tick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            var data = PlayerState.read(player);
            SkillConfig.snapshot(data);
            PlayerState.save(player, data);
        }
        var s = PlayerState.get(player);
        if (!usable(player, s)) { invalidate(player); return; }
        var runtime = session(player);
        long now = player.serverLevel().getGameTime();
        if (runtime.position != null) {
            double travelled = player.position().distanceTo(runtime.position);
            runtime.moved = SkillMath.validMovement(travelled, SkillConfig.MAX_TRAINING_STEP.get());
            if (player.isSprinting() && player.onGround() && !player.isPassenger() && travelled > 0.01 && travelled <= SkillConfig.MAX_TRAINING_STEP.get()) {
                runtime.distance += travelled;
                if (runtime.distance >= SkillConfig.SPRINT_DISTANCE.get()) {
                    runtime.distance %= SkillConfig.SPRINT_DISTANCE.get();
                    award(player, SkillDefinitions.PHYSICAL, "sprint", SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
                    if (runtime.paid.contains(SkillDefinitions.SPEED))
                        award(player, SkillDefinitions.SPEED, "speed:movement", SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
                }
            }
            if (runtime.flightOwned && player.getAbilities().flying && travelled > 0.05 && travelled <= SkillConfig.MAX_TRAINING_STEP.get()) {
                award(player, SkillDefinitions.FLIGHT, "flight:movement", SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
            }
        }
        runtime.position = player.position();
        if (now >= runtime.paidUntil) runtime.paid.clear();
        List<LivingEntity> targets = List.of();
        if (player.tickCount % 20 == 0) {
            if (s.skillEnabled(SkillDefinitions.PERCEPTION)) targets = targets(player, s);
            float healing = 0;
            if (effective(player, s, SkillDefinitions.REGEN, targets)) {
                float requested = (float) (SkillConfig.HEAL.get() * SkillMath.output(s.skillOutput(SkillDefinitions.REGEN)));
                // Resolve the vanilla/NeoForge healing veto before charging the aggregate payment.
                var event = new LivingHealEvent(player, requested);
                NeoForge.EVENT_BUS.post(event);
                if (!event.isCanceled() && Float.isFinite(event.getAmount())) {
                    healing = Math.max(0, Math.min(Math.min(requested, event.getAmount()),
                        player.getMaxHealth() - player.getHealth()));
                }
            }
            Set<String> planned = new HashSet<>();
            Map<String, Double> plannedDrains = new HashMap<>();
            double cost = 0;
            for (var d : SkillDefinitions.ALL) {
                if (d.intrinsic() && effective(player, s, d.id(), targets)) {
                    if (SkillDefinitions.REGEN.equals(d.id()) && healing <= 0) continue;
                    planned.add(d.id());
                    double drain = SkillDefinitions.REGEN.equals(d.id())
                        ? SkillMath.healingDrain(d.upkeepMana(), s.skillOutput(d.id()),
                            healing, SkillConfig.HEAL.get())
                        : SkillMath.drain(d.upkeepMana(), s.skillOutput(d.id()), true);
                    plannedDrains.put(d.id(), drain);
                    cost += drain;
                }
            }
            runtime.paid.clear();
            runtime.leases.clear();
            if (cost > 0) {
                var mana = ManaBackends.resolve(player).mana();
                boolean paid;
                try { paid = mana.getCurrent() >= cost && mana.consume(cost); }
                catch (RuntimeException ignored) { paid = false; }
                if (paid) {
                    runtime.paid.addAll(planned);
                    for (String id : planned) {
                        runtime.leases.put(id, new SkillMath.Lease(s.skillOutput(id), s.skillStyle(id),
                            plannedDrains.get(id)));
                    }
                    runtime.paidUntil = now + 20;
                }
                else {
                    var data = PlayerState.read(player);
                    for (var d : SkillDefinitions.ALL) if (d.intrinsic()) setEnabled(data, d.id(), false);
                    PlayerState.save(player, data);
                    player.displayClientMessage(Component.literal("Intrinsic effects stopped: insufficient mana."), true);
                }
            }
            if (runtime.paid.contains(SkillDefinitions.REGEN)) {
                float before = player.getHealth();
                // The heal event was already resolved above, so do not post it twice through heal().
                if (player.isAlive()) player.setHealth(Math.min(player.getMaxHealth(), before + healing));
                double healed = player.getHealth() - before;
                if (healed > 0 && runtime.injury > 0) {
                    runtime.injury = Math.max(0, runtime.injury - healed);
                    award(player, SkillDefinitions.REGEN, "regen:injury", SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
                }
            }
            if (runtime.paid.contains(SkillDefinitions.PERCEPTION) && !targets.isEmpty()) {
                var target = targets.getFirst();
                player.displayClientMessage(Component.literal(target.getType().getDescription().getString() + " — "
                    + Math.round(player.distanceTo(target)) + " blocks"), true);
                if (runtime.moved) award(player, SkillDefinitions.PERCEPTION, "scan:" + target.getUUID(), SkillConfig.SCAN_INTERVAL.get(), SkillConfig.ACTION_XP.get());
            }
        }
        // Never keep a paid movement effect after permission loss or zero output.
        runtime.paid.removeIf(id -> !s.skillEnabled(id) || s.skillOutput(id) == 0);
        boolean speed = SkillMath.effectActive(s.skillGranted(SkillDefinitions.SPEED),
            s.skillEnabled(SkillDefinitions.SPEED), lease(runtime, SkillDefinitions.SPEED).output(),
            runtime.paid.contains(SkillDefinitions.SPEED))
            && effective(player, s, SkillDefinitions.SPEED, List.of());
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SPEED_MODIFIER);
            if (speed) attribute.addTransientModifier(new AttributeModifier(SPEED_MODIFIER,
                SkillConfig.SPEED_BONUS.get() * SkillMath.output(lease(runtime, SkillDefinitions.SPEED).output()),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (runtime.paid.contains(SkillDefinitions.FLIGHT) && !player.isPassenger() && !player.isFallFlying()) {
            applyFlight(player, runtime);
        } else releaseFlight(player, runtime);
        if (!runtime.flightOwned) runtime.paid.remove(SkillDefinitions.FLIGHT);
        var data = PlayerState.read(player);
        data.putDouble("SkillDrain", currentDrain(player, runtime));
        data.putInt("SkillMaxLevel", SkillConfig.MAX_LEVEL.get());
        data.putInt("SkillXpPerLevel", SkillConfig.XP_PER_LEVEL.get());
        data.putInt("SkillModeLevelStep", SkillConfig.MODE_LEVEL_STEP.get());
        PlayerState.save(player, data);
    }
    private static double currentDrain(ServerPlayer player, Session runtime) {
        var state = PlayerState.get(player);
        double drain = 0;
        for (String id : runtime.paid) {
            if (SkillDefinitions.SPEED.equals(id)
                    && !(player.isSprinting() && player.onGround() && runtime.moved)) continue;
            if (SkillDefinitions.REGEN.equals(id) && player.getHealth() >= player.getMaxHealth()) continue;
            if (!state.skillEnabled(id) || state.skillOutput(id) == 0) continue;
            drain += lease(runtime, id).drain();
        }
        return drain;
    }
    private static SkillMath.Lease lease(Session runtime, String id) {
        return runtime.leases.getOrDefault(id, SkillMath.Lease.NONE);
    }

    private static void applyFlight(ServerPlayer player, Session runtime) {
        var abilities = player.getAbilities();
        boolean acquired = false;
        if (!runtime.flightOwned) {
            // Pre-existing creative/modded flight belongs to its original provider.
            if (abilities.mayfly) return;
            runtime.oldFlyingSpeed = abilities.getFlyingSpeed();
            runtime.flightOwned = true;
            abilities.mayfly = true;
            abilities.flying = true;
            acquired = true;
        } else if (!abilities.mayfly || abilities.getFlyingSpeed() != runtime.flightSpeed) {
            releaseFlight(player, runtime);
            return;
        }
        runtime.flightSpeed = (float) Math.max(0.005,
            SkillConfig.FLIGHT_SPEED.get() * SkillMath.output(lease(runtime, SkillDefinitions.FLIGHT).output()));
        if (acquired || abilities.getFlyingSpeed() != runtime.flightSpeed) {
            abilities.setFlyingSpeed(runtime.flightSpeed);
            player.onUpdateAbilities();
            var data = PlayerState.read(player);
            data.putBoolean("SkillFlightOwned", true);
            data.putFloat("SkillFlightPrevious", runtime.oldFlyingSpeed);
            data.putFloat("SkillFlightSpeed", runtime.flightSpeed);
            PlayerState.save(player, data);
        }
        if (abilities.flying && "Cruise".equals(lease(runtime, SkillDefinitions.FLIGHT).style())) {
            player.setDeltaMovement(player.getLookAngle().scale(runtime.flightSpeed * SkillConfig.CRUISE_MULTIPLIER.get()));
            player.hurtMarked = true;
        }
    }

    private static void releaseFlight(ServerPlayer player, Session runtime) {
        if (!runtime.flightOwned) return;
        var abilities = player.getAbilities();
        // If another provider changed speed, relinquish ownership rather than revoke its flight.
        if (abilities.getFlyingSpeed() == runtime.flightSpeed) {
            abilities.setFlyingSpeed(runtime.oldFlyingSpeed);
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
            }
            player.onUpdateAbilities();
        }
        runtime.flightOwned = false;
        var data = PlayerState.read(player);
        data.remove("SkillFlightOwned");
        data.remove("SkillFlightPrevious");
        data.remove("SkillFlightSpeed");
        PlayerState.save(player, data);
    }

    public static double resistance(ServerPlayer player) {
        var runtime = SESSIONS.get(player.getUUID());
        var s = PlayerState.get(player);
        return runtime != null && usable(player, s) && s.skillEnabled(SkillDefinitions.RESISTANCE)
            && runtime.paidUntil > player.serverLevel().getGameTime() && runtime.paid.contains(SkillDefinitions.RESISTANCE)
            ? SkillConfig.RESISTANCE.get() * SkillMath.output(lease(runtime, SkillDefinitions.RESISTANCE).output()) : 0;
    }
    private static void incoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            double reduction = resistance(player);
            if (reduction > 0) event.setAmount((float) (event.getAmount() * (1 - reduction)));
        }
    }
    private static void damaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer player && eligible(player)
                && event.getSource().getEntity() instanceof Enemy) {
            var runtime = session(player);
            runtime.injury = Math.min(player.getMaxHealth(), runtime.injury + event.getNewDamage());
            if (resistance(player) > 0) award(player, SkillDefinitions.RESISTANCE,
                "resist:" + event.getSource().getEntity().getUUID(), SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
        }
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() != player) {
            award(player, SkillDefinitions.PHYSICAL, "hit:" + event.getEntity().getUUID(), SkillConfig.ACTION_INTERVAL.get(), SkillConfig.ACTION_XP.get());
        }
    }
}

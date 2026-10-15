package com.brightwebs.jaycee.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class Gameplay {
    private record Rate(long tick, int count) {}
    private static final Map<UUID, Rate> ACTION_RATES = new HashMap<>();
    private Gameplay() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(GameplayNetwork::register);
        modBus.addListener((FMLLoadCompleteEvent event) -> event.enqueueWork(ManaBackends::probeOptionalMods));
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(Gameplay::login);
        bus.addListener(Gameplay::logout);
        bus.addListener(Gameplay::clonePlayer);
        bus.addListener(Gameplay::respawn);
        bus.addListener(Gameplay::dimension);
        bus.addListener(Gameplay::tick);
        bus.addListener(Gameplay::commands);
    }

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jayceemana")
            .executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var state = PlayerState.get(player);
                var resolved = ManaBackends.resolve(player);
                context.getSource().sendSuccess(() -> Component.literal("Mana: selected=" + state.backend()
                    + ", effective=" + resolved.id() + ", available=" + String.join(", ", ManaBackends.available(player))), false);
                return 1;
            })
            .then(Commands.argument("backend", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    var player = context.getSource().getPlayer();
                    return SharedSuggestionProvider.suggest(player == null ? java.util.List.of() : ManaBackends.available(player), builder);
                })
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    String selected = StringArgumentType.getString(context, "backend");
                    if (!ManaBackends.available(player).contains(selected)) {
                        context.getSource().sendFailure(Component.literal("Unavailable mana backend. Available: "
                            + String.join(", ", ManaBackends.available(player))));
                        return 0;
                    }
                    var data = PlayerState.read(player);
                    data.putString("Backend", selected);
                    PlayerState.save(player, data);
                    GameplayNetwork.sync(player);
                    context.getSource().sendSuccess(() -> Component.literal("Mana backend selected: " + selected
                        + " (effective: " + ManaBackends.resolve(player).id() + ")"), false);
                    return 1;
                })));
        var spiritual = Commands.literal("jayceesp").requires(source -> source.hasPermission(2))
            .executes(context -> {
                var state = PlayerState.get(context.getSource().getPlayerOrException());
                context.getSource().sendSuccess(() -> Component.literal("SP " + state.soul() + "/" + state.maxSoul()
                    + ", barrier " + state.barrier() + "/" + state.maxBarrier()
                    + ", spiritual resistance " + state.soulResistance()), false);
                return 1;
            });
        for (String operation : java.util.List.of("damage", "heal")) {
            spiritual.then(Commands.literal(operation)
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 1_000_000))
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();
                        if (!PlayerState.get(player).unlocked()) {
                            context.getSource().sendFailure(Component.literal("Unlock Jaycee with three essences first."));
                            return 0;
                        }
                        double amount = DoubleArgumentType.getDouble(context, "amount");
                        if ("damage".equals(operation)) damageSoul(player, amount);
                        else healSoul(player, amount);
                        var state = PlayerState.get(player);
                        context.getSource().sendSuccess(() -> Component.literal("Spiritual " + operation
                            + ": SP " + state.soul() + "/" + state.maxSoul() + ", barrier " + state.barrier()), false);
                        return 1;
                    })));
        }
        event.getDispatcher().register(spiritual);
    }

    public static int absorbEssence(Player player) {
        if (player.level().isClientSide) return PlayerState.get(player).consumed();
        var data = PlayerState.read(player);
        int consumed = Math.min(3, PlayerState.get(player).consumed() + 1);
        data.putInt("Consumed", consumed);
        PlayerState.save(player, data);
        if (player instanceof ServerPlayer serverPlayer) GameplayNetwork.sync(serverPlayer);
        return consumed;
    }

    /** Restores SP independently of vanilla health; returns actual SP restored. */
    public static double healSoul(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !player.isAlive() || !PlayerState.get(player).unlocked()) return 0;
        var state = PlayerState.get(player);
        double restored = Math.min(amount, state.maxSoul() - state.soul());
        if (restored <= 0) return 0;
        var data = PlayerState.read(player);
        data.putDouble("Soul", state.soul() + restored);
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
        return restored;
    }

    /** Adds persistent barrier up to max SP. Only explicit damageSoul calls consume it, never vanilla HP damage. */
    public static double addSoulBarrier(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !player.isAlive() || !PlayerState.get(player).unlocked()) return 0;
        var state = PlayerState.get(player);
        double added = Math.min(amount, state.maxBarrier() - state.barrier());
        if (added <= 0) return 0;
        var data = PlayerState.read(player);
        data.putDouble("Barrier", state.barrier() + added);
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
        return added;
    }

    /**
     * Applies spiritual damage only: resistance reduces the amount, barrier pays next,
     * then SP. Returns SP actually lost. Reaching zero applies a nonfatal debuff and
     * locks casting while SP remains zero; vanilla health is never read or changed.
     */
    public static double damageSoul(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !player.isAlive() || player.isSpectator()) return 0;
        var state = PlayerState.get(player);
        if (!state.unlocked()) return 0;
        double mitigated = amount * (1 - state.soulResistance());
        double barrierLost = Math.min(state.barrier(), mitigated);
        double soulLost = Math.min(state.soul(), mitigated - barrierLost);
        if (barrierLost <= 0 && soulLost <= 0) return 0;
        var data = PlayerState.read(player);
        data.putDouble("Barrier", state.barrier() - barrierLost);
        data.putDouble("Soul", state.soul() - soulLost);
        PlayerState.save(player, data);
        if (state.soul() - soulLost <= 0) applyExhaustion(player);
        GameplayNetwork.sync(player);
        return soulLost;
    }

    /** Sets the resisted fraction of explicit spiritual damage; invalid input leaves it unchanged. */
    public static double setSoulResistance(ServerPlayer player, double fraction) {
        if (!Double.isFinite(fraction)) return PlayerState.get(player).soulResistance();
        var data = PlayerState.read(player);
        double resistance = Math.clamp(fraction, 0, 1);
        data.putDouble("SoulResistance", resistance);
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
        return resistance;
    }

    /** Sets max SP to 1..1,000,000 and clamps current SP/barrier without filling either pool. */
    public static void setMaxSoul(ServerPlayer player, double maximum) {
        if (!Double.isFinite(maximum)) throw new IllegalArgumentException("Maximum SP must be finite");
        var state = PlayerState.get(player);
        var data = PlayerState.read(player);
        double max = PlayerState.maximum(maximum);
        data.putDouble("MaxSoul", max);
        data.putDouble("Soul", PlayerState.bounded(state.soul(), max));
        data.putDouble("Barrier", PlayerState.bounded(state.barrier(), max));
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
    }

    /** Configures only the saved standalone pool; external adapters are never mutated. */
    public static void configureStandaloneMana(ServerPlayer player, double maximum, double regenPerSecond) {
        if (!Double.isFinite(maximum) || !Double.isFinite(regenPerSecond)) {
            throw new IllegalArgumentException("Maximum mana and regeneration must be finite");
        }
        var state = PlayerState.get(player);
        var data = PlayerState.read(player);
        double max = PlayerState.maximum(maximum);
        data.putDouble("MaxMana", max);
        data.putDouble("Mana", PlayerState.bounded(state.mana(), max));
        data.putDouble("ManaRegen", PlayerState.bounded(regenPerSecond, 10000));
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
    }

    private static void applyExhaustion(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerState.save(player, PlayerState.read(player));
            GameplayNetwork.sync(player);
        }
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTION_RATES.remove(event.getEntity().getUUID());
    }

    private static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide) {
            PlayerState.save(event.getEntity(), PlayerState.read(event.getOriginal()));
            ACTION_RATES.remove(event.getEntity().getUUID());
        }
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GameplayNetwork.sync(player);
    }

    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GameplayNetwork.sync(player);
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        var state = PlayerState.get(player);
        if (player.isAlive() && !player.isSpectator()) {
            var data = PlayerState.read(player);
            if (ManaBackends.STANDALONE.equals(ManaBackends.resolve(player).id())) {
                data.putDouble("Mana", Math.min(state.maxMana(), state.mana() + state.manaRegen()));
            }
            if (state.unlocked()) {
                if (state.soul() == 0) {
                    applyExhaustion(player);
                }
                data.putDouble("Soul", Math.min(state.maxSoul(), state.soul() + 0.5));
            }
            CompoundTag cooldowns = data.getCompound("Cooldowns");
            long now = player.serverLevel().getGameTime();
            for (String id : java.util.Set.copyOf(cooldowns.getAllKeys())) {
                if (cooldowns.getLong(id) <= now) cooldowns.remove(id);
            }
            data.put("Cooldowns", cooldowns);
            PlayerState.save(player, data);
        }
        GameplayNetwork.sync(player);
    }

    static void handle(ServerPlayer player, GameplayNetwork.Action action) {
        long now = player.serverLevel().getGameTime();
        Rate previous = ACTION_RATES.get(player.getUUID());
        int count = previous != null && previous.tick() == now ? previous.count() + 1 : 1;
        ACTION_RATES.put(player.getUUID(), new Rate(now, count));
        if (count > 12) return;
        var state = PlayerState.get(player);
        if (!state.unlocked() || !player.isAlive() || player.isSpectator()) return;
        var data = PlayerState.read(player);
        switch (action.action()) {
            case "page" -> {
                if (action.first() < 0 || action.first() >= PlayerState.PAGES) return;
                data.putInt("Page", action.first());
            }
            case "page_delta" -> {
                if (action.first() != -1 && action.first() != 1) return;
                data.putInt("Page", Math.floorMod(state.page() + action.first(), PlayerState.PAGES));
            }
            case "portrait" -> {
                if (action.first() != 0 && action.first() != 1) return;
                data.putBoolean("Portrait3d", action.first() == 1);
            }
            case "backend" -> {
                if (!ManaBackends.available(player).contains(action.value())) return;
                data.putString("Backend", action.value());
            }
            case "equip" -> {
                if (!PlayerState.validSlot(action.first(), action.second())
                        || (!action.value().isEmpty() && AbilityCatalog.find(action.value()) == null)) return;
                var abilities = data.getCompound("Abilities");
                abilities.putString(PlayerState.key(action.first(), action.second()), action.value());
                data.put("Abilities", abilities);
                var modes = data.getCompound("Modes");
                modes.putInt(PlayerState.key(action.first(), action.second()), 0);
                data.put("Modes", modes);
            }
            case "mode" -> {
                if (!PlayerState.validSlot(action.first(), action.second())) return;
                var art = AbilityCatalog.find(state.ability(action.first(), action.second()));
                int mode;
                try { mode = Integer.parseInt(action.value()); } catch (NumberFormatException ignored) { return; }
                if (art == null || mode < 0 || mode >= art.modes().size()) return;
                var modes = data.getCompound("Modes");
                modes.putInt(PlayerState.key(action.first(), action.second()), mode);
                data.put("Modes", modes);
            }
            case "cycle_mode" -> {
                if (!PlayerState.validSlot(action.first(), action.second())) return;
                var art = AbilityCatalog.find(state.ability(action.first(), action.second()));
                if (art == null || art.modes().isEmpty()) return;
                var modes = data.getCompound("Modes");
                modes.putInt(PlayerState.key(action.first(), action.second()),
                    (state.mode(action.first(), action.second()) + 1) % art.modes().size());
                data.put("Modes", modes);
            }
            case "activate" -> {
                activate(player, state, action.first(), now);
                return;
            }
            default -> { return; }
        }
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
    }

    private static void activate(ServerPlayer player, PlayerState state, int slot, long now) {
        if (!PlayerState.validSlot(state.page(), slot) || state.soul() <= 0) return;
        var art = AbilityCatalog.find(state.ability(state.page(), slot));
        if (art == null) return;
        if (!art.implemented()) {
            player.displayClientMessage(Component.literal(art.name() + " is a preview; no resources spent."), true);
            return;
        }
        int mode = state.mode(state.page(), slot);
        if (mode < 0 || mode >= art.modes().size() || state.cooldownRemaining(art.id(), now) > 0) return;
        if ((mode == 0 && state.soul() >= state.maxSoul())
                || (mode == 1 && state.barrier() >= state.maxBarrier())) return;
        var mana = ManaBackends.resolve(player).mana();
        try {
            if (mana.getCurrent() < art.manaCost() || !mana.consume(art.manaCost())) return;
        } catch (RuntimeException ignored) { return; }
        // Read again after consume so standalone mana is not overwritten by an older snapshot.
        var data = PlayerState.read(player);
        if (mode == 0) data.putDouble("Soul", Math.min(state.maxSoul(), state.soul() + 30));
        else data.putDouble("Barrier", Math.min(state.maxBarrier(), state.barrier() + 30));
        var cooldowns = data.getCompound("Cooldowns");
        cooldowns.putLong(art.id(), now + art.cooldownTicks());
        data.put("Cooldowns", cooldowns);
        data.putString("LastActivationId", art.id());
        data.putLong("LastActivationTick", now);
        data.putInt("LastActivationMode", mode);
        PlayerState.save(player, data);
        GameplayNetwork.sync(player);
    }
}

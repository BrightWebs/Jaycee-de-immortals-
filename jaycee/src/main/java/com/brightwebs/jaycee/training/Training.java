package com.brightwebs.jaycee.training;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.AbilityEvent;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.status.StatusData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Natural training. Every gain is computed on the server from what the player actually did:
 * real distance moved (running into a wall moves nothing), damage really dealt to things that
 * fight back, mana really spent. Gains are scaled by racial talent, capped per second (macros
 * can't pile it up), shrink on the same target over and over, and stop at the race's level cap.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Training {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);
    public static final Supplier<AttachmentType<TrainingData>> DATA = ATTACHMENTS.register("training",
            () -> AttachmentType.builder(() -> TrainingData.EMPTY)
                    .serialize(TrainingData.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, TrainingData.STREAM_CODEC)
                    .build());

    /** Server-side, per player, not saved. */
    private static final class Live {
        final Map<String, Integer> levels = new HashMap<>();
        final Map<String, Double> exp = new HashMap<>();
        final Map<String, TrainingData.Recent> recent = new LinkedHashMap<>();
        final EnumMap<Attr, Double> thisSecond = new EnumMap<>(Attr.class);
        final Map<UUID, Integer> hitsOn = new HashMap<>();
        final Set<Long> chunks = new HashSet<>();
        Vec3 lastPos;
        float lastHealth = -1;
        long lastJump;
        boolean dirty, loaded;
    }
    private static final Map<UUID, Live> LIVE = new HashMap<>();

    private static final double MAX_PER_SECOND = 80;
    private static final int FEED_TICKS = 100; // an entry stays 5 seconds after the activity stops

    private Training() {}

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    public static TrainingData get(Player p) { return p.getData(DATA); }
    public static int level(Player p, Attr a) { return get(p).level(a); }

    private static Live live(ServerPlayer p) {
        Live l = LIVE.computeIfAbsent(p.getUUID(), u -> new Live());
        if (!l.loaded) {
            TrainingData d = get(p);
            l.levels.putAll(d.levels()); l.exp.putAll(d.exp()); l.loaded = true;
        }
        return l;
    }

    // ---- gaining -------------------------------------------------------------------

    public static void gain(ServerPlayer p, Attr a, double amount, String activity) {
        if (amount <= 0 || p.isSpectator()) return;
        Live l = live(p);
        int talent = Potential.talent(Potential.line(p), a);
        amount *= Potential.growth(talent);
        double room = MAX_PER_SECOND - l.thisSecond.getOrDefault(a, 0.0);
        if (room <= 0) return;
        amount = Math.min(amount, room);
        l.thisSecond.merge(a, amount, Double::sum);

        int lvl = l.levels.getOrDefault(a.key(), 0), cap = Potential.limit(p, a);
        if (lvl >= cap) return;
        double exp = l.exp.getOrDefault(a.key(), 0.0) + amount;
        boolean up = false;
        while (lvl < cap && exp >= Potential.need(lvl)) { exp -= Potential.need(lvl); lvl++; up = true; }
        if (lvl >= cap) exp = 0;
        l.levels.put(a.key(), lvl); l.exp.put(a.key(), exp);

        long now = p.level().getGameTime();
        TrainingData.Recent old = l.recent.get(a.key());
        int prev = old != null && old.activity().equals(activity) && now - old.lastTick() < FEED_TICKS ? old.gained() : 0;
        l.recent.remove(a.key());
        l.recent.put(a.key(), new TrainingData.Recent(a.key(), activity, prev + (int) Math.ceil(amount), now));
        l.dirty = true;
        if (up) {
            applyBonuses(p);
            p.sendOverlayMessage(Component.translatable("jaycee.training.level_up",
                    Component.translatable("attr.jaycee." + a.key()), lvl));
        }
    }

    private static void flush(ServerPlayer p, Live l) {
        long now = p.level().getGameTime();
        l.recent.values().removeIf(r -> now - r.lastTick() > FEED_TICKS + 20);
        List<TrainingData.Recent> feed = new ArrayList<>(l.recent.values());
        java.util.Collections.reverse(feed);
        p.setData(DATA, new TrainingData(Map.copyOf(l.levels), Map.copyOf(l.exp), List.copyOf(feed)));
        l.dirty = false;
    }

    // ---- permanent body bonuses ------------------------------------------------------

    private record Bonus(Attr attr, Holder<Attribute> attribute, double perLevel, AttributeModifier.Operation op) {}
    private static final List<Bonus> BONUSES = List.of(
            new Bonus(Attr.PHYSICAL_STRENGTH, Attributes.ATTACK_DAMAGE, 0.06, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.PHYSICAL_STRENGTH, Attributes.BLOCK_BREAK_SPEED, 0.01, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Bonus(Attr.SPEED, Attributes.MOVEMENT_SPEED, 0.006, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Bonus(Attr.AGILITY, Attributes.JUMP_STRENGTH, 0.003, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.AGILITY, Attributes.SAFE_FALL_DISTANCE, 0.06, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.ENDURANCE, Attributes.ARMOR_TOUGHNESS, 0.08, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.ENDURANCE, Attributes.KNOCKBACK_RESISTANCE, 0.004, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.VITALITY, Attributes.MAX_HEALTH, 0.4, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.LUNG_CAPACITY, Attributes.OXYGEN_BONUS, 0.1, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.SWIMMING_SPEED, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.01, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.PERCEPTION, Attributes.ENTITY_INTERACTION_RANGE, 0.02, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.PERCEPTION, Attributes.BLOCK_INTERACTION_RANGE, 0.02, AttributeModifier.Operation.ADD_VALUE),
            new Bonus(Attr.COMBAT_MASTERY, Attributes.ATTACK_SPEED, 0.012, AttributeModifier.Operation.ADD_VALUE));

    private static Identifier bonusId(Bonus b) {
        return Identifier.fromNamespaceAndPath(JayceeMod.ID, "train_" + b.attr().key() + "_" + BuiltInRegistries.ATTRIBUTE.getKey(b.attribute().value()).getPath());
    }

    public static void applyBonuses(Player p) {
        TrainingData d = p instanceof ServerPlayer sp && LIVE.containsKey(sp.getUUID()) ? null : get(p);
        for (Bonus b : BONUSES) {
            int lvl = d != null ? d.level(b.attr()) : live((ServerPlayer) p).levels.getOrDefault(b.attr().key(), 0);
            AttributeInstance i = p.getAttribute(b.attribute());
            if (i == null) continue;
            Identifier id = bonusId(b);
            i.removeModifier(id);
            if (lvl > 0) i.addTransientModifier(new AttributeModifier(id, b.perLevel() * lvl, b.op()));
        }
    }

    // ---- activity hooks -----------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            Live l = live(p);
            if (p.tickCount % 10 == 0 && l.dirty) flush(p, l);
            if (p.tickCount % 20 != 0 || !p.isAlive()) return;
            l.thisSecond.clear();
            Vec3 pos = p.position();
            double moved = l.lastPos == null ? 0 : Math.sqrt(Math.pow(pos.x - l.lastPos.x, 2) + Math.pow(pos.z - l.lastPos.z, 2));
            boolean teleported = moved > 40;
            l.lastPos = pos;
            if (!teleported && moved > 0.5 && p.getVehicle() == null && !p.getAbilities().flying) {
                if (p.isInWater()) {
                    gain(p, Attr.SWIMMING_SPEED, moved * 1.5, "swimming");
                    gain(p, Attr.ENDURANCE, 0.6, "swimming");
                } else if (p.isSprinting()) {
                    gain(p, Attr.SPEED, moved, "running");
                    gain(p, Attr.ENDURANCE, 0.5, "running");
                } else if (p.onGround()) {
                    gain(p, Attr.SPEED, moved * 0.3, "walking");
                }
            }
            if (p.isEyeInFluid(FluidTags.WATER) && p.getAirSupply() < p.getMaxAirSupply()) gain(p, Attr.LUNG_CAPACITY, 2.5, "underwater");
            // exploring new ground
            long ck = p.chunkPosition().pack();
            if (l.chunks.add(ck)) gain(p, Attr.PERCEPTION, 3, "exploring");
            if (l.chunks.size() > 4096) l.chunks.clear();
            // sensing enemies and enduring pressure from stronger beings
            double myEp = Math.max(1, EnergyAPI.get(p).ep());
            int enemies = 0; double pressure = 0, aura = 0;
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), e -> e != p && e.isAlive())) {
                if (e instanceof Enemy) enemies++;
                double ratio = EnergyAPI.get(e).ep() / myEp;
                if (ratio > 1.5 && e.distanceTo(p) < 12) { pressure = Math.max(pressure, Math.min(ratio, 10)); aura = Math.max(aura, EnergyAPI.get(e).aura() / Math.max(1, EnergyAPI.get(p).maxAura())); }
            }
            if (enemies > 0) gain(p, Attr.PERCEPTION, Math.min(3, enemies) * 0.5, "sensing_enemies");
            if (pressure > 0) gain(p, Attr.MENTAL_FORTITUDE, pressure, "pressure");
            if (aura > 1) gain(p, Attr.AURA_CONTROL, Math.min(aura, 6), "aura_exposure");
            // natural recovery
            float hp = p.getHealth();
            if (l.lastHealth >= 0 && hp > l.lastHealth && !p.hasEffect(MobEffects.REGENERATION)) {
                gain(p, Attr.REGENERATION, (hp - l.lastHealth) * 4, "recovering");
                StatusData.addCounter(p, "healed", hp - l.lastHealth);
            }
            l.lastHealth = hp;
            // Regeneration's own bonus: faster natural healing
            int regen = l.levels.getOrDefault(Attr.REGENERATION.key(), 0);
            if (regen > 0 && p.tickCount % 100 == 0 && hp < p.getMaxHealth() && p.getFoodData().getFoodLevel() > 6) p.heal(0.05f * regen);
        });
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onJump", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            Live l = live(p);
            long now = p.level().getGameTime();
            Vec3 v = p.getDeltaMovement();
            if (now - l.lastJump < 8 || v.horizontalDistance() < 0.05) return;
            l.lastJump = now;
            gain(p, Attr.AGILITY, 1.5, p.isSprinting() ? "parkour" : "jumping");
        });
    }

    private static boolean worthy(ServerPlayer p, LivingEntity t) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).getPath();
        return !id.equals("armor_stand") && t != p;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onDamage", () -> {
            float dmg = event.getInflictedDamage();
            if (dmg <= 0) return;
            DamageSource src = event.getSource();
            LivingEntity victim = event.getEntity();
            if (src.getEntity() instanceof ServerPlayer p && worthy(p, victim)) {
                Live l = live(p);
                int hits = l.hitsOn.merge(victim.getUUID(), 1, Integer::sum);
                if (l.hitsOn.size() > 256) l.hitsOn.clear();
                double k = (victim instanceof Enemy || victim instanceof Player ? 1.0 : 0.3) / (1 + hits / 15.0);
                double d = Math.min(dmg, 40) * k;
                if (src.getDirectEntity() instanceof Projectile) {
                    gain(p, Attr.ARCHERY, d * 2, "archery");
                    gain(p, Attr.TARGETING, p.distanceTo(victim) * 1.2 * k, "long_shot");
                } else if (src.getDirectEntity() == p) {
                    gain(p, Attr.PHYSICAL_STRENGTH, d * 2, "melee_combat");
                    if (p.getMainHandItem().is(ItemTags.SWORDS) || p.getMainHandItem().is(ItemTags.AXES)) gain(p, Attr.COMBAT_MASTERY, d * 2.5, "weapon_combat");
                    gain(p, Attr.ENDURANCE, d * 0.5, "combat");
                }
                gain(p, Attr.PERCEPTION, 0.5 * k, "combat_awareness");
            }
            if (victim instanceof ServerPlayer p) {
                boolean env = src.getEntity() == null;
                double k = env ? 0.25 : 1.0;
                gain(p, Attr.VITALITY, Math.min(dmg, 40) * 2 * k, env ? "enduring_hazards" : "taking_hits");
                gain(p, Attr.ENDURANCE, Math.min(dmg, 40) * k, "taking_hits");
                if (src.is(DamageTypeTags.WITCH_RESISTANT_TO) || src.getMsgId().contains("magic")) gain(p, Attr.MENTAL_FORTITUDE, dmg * 3, "resisting_magic");
                if (!env) StatusData.addCounter(p, "physical_taken", src.getDirectEntity() == src.getEntity() ? dmg : 0);
            }
        });
    }

    /** Archery and Targeting make projectiles hit harder; Mental Fortitude blunts magic. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onIncoming", () -> {
            DamageSource src = event.getSource();
            if (src.getDirectEntity() instanceof Projectile && src.getEntity() instanceof ServerPlayer p) {
                TrainingData d = get(p);
                event.setAmount(event.getAmount() * (float) (1 + 0.008 * d.level(Attr.ARCHERY) + 0.004 * d.level(Attr.TARGETING)));
            }
            if (event.getEntity() instanceof ServerPlayer p && (src.is(DamageTypeTags.WITCH_RESISTANT_TO) || src.getMsgId().contains("magic"))) {
                event.setAmount(event.getAmount() * (float) (1 - Math.min(0.4, 0.005 * level(p, Attr.MENTAL_FORTITUDE))));
            }
        });
    }

    /** Magic Control makes skills and spells cheaper. */
    @SubscribeEvent
    public static void onActivate(AbilityEvent.Activate event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onActivate", () -> {
            ServerPlayer p = event.getPlayer();
            double cut = Math.min(0.5, 0.005 * level(p, Attr.MAGIC_CONTROL));
            if (event.getAbility().isMagic() && StatusData.title(p).equals("magicule_master")) cut += 0.1;
            event.setManaCost((int) Math.ceil(event.getManaCost() * (1 - cut)));
        });
    }

    @SubscribeEvent
    public static void onActivated(AbilityEvent.Activated event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onActivated", () -> {
            ServerPlayer p = event.getPlayer();
            if (event.getManaSpent() > 0) {
                gain(p, Attr.MAGIC_CONTROL, event.getManaSpent() * 0.6, event.getAbility().isMagic() ? "casting_magic" : "using_skills");
                StatusData.addCounter(p, "mana_spent", event.getManaSpent());
            }
            if (event.getAuraSpent() > 0) gain(p, Attr.AURA_CONTROL, event.getAuraSpent() * 0.6, "manipulating_aura");
        });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onLogin", () -> {
            LIVE.remove(event.getEntity().getUUID());
            applyBonuses(event.getEntity());
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onRespawn", () -> { applyBonuses(event.getEntity()); });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("Training.onLogout", () -> {
            if (event.getEntity() instanceof ServerPlayer p && LIVE.containsKey(p.getUUID())) flush(p, LIVE.get(p.getUUID()));
            LIVE.remove(event.getEntity().getUUID());
        });
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(BrightCore.commandRoot())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stats").then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("attribute", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Attr.values()).map(Attr::key), b))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Attr a = Attr.byKey(StringArgumentType.getString(c, "attribute"));
                                    if (a == null) return 0;
                                    Live l = live(p);
                                    l.levels.put(a.key(), Math.min(IntegerArgumentType.getInteger(c, "level"), Potential.limit(p, a)));
                                    l.exp.put(a.key(), 0.0);
                                    flush(p, l); applyBonuses(p);
                                    return 1;
                                })))))));
    }
}

package com.brightwebs.jaycee.bonds;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import com.brightwebs.jaycee.network.JayceeNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Naming, as Tensura: Reincarnated does it (RequestNamingKeyPacket / RequestNamingMenuPacket).
 *
 * N while looking at a creature within 6 blocks opens the naming pop-up; Shift + N toggles whether
 * others may name you. The target must submit: below 25% health, or weaker than 1% of your EP.
 * It must also be weaker than you, sane, unnamed and without another master.
 *
 * Three ways to name, each gives the target EP and costs you that much Magicule (capped):
 *   Subdue  x0.5 of its EP, never lowers your max Magicule
 *   Evolve  x1.5, it may evolve; 20% chance the cost comes out of your max Magicule
 *   Endow   x9,   it may evolve and grows its skills; 50% chance the cost comes out of your max
 */
public final class Naming {
    public static final double LOW_HP = 25, MAX_EP_PERCENT = 1, MAX_GAIN = 1_000_000, MAX_COST = 3_000_000;
    public static final double[] GAIN = {0.5, 1.5, 9.0};
    public static final double[] LOSE_MAX_CHANCE = {0, 20, 50};
    private static final Set<String> UNNAMEABLE = Set.of("ender_dragon", "wither", "armor_stand");

    private Naming() {}

    /** N pressed. Shift toggles your own nameable status; otherwise opens naming for the creature in sight. */
    public static void key(ServerPlayer player, boolean shift) {
        if (shift) {
            boolean now = !player.getData(Bonds.NAMEABLE);
            player.setData(Bonds.NAMEABLE, now);
            player.sendSystemMessage(Component.translatable("jaycee.naming.nameable_status",
                    Component.translatable(now ? "jaycee.message.enabled" : "jaycee.message.disabled")).withStyle(ChatFormatting.GOLD));
            return;
        }
        LivingEntity sub = target(player, 6);
        if (sub == null || !canName(player, sub)) return;
        double ep = EnergyAPI.ep(sub);
        double[] gains = new double[3], costs = new double[3];
        for (int i = 0; i < 3; i++) { gains[i] = Math.min(MAX_GAIN, ep * GAIN[i]); costs[i] = Math.min(ep * GAIN[i], MAX_COST); }
        String race = sub instanceof Player p ? RaceAPI.race(p).map(r -> r.name().getString()).orElse("")
                : sub.getType().getDescription().getString();
        PacketDistributor.sendToPlayer(player, new JayceeNetwork.OpenNaming(sub.getId(), sub.getName().getString(), race, ep,
                sub.getHealth() / Math.max(1, sub.getMaxHealth()), gains[0], gains[1], gains[2], costs[0], costs[1], costs[2]));
    }

    /** The living being you're looking at, within range. */
    static LivingEntity target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1), end = eye.add(look.scale(range));
        LivingEntity best = null;
        double bestD = range * range;
        for (Entity e : player.level().getEntities(player, new AABB(eye, end).inflate(1))) {
            if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
            var hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < bestD) { bestD = d; best = le; }
        }
        return best;
    }

    private static void deny(Player p, String key) {
        p.sendSystemMessage(Component.translatable("jaycee.naming." + key).withStyle(ChatFormatting.RED));
    }

    public static boolean canName(ServerPlayer player, LivingEntity sub) {
        if (!sub.isAlive() || sub == player) { deny(player, "cannot_name"); return false; }
        if (sub instanceof Player p) {
            if (!p.getData(Bonds.NAMEABLE)) { deny(player, "cannot_name"); return false; }
        } else if (!(sub instanceof Mob) || UNNAMEABLE.contains(BuiltInRegistries.ENTITY_TYPE.getKey(sub.getType()).getPath())) {
            deny(player, "cannot_name"); return false;
        }
        if (sub instanceof Player master && Bonds.isSubordinateOf(player, master)) { deny(player, "name_owner"); return false; }
        if (!sub.getData(Bonds.NAME).isEmpty()) { deny(player, "already_named"); return false; }
        String owner = Bonds.owner(sub);
        if (!owner.isEmpty() && !owner.equals(player.getStringUUID())) { deny(player, "had_owner"); return false; }
        if (player.isCreative()) return true;
        boolean submits = owner.equals(player.getStringUUID())
                || sub.getHealth() <= sub.getMaxHealth() * LOW_HP / 100
                || EnergyAPI.ep(sub) <= EnergyAPI.ep(player) * MAX_EP_PERCENT / 100;
        if (!submits) { deny(player, "not_submit"); return false; }
        if (EnergyAPI.ep(sub) >= EnergyAPI.ep(player)) { deny(player, "lack_ep"); return false; }
        return true;
    }

    /** The pop-up's Name button. type 0 Subdue, 1 Evolve, 2 Endow. */
    public static void name(ServerPlayer owner, int entityId, String rawName, int type) {
        if (type < 0 || type > 2) return;
        String name = rawName.strip();
        if (name.isEmpty() || name.length() > 32) return;
        if (!(owner.level().getEntity(entityId) instanceof LivingEntity sub) || sub.distanceToSqr(owner) > 256) return;
        if (!canName(owner, sub)) return;

        double subEp = EnergyAPI.ep(sub), raw = subEp * GAIN[type];
        double gain = Math.min(MAX_GAIN, raw), cost = Math.min(raw, MAX_COST);
        EnergyData od = EnergyAPI.get(owner);
        Alignment align = RaceAPI.race(owner).map(Race::alignment).orElse(Alignment.NEUTRAL);
        boolean free = owner.isCreative();
        if (!free) {
            boolean lacks = od.maxMana() <= cost || (align == Alignment.NEUTRAL ? od.aura() + od.mana() < cost : od.mana() < cost);
            if (lacks) { deny(owner, "lack_magicule"); return; }
        }

        sub.setData(Bonds.NAME, name);
        sub.setCustomName(Component.literal(name));
        sub.setData(Bonds.OWNER, owner.getStringUUID());
        sub.setData(Bonds.ORDER, "follow");
        if (sub instanceof Mob m) { m.setPersistenceRequired(); m.setTarget(null); }
        if (sub instanceof TamableAnimal t) t.tame(owner);
        Bonds.add(owner, sub, name);

        ServerLevel level = (ServerLevel) owner.level();
        level.playSound(null, sub.getX(), sub.getY(), sub.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1);
        sub.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        Fx.spiral(level, BrightParticles.HOLY, sub.position(), sub.getBbWidth() + 0.4, sub.getBbHeight() + 0.5, 2, 40, 0);
        Fx.burst(level, BrightParticles.SPARK, sub.position().add(0, sub.getBbHeight() / 2, 0), 30, 0.6);
        if (sub instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("jaycee.naming.name_success", name, owner.getName()).withStyle(ChatFormatting.GOLD));
        }
        owner.sendSystemMessage(Component.translatable("jaycee.naming.named", sub.getType().getDescription(), name).withStyle(ChatFormatting.GOLD));

        // the target's reward: half the gain to each max, and filled up
        EnergyData sd = EnergyAPI.get(sub);
        EnergyData grown = sd.withMax(sd.maxAura() + gain / 2, sd.maxMana() + gain / 2, sd.maxSpirit());
        EnergyAPI.set(sub, grown.withAura(Math.max(grown.aura(), grown.maxAura())).withMana(Math.max(grown.mana(), grown.maxMana())));
        if (type > 0) evolve(sub);
        sub.heal(sub.getMaxHealth());
        List<MobEffectInstance> bad = new ArrayList<>();
        for (MobEffectInstance e : sub.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e);
        for (MobEffectInstance e : bad) sub.removeEffect(e.getEffect());

        if (free) return;
        boolean loseMax = owner.getRandom().nextFloat() * 100 < LOSE_MAX_CHANCE[type] && LOSE_MAX_CHANCE[type] > 0;
        od = EnergyAPI.get(owner);
        if (align == Alignment.NEUTRAL) {
            double mp = Math.min(od.mana(), cost), ap = cost - mp;
            od = od.withMana(od.mana() - mp).withAura(Math.max(0, od.aura() - ap));
            if (loseMax) od = od.withMax(Math.max(1, od.maxAura() - ap), Math.max(1, od.maxMana() - mp), od.maxSpirit());
        } else {
            od = od.withMana(od.mana() - cost);
            if (loseMax) od = od.withMax(od.maxAura(), Math.max(1, od.maxMana() - cost), od.maxSpirit());
        }
        EnergyAPI.set(owner, od);
    }

    /** Evolve and Endow: a named player takes their race's Harvest Festival form, as in Tensura. */
    private static void evolve(LivingEntity sub) {
        if (!(sub instanceof ServerPlayer p)) return;
        // Tensura's getHarvestFestivalEvolution (Human -> Enlightened Human, Ogre -> Kijin...)
        RaceAPI.race(p).ifPresent(r -> {
            String to = com.brightwebs.jaycee.race.TensuraRequirements.HARVEST.get(r.id().getPath());
            Race next = to == null ? null : RaceRegistry.get(com.brightwebs.jaycee.race.JayceeRaces.id(to));
            if (next != null) RaceAPI.set(p, next);
        });
    }
}

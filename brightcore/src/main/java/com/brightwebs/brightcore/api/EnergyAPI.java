package com.brightwebs.brightcore.api;

import com.brightwebs.brightcore.api.race.Alignment;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.brightwebs.brightcore.data.EnergyData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

// aura, mana, spirit and EP for anything alive
//
// killing stuff adds part of its power to your max aura and max mana
// (the exact math is in absorbKill)
public final class EnergyAPI {
    public static final double BASE_GAIN = 0.01, MAX_GAIN = 0.10;
    // tensura's defaults: 1% gain, weak prey cuts it by up to 90%, killing a player gives 5% at most
    public static final double GAIN_PERCENT = 1.0, MAX_EP_REDUCTION = 0.90, DEATH_PENALTY = 0.05;

    // starting energy for mobs: {aura, mana}. content mods add their own
    private static final Map<EntityType<?>, double[]> CREATURE_ENERGY = new HashMap<>();
    // if a mob isn't in the list, base it on its max health
    private static Function<LivingEntity, double[]> fallback = e -> {
        double hp = e.getMaxHealth();
        return new double[]{hp * 40, hp * 10};
    };

    private EnergyAPI() {}

    // sets a mob type's starting aura and mana
    public static void setCreatureEnergy(EntityType<?> type, double aura, double mana) {
        CREATURE_ENERGY.put(type, new double[]{aura, mana});
    }

    public static void setFallback(Function<LivingEntity, double[]> f) {
        fallback = f;
    }

    // reading

    // this thing's energy. rolls a mob's starting numbers the first time (server side)
    public static EnergyData get(LivingEntity entity) {
        EnergyData data = entity.getData(BrightAttachments.ENERGY);
        if (!data.initialized() && !(entity instanceof Player) && !entity.level().isClientSide()) {
            double[] base = CREATURE_ENERGY.getOrDefault(entity.getType(), null);
            if (base == null) base = fallback.apply(entity);
            // +-15% so two wolves aren't exactly the same
            double roll = 0.85 + entity.getRandom().nextDouble() * 0.3;
            double spirit = Math.max(10, entity.getMaxHealth() * 5);
            data = EnergyData.EMPTY.withMax(base[0] * roll, base[1] * roll, spirit).refilled();
            entity.setData(BrightAttachments.ENERGY, data);
        }
        return data;
    }

    public static double ep(LivingEntity entity) {
        return get(entity).ep();
    }

    public static void set(LivingEntity entity, EnergyData data) {
        entity.setData(BrightAttachments.ENERGY, data);
    }

    // changing

    public static boolean spendMana(LivingEntity entity, double amount) {
        if (amount <= 0) return true;
        EnergyData d = get(entity);
        if (d.mana() < amount) return false;
        set(entity, d.withMana(d.mana() - amount));
        return true;
    }

    public static boolean spendAura(LivingEntity entity, double amount) {
        if (amount <= 0) return true;
        EnergyData d = get(entity);
        if (d.aura() < amount) return false;
        set(entity, d.withAura(d.aura() - amount));
        return true;
    }

    // new race: roll aura and mana in its range, start full
    public static void rollFor(LivingEntity entity, Race race) {
        var r = entity.getRandom();
        double aura = race.minAura() + r.nextDouble() * (race.maxAura() - race.minAura());
        double mana = race.minMana() + r.nextDouble() * (race.maxMana() - race.minMana());
        set(entity, EnergyData.EMPTY.withMax(aura, mana, race.spirit()).refilled());
    }

    // evolving: pools go up to at least the new race's minimum, never down, then refill
    public static void raiseFor(LivingEntity entity, Race race) {
        EnergyData d = get(entity);
        var r = entity.getRandom();
        double aura = Math.max(d.maxAura(), race.minAura() + r.nextDouble() * (race.maxAura() - race.minAura()));
        double mana = Math.max(d.maxMana(), race.minMana() + r.nextDouble() * (race.maxMana() - race.minMana()));
        set(entity, d.withMax(aura, mana, Math.max(d.maxSpirit(), race.spirit())).refilled());
    }

    // tensura's kill formula
    //   gained EP = victim EP x (1 - 1% for each time your EP fits in theirs, max 90%)
    //   max aura += gained x 1% x aura factor (neutral 2, everyone else 1)
    //   max mana += gained x 1% x mana factor (neutral 1, everyone else 2)
    // killing a player can't give more than 5% of their EP
    // returns the EP gained
    public static double absorbKill(LivingEntity killer, LivingEntity victim, Alignment alignment) {
        EnergyData k = get(killer), v = get(victim);
        double victimEP = v.ep();
        if (victimEP <= 0) return 0;
        int times = (int) (k.ep() / victimEP);
        double reduction = Math.clamp(0.01 * times, 0, MAX_EP_REDUCTION);
        double total = victimEP * (1 - reduction);
        boolean neutral = alignment == Alignment.NEUTRAL;
        double gainAura = total * GAIN_PERCENT / 100 * (neutral ? 2 : 1);
        double gainMana = total * GAIN_PERCENT / 100 * (neutral ? 1 : 2);
        if (victim instanceof Player && gainAura + gainMana > victimEP * DEATH_PENALTY) {
            double cap = victimEP * DEATH_PENALTY, sum = gainAura + gainMana;
            gainAura = cap * gainAura / sum;
            gainMana = cap * gainMana / sum;
        }
        set(killer, k.withMax(k.maxAura() + gainAura, k.maxMana() + gainMana, k.maxSpirit()).withAura(k.aura()).withMana(k.mana()));
        return gainAura + gainMana;
    }

    // display

    // 950, 12.4k, 3.10M, 1.00B
    public static String format(double v) {
        if (v < 1_000) return String.valueOf(Math.round(v));
        if (v < 1_000_000) return String.format(Locale.ROOT, "%.1fk", v / 1_000);
        if (v < 1_000_000_000) return String.format(Locale.ROOT, "%.2fM", v / 1_000_000);
        return String.format(Locale.ROOT, "%.2fB", v / 1_000_000_000);
    }
}

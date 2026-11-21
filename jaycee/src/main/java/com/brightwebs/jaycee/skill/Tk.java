package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// small shared rules from tensura so every skill spends magicules, counts mastery
// and says when it fails the same way
final class Tk {
    private Tk() {}

    // tensura's base: passives "tick" every 100 game ticks and every 10th tick is a mastery point
    static final int SKILL_TICK = 100, MASTERY_ACTIVATE_TIME = 10, MASTERY_HOLD_TICK = 60;

    // spends magicules (mana). false with tensura's message if there isn't enough. creative is free
    static boolean spend(ServerPlayer p, Ability a, double cost) {
        if (p.isCreative() || cost <= 0) return true;
        if (EnergyAPI.spendMana(p, cost)) return true;
        p.sendOverlayMessage(Component.translatable("jaycee.skill.lack_magicule", a.name()).withStyle(ChatFormatting.RED));
        return false;
    }

    // counts skill "ticks" and gives a mastery point every 10th, like tensura's activatedTimes
    static void activatedTime(ServerPlayer p, Ability a) {
        String key = "act_" + a.id().getPath();
        double t = StatusData.counter(p, key);
        if (t % MASTERY_ACTIVATE_TIME == 0) AbilityAPI.addMasteryPoint(p, a);
        StatusData.addCounter(p, key, 1);
    }

    static boolean mastered(ServerPlayer p, Ability a) { return AbilityAPI.isMastered(p, a); }

    // adds magicules. withMax raises the max too (tensura's GainType.MAX)
    static void gainMana(LivingEntity e, double amount, boolean withMax) {
        EnergyData d = EnergyAPI.get(e);
        double max = withMax ? d.maxMana() + amount : d.maxMana();
        EnergyAPI.set(e, new EnergyData(d.aura(), d.maxAura(), Math.min(max, d.mana() + amount), max, d.spirit(), d.maxSpirit(), d.initialized()));
    }

    // takes up to amount magicules from a target, returns how much it got
    static double drainMana(LivingEntity e, double amount) {
        EnergyData d = EnergyAPI.get(e);
        double take = Math.min(d.mana(), amount);
        EnergyAPI.set(e, d.withMana(d.mana() - take));
        return take;
    }

    static void fail(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }

    // ring of particles (magic circles)
    static void ring(ServerLevel l, ParticleOptions part, Vec3 c, double r, int points, double y) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            l.sendParticles(part, c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }
}

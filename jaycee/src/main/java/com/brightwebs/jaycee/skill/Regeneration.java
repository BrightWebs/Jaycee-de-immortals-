package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.fx.BrightParticles;
import com.brightwebs.brightcore.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The regeneration chain from Tensura: Reincarnated.
 *  Self-Regeneration (Common): 2 HP a second while hurt (4 + 8 spiritual health mastered), 100 magicules every 5 s.
 *  Ultraspeed Regeneration (Extra): all missing HP at once, 100 magicules per HP (60 mastered).
 *    Learned with Self-Regeneration mastered, the sea boss defeated (Tensura: Charybdis; here the
 *    Elder Guardian) and more than 300,000 EP.
 *  Infinite Regeneration (Extra): the same, and spiritual health too. Learned with Ultraspeed
 *    Regeneration and more than 2,000,000 EP.
 * The stronger one takes over: a lower link does nothing while a higher one is switched on.
 */
final class Regeneration {
    static final double COST = 100, COST_MASTERED = 60;
    private Regeneration() {}

    static Ability skill(String id) { return AbilityRegistry.get(JayceeSkills.id(id)); }

    static boolean overridden(ServerPlayer p, String by) {
        Ability a = skill(by);
        return a != null && AbilityAPI.isSwitchedOn(p, a);
    }

    static void restoreSpirit(ServerPlayer p, double amount) {
        EnergyData d = EnergyAPI.get(p);
        if (d.spirit() < d.maxSpirit()) EnergyAPI.set(p, d.withSpirit(Math.min(d.maxSpirit(), d.spirit() + amount)));
    }

    static void instant(ServerPlayer p, Ability a, boolean infinite) {
        if (!infinite && overridden(p, "infinite_regeneration")) return;
        if (p.tickCount % Tk.SKILL_TICK == 0) Tk.activatedTime(p, a);
        float lacking = p.getMaxHealth() - p.getHealth();
        if (infinite && p.tickCount % 20 == 0) restoreSpirit(p, EnergyAPI.get(p).maxSpirit());
        if (lacking <= 0 || !p.isAlive()) return;
        double perHp = Tk.mastered(p, a) ? COST_MASTERED : COST;
        double mana = p.isCreative() ? Double.MAX_VALUE : EnergyAPI.get(p).mana();
        float heal = (float) Math.min(lacking, mana / perHp);
        if (heal > 0) {
            if (!p.isCreative()) EnergyAPI.spendMana(p, heal * perHp);
            p.heal(heal);
            if (heal >= 1) Fx.aura((ServerLevel) p.level(), BrightParticles.HEAL, p, Math.min(12, 2 + (int) heal));
        }
        if (heal < lacking) {
            p.sendSystemMessage(Component.translatable("jaycee.skill.lack_magicule.toggled_off", a.name()).withStyle(ChatFormatting.RED));
            AbilityAPI.setSwitchedOn(p, a, false);
        }
    }
}

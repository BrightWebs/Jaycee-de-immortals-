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

// tensura's regen chain
//   self regeneration (common): 2 HP a second while hurt (4 + 8 spiritual health mastered), 100 magicules every 5s
//   ultraspeed regeneration (extra): all missing HP at once, 100 magicules per HP (60 mastered)
//     you get it with self regen mastered, the sea boss beaten (charybdis in tensura, the elder guardian here)
//     and over 300,000 EP
//   infinite regeneration (extra): same thing plus spiritual health. you get it with ultraspeed and over 2,000,000 EP
// the stronger one takes over, a lower one does nothing while a higher one is on
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

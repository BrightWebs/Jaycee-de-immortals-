package com.brightwebs.brightcore.data;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.skill.BrightSkills;
import com.brightwebs.brightcore.api.skill.DamageKind;
import com.brightwebs.brightcore.api.skill.ResistanceSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Resistances in action: they cut incoming damage, grow with every hit they take,
 * are learned by surviving enough of one kind of harm, and become Nullifications
 * once mastered by a being of 1M EP.
 */
@EventBusSubscriber(modid = BrightCore.ID)
public final class SkillEvents {
    private SkillEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("SkillEvents.onIncomingDamage", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
            for (DamageKind kind : DamageKind.values()) {
                if (!kind.matches(event.getSource())) continue;
                ResistanceSkill nul = BrightSkills.nullification(kind), res = BrightSkills.resistance(kind);
                if (res == null || nul == null) continue;
                ResistanceSkill best = AbilityAPI.has(player, nul) ? nul : AbilityAPI.has(player, res) ? res : null;
    
                if (best == null) {
                    learnByEndurance(player, kind, res, event.getAmount());
                } else {
                    float reduced = best.reduce(event.getAmount(), player.getHealth(), AbilityAPI.masteryFraction(player, best));
                    event.setAmount(reduced);
                    AbilityAPI.addMasteryPoint(player, best);
                    if (best == res && AbilityAPI.isMastered(player, res)
                            && EnergyAPI.get(player).ep() >= BrightSkills.EP_FOR_NULLIFICATION && AbilityAPI.learn(player, nul)) {
                        player.sendSystemMessage(Component.translatable("brightcore.message.acquired", nul.name()));
                    }
                }
                if (event.getAmount() <= 0) {
                    event.setCanceled(true);
                    return;
                }
            }
        });
    }

    /**
     * Tensura's learning: a hit above 15 damage is a learning point; smaller hits add up, and every
     * 150 damage taken that way is another point. 500 points and the resistance is learned.
     */
    private static void learnByEndurance(ServerPlayer player, DamageKind kind, ResistanceSkill res, float amount) {
        AbilityData data = AbilityAPI.get(player);
        String pk = kind.key() + "_points";
        double points = data.damageTaken().getOrDefault(pk, 0.0), pool = data.damageTaken().getOrDefault(kind.key(), 0.0);
        if (amount > BrightSkills.HIT_FOR_POINT) {
            points += 1;
        } else {
            pool += amount;
            if (pool >= BrightSkills.HIT_FOR_POINT * BrightSkills.POOL_MULTIPLIER) { points += 1; pool = 0; }
        }
        if (points >= BrightSkills.POINTS_TO_LEARN) {
            player.setData(BrightAttachments.ABILITIES, data.withDamageTaken(pk, 0).withDamageTaken(kind.key(), 0));
            if (AbilityAPI.learn(player, res)) {
                player.sendSystemMessage(Component.translatable("brightcore.message.acquired", res.name()));
            }
        } else {
            player.setData(BrightAttachments.ABILITIES, data.withDamageTaken(pk, points).withDamageTaken(kind.key(), pool));
        }
    }
}

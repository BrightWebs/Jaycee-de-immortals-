package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceEvent;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.race.JayceeRaces;
import com.brightwebs.jaycee.status.StatusData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * How the beta's skills are earned, Tensura style: by doing things, not by buying them.
 *   Magic Perception    spend 300 mana in total
 *   Self-Regeneration   recover 200 HP naturally
 *   Body Armor          endure 150 damage from attacks
 *   Raphael             Great Sage mastered and 1,000,000 EP: Great Sage evolves
 *   Fireball            every reincarnated soul knows this basic spell (beta)
 * Also: Merfolk's natural form (tail) until it evolves.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Acquisition {
    private Acquisition() {}

    private static Ability skill(String id) { return AbilityRegistry.get(JayceeSkills.id(id)); }

    private static void learn(ServerPlayer p, String id) {
        Ability a = skill(id);
        if (a != null && AbilityAPI.learn(p, a)) {
            p.sendSystemMessage(Component.translatable("jaycee.skill.acquired", a.name()).withStyle(ChatFormatting.AQUA));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("Acquisition.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
            if (RaceAPI.race(p).isEmpty()) return;
            if (StatusData.counter(p, "beta_fireball") == 0) { StatusData.addCounter(p, "beta_fireball", 1); learn(p, "fireball"); }
            // Tensura: Magic Sense comes to slimes, or to anyone forced to sense without sight.
            String line = RaceAPI.race(p).map(r -> com.brightwebs.jaycee.race.JayceeRaces.lineOf(r.id().getPath())).orElse("");
            if ("slime".equals(line) || p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) || p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                learn(p, "magic_perception");
            }
            // Tensura: Self-Regeneration after beating 500 slimes.
            if (StatusData.counter(p, "slimes_killed") >= 500) learn(p, "self_regeneration");
            if (StatusData.counter(p, "physical_taken") >= 150) learn(p, "body_armor");
            Ability sage = skill("great_sage"), raphael = skill("raphael");
            if (sage != null && raphael != null && AbilityAPI.has(p, sage) && AbilityAPI.isMastered(p, sage)
                    && EnergyAPI.get(p).ep() >= 1_000_000) {
                AbilityAPI.forget(p, sage);
                AbilityAPI.learn(p, raphael);
                p.sendSystemMessage(Component.translatable("jaycee.skill.raphael_evolved").withStyle(ChatFormatting.GOLD));
            }
            // Self-Regeneration became a toggle (on by default): switch it on once for older saves.
            Ability regen = skill("self_regeneration");
            if (regen != null && AbilityAPI.has(p, regen) && StatusData.counter(p, "regen_toggle_init") == 0) {
                StatusData.addCounter(p, "regen_toggle_init", 1);
                AbilityAPI.setToggled(p, regen, true);
            }
            merfolk(p);
        });
    }

    @SubscribeEvent
    public static void onEvolve(RaceEvent.Evolve event) {
        com.brightwebs.brightcore.api.util.Safe.run("Acquisition.onEvolve", () -> {
            StatusData.addCounter(event.getPlayer(), "evolutions", 1);
        });
    }

    /** Starting Merfolk keep their tail: strong in water, slow on land, and they dry out. */
    private static void merfolk(ServerPlayer p) {
        var race = RaceAPI.race(p).orElse(null);
        if (race == null || !JayceeRaces.lineOf(race.id().getPath()).equals("merfolk")) return;
        if (p.isInWater()) {
            SkillHelper.keep(p, MobEffects.WATER_BREATHING, 0);
            SkillHelper.keep(p, MobEffects.DOLPHINS_GRACE, 0);
            StatusData.addCounter(p, "dry", -StatusData.counter(p, "dry"));
            return;
        }
        if (race.tier() > 1) return; // evolved Merfolk adapt to land
        SkillHelper.keep(p, MobEffects.SLOWNESS, 1);
        if (p.isInWaterOrRain()) { StatusData.addCounter(p, "dry", -StatusData.counter(p, "dry")); return; }
        double dry = StatusData.addCounter(p, "dry", 2);
        if (dry > 240 && p.tickCount % 200 == 0) {
            p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), p.damageSources().dryOut(), 1f);
            p.sendOverlayMessage(Component.translatable("jaycee.race.merfolk.drying"));
        }
    }
}

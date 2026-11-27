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

// how you get the beta skills, tensura style: by doing stuff, not buying them
//   magic perception    spend 300 mana total
//   self regeneration   heal 200 HP naturally
//   body armor          take 150 damage from attacks
//   raphael             great sage mastered and 1,000,000 EP, great sage evolves
//   fireball            every reincarnated soul knows this one (beta)
// also merfolk keep their tail until they evolve
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

    // the newer batch, same conditions as tensura where we have the same stuff.
    // tensura uses its own mobs for some of these, so we swap in vanilla ones:
    //   paralysis  tensura: 500 evil centipedes      here: 50 cave spiders
    //   poison     tensura: 100 spider eyes or 100 black spiders   here: 100 spider eyes or 100 spiders
    //   corrosion  tensura: 100 rotten flesh eaten (or a tempest serpent / orc lord)
    //   sense heat source  rimuru got it from the tempest serpent, here: 30 magma cubes
    //   universal perception  magic sense and sense heat source both mastered
    private static void more(ServerPlayer p, double ep) {
        Pressure.learnChecks(p);
        // natural race skills (also catches players who were already that race before these existed)
        RaceAPI.race(p).ifPresent(r -> { for (String id : com.brightwebs.jaycee.race.RaceExtras.intrinsics(r.id().getPath())) learn(p, id); });
        if (ep > 0) learn(p, "danger_sense");  // tensura's danger sense has no EP gate
        if (ep > 50_000) learn(p, "shadow_motion");
        if (ep > 80_000) learn(p, "ultra_instinct");
        if (ep > 30_000 && StatusData.counter(p, "spiders_killed") >= 50) learn(p, "snake_eye");
        if (ep > 3_000) learn(p, "strength");
        if (ep > 8_000) learn(p, "thought_acceleration");
        if (ep > 10_000) { learn(p, "voice_cannon"); learn(p, "water_blade"); learn(p, "hydraulic_propulsion"); learn(p, "gravity_flight"); }
        if (ep > 15_000) learn(p, "strengthen_body");
        if (ep > 20_000) learn(p, "steel_strength");
        var stats = p.getStats();
        if (StatusData.counter(p, "cave_spiders_killed") >= 50) learn(p, "paralysis");
        if (stats.getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.SPIDER_EYE)) >= 100
                || StatusData.counter(p, "spiders_killed") >= 100) learn(p, "poison");
        if (stats.getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.ROTTEN_FLESH)) >= 100) learn(p, "corrosion");
        if (StatusData.counter(p, "magma_cubes_killed") >= 30) learn(p, "sense_heat_source");
        Ability sense = skill("magic_perception"), heat = skill("sense_heat_source");
        if (sense != null && heat != null && AbilityAPI.isMastered(p, sense) && AbilityAPI.isMastered(p, heat)) learn(p, "universal_perception");
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("Acquisition.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
            if (RaceAPI.race(p).isEmpty()) return;
            if (StatusData.counter(p, "beta_fireball") == 0) { StatusData.addCounter(p, "beta_fireball", 1); learn(p, "fireball"); }
            // tensura: slimes get magic sense, or anyone forced to sense stuff without seeing
            String line = RaceAPI.race(p).map(r -> com.brightwebs.jaycee.race.JayceeRaces.lineOf(r.id().getPath())).orElse("");
            if ("slime".equals(line) || p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) || p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                learn(p, "magic_perception");
            }
            // tensura: self regen after killing 500 slimes
            if (StatusData.counter(p, "slimes_killed") >= 500) learn(p, "self_regeneration");
            // tensura's regen chain
            Ability selfRegen = skill("self_regeneration"), ultra = skill("ultraspeed_regeneration");
            double ep = EnergyAPI.get(p).ep();
            if (selfRegen != null && AbilityAPI.isMastered(p, selfRegen) && StatusData.counter(p, "sea_boss_killed") > 0 && ep > 300_000) learn(p, "ultraspeed_regeneration");
            if (ultra != null && AbilityAPI.has(p, ultra) && ep > 2_000_000) learn(p, "infinite_regeneration");
            if (StatusData.counter(p, "physical_taken") >= 150) learn(p, "body_armor");
            more(p, ep);
            Ability sage = skill("great_sage"), raphael = skill("raphael");
            if (sage != null && raphael != null && AbilityAPI.has(p, sage) && AbilityAPI.isMastered(p, sage)
                    && EnergyAPI.get(p).ep() >= 1_000_000) {
                AbilityAPI.forget(p, sage);
                AbilityAPI.learn(p, raphael);
                p.sendSystemMessage(Component.translatable("jaycee.skill.raphael_evolved").withStyle(ChatFormatting.GOLD));
            }
            // self regen is a toggle now (on by default), turn it on once for old saves
            Ability regen = skill("self_regeneration");
            if (regen != null && AbilityAPI.has(p, regen) && StatusData.counter(p, "regen_toggle_init") == 0) {
                StatusData.addCounter(p, "regen_toggle_init", 1);
                AbilityAPI.setToggled(p, regen, true);
            }
            com.brightwebs.jaycee.race.RaceSkills.raceChanged(p);
            if (p.tickCount % 20 == 0) com.brightwebs.jaycee.appearance.Appearance.raceChanged(p);  // keep the look valid for the form you're in
            merfolk(p);
        });
    }

    @SubscribeEvent
    public static void onEvolve(RaceEvent.Evolve event) {
        com.brightwebs.brightcore.api.util.Safe.run("Acquisition.onEvolve", () -> {
            StatusData.addCounter(event.getPlayer(), "evolutions", 1);
        });
    }

    // starting merfolk keep the tail: strong in water, slow on land, and they dry out
    private static void merfolk(ServerPlayer p) {
        var race = RaceAPI.race(p).orElse(null);
        if (race == null || !JayceeRaces.lineOf(race.id().getPath()).equals("merfolk")) return;
        if (p.isInWater()) {
            SkillHelper.keep(p, MobEffects.WATER_BREATHING, 0);
            SkillHelper.keep(p, MobEffects.DOLPHINS_GRACE, 0);
            StatusData.addCounter(p, "dry", -StatusData.counter(p, "dry"));
            return;
        }
        if (race.tier() > 1) return;  // evolved merfolk can handle land
        SkillHelper.keep(p, MobEffects.SLOWNESS, 1);
        if (p.isInWaterOrRain()) { StatusData.addCounter(p, "dry", -StatusData.counter(p, "dry")); return; }
        double dry = StatusData.addCounter(p, "dry", 2);
        if (dry > 240 && p.tickCount % 200 == 0) {
            p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), p.damageSources().dryOut(), 1f);
            p.sendOverlayMessage(Component.translatable("jaycee.race.merfolk.drying"));
        }
    }
}

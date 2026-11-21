package com.brightwebs.jaycee.command;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import com.brightwebs.brightcore.api.AbilityRegistry;
import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.SkillType;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.data.AbilityData;
import com.brightwebs.brightcore.data.BrightAttachments;
import com.brightwebs.brightcore.data.EnergyData;
import com.brightwebs.brightcore.data.RaceData;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.bonds.Bonds;
import com.brightwebs.jaycee.status.DemonLord;
import com.brightwebs.jaycee.status.StatusData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// commands laid out the same way tensura has them, just under /jaycee instead of /tensura.
// players get /ability, /evolve, /nameable and /despawn. ops get /jaycee edit, get and reset.
@EventBusSubscriber(modid = JayceeMod.ID)
public final class JayceeCommands {
    private JayceeCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SKILLS =
            (c, b) -> SharedSuggestionProvider.suggestResource(AbilityRegistry.REGISTRY.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> RACES =
            (c, b) -> SharedSuggestionProvider.suggestResource(RaceRegistry.REGISTRY.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> SKILL_TYPES = (c, b) -> SharedSuggestionProvider.suggest(
            Arrays.stream(SkillType.values()).filter(t -> t != SkillType.MAGIC).map(t -> t.name().toLowerCase(Locale.ROOT)), b);
    private static final SuggestionProvider<CommandSourceStack> MAGIC_TYPES =
            (c, b) -> SharedSuggestionProvider.suggest(List.of("aspectual", "elemental", "holy", "summoning", "other"), b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(abilityCommand());
        d.register(evolveCommand());
        d.register(Commands.literal("nameable").then(Commands.argument("nameable", BoolArgumentType.bool()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean on = BoolArgumentType.getBool(c, "nameable");
            p.setData(Bonds.NAMEABLE, on);
            ok(c, "Others " + (on ? "can" : "can't") + " name you now.");
            return 1;
        })));
        d.register(Commands.literal("despawn").then(Commands.argument("entity", EntityArgument.entities()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = 0;
            for (Entity e : EntityArgument.getEntities(c, "entity")) {
                // like tensura you can only despawn your own subordinates
                if (Bonds.isSubordinateOf(e, p)) {
                    Bonds.command(p, e.getStringUUID(), "release");
                    e.discard();
                    n++;
                }
            }
            int count = n;
            ok(c, "Despawned " + count + ".");
            return n;
        })));
        d.register(Commands.literal(BrightCore.commandRoot()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("edit")
                        .then(editAbility())
                        .then(editStat())
                        .then(editRace())
                        .then(editAwakening())
                        .then(editForceEvo())
                        .then(editOwner()))
                .then(Commands.literal("get")
                        .then(getStat())
                        .then(getRace())
                        .then(getAbility())
                        .then(getAwakening())
                        .then(getOwner()))
                .then(reset()));
    }

    // ---------------------------------------------------------------- helpers

    private static void ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.DARK_GREEN), true);
    }

    private static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    private static Ability skill(CommandContext<CommandSourceStack> c) {
        return AbilityRegistry.get(IdentifierArgument.getId(c, "skill"));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Identifier> skillArg() {
        return Commands.argument("skill", IdentifierArgument.id()).suggests(SKILLS);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> entities() {
        return Commands.argument("entity", EntityArgument.entities());
    }

    private static List<ServerPlayer> players(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        List<ServerPlayer> out = new ArrayList<>();
        for (Entity e : EntityArgument.getEntities(c, "entity")) if (e instanceof ServerPlayer p) out.add(p);
        return out;
    }

    private static List<LivingEntity> living(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : EntityArgument.getEntities(c, "entity")) if (e instanceof LivingEntity l) out.add(l);
        return out;
    }

    /** the skill groups tensura's "all" and "random" work on */
    private static Predicate<Ability> group(String kind, String sub) {
        return switch (kind) {
            case "skill" -> a -> !a.isMagic() && (sub == null || a.type().name().equalsIgnoreCase(sub));
            case "magic" -> a -> a.isMagic() && (sub == null || a.magicClass().equalsIgnoreCase(sub));
            default -> a -> true;
        };
    }

    private static List<Ability> allSkills(Predicate<Ability> filter) {
        List<Ability> out = new ArrayList<>();
        for (Ability a : AbilityRegistry.REGISTRY) if (filter.test(a)) out.add(a);
        return out;
    }

    // ---------------------------------------------------------------- /ability (players)

    private static LiteralArgumentBuilder<CommandSourceStack> abilityCommand() {
        return Commands.literal("ability")
                .then(Commands.literal("set").then(skillArg().then(Commands.argument("mode", IntegerArgumentType.integer(1, 9))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, AbilityData.SLOTS_PER_PAGE)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Ability a = skill(c);
                            if (a == null || !AbilityAPI.has(p, a)) return fail(c, "You don't have that skill.");
                            int mode = Math.min(IntegerArgumentType.getInteger(c, "mode"), Math.max(1, a.modes().size())) - 1;
                            AbilityAPI.setMode(p, a, mode);
                            int slot = AbilityAPI.get(p).page() * AbilityData.SLOTS_PER_PAGE + IntegerArgumentType.getInteger(c, "slot") - 1;
                            AbilityAPI.assign(p, slot, a);
                            ok(c, a.name().getString() + " is on slot " + IntegerArgumentType.getInteger(c, "slot") + ".");
                            return 1;
                        })))))
                .then(Commands.literal("preset").then(Commands.literal("set").then(Commands.argument("preset", IntegerArgumentType.integer(1, AbilityData.PAGES)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    AbilityAPI.setPage(p, IntegerArgumentType.getInteger(c, "preset") - 1);
                    ok(c, "Switched to preset " + IntegerArgumentType.getInteger(c, "preset") + ".");
                    return 1;
                }))))
                .then(toggleBranch(false));
    }

    /** /ability toggle (your own skills) and /jaycee edit ability toggle <entity> (someone else's) */
    private static LiteralArgumentBuilder<CommandSourceStack> toggleBranch(boolean edit) {
        LiteralArgumentBuilder<CommandSourceStack> t = Commands.literal("toggle");
        ArgumentBuilder<CommandSourceStack, ?> root = edit ? entities() : t;
        root.then(skillArg().then(Commands.argument("toggle", BoolArgumentType.bool()).executes(c -> {
            Ability a = skill(c);
            if (a == null) return fail(c, "No skill like that.");
            int n = 0;
            for (ServerPlayer p : edit ? players(c) : List.of(c.getSource().getPlayerOrException())) {
                if (AbilityAPI.has(p, a)) { toggle(p, a, BoolArgumentType.getBool(c, "toggle")); n++; }
            }
            ok(c, a.name().getString() + " turned " + (BoolArgumentType.getBool(c, "toggle") ? "on" : "off") + " for " + n + ".");
            return n;
        })));
        for (String mode : List.of("all", "random")) {
            ArgumentBuilder<CommandSourceStack, ?> m = Commands.literal(mode);
            m.then(toggleEnd(edit, mode, c -> group("all", null)));
            if (edit) m.then(Commands.literal("everything").then(toggleEnd(edit, mode, c -> group("all", null))));
            m.then(Commands.literal("skill").then(toggleEnd(edit, mode, c -> group("skill", null)))
                    .then(Commands.argument("skilltype", StringArgumentType.word()).suggests(SKILL_TYPES)
                            .then(toggleEnd(edit, mode, c -> group("skill", StringArgumentType.getString(c, "skilltype"))))));
            m.then(Commands.literal("magic").then(toggleEnd(edit, mode, c -> group("magic", null)))
                    .then(Commands.argument("magictype", StringArgumentType.word()).suggests(MAGIC_TYPES)
                            .then(toggleEnd(edit, mode, c -> group("magic", StringArgumentType.getString(c, "magictype"))))));
            root.then(m);
        }
        if (edit) t.then(root);
        return t;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> toggleEnd(boolean edit, String mode,
                                                                   java.util.function.Function<CommandContext<CommandSourceStack>, Predicate<Ability>> filter) {
        return Commands.argument("toggle", BoolArgumentType.bool()).executes(c -> {
            boolean on = BoolArgumentType.getBool(c, "toggle");
            int n = 0;
            for (ServerPlayer p : edit ? players(c) : List.of(c.getSource().getPlayerOrException())) {
                List<Ability> owned = new ArrayList<>(AbilityAPI.owned(p).stream()
                        .filter(a -> a.isToggle() || a.isSwitchable()).filter(filter.apply(c)).toList());
                if (mode.equals("random") && !owned.isEmpty()) owned = List.of(owned.get(p.getRandom().nextInt(owned.size())));
                for (Ability a : owned) { toggle(p, a, on); n++; }
            }
            ok(c, "Turned " + (on ? "on " : "off ") + n + " skill(s).");
            return n;
        });
    }

    private static void toggle(ServerPlayer p, Ability a, boolean on) {
        if (a.isToggle()) AbilityAPI.setToggled(p, a, on);
        if (a.isSwitchable()) AbilityAPI.setSwitchedOn(p, a, on);
    }

    // ---------------------------------------------------------------- /evolve (players)

    private static LiteralArgumentBuilder<CommandSourceStack> evolveCommand() {
        return Commands.literal("evolve")
                .then(Commands.literal("race").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (Race.Evolution e : RaceAPI.next(p)) {
                        if (RaceAPI.evolve(p, e.target())) { ok(c, "You evolved."); return 1; }
                    }
                    return fail(c, "You don't meet the requirements for any evolution yet.");
                }))
                .then(Commands.literal("demonLord").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return DemonLord.begin(p) ? 1 : fail(c, "You can't start the Harvest Festival right now.");
                }))
                .then(Commands.literal("hero").executes(c -> fail(c, "The hero path isn't in yet.")));
    }

    // ---------------------------------------------------------------- /jaycee edit ability

    private static LiteralArgumentBuilder<CommandSourceStack> editAbility() {
        LiteralArgumentBuilder<CommandSourceStack> ab = Commands.literal("ability");

        // grant / revoke
        for (boolean grant : new boolean[]{true, false}) {
            ArgumentBuilder<CommandSourceStack, ?> ent = entities();
            ent.then(skillArg().executes(c -> {
                Ability a = skill(c);
                if (a == null) return fail(c, "No skill like that.");
                int n = 0;
                for (ServerPlayer p : players(c)) if (grant ? AbilityAPI.learn(p, a) : AbilityAPI.forget(p, a)) n++;
                ok(c, (grant ? "Granted " : "Revoked ") + a.name().getString() + " for " + n + ".");
                return n;
            }));
            for (String mode : List.of("all", "random")) {
                ArgumentBuilder<CommandSourceStack, ?> m = Commands.literal(mode);
                m.executes(c -> grantMany(c, grant, mode, group("all", null)));
                m.then(Commands.literal("skill").executes(c -> grantMany(c, grant, mode, group("skill", null)))
                        .then(Commands.argument("skilltype", StringArgumentType.word()).suggests(SKILL_TYPES)
                                .executes(c -> grantMany(c, grant, mode, group("skill", StringArgumentType.getString(c, "skilltype"))))));
                m.then(Commands.literal("magic").executes(c -> grantMany(c, grant, mode, group("magic", null)))
                        .then(Commands.argument("magictype", StringArgumentType.word()).suggests(MAGIC_TYPES)
                                .executes(c -> grantMany(c, grant, mode, group("magic", StringArgumentType.getString(c, "magictype"))))));
                ent.then(m);
            }
            ab.then(Commands.literal(grant ? "grant" : "revoke").then(ent));
        }

        // mastery <entity> <skill> <mastery>|max, mastery <entity> all ... <mastery>|max
        ArgumentBuilder<CommandSourceStack, ?> mEnt = entities();
        mEnt.then(skillArg()
                .then(Commands.argument("mastery", IntegerArgumentType.integer(0)).executes(c -> mastery(c, List.of(skill(c)), IntegerArgumentType.getInteger(c, "mastery"))))
                .then(Commands.literal("max").executes(c -> mastery(c, List.of(skill(c)), -1))));
        for (String mode : List.of("all", "random")) {
            ArgumentBuilder<CommandSourceStack, ?> m = Commands.literal(mode);
            masteryEnds(m, mode, c -> group("all", null));
            ArgumentBuilder<CommandSourceStack, ?> sk = Commands.literal("skill");
            masteryEnds(sk, mode, c -> group("skill", null));
            ArgumentBuilder<CommandSourceStack, ?> skt = Commands.argument("skilltype", StringArgumentType.word()).suggests(SKILL_TYPES);
            masteryEnds(skt, mode, c -> group("skill", StringArgumentType.getString(c, "skilltype")));
            sk.then(skt);
            ArgumentBuilder<CommandSourceStack, ?> mg = Commands.literal("magic");
            masteryEnds(mg, mode, c -> group("magic", null));
            ArgumentBuilder<CommandSourceStack, ?> mgt = Commands.argument("magictype", StringArgumentType.word()).suggests(MAGIC_TYPES);
            masteryEnds(mgt, mode, c -> group("magic", StringArgumentType.getString(c, "magictype")));
            mg.then(mgt);
            m.then(sk).then(mg);
            mEnt.then(m);
        }
        ab.then(Commands.literal("mastery").then(mEnt));

        // toggle
        ab.then(toggleBranch(true));

        // cooldown <entity> <skill> <seconds> <mode>, cooldown <entity> all <seconds>
        ab.then(Commands.literal("cooldown").then(entities()
                .then(skillArg().then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(c -> cooldown(c, List.of(skill(c))))
                        .then(Commands.argument("mode", IntegerArgumentType.integer(1, 9)).executes(c -> cooldown(c, List.of(skill(c)))))))
                .then(Commands.literal("all").then(Commands.argument("seconds", IntegerArgumentType.integer(0)).executes(c -> cooldown(c, null))))));
        return ab;
    }

    private static void masteryEnds(ArgumentBuilder<CommandSourceStack, ?> node, String mode,
                                    java.util.function.Function<CommandContext<CommandSourceStack>, Predicate<Ability>> filter) {
        node.then(Commands.argument("mastery", IntegerArgumentType.integer(0)).executes(c -> masteryGroup(c, mode, filter.apply(c), IntegerArgumentType.getInteger(c, "mastery"))))
                .then(Commands.literal("max").executes(c -> masteryGroup(c, mode, filter.apply(c), -1)));
    }

    private static int grantMany(CommandContext<CommandSourceStack> c, boolean grant, String mode, Predicate<Ability> filter) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            List<Ability> pool = grant ? allSkills(filter).stream().filter(a -> !AbilityAPI.has(p, a)).toList()
                    : AbilityAPI.owned(p).stream().filter(filter).toList();
            if (mode.equals("random") && !pool.isEmpty()) pool = List.of(pool.get(p.getRandom().nextInt(pool.size())));
            for (Ability a : pool) if (grant ? AbilityAPI.learn(p, a) : AbilityAPI.forget(p, a)) n++;
        }
        ok(c, (grant ? "Granted " : "Revoked ") + n + " skill(s).");
        return n;
    }

    private static int mastery(CommandContext<CommandSourceStack> c, List<Ability> skills, int points) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            for (Ability a : skills) {
                if (a == null || !AbilityAPI.has(p, a)) continue;
                AbilityAPI.setMastery(p, a, points < 0 ? a.type().maxMastery() : points);
                n++;
            }
        }
        ok(c, "Set mastery on " + n + " skill(s).");
        return n;
    }

    private static int masteryGroup(CommandContext<CommandSourceStack> c, String mode, Predicate<Ability> filter, int points) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            List<Ability> owned = AbilityAPI.owned(p).stream().filter(filter).toList();
            if (mode.equals("random") && !owned.isEmpty()) owned = List.of(owned.get(p.getRandom().nextInt(owned.size())));
            for (Ability a : owned) { AbilityAPI.setMastery(p, a, points < 0 ? a.type().maxMastery() : points); n++; }
        }
        ok(c, "Set mastery on " + n + " skill(s).");
        return n;
    }

    private static int cooldown(CommandContext<CommandSourceStack> c, List<Ability> skills) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(c, "seconds") * 20, n = 0;
        for (ServerPlayer p : players(c)) {
            for (Ability a : skills == null ? AbilityAPI.owned(p) : skills) {
                if (a == null || !AbilityAPI.has(p, a)) continue;
                if (ticks == 0) AbilityAPI.resetCooldown(p, a); else AbilityAPI.setCooldown(p, a, ticks);
                n++;
            }
        }
        ok(c, "Set the cooldown on " + n + " skill(s).");
        return n;
    }

    // ---------------------------------------------------------------- /jaycee edit stat

    private enum Stat { AURA, MAGICULE, EP, SPIRITUAL_HP }

    private static String statName(Stat s) {
        return switch (s) { case AURA -> "aura"; case MAGICULE -> "magicule"; case EP -> "ep"; case SPIRITUAL_HP -> "spiritualHP"; };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editStat() {
        ArgumentBuilder<CommandSourceStack, ?> ent = entities();
        for (Stat s : Stat.values()) {
            ent.then(Commands.literal(statName(s))
                    .then(Commands.literal("current")
                            .then(Commands.literal("add").then(Commands.argument("amount", DoubleArgumentType.doubleArg()).executes(c -> stat(c, s, false, true, DoubleArgumentType.getDouble(c, "amount"), false))))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(c -> stat(c, s, false, false, DoubleArgumentType.getDouble(c, "amount"), false)))
                                    .then(Commands.literal("max").executes(c -> stat(c, s, false, false, Double.NaN, false)))))
                    .then(Commands.literal("max")
                            .then(Commands.literal("add").then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                    .executes(c -> stat(c, s, true, true, DoubleArgumentType.getDouble(c, "amount"), false))
                                    .then(Commands.argument("resetCurrent", BoolArgumentType.bool())
                                            .executes(c -> stat(c, s, true, true, DoubleArgumentType.getDouble(c, "amount"), BoolArgumentType.getBool(c, "resetCurrent"))))))
                            .then(Commands.literal("set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1))
                                    .executes(c -> stat(c, s, true, false, DoubleArgumentType.getDouble(c, "amount"), false))
                                    .then(Commands.argument("resetCurrent", BoolArgumentType.bool())
                                            .executes(c -> stat(c, s, true, false, DoubleArgumentType.getDouble(c, "amount"), BoolArgumentType.getBool(c, "resetCurrent"))))))));
        }
        return Commands.literal("stat").then(ent);
    }

    /** NaN amount on "current set max" means fill it to the max */
    private static int stat(CommandContext<CommandSourceStack> c, Stat s, boolean max, boolean add, double amount, boolean resetCurrent) throws CommandSyntaxException {
        int n = 0;
        for (LivingEntity e : living(c)) {
            EnergyData d = EnergyAPI.get(e);
            if (max) {
                double a = d.maxAura(), m = d.maxMana(), sp = d.maxSpirit();
                switch (s) {
                    case AURA -> a = add ? a + amount : amount;
                    case MAGICULE -> m = add ? m + amount : amount;
                    case SPIRITUAL_HP -> sp = add ? sp + amount : amount;
                    case EP -> {
                        // ep is aura + magicule, so split the change between them
                        double target = add ? a + m + amount : amount, total = Math.max(1, a + m);
                        a = target * (a / total);
                        m = target - a;
                    }
                }
                d = d.withMax(Math.max(1, a), Math.max(1, m), Math.max(1, sp));
                if (resetCurrent) d = d.refilled();
            } else {
                switch (s) {
                    case AURA -> d = d.withAura(Double.isNaN(amount) ? d.maxAura() : add ? d.aura() + amount : amount);
                    case MAGICULE -> d = d.withMana(Double.isNaN(amount) ? d.maxMana() : add ? d.mana() + amount : amount);
                    case SPIRITUAL_HP -> d = d.withSpirit(Double.isNaN(amount) ? d.maxSpirit() : add ? d.spirit() + amount : amount);
                    case EP -> {
                        if (Double.isNaN(amount)) { d = d.withAura(d.maxAura()).withMana(d.maxMana()); break; }
                        double cur = d.aura() + d.mana(), target = add ? cur + amount : amount, total = Math.max(1, d.maxAura() + d.maxMana());
                        d = d.withAura(target * d.maxAura() / total).withMana(target * d.maxMana() / total);
                    }
                }
            }
            EnergyAPI.set(e, d);
            n++;
        }
        ok(c, "Changed " + statName(s) + " for " + n + ".");
        return n;
    }

    // ---------------------------------------------------------------- /jaycee edit race, awakening, forceevo, owner

    private static LiteralArgumentBuilder<CommandSourceStack> editRace() {
        return Commands.literal("race").then(entities().then(Commands.literal("set").then(Commands.argument("race", IdentifierArgument.id()).suggests(RACES)
                .executes(c -> setRace(c, true))
                .then(Commands.argument("resetExistenceStats", BoolArgumentType.bool()).executes(c -> setRace(c, BoolArgumentType.getBool(c, "resetExistenceStats")))))));
    }

    private static int setRace(CommandContext<CommandSourceStack> c, boolean resetStats) throws CommandSyntaxException {
        Race race = RaceRegistry.get(IdentifierArgument.getId(c, "race"));
        if (race == null) return fail(c, "No race like that.");
        int n = 0;
        for (ServerPlayer p : players(c)) {
            EnergyData before = EnergyAPI.get(p);
            RaceAPI.set(p, race);
            if (!resetStats) EnergyAPI.set(p, before);
            n++;
        }
        ok(c, "Set the race to " + race.name().getString() + " for " + n + ".");
        return n;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editAwakening() {
        return Commands.literal("awakening").then(entities()
                .then(Commands.literal("demonLord")
                        .then(Commands.literal("soul")
                                .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer()).executes(c -> souls(c, true))))
                                .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(c -> souls(c, false)))))
                        .then(Commands.literal("seed").then(Commands.argument("set", BoolArgumentType.bool()).executes(c -> flag(c, "demon_lord_seed"))))
                        .then(Commands.literal("awakened").then(Commands.argument("set", BoolArgumentType.bool()).executes(c -> flag(c, "true_demon_lord")))))
                .then(Commands.literal("humanKill")
                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer()).executes(c -> humanKills(c, true))))
                        .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(c -> humanKills(c, false))))));
    }

    /** souls in the command are whole souls, stored as soul points (1 soul = 1,000 points) */
    private static int souls(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(c, "amount"), n = 0;
        for (ServerPlayer p : players(c)) {
            DemonLord.setSoulPoints(p, (add ? DemonLord.soulPoints(p) : 0) + amount * 1000.0);
            n++;
        }
        ok(c, "Changed souls for " + n + ".");
        return n;
    }

    private static int flag(CommandContext<CommandSourceStack> c, String key) throws CommandSyntaxException {
        boolean on = BoolArgumentType.getBool(c, "set");
        int n = 0;
        for (ServerPlayer p : players(c)) { DemonLord.setFlag(p, key, on ? 1 : 0); n++; }
        ok(c, "Done for " + n + ".");
        return n;
    }

    private static int humanKills(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(c, "amount"), n = 0;
        for (ServerPlayer p : players(c)) {
            double now = StatusData.counter(p, "human_kills");
            StatusData.addCounter(p, "human_kills", (add ? amount : amount - now));
            n++;
        }
        ok(c, "Changed human kills for " + n + ".");
        return n;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editForceEvo() {
        return Commands.literal("forceevo").then(entities()
                .then(Commands.literal("race").executes(c -> {
                    int n = 0;
                    for (ServerPlayer p : players(c)) {
                        var next = RaceAPI.next(p);
                        if (next.isEmpty()) continue;
                        Race to = RaceRegistry.get(next.get(0).target());
                        if (to != null) { RaceAPI.set(p, to); n++; }
                    }
                    ok(c, "Force evolved " + n + ".");
                    return n;
                }))
                .then(Commands.literal("demonLord").executes(c -> {
                    int n = 0;
                    for (ServerPlayer p : players(c)) { DemonLord.forceAwaken(p); n++; }
                    ok(c, "Awakened " + n + " as a True Demon Lord.");
                    return n;
                }))
                .then(Commands.literal("hero").executes(c -> fail(c, "The hero path isn't in yet."))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editOwner() {
        return Commands.literal("owner").then(entities()
                .then(Commands.literal("name")
                        .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                            String name = StringArgumentType.getString(c, "name");
                            int n = 0;
                            for (LivingEntity e : living(c)) { e.setData(Bonds.NAME, name); e.setCustomName(Component.literal(name)); n++; }
                            ok(c, "Named " + n + ".");
                            return n;
                        })))
                        .then(Commands.literal("remove").executes(c -> {
                            int n = 0;
                            for (LivingEntity e : living(c)) { e.setData(Bonds.NAME, ""); e.setCustomName(null); n++; }
                            ok(c, "Removed the name from " + n + ".");
                            return n;
                        })))
                .then(Commands.literal("permanent")
                        .then(Commands.literal("set").then(Commands.argument("owner", EntityArgument.player()).executes(c -> {
                            ServerPlayer owner = EntityArgument.getPlayer(c, "owner");
                            int n = 0;
                            for (LivingEntity e : living(c)) {
                                if (e == owner) continue;
                                e.setData(Bonds.OWNER, owner.getStringUUID());
                                if (e instanceof Mob m) m.setPersistenceRequired();
                                Bonds.add(owner, e, e.getData(Bonds.NAME).isEmpty() ? e.getName().getString() : e.getData(Bonds.NAME));
                                n++;
                            }
                            ok(c, "Set the owner for " + n + ".");
                            return n;
                        })))
                        .then(Commands.literal("remove").executes(c -> {
                            int n = 0;
                            for (LivingEntity e : living(c)) {
                                String own = Bonds.owner(e);
                                if (own.isEmpty()) continue;
                                ServerPlayer owner = c.getSource().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(own));
                                if (owner != null) Bonds.command(owner, e.getStringUUID(), "release"); else e.setData(Bonds.OWNER, "");
                                n++;
                            }
                            ok(c, "Removed the owner from " + n + ".");
                            return n;
                        }))));
    }

    // ---------------------------------------------------------------- /jaycee get

    private interface Reader { String read(LivingEntity e); }

    /** tensura's get commands work as "get x <entity> ..." and "get x self ..." */
    private static void getLeaf(ArgumentBuilder<CommandSourceStack, ?> entNode, ArgumentBuilder<CommandSourceStack, ?> selfNode,
                                List<String> path, Reader reader) {
        ArgumentBuilder<CommandSourceStack, ?> a = null, b = null;
        for (int i = path.size() - 1; i >= 0; i--) {
            LiteralArgumentBuilder<CommandSourceStack> la = Commands.literal(path.get(i)), lb = Commands.literal(path.get(i));
            if (a == null) {
                la.executes(c -> { for (LivingEntity e : living(c)) ok(c, e.getName().getString() + ": " + reader.read(e)); return 1; });
                lb.executes(c -> { ok(c, reader.read(c.getSource().getPlayerOrException())); return 1; });
            } else { la.then(a); lb.then(b); }
            a = la; b = lb;
        }
        entNode.then(a);
        selfNode.then(b);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getStat() {
        ArgumentBuilder<CommandSourceStack, ?> ent = entities(), self = Commands.literal("self");
        getLeaf(ent, self, List.of("aura", "current"), e -> EnergyAPI.format(EnergyAPI.get(e).aura()));
        getLeaf(ent, self, List.of("aura", "max"), e -> EnergyAPI.format(EnergyAPI.get(e).maxAura()));
        getLeaf(ent, self, List.of("magicule", "current"), e -> EnergyAPI.format(EnergyAPI.get(e).mana()));
        getLeaf(ent, self, List.of("magicule", "max"), e -> EnergyAPI.format(EnergyAPI.get(e).maxMana()));
        getLeaf(ent, self, List.of("ep", "current"), e -> EnergyAPI.format(EnergyAPI.get(e).aura() + EnergyAPI.get(e).mana()));
        getLeaf(ent, self, List.of("ep", "max"), e -> EnergyAPI.format(EnergyAPI.get(e).ep()));
        getLeaf(ent, self, List.of("spiritualHealth", "current"), e -> EnergyAPI.format(EnergyAPI.get(e).spirit()));
        getLeaf(ent, self, List.of("spiritualHealth", "max"), e -> EnergyAPI.format(EnergyAPI.get(e).maxSpirit()));
        return Commands.literal("stat").then(ent).then(self);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getRace() {
        ArgumentBuilder<CommandSourceStack, ?> ent = entities(), self = Commands.literal("self");
        getLeaf(ent, self, List.of("race"), e -> e instanceof ServerPlayer p
                ? RaceAPI.race(p).map(r -> r.name().getString()).orElse("none") : e.getType().getDescription().getString());
        getLeaf(ent, self, List.of("alignment"), e -> e instanceof ServerPlayer p
                ? RaceAPI.race(p).map(r -> r.alignment().label()).orElse("none") : "none");
        return Commands.literal("race").then(ent).then(self);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getAbility() {
        return Commands.literal("ability")
                .then(Commands.literal("check")
                        .then(entities().then(skillArg().executes(c -> {
                            Ability a = skill(c);
                            for (ServerPlayer p : players(c)) ok(c, p.getName().getString() + ": " + describe(p, a));
                            return 1;
                        })))
                        .then(Commands.literal("self").then(skillArg().executes(c -> {
                            ok(c, describe(c.getSource().getPlayerOrException(), skill(c)));
                            return 1;
                        }))))
                .then(Commands.literal("all")
                        .then(entities().executes(c -> { for (ServerPlayer p : players(c)) ok(c, p.getName().getString() + ": " + list(p)); return 1; }))
                        .then(Commands.literal("self").executes(c -> { ok(c, list(c.getSource().getPlayerOrException())); return 1; })));
    }

    private static String describe(ServerPlayer p, Ability a) {
        if (a == null) return "no skill like that";
        if (!AbilityAPI.has(p, a)) return "doesn't have " + a.name().getString();
        return a.name().getString() + ", mastery " + AbilityAPI.mastery(p, a) + "/" + a.type().maxMastery()
                + (a.isToggle() ? ", " + (AbilityAPI.isToggled(p, a) ? "on" : "off") : "")
                + (a.isSwitchable() ? ", passives " + (AbilityAPI.isSwitchedOn(p, a) ? "on" : "off") : "");
    }

    private static String list(ServerPlayer p) {
        Collection<Ability> owned = AbilityAPI.owned(p);
        return owned.isEmpty() ? "no skills" : String.join(", ", owned.stream().map(a -> a.name().getString()).toList());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getAwakening() {
        ArgumentBuilder<CommandSourceStack, ?> ent = entities(), self = Commands.literal("self");
        getLeaf(ent, self, List.of("demonLord", "soul"), e -> e instanceof ServerPlayer p ? String.valueOf((long) (DemonLord.soulPoints(p) / 1000)) : "0");
        getLeaf(ent, self, List.of("demonLord", "seed"), e -> e instanceof ServerPlayer p && DemonLord.isSeed(p) ? "true" : "false");
        getLeaf(ent, self, List.of("demonLord", "awakened"), e -> e instanceof ServerPlayer p && DemonLord.isTrue(p) ? "true" : "false");
        getLeaf(ent, self, List.of("humanKill"), e -> e instanceof ServerPlayer p ? String.valueOf((long) StatusData.counter(p, "human_kills")) : "0");
        return Commands.literal("awakening").then(ent).then(self);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getOwner() {
        ArgumentBuilder<CommandSourceStack, ?> ent = entities(), self = Commands.literal("self");
        getLeaf(ent, self, List.of("name"), e -> e.getData(Bonds.NAME).isEmpty() ? "no name" : e.getData(Bonds.NAME));
        getLeaf(ent, self, List.of("permanent"), e -> {
            String own = Bonds.owner(e);
            if (own.isEmpty()) return "no owner";
            var server = e.level().getServer();
            ServerPlayer o = server == null ? null : server.getPlayerList().getPlayer(java.util.UUID.fromString(own));
            return o != null ? o.getName().getString() : own;
        });
        return Commands.literal("owner").then(ent).then(self);
    }

    // ---------------------------------------------------------------- /jaycee reset

    private static LiteralArgumentBuilder<CommandSourceStack> reset() {
        return Commands.literal("reset").then(entities()
                .then(Commands.literal("race").executes(c -> resetRace(c))
                        .then(Commands.argument("race", IdentifierArgument.id()).suggests(RACES).executes(c -> setRace(c, true))))
                .then(Commands.literal("skill").executes(c -> resetSkills(c)))
                .then(Commands.literal("statistic").executes(c -> {
                    int n = 0;
                    for (ServerPlayer p : players(c)) {
                        RaceAPI.race(p).ifPresent(r -> EnergyAPI.rollFor(p, r));
                        n++;
                    }
                    ok(c, "Reset the stats for " + n + ".");
                    return n;
                }))
                .then(Commands.literal("awakening").executes(c -> resetAwakening(c)))
                .then(Commands.literal("everything").executes(c -> {
                    resetSkills(c);
                    resetAwakening(c);
                    return resetRace(c);
                })));
    }

    private static int resetRace(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            // no race means the race menu shows up again
            p.setData(BrightAttachments.RACE, RaceData.EMPTY);
            p.setData(BrightAttachments.ENERGY, EnergyData.EMPTY);
            n++;
        }
        ok(c, "Reset the race for " + n + ".");
        return n;
    }

    private static int resetSkills(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            for (Ability a : List.copyOf(AbilityAPI.owned(p))) AbilityAPI.forget(p, a);
            n++;
        }
        ok(c, "Reset the skills for " + n + ".");
        return n;
    }

    private static int resetAwakening(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer p : players(c)) {
            DemonLord.setFlag(p, "demon_lord_seed", 0);
            DemonLord.setFlag(p, "true_demon_lord", 0);
            DemonLord.setSoulPoints(p, 0);
            n++;
        }
        ok(c, "Reset the awakening for " + n + ".");
        return n;
    }
}

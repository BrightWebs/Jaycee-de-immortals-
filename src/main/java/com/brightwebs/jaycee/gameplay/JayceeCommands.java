package com.brightwebs.jaycee.gameplay;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.gameplay.divine.DivineAppraisal;
import com.brightwebs.jaycee.gameplay.divine.DivinityModel;
import com.brightwebs.jaycee.gameplay.divine.DivinityNetwork;
import com.brightwebs.jaycee.gameplay.divine.DivinitySavedData;
import com.brightwebs.jaycee.gameplay.divine.DivinityService;
import com.brightwebs.jaycee.gameplay.divine.EssenceProgression;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** One public command tree; operator status never implies divine authority. */
public final class JayceeCommands {
    static final Predicate<CommandSourceStack> OPERATOR = source -> source.hasPermission(2);
    static final Predicate<CommandSourceStack> SUPREME = source -> {
        var player = source.getPlayer();
        return player != null && DivinityService.model(player).supreme(player.getUUID());
    };
    private JayceeCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("jc").executes(JayceeCommands::help);
        root.then(Commands.literal("help").executes(JayceeCommands::help));
        var get = Commands.literal("get").executes(c -> own(c, "status"));
        for (String field : List.of("status", "essence", "ep", "mana", "level", "grade", "patron",
                "abilities", "mastery", "traits", "subordinates", "grants")) {
            get.then(Commands.literal(field).executes(c -> own(c, field)));
        }
        get.then(Commands.literal("trait").executes(c -> own(c, "traits")));
        get.then(Commands.literal("ability").executes(c -> own(c, "abilities"))
            .then(Commands.literal("list").executes(c -> own(c, "abilities")))
            .then(Commands.literal("mastery").then(abilityArgument("id").executes(JayceeCommands::masteryInfo))));
        root.then(get);
        root.then(abilities());
        root.then(EssenceGrowth.commands());
        root.then(Commands.literal("patron").executes(c -> own(c, "patron"))
            .then(Commands.literal("list").executes(c -> reply(c, "Known NPC patrons: "
                + String.join(", ", DivinityModel.NPC_PATRONS.stream().sorted().toList())))));
        var essence = Commands.literal("essence").executes(c -> own(c, "essence"))
            .then(Commands.literal("offers").executes(JayceeCommands::offers));
        for (String operation : List.of("accept", "decline", "cancel")) {
            essence.then(Commands.literal(operation).then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> {
                    var p = c.getSource().getPlayer();
                    return SharedSuggestionProvider.suggest(p == null ? List.of()
                        : DivinityService.model(p).pending(p.getUUID()).stream().map(o -> o.id().toString()).toList(), b);
                }).executes(c -> offer(c, operation))));
        }
        root.then(essence);
        root.then(Commands.literal("grade").executes(c -> own(c, "grade"))
            .then(Commands.literal("inspect").requires(SUPREME)
                .then(Commands.argument("player", EntityArgument.player()).executes(JayceeCommands::inspectGrade))));
        root.then(Commands.literal("subordinate").executes(c -> own(c, "subordinates"))
            .then(Commands.literal("list").executes(c -> own(c, "subordinates"))));
        root.then(edit());
        var config = Commands.literal("config").requires(OPERATOR).executes(JayceeCommands::config)
            .then(Commands.literal("show").executes(JayceeCommands::config))
            .then(Commands.literal("reload").executes(c -> fail(c,
                "No command-driven configuration reload is implemented. Edit the serverconfig TOML files "
                    + "and restart the server to apply them.")));
        var debug = Commands.literal("debug").requires(OPERATOR);
        Gameplay.debugCommands(debug);
        DivinityService.registerDebug(debug);
        var authority = debug.getArguments().stream().filter(n -> n.getName().equals("jayceeauthority")).findFirst().orElseThrow();
        config.then(Commands.literal("authority").requires(OPERATOR).redirect(authority));
        root.then(config);
        debug.then(Commands.literal("essence").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            var stack = new ItemStack(JayceeMod.ESSENCE.get(), 3);
            p.getInventory().add(stack);
            if (!stack.isEmpty()) p.drop(stack, false);
            return reply(c, "Received 3 legacy unlock items; no Essence balance or earned progression added.");
        }));
        root.then(debug);
        var registered = dispatcher.register(root);
        dispatcher.register(Commands.literal("jaycee").redirect(registered).executes(registered.getCommand()));
        var debugNode = registered.getChild("debug");
        for (String legacy : List.of("jayceetest", "jayceeplayertest", "jayceeskill", "jayceemana", "jayceesp",
                "jayceeauthority", "divineadmin")) compatibilityAlias(dispatcher, legacy, debugNode.getChild(legacy));
        compatibilityAlias(dispatcher, "jayceeessence", debugNode.getChild("essence"));
        dispatcher.register(Commands.literal("divineoffer").redirect(registered.getChild("essence")));
    }

    private static void compatibilityAlias(CommandDispatcher<CommandSourceStack> dispatcher, String name,
            CommandNode<CommandSourceStack> target) {
        var alias = Commands.literal(name).requires(OPERATOR).redirect(target);
        if (target.getCommand() != null) alias.executes(target.getCommand());
        dispatcher.register(alias);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> abilities() {
        var node = Commands.literal("ability").executes(c -> own(c, "abilities"))
            .then(Commands.literal("list").executes(c -> own(c, "abilities")))
            .then(Commands.literal("info").then(abilityArgument("id").executes(JayceeCommands::abilityInfo)))
            .then(Commands.literal("mastery").then(abilityArgument("id").executes(JayceeCommands::masteryInfo)));
        var modes = Commands.literal("mode");
        var equip = Commands.literal("equip");
        var activate = Commands.literal("activate");
        for (int slot = 0; slot < PlayerState.SLOTS; slot++) {
            final int index = slot;
            for (String name : List.of("slot" + (slot + 1), Integer.toString(slot + 1))) {
                modes.then(Commands.literal(name).then(Commands.argument("mode", StringArgumentType.greedyString())
                    .suggests((c, b) -> {
                        var p = c.getSource().getPlayer();
                        var s = p == null ? null : PlayerState.get(p);
                        var a = s == null ? null : AbilityCatalog.find(s.ability(s.page(), index));
                        return SharedSuggestionProvider.suggest(a == null ? List.of() : a.modes(), b);
                    }).executes(c -> mode(c, index))));
                equip.then(Commands.literal(name).then(abilityArgument("id")
                    .executes(c -> equip(c, index, StringArgumentType.getString(c, "id"))))
                    .then(Commands.literal("clear").executes(c -> equip(c, index, ""))));
                activate.then(Commands.literal(name).executes(c -> activate(c, index)));
            }
        }
        node.then(modes).then(equip).then(activate);
        node.then(Commands.literal("toggle").then(abilityArgument("id")
            .executes(c -> toggle(c, null))
            .then(Commands.literal("on").executes(c -> toggle(c, true)))
            .then(Commands.literal("off").executes(c -> toggle(c, false)))));
        node.then(Commands.literal("cooldown").then(abilityArgument("id").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            if (!PlayerState.get(p).abilityOwned(id)) return fail(c, "Ability is unknown or not owned.");
            return reply(c, id + ": " + PlayerState.get(p).cooldownRemaining(id, p.serverLevel().getGameTime())
                + " ticks remaining.");
        })));
        return node;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> abilityArgument(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((c, b) ->
            SharedSuggestionProvider.suggest(abilityIds(), b));
    }

    static List<String> abilityIds() {
        return AbilityCatalog.registered().stream().filter(AbilityCatalog.Art::implemented).map(AbilityCatalog.Art::id).toList();
    }

    static List<String> traitIds() {
        return SkillDefinitions.ALL.stream().filter(SkillDefinition::intrinsic).map(SkillDefinition::id).toList();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> edit() {
        var target = Commands.argument("player", EntityArgument.player());
        var essence = Commands.literal("essence");
        for (String operation : List.of("add", "set")) essence.then(Commands.literal(operation)
            .then(Commands.argument("amount", LongArgumentType.longArg(0))
                .executes(c -> editLedger(c, "essence_" + operation, "", LongArgumentType.getLong(c, "amount")))));
        target.then(essence);
        target.then(Commands.literal("ep").then(Commands.literal("set")
            .then(Commands.argument("amount", LongArgumentType.longArg(0))
                .executes(c -> editLedger(c, "ep_set", "", LongArgumentType.getLong(c, "amount"))))));
        target.then(Commands.literal("level").then(Commands.literal("set")
            .then(Commands.argument("amount", LongArgumentType.longArg(1, Integer.MAX_VALUE))
                .executes(c -> editLedger(c, "level_set", "", LongArgumentType.getLong(c, "amount"))))));
        var grade = Commands.literal("grade");
        for (String name : List.of("recalc", "recalculate")) grade.then(Commands.literal(name)
            .executes(c -> editLedger(c, "grade_recalc", "", 0)));
        target.then(grade);
        var patron = Commands.literal("patron");
        for (String op : List.of("grant", "revoke")) patron.then(Commands.literal(op)
            .then(Commands.argument("patron", StringArgumentType.word()).suggests((c, b) ->
                SharedSuggestionProvider.suggest(DivinityModel.NPC_PATRONS.stream().sorted(), b))
                .executes(c -> editLedger(c, "patron_" + op, StringArgumentType.getString(c, "patron"), 0))));
        target.then(patron);
        for (boolean trait : List.of(false, true)) {
            var category = Commands.literal(trait ? "trait" : "ability");
            for (String op : List.of("grant", "revoke")) category.then(Commands.literal(op)
                .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) ->
                    SharedSuggestionProvider.suggest(trait ? traitIds() : abilityIds(), b))
                    .executes(c -> {
                        String id = StringArgumentType.getString(c, "id");
                        if (!(trait ? traitIds() : abilityIds()).contains(id))
                            return fail(c, trait ? "Unknown intrinsic trait." : "Unknown implemented ability.");
                        return editLedger(c, "ability_" + op, id, 0);
                    })));
            if (!trait) category.then(Commands.literal("mastery").then(abilityArgument("id")
                .then(Commands.literal("max").executes(c -> mastery(c, -1)))
                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                    .executes(c -> mastery(c, LongArgumentType.getLong(c, "amount"))))));
            target.then(category);
        }
        return Commands.literal("edit").requires(OPERATOR).then(target);
    }

    private static int editLedger(CommandContext<CommandSourceStack> c, String operation, String value, long amount)
            throws CommandSyntaxException {
        if (!OPERATOR.test(c.getSource())) return fail(c, "Operator permission level 2 is required.");
        var p = EntityArgument.getPlayer(c, "player");
        try {
            DivinityService.commandEdit(p, operation, value, amount);
            if (operation.equals("grade_recalc"))
                return reply(c, p.getGameProfile().getName() + ": "
                    + progressText(DivinityService.model(p).progression(p.getUUID()))
                    + " Grade is computed from saved earned Essence; no divine rank or authority was changed.");
            return reply(c, "Updated " + p.getGameProfile().getName() + ": " + operation
                + (value.isEmpty() ? "" : " " + value) + ". This is an operator override, not natural progression.");
        } catch (IllegalArgumentException | ArithmeticException e) {
            return fail(c, e.getMessage());
        }
    }

    private static int mastery(CommandContext<CommandSourceStack> c, long amount) throws CommandSyntaxException {
        if (!OPERATOR.test(c.getSource())) return fail(c, "Operator permission level 2 is required.");
        var p = EntityArgument.getPlayer(c, "player");
        String id = StringArgumentType.getString(c, "id");
        var state = PlayerState.get(p);
        if (!abilityIds().contains(id) || !state.abilityOwned(id))
            return fail(c, "Mastery requires a known, already-owned implemented ability.");
        long maximum = (long) SkillConfig.MAX_LEVEL.get() * SkillConfig.MAX_LEVEL.get() * SkillConfig.XP_PER_LEVEL.get();
        if (amount > maximum) return fail(c, "Maximum mastery XP is " + maximum + "; use max or an amount within that bound.");
        long xp = amount < 0 ? maximum : amount;
        var data = PlayerState.read(p);
        SkillConfig.snapshot(data);
        SkillRuntime.edit(data, id, entry -> entry.putLong("Xp", xp));
        PlayerState.save(p, data);
        SkillRuntime.invalidate(p);
        GameplayNetwork.sync(p);
        return reply(c, "Set " + p.getGameProfile().getName() + "'s " + id + " mastery XP to " + xp + ".");
    }

    private static int own(CommandContext<CommandSourceStack> c, String field) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var s = PlayerState.get(p);
        var m = DivinityService.model(p);
        var profile = m.profile(p.getUUID());
        return switch (field) {
            case "essence" -> reply(c, "Essence: " + profile.essence() + "; lifetime earned: " + m.earned(p.getUUID())
                + ". EP and mana are separate resources.");
            case "ep" -> reply(c, "Permanent EP: " + profile.ep() + ".");
            case "mana" -> {
                var resolved = ManaBackends.resolve(p);
                yield reply(c, "Mana: " + resolved.mana().getCurrent() + "/" + resolved.mana().getMax()
                    + "; regeneration " + resolved.mana().getRegen() + "/second; backend " + resolved.id() + ".");
            }
            case "level" -> reply(c, progressText(m.progression(p.getUUID())));
            case "grade" -> reply(c, "Grade " + m.progression(p.getUUID()).grade()
                + "; divine rank " + profile.rank() + ". Grade is derived from earned Essence, not spendable balance or EP.");
            case "patron" -> {
                String playerPatron = profile.patron() == null ? "none" : m.profile(profile.patron()).displayName();
                yield reply(c, "Player patron: " + playerPatron + "; retained NPC patrons: "
                    + String.join(", ", profile.npcPatrons().stream().sorted().toList()) + ".");
            }
            case "abilities" -> reply(c, "Owned implemented abilities: " + String.join(", ",
                abilityIds().stream().filter(s::abilityOwned).toList()) + ". Use /jc ability info <id>.");
            case "grants" -> reply(c, "Explicit owned grants: " + String.join(", ",
                s.divinity().getCompound("Grants").getAllKeys().stream().filter(abilityIds()::contains).sorted().toList()));
            case "mastery" -> reply(c, "Mastery: " + String.join(", ", abilityIds().stream().filter(s::abilityOwned)
                .map(id -> id + " " + s.skillLevel(id) + "/" + s.skillMaxLevel() + " (" + s.skillXp(id) + " XP)").toList()));
            case "traits" -> reply(c, "Owned intrinsic traits: "
                + String.join(", ", StatusData.traits(s).stream().map(StatusData.Entry::name).toList()));
            case "subordinates" -> {
                var names = new java.util.ArrayList<String>();
                m.followers(p.getUUID()).forEach(f -> names.add(f.displayName() + " [player]"));
                m.mobs().stream().filter(mob -> p.getUUID().equals(mob.master()))
                    .forEach(mob -> names.add((mob.divineName().isEmpty() ? mob.original() : mob.divineName())
                        + " [" + mob.status() + "]"));
                yield reply(c, "Your subordinates: " + (names.isEmpty() ? "none" : String.join(", ", names)));
            }
            default -> reply(c, p.getGameProfile().getName() + ": Essence " + profile.essence()
                + ", earned " + m.earned(p.getUUID()) + ", EP " + profile.ep() + ", divine rank " + profile.rank()
                + ". " + progressText(m.progression(p.getUUID())) + " Use /jc get <field> for details.");
        };
    }

    private static String progressText(EssenceProgression.Progress progress) {
        return "Level " + progress.level() + ", grade " + progress.grade() + "; earned progress "
            + progress.intoLevel() + "/" + (progress.nextCostCapped() ? "at least " : "")
            + progress.nextCost() + " Essence toward the next level.";
    }

    private static int inspectGrade(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        if (!SUPREME.test(c.getSource())) return fail(c, "Only the assigned Jaycee authority may inspect another player's progression.");
        var viewer = c.getSource().getPlayerOrException();
        var target = EntityArgument.getPlayer(c, "player");
        double range = DivinityService.model(viewer).config().distance();
        if (!viewer.isAlive() || viewer.isSpectator() || !target.isAlive() || target.isSpectator()
                || viewer.serverLevel() != target.serverLevel() || viewer.distanceToSqr(target) > range * range
                || (viewer != target && !viewer.hasLineOfSight(target)))
            return fail(c, "Appraisal requires a living, visible player within " + range + " blocks in your dimension.");
        return reply(c, DivineAppraisal.describe(target));
    }

    private static int abilityInfo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var state = PlayerState.get(p);
        String id = StringArgumentType.getString(c, "id");
        var art = AbilityCatalog.find(id, state);
        if (art == null || !art.implemented()) return fail(c, "Unknown implemented ability.");
        return reply(c, art.name() + " (" + id + "): " + (state.abilityOwned(id) ? "owned" : "not owned")
            + "; modes " + String.join(", ", art.modes()) + "; activation mana " + art.manaCost()
            + "; cooldown remaining " + state.cooldownRemaining(id, p.serverLevel().getGameTime())
            + " ticks; mastery " + state.skillLevel(id) + "/" + state.skillMaxLevel() + " ("
            + state.skillXp(id) + " XP). " + state.skillEffectSummary(id));
    }

    private static int masteryInfo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var state = PlayerState.get(c.getSource().getPlayerOrException());
        String id = StringArgumentType.getString(c, "id");
        if (!abilityIds().contains(id) || !state.abilityOwned(id))
            return fail(c, "Mastery is available only for a known, owned implemented ability.");
        return reply(c, id + ": mastery " + state.skillLevel(id) + "/" + state.skillMaxLevel()
            + ", " + state.skillXp(id) + " XP.");
    }

    static int modeIndex(List<String> modes, String input) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).equalsIgnoreCase(input)) return i;
        try {
            int value = Integer.parseInt(input);
            return value >= 1 && value <= modes.size() ? value - 1 : -1;
        } catch (NumberFormatException ignored) { return -1; }
    }

    private static int mode(CommandContext<CommandSourceStack> c, int slot) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var s = PlayerState.get(p);
        var art = AbilityCatalog.find(s.ability(s.page(), slot));
        if (art == null || !s.abilityOwned(art.id())) return fail(c, "Equip an owned ability in this slot first.");
        int mode = modeIndex(art.modes(), StringArgumentType.getString(c, "mode"));
        if (!p.isAlive() || p.isSpectator() || mode < 0 || !SkillRuntime.modeAllowed(s, art.id(), mode))
            return fail(c, "Mode is unavailable. Use its name or a 1-based index; mastery restrictions still apply.");
        Gameplay.handle(p, new GameplayNetwork.Action("mode", s.page(), slot, Integer.toString(mode)));
        if (PlayerState.get(p).mode(s.page(), slot) != mode) return fail(c, "Mode change was rejected; try again next tick.");
        return reply(c, "Slot " + (slot + 1) + " mode: " + art.modes().get(mode) + ".");
    }

    private static int equip(CommandContext<CommandSourceStack> c, int slot, String id) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var s = PlayerState.get(p);
        if (!p.isAlive() || p.isSpectator() || (!id.isEmpty() && !s.abilityOwned(id)))
            return fail(c, "Only owned implemented abilities can be equipped while alive.");
        Gameplay.handle(p, new GameplayNetwork.Action("equip", s.page(), slot, id));
        if (!PlayerState.get(p).ability(s.page(), slot).equals(id)) return fail(c, "Equipment change was rejected.");
        return reply(c, "Slot " + (slot + 1) + ": " + (id.isEmpty() ? "empty" : id) + ".");
    }

    private static int activate(CommandContext<CommandSourceStack> c, int slot) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var s = PlayerState.get(p);
        String id = s.ability(s.page(), slot);
        if (!s.abilityOwned(id)) return fail(c, "Equip an owned implemented ability first.");
        var result = Gameplay.activate(p, s, slot, p.serverLevel().getGameTime());
        if (result != AbilityEngine.Result.ACTIVATED) return fail(c, "Cast rejected: " + result + ".");
        return reply(c, SkillDefinitions.intrinsic(id)
            ? id + ": " + (PlayerState.get(p).skillEnabled(id) ? "enabled" : "disabled") + "."
            : "Activated " + id + ".");
    }

    private static int toggle(CommandContext<CommandSourceStack> c, Boolean desired) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id");
        var s = PlayerState.get(p);
        if (!traitIds().contains(id) || !s.abilityOwned(id))
            return fail(c, "Only an owned intrinsic can be toggled.");
        boolean enabled = desired == null ? !s.skillEnabled(id) : desired;
        if (!SkillRuntime.action(p, new GameplayNetwork.Action("skill_toggle", enabled ? 1 : 0, 0, id))
                || PlayerState.get(p).skillEnabled(id) != enabled)
            return fail(c, "Toggle rejected; check survival eligibility, mana, cooldown, output and mastery.");
        return reply(c, id + ": " + (enabled ? "enabled" : "disabled") + ".");
    }

    private static int offers(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        var offers = DivinityService.model(p).pending(p.getUUID());
        if (offers.isEmpty()) return reply(c, "No pending Essence offers.");
        for (var offer : offers) reply(c, offer.id() + ": " + offer.proposal().amount()
            + " Essence; " + (offer.proposal().donor().equals(p.getUUID()) ? "outgoing" : "incoming")
            + "; expires " + offer.expires() + ". /jc essence accept|decline|cancel " + offer.id());
        return offers.size();
    }

    private static int offer(CommandContext<CommandSourceStack> c, String operation) throws CommandSyntaxException {
        var p = c.getSource().getPlayerOrException();
        try {
            UUID id = UUID.fromString(StringArgumentType.getString(c, "id"));
            var pending = DivinityService.model(p).pending(p.getUUID());
            if (pending.stream().noneMatch(o -> o.id().equals(id))) return fail(c, "No such pending offer for you.");
            DivinityService.handle(p, new DivinityNetwork.Request(operation, DivinityNetwork.NONE, id, 0, "", "", false));
            var result = DivinityService.snapshot(p).getString("Result");
            reply(c, result);
            return DivinityService.model(p).pending(p.getUUID()).stream().noneMatch(o -> o.id().equals(id)) ? 1 : 0;
        } catch (IllegalArgumentException e) { return fail(c, e.getMessage()); }
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        reply(c, "/jc (/jaycee): help; get [status|essence|ep|mana|level|grade|patron|abilities|mastery|traits|subordinates]; "
            + "ability list|info|equip|activate|mode|toggle|cooldown; patron [list]; essence [offers|accept|decline|cancel]; grade; subordinate [list].");
        if (SUPREME.test(c.getSource())) reply(c, "Assigned Jaycee only: /jc grade inspect <player> "
            + "(living nearby target in your dimension and line of sight).");
        if (OPERATOR.test(c.getSource())) reply(c, "OP2: /jc edit <player> essence add|set <amount>; ep set <amount>; "
            + "level set <level>; grade recalculate; patron grant|revoke <patron>; ability grant|revoke <id>; "
            + "ability mastery <id> <XP|max>; trait grant|revoke <id>. /jc config show|reload; "
            + "/jc config authority assign|reassign <player>|unassign; /jc debug.");
        return 1;
    }

    private static int config(CommandContext<CommandSourceStack> c) {
        reply(c, "Server settings: world's serverconfig/jaycee-server.toml (skills) and "
            + "jaycee-divinity-server.toml (divinity), jaycee-progression-server.toml (appraisal/rewards). "
            + "Client HUD: config/jaycee-client.toml. "
            + "Mastery: maximum level " + SkillConfig.MAX_LEVEL.get() + ", XP coefficient " + SkillConfig.XP_PER_LEVEL.get()
            + ", XP/minute " + SkillConfig.XP_PER_MINUTE.get() + ". "
            + "Config values are read-only here; only the explicit /jc config authority operations change authority.");
        var model = DivinitySavedData.get(c.getSource().getServer()).model;
        var balance = model.config();
        return reply(c, "Active world tuning: EP cap " + balance.epCap() + ", Essence cap " + balance.essenceCap()
            + ", supreme reserve cap " + balance.supremeCap() + ", supreme reserve refill " + balance.regenPerSecond()
            + "/second, interaction range " + balance.distance() + " blocks; progression coefficient "
            + balance.progression().coefficient() + ", grade minimum levels " + balance.progression().gradeMinimumLevels()
            + ". Assigned authority: " + (model.authority() == null ? "none" : model.authority()) + ".");
    }

    private static int reply(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text == null ? "Operation rejected." : text));
        return 0;
    }
}

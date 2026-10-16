package com.brightwebs.jaycee.gameplay;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Main-method command-tree regression checks; gameplay execution remains server-bound. */
public final class JayceeCommandsTest {
    public static void main(String[] args) throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        JayceeCommands.register(dispatcher);
        var root = dispatcher.getRoot().getChild("jc");
        check(root != null, "short root registered");
        check(dispatcher.getRoot().getChild("jaycee").getRedirect() == root, "long root aliases the same tree");
        check(dispatcher.getRoot().getChild("jaycee").getCommand() == root.getCommand(),
            "bare long root shares the short root command");
        check(dispatcher.execute("jc", source(0)) == 1 && dispatcher.execute("jaycee", source(0)) == 1,
            "bare short and long roots both execute help");
        for (String name : List.of("help", "get", "ability", "patron", "essence", "grade", "subordinate", "edit", "config", "debug"))
            check(root.getChild(name) != null, "missing root branch " + name);
        for (String legacy : List.of("jayceetest", "jayceeplayertest", "jayceeskill", "jayceemana", "jayceesp",
                "jayceeessence", "jayceeauthority", "divineadmin")) {
            var alias = dispatcher.getRoot().getChild(legacy);
            String debugName = legacy.equals("jayceeessence") ? "essence" : legacy;
            check(alias.getRedirect() == root.getChild("debug").getChild(debugName), "legacy alias shares implementation " + legacy);
            check(!alias.canUse(source(0)) && !alias.canUse(source(1)) && alias.canUse(source(2)),
                "legacy diagnostic requires OP2: " + legacy);
        }
        check(dispatcher.getRoot().getChild("divineoffer").getRedirect() == root.getChild("essence"),
            "clickable legacy offers share consent commands");
        check(dispatcher.getRoot().getChild("divineoffer").canUse(source(0)),
            "ordinary recipients may use clickable legacy offers");
        var authority = root.getChild("config").getChild("authority");
        check(authority.getRedirect() == root.getChild("debug").getChild("jayceeauthority"),
            "authority config and diagnostics share one implementation");
        for (String operation : List.of("assign", "reassign", "unassign"))
            check(authority.getRedirect().getChild(operation) != null, "explicit authority operation " + operation);
        check(!authority.canUse(source(0)) && authority.canUse(source(2)), "authority command requires OP2");
        var inspect = root.getChild("grade").getChild("inspect");
        check(inspect != null && inspect.getChild("player") != null, "authority appraisal command registered");
        check(inspect.getRequirement() == JayceeCommands.SUPREME, "appraisal requires assigned authority rather than operator");
        check(!inspect.canUse(source(0)) && !inspect.canUse(source(2)) && !inspect.canUse(source(4)),
            "operator permission alone never permits private progression inspection");
        var gradeEdit = root.getChild("edit").getChild("player").getChild("grade");
        check(gradeEdit.getChild("recalculate") != null && gradeEdit.getChild("recalc") != null,
            "exact grade recalculate syntax and shorthand alias both supported");
        for (String restricted : List.of("edit", "config", "debug")) {
            check(root.getChild(restricted).getRequirement() == JayceeCommands.OPERATOR, restricted + " needs OP2, not supreme");
            check(!root.getChild(restricted).canUse(source(0)), restricted + " denied to ordinary player");
            check(!root.getChild(restricted).canUse(source(1)), restricted + " denied to permission level one");
            check(root.getChild(restricted).canUse(source(2)), restricted + " available to operator");
        }
        for (String ordinary : List.of("help", "get", "ability", "patron", "essence", "grade", "subordinate"))
            check(root.getChild(ordinary).canUse(source(0)), ordinary + " available without OP");
        var ability = root.getChild("ability");
        for (String operation : List.of("mode", "equip", "activate"))
            for (String slot : List.of("slot1", "slot2", "slot3", "slot4", "1", "2", "3", "4"))
                check(ability.getChild(operation).getChild(slot) != null, operation + " supports " + slot);
        check(ability.getChild("mode").getChild("0") == null && ability.getChild("mode").getChild("5") == null,
            "numeric slots are bounded to one through four");
        var numericMode = dispatcher.parse("jc ability mode 1 Restore", source(0));
        check(numericMode.getContext().getCommand() != null && !numericMode.getReader().canRead()
                && numericMode.getExceptions().isEmpty(),
            "numeric slot mode syntax parses to an executable command");
        for (String operation : List.of("accept", "decline", "cancel"))
            check(root.getChild("essence").getChild(operation).getChild("id") != null, "offer consent operation " + operation);
        check(JayceeCommands.abilityIds().stream().allMatch(id -> AbilityCatalog.find(id).implemented()),
            "suggest only executable registry entries");
        check(JayceeCommands.traitIds().equals(SkillDefinitions.ALL.stream().filter(SkillDefinition::intrinsic)
            .map(SkillDefinition::id).toList()), "traits derive from actual intrinsic registry");
        check(!JayceeCommands.traitIds().contains(SkillDefinitions.RESTORE), "Soul Restore is not an intrinsic trait");
        check(!JayceeCommands.traitIds().contains(SkillDefinitions.PHYSICAL), "training is not an intrinsic trait");
        check(JayceeCommands.modeIndex(List.of("Restore", "Fortify"), "restore") == 0, "case insensitive mode name");
        check(JayceeCommands.modeIndex(List.of("Restore", "Fortify"), "2") == 1, "1-based numeric modes");
        check(JayceeCommands.modeIndex(List.of("Restore", "Fortify"), "0") == -1, "zero mode rejected");
        check(JayceeCommands.modeIndex(List.of("Restore", "Fortify"), "3") == -1, "out of range mode rejected");
        check(JayceeCommands.modeIndex(List.of("Restore", "Fortify"), "missing") == -1, "unknown mode rejected");
        check(JayceeCommands.modeIndex(List.of(), "1") == -1, "empty mode registry safe");
        System.out.println("JayceeCommandsTest passed");
    }

    private static CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permission,
            "test", Component.literal("test"), null, null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

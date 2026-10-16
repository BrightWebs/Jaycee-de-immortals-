package com.brightwebs.jaycee.gameplay;

import net.neoforged.neoforge.network.PacketDistributor;

/** Client-facing action senders; state changes arrive only through server synchronization. */
public final class GameplayActions {
    private GameplayActions() {}
    public static void page(int page) { send("page", page, 0, ""); }
    public static void pageDelta(int delta) { send("page_delta", delta, 0, ""); }
    public static void equip(int page, int slot, String id) { send("equip", page, slot, id); }
    public static void mode(int page, int slot, int mode) { send("mode", page, slot, Integer.toString(mode)); }
    public static void cycleMode(int page, int slot) { send("cycle_mode", page, slot, ""); }
    public static void activate(int slot) { send("activate", slot, 0, ""); }
    public static void portrait(boolean threeDimensional) { send("portrait", threeDimensional ? 1 : 0, 0, ""); }
    public static void backend(String id) { send("backend", 0, 0, id); }
    public static void skillToggle(String id, boolean enabled) {
        if (SkillDefinitions.intrinsic(id)) send("skill_toggle", enabled ? 1 : 0, 0, id);
    }
    public static void skillOutput(String id, int output) {
        if (SkillDefinitions.intrinsic(id) && output >= 0 && output <= 100) send("skill_output", output, 0, id);
    }
    public static void skillMode(String id, int mode) {
        var definition = SkillDefinitions.find(id);
        if (definition != null && definition.intrinsic() && mode >= 0 && mode < definition.modes().size())
            send("skill_mode", mode, 0, id);
    }
    public static void skillStyle(String id, int style) {
        var definition = SkillDefinitions.find(id);
        if (definition != null && definition.intrinsic() && style >= 0 && style < definition.styles().size())
            send("skill_style", style, 0, id);
    }
    private static void send(String action, int first, int second, String value) {
        if (value == null || value.length() > 128) return;
        PacketDistributor.sendToServer(new GameplayNetwork.Action(action, first, second, value));
    }
}

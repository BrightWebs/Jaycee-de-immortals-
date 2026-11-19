package com.brightwebs.jaycee.status;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.training.Attr;
import com.brightwebs.jaycee.training.Training;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Titles: earned by what you do, equipped one at a time from the Titles page, each with its
 * own passive effect. An equipped title shows above your name when "Display Title Publicly" is on.
 */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class Titles {
    /** One passive effect: a display line and, optionally, an attribute bonus. */
    public record Effect(String label, String value, Holder<Attribute> attribute, double amount, AttributeModifier.Operation op) {}
    public record Title(String id, String rank, int emblem, List<Effect> effects, Predicate<ServerPlayer> unlock) {}

    public static final Map<String, Title> ALL = new LinkedHashMap<>();

    private static void add(String id, String rank, int emblem, Predicate<ServerPlayer> unlock, Effect... effects) {
        ALL.put(id, new Title(id, rank, emblem, List.of(effects), unlock));
    }

    static {
        add("true_demon_lord", "greater", 1, p -> DemonLord.isTrue(p),
                new Effect("attack", "+12", Attributes.ATTACK_DAMAGE, 12, AttributeModifier.Operation.ADD_VALUE),
                new Effect("max_health", "+40", Attributes.MAX_HEALTH, 40, AttributeModifier.Operation.ADD_VALUE));
        add("demon_lord_seed", "greater", 1, p -> DemonLord.isSeed(p),
                new Effect("attack", "+4", Attributes.ATTACK_DAMAGE, 4, AttributeModifier.Operation.ADD_VALUE),
                new Effect("max_health", "+10", Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE));
        add("evolved_one", "unique", 0, p -> StatusData.counter(p, "evolutions") >= 1,
                new Effect("max_health", "+4", Attributes.MAX_HEALTH, 4, AttributeModifier.Operation.ADD_VALUE),
                new Effect("armor", "+2", Attributes.ARMOR, 2, AttributeModifier.Operation.ADD_VALUE));
        add("wind_runner", "extra", 2, p -> Training.level(p, Attr.SPEED) >= 15,
                new Effect("speed", "+5%", Attributes.MOVEMENT_SPEED, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        add("magicule_master", "extra", 4, p -> Training.level(p, Attr.MAGIC_CONTROL) >= 10,
                new Effect("magic_cost", "-10%", null, 0, null));
        add("monster_slayer", "common", 5, p -> StatusData.counter(p, "monsters_killed") >= 50,
                new Effect("attack", "+1", Attributes.ATTACK_DAMAGE, 1, AttributeModifier.Operation.ADD_VALUE));
        add("reincarnated", "common", 3, p -> com.brightwebs.brightcore.api.race.RaceAPI.race(p).isPresent(),
                new Effect("max_health", "+2", Attributes.MAX_HEALTH, 2, AttributeModifier.Operation.ADD_VALUE));
    }

    private static final Identifier MOD = Identifier.fromNamespaceAndPath(JayceeMod.ID, "title_effect");

    private Titles() {}

    public static Title get(String id) { return ALL.get(id); }

    public static boolean owns(Player p, String id) { return StatusData.titles(p).contains(id); }

    public static void grant(ServerPlayer p, String id) {
        if (owns(p, id)) return;
        p.setData(StatusData.TITLES, StatusData.withAdded(StatusData.titles(p), id));
        p.sendSystemMessage(Component.translatable("jaycee.title.acquired", Component.translatable("title.jaycee." + id))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (StatusData.title(p).isEmpty()) equip(p, id);
    }

    public static void equip(ServerPlayer p, String id) {
        if (!id.isEmpty() && !owns(p, id)) return;
        p.setData(StatusData.TITLE, id);
        apply(p);
        p.refreshDisplayName();
    }

    public static void setPublic(ServerPlayer p, boolean on) {
        p.setData(StatusData.TITLE_PUBLIC, on);
        p.refreshDisplayName();
    }

    /** Applies the equipped title's attribute effects (and clears the old ones). */
    public static void apply(Player p) {
        for (Holder<Attribute> a : List.of(Attributes.ATTACK_DAMAGE, Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.MOVEMENT_SPEED)) {
            AttributeInstance i = p.getAttribute(a);
            if (i != null) i.removeModifier(MOD);
        }
        Title t = ALL.get(StatusData.title(p));
        if (t == null) return;
        for (Effect e : t.effects()) {
            if (e.attribute() == null) continue;
            AttributeInstance i = p.getAttribute(e.attribute());
            if (i != null && i.getModifier(MOD) == null) i.addTransientModifier(new AttributeModifier(MOD, e.amount(), e.op()));
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("Titles.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 0) return;
            for (Title t : ALL.values()) if (!owns(p, t.id()) && t.unlock().test(p)) grant(p, t.id());
            Title t = ALL.get(StatusData.title(p));
            if (t != null && t.effects().stream().anyMatch(e -> e.attribute() != null
                    && p.getAttribute(e.attribute()) != null && p.getAttribute(e.attribute()).getModifier(MOD) == null)) apply(p);
        });
    }

    /** "[Title] Name" above the head and in chat while the title is public. */
    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat event) {
        com.brightwebs.brightcore.api.util.Safe.run("Titles.onName", () -> {
            Player p = event.getEntity();
            String id = StatusData.title(p);
            if (id.isEmpty() || !StatusData.titlePublic(p) || !ALL.containsKey(id)) return;
            event.setDisplayname(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("title.jaycee." + id).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(event.getDisplayname()));
        });
    }
}

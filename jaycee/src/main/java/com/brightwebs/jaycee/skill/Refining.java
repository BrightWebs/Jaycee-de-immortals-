package com.brightwebs.jaycee.skill;

import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// tensura's refining: stuff in a skill's storage that can be smelted gets refined one at a time,
// no furnace or fuel. great sage does its own storage automatically, predator's analysis does the
// stomach while it's open
public final class Refining {
    public static final int SAGE_SIZE = 27, TICKS_PER_ITEM = 10;
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);
    public static final Supplier<AttachmentType<List<ItemStack>>> SAGE = ATTACHMENTS.register("sage_storage",
            () -> AttachmentType.<List<ItemStack>>builder(() -> List.of())
                    .serialize(ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items"))
                    .copyOnDeath()
                    .build());
    // storage menus that are open right now, so refining updates what the player sees
    private static final java.util.Map<UUID, SimpleContainer> OPEN_SAGE = new java.util.HashMap<>(), OPEN_STOMACH = new java.util.HashMap<>();

    private Refining() {}

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    private static List<ItemStack> load(ServerPlayer p, Supplier<AttachmentType<List<ItemStack>>> type, int size) {
        List<ItemStack> list = new ArrayList<>(p.getData(type));
        while (list.size() < size) list.add(ItemStack.EMPTY);
        return list;
    }

    // great sage's refining storage
    static void openSage(ServerPlayer p) {
        SimpleContainer c = new SimpleContainer(load(p, SAGE, SAGE_SIZE).toArray(new ItemStack[0])) {
            @Override public void setChanged() { super.setChanged(); p.setData(SAGE, List.copyOf(getItems())); }
        };
        p.openMenu(new SimpleMenuProvider((id, inv, who) -> ChestMenu.threeRows(id, inv, c),
                Component.translatable("jaycee.skill.refine.title")));
        OPEN_SAGE.put(p.getUUID(), c);
    }

    // predator's analysis: open the stomach and refine it while it's open
    static void openStomachRefining(ServerPlayer p) {
        OPEN_STOMACH.put(p.getUUID(), Stomach.open(p));
    }

    static void tick(ServerPlayer p) {
        if (p.tickCount % TICKS_PER_ITEM != 0) return;
        boolean menuOpen = p.containerMenu instanceof ChestMenu;
        if (!menuOpen) { OPEN_SAGE.remove(p.getUUID()); OPEN_STOMACH.remove(p.getUUID()); }
        if (GreatSage.hasSage(p)) refineOne(p, SAGE, SAGE_SIZE, OPEN_SAGE.get(p.getUUID()));
        SimpleContainer stomach = OPEN_STOMACH.get(p.getUUID());
        if (stomach != null) refineOne(p, Stomach.STOMACH, Stomach.SIZE, stomach);
    }

    // smelts one item from the first smeltable stack, result stays in the same storage
    // works on the open menu's container if there is one (its setChanged saves it)
    private static void refineOne(ServerPlayer p, Supplier<AttachmentType<List<ItemStack>>> type, int size, SimpleContainer open) {
        ServerLevel level = (ServerLevel) p.level();
        SimpleContainer c = open != null ? open : new SimpleContainer(load(p, type, size).toArray(new ItemStack[0]));
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) continue;
            SingleRecipeInput input = new SingleRecipeInput(in.copyWithCount(1));
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack out = recipe.get().value().assemble(input);
            if (out.isEmpty() || !c.canAddItem(out)) continue;
            in.shrink(1);
            c.addItem(out.copy());
            if (open != null) c.setChanged(); else p.setData(type, List.copyOf(c.getItems()));
            if (p.containerMenu instanceof ChestMenu menu) menu.broadcastChanges();
            return;
        }
    }
}

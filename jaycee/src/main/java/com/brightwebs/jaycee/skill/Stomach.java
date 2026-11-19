package com.brightwebs.jaycee.skill;

import com.brightwebs.jaycee.JayceeMod;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Predator's Stomach: 54 slots of storage that only its owner can open, kept through death. */
public final class Stomach {
    public static final int SIZE = 54;
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JayceeMod.ID);

    public static final Supplier<AttachmentType<List<ItemStack>>> STOMACH = ATTACHMENTS.register("stomach",
            () -> AttachmentType.<List<ItemStack>>builder(() -> List.of())
                    .serialize(ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items"))
                    .copyOnDeath()
                    .build());

    private Stomach() {}

    public static void init(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    private static List<ItemStack> items(ServerPlayer p) {
        List<ItemStack> list = new ArrayList<>(p.getData(STOMACH));
        while (list.size() < SIZE) list.add(ItemStack.EMPTY);
        return list;
    }

    /** Puts an item in the Stomach; if it's full, drops it at the player's feet. */
    static void store(ServerPlayer p, ItemStack stack) {
        SimpleContainer c = new SimpleContainer(items(p).toArray(new ItemStack[0]));
        ItemStack left = c.addItem(stack);
        p.setData(STOMACH, List.copyOf(c.getItems()));
        if (!left.isEmpty()) {
            p.spawnAtLocation((net.minecraft.server.level.ServerLevel) p.level(), left);
            p.sendOverlayMessage(Component.translatable("jaycee.skill.stomach.full"));
        }
    }

    static SimpleContainer open(ServerPlayer p) {
        SimpleContainer c = new SimpleContainer(items(p).toArray(new ItemStack[0])) {
            @Override public void setChanged() {
                super.setChanged();
                p.setData(STOMACH, List.copyOf(getItems()));
            }
        };
        p.openMenu(new SimpleMenuProvider((id, inv, who) -> ChestMenu.sixRows(id, inv, c),
                Component.translatable("jaycee.skill.stomach.title")));
        return c;
    }
}

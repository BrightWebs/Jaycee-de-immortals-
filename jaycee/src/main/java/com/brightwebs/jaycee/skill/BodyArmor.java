package com.brightwebs.jaycee.skill;

import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.util.Safe;
import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

// body armor like tensura: press to grow scale armor into every empty armor slot (50 magicules),
// press again with all four filled to shed it. the scales aren't real items, they disappear if you
// take them off, throw them or drop them. each piece you wear gives mastery over time
@EventBusSubscriber(modid = JayceeMod.ID)
public final class BodyArmor {
    static final double COST = 50;
    private static final String TAG = "jaycee_body_armor";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Item[] PIECES = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};

    private BodyArmor() {}

    static boolean isScale(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(TAG);
    }

    private static ItemStack scale(Item item) {
        ItemStack s = new ItemStack(item);
        CompoundTag t = new CompoundTag();
        t.putBoolean(TAG, true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, Component.translatable("item.jaycee.body_armor"));
        s.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3E5A3A));
        return s;
    }

    static boolean press(ServerPlayer p, Ability a) {
        ServerLevel level = (ServerLevel) p.level();
        boolean full = true;
        for (EquipmentSlot slot : SLOTS) if (p.getItemBySlot(slot).isEmpty()) full = false;
        if (full) {
            boolean any = false;
            for (EquipmentSlot slot : SLOTS) if (isScale(p.getItemBySlot(slot))) { p.setItemSlot(slot, ItemStack.EMPTY); any = true; }
            if (!any) return false;
            level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1, 0.6f);
            com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.SHARD, p.position().add(0, 1, 0), 16, 0.25);
            return true;
        }
        if (!Tk.spend(p, a, COST)) return false;
        for (int i = 0; i < SLOTS.length; i++) if (p.getItemBySlot(SLOTS[i]).isEmpty()) p.setItemSlot(SLOTS[i], scale(PIECES[i]));
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1, 0.8f);
        com.brightwebs.brightcore.fx.Fx.spiral(level, com.brightwebs.brightcore.fx.BrightParticles.SHARD, p.position(), 0.7, 2, 2, 24, 0); com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.SPARK, p.position().add(0, 1, 0), 8, 0.2);
        return true;
    }

    // mastery for each scale piece worn, and scales outside the armor slots disappear
    static void tick(ServerPlayer p, Ability a) {
        if (p.tickCount % Tk.SKILL_TICK == 0) {
            for (EquipmentSlot slot : SLOTS) if (isScale(p.getItemBySlot(slot))) Tk.activatedTime(p, a);
        }
        if (p.tickCount % 10 == 0) {
            var inv = p.getInventory();
            for (int i = 0; i < 36; i++) if (isScale(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);  // hotbar + main
            if (isScale(p.getOffhandItem())) p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            if (isScale(p.containerMenu.getCarried())) p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        Safe.run("BodyArmor.onToss", () -> {
            if (isScale(event.getEntity().getItem())) { event.getEntity().discard(); event.setCanceled(true); }
        });
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        Safe.run("BodyArmor.onDrops", () -> event.getDrops().removeIf((ItemEntity e) -> isScale(e.getItem())));
    }
}

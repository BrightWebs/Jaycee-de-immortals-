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
    private static final String[] NAMES = {"helmet", "chestplate", "leggings", "boots"};
    // tensura's armorsaurus numbers: 5 / 10 / 8 / 5 armor, 4 toughness a piece, knockback resistance 0.5 over the full set
    private static final int[] ARMOR = {5, 10, 8, 5};
    private static final net.minecraft.world.entity.EquipmentSlotGroup[] GROUPS = {net.minecraft.world.entity.EquipmentSlotGroup.HEAD,
            net.minecraft.world.entity.EquipmentSlotGroup.CHEST, net.minecraft.world.entity.EquipmentSlotGroup.LEGS, net.minecraft.world.entity.EquipmentSlotGroup.FEET};
    // our own look for it (assets/jaycee/equipment/body_armor.json), not dyed leather anymore
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> LOOK =
            net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(JayceeMod.ID, "body_armor"));

    public static net.minecraft.resources.Identifier model(String piece) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(JayceeMod.ID, "body_armor_" + piece);
    }

    private BodyArmor() {}

    static boolean isScale(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(TAG);
    }

    private static ItemStack scale(int i) {
        ItemStack s = new ItemStack(PIECES[i]);
        mark(s, "item.jaycee.body_armor_" + NAMES[i], NAMES[i]);
        s.set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(SLOTS[i]).setAsset(LOOK)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE).build());
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(JayceeMod.ID, "body_armor_" + NAMES[i]);
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, ARMOR[i],
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), GROUPS[i])
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 4,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), GROUPS[i])
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 0.125,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), GROUPS[i])
                .build());
        return s;
    }

    // tensura's armorsaurus gauntlet: 7 attack + its tier's 16, slow swing, and you can block with it
    private static ItemStack gauntlet() {
        ItemStack s = new ItemStack(Items.SHIELD);
        mark(s, "item.jaycee.body_armor_gauntlet", "gauntlet");
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID, 22, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID, -2.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build());
        return s;
    }

    // the parts every scale piece shares: our tag, name, look, and it never breaks
    private static void mark(ItemStack s, String name, String piece) {
        CompoundTag t = new CompoundTag();
        t.putBoolean(TAG, true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.ITEM_NAME, Component.translatable(name));
        s.set(DataComponents.ITEM_MODEL, model(piece));
        s.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
    }

    static boolean isGauntlet(ItemStack s) {
        return isScale(s) && model("gauntlet").equals(s.get(DataComponents.ITEM_MODEL));
    }

    static boolean press(ServerPlayer p, Ability a) {
        ServerLevel level = (ServerLevel) p.level();
        // shift: grow or put away the gauntlet on your main hand, like tensura
        if (p.isShiftKeyDown()) {
            ItemStack hand = p.getMainHandItem();
            if (isGauntlet(hand)) {
                p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1, 0.6f);
                return true;
            }
            if (!hand.isEmpty()) { p.sendOverlayMessage(Component.translatable("jaycee.skill.body_armor.hand_full")); return false; }
            if (!Tk.spend(p, a, COST)) return false;
            p.setItemSlot(EquipmentSlot.MAINHAND, gauntlet());
            level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1, 0.7f);
            com.brightwebs.brightcore.fx.Fx.burst(level, com.brightwebs.brightcore.fx.BrightParticles.SHARD, p.getEyePosition().add(p.getLookAngle().scale(0.6)), 12, 0.15);
            return true;
        }
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
        for (int i = 0; i < SLOTS.length; i++) if (p.getItemBySlot(SLOTS[i]).isEmpty()) p.setItemSlot(SLOTS[i], scale(i));
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
            int held = inv.getSelectedSlot();
            for (int i = 0; i < 36; i++) {
                // the gauntlet's allowed in the hand you're holding it in, nothing else
                if (i == held && isGauntlet(inv.getItem(i))) continue;
                if (isScale(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);  // hotbar + main
            }
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

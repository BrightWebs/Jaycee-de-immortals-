package com.brightwebs.jaycee;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import com.brightwebs.jaycee.gameplay.Gameplay;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.brightwebs.jaycee.config.HudConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(JayceeMod.ID)
public final class JayceeMod {
 public static final String ID="jaycee";
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 public static final DeferredItem<Item> DIVINE_TOTEM=ITEMS.register("divine_totem",()->new com.brightwebs.jaycee.gameplay.DivineTotemService.Totem(new Item.Properties().stacksTo(1)));
 public static final DeferredItem<Item> ESSENCE=ITEMS.register("jayces_essence",()->new Essence(new Item.Properties().stacksTo(16)));
 public JayceeMod(IEventBus bus,ModContainer container){ ITEMS.register(bus); Gameplay.register(bus); container.registerConfig(ModConfig.Type.CLIENT,HudConfig.SPEC); }
 public static boolean unlocked(Player p){return PlayerState.get(p).unlocked();}
 public static final class Essence extends Item {
  public Essence(Properties properties){super(properties);}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
    ItemStack stack=player.getItemInHand(hand);
    if(!level.isClientSide)player.displayClientMessage(Component.literal("Legacy Essence item disabled. Essence is intangible; use the authorized command for Jaycee's three awakenings."),true);
    return InteractionResultHolder.fail(stack);
  }
 }
}

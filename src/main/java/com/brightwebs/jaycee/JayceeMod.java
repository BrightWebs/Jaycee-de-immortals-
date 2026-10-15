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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.commands.Commands;
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
 public static final DeferredItem<Item> ESSENCE=ITEMS.register("jayces_essence",()->new Essence(new Item.Properties().stacksTo(16)));
 public JayceeMod(IEventBus bus,ModContainer container){ ITEMS.register(bus); Gameplay.register(bus); container.registerConfig(ModConfig.Type.CLIENT,HudConfig.SPEC); NeoForge.EVENT_BUS.addListener(this::commands); }
 private void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("jayceeessence").requires(s->s.hasPermission(2)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.getInventory().add(new ItemStack(ESSENCE.get(),3));c.getSource().sendSuccess(()->Component.literal("Received 3 Jayce's Essences"),false);return 1;}));}
 public static boolean unlocked(Player p){return PlayerState.get(p).unlocked();}
 public static final class Essence extends Item {
  public Essence(Properties properties){super(properties);}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
    ItemStack stack=player.getItemInHand(hand);
    if(!level.isClientSide){int n=Gameplay.absorbEssence(player);if(!player.getAbilities().instabuild)stack.shrink(1);player.displayClientMessage(Component.literal(n==3?"Jaycee UI unlocked. Press M.":"Essence absorbed: "+n+" / 3"),true);}
    return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
 }
}

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

@Mod(JayceeMod.ID)
public final class JayceeMod {
 public static final String ID="jaycee";
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 public static final DeferredItem<Item> ESSENCE=ITEMS.register("jayces_essence",()->new Essence(new Item.Properties().stacksTo(16)));
 public JayceeMod(IEventBus bus){ ITEMS.register(bus); NeoForge.EVENT_BUS.addListener(this::commands); PhaseOneGameplay.register(); DivineBestowal.register(); JayceeRoleSync.attach(); bus.addListener(JayceeRoleSync::register); }
 private void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("jayceeessence").requires(s->s.hasPermission(2)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.getInventory().add(new ItemStack(ESSENCE.get(),3));c.getSource().sendSuccess(()->Component.literal("Received 3 Jayce's Essences"),false);return 1;}));}
 public static boolean unlocked(Player p){return p.getPersistentData().getInt("JayceeConsumed")>=3;}
 public static final class Essence extends Item {
  public Essence(Properties properties){super(properties);}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
    ItemStack stack=player.getItemInHand(hand);
    if(!level.isClientSide){
      int previous=player.getPersistentData().getInt("JayceeConsumed");
      player.getPersistentData().putInt("JayceeConsumed",Math.min(3,previous+1));
      if(player instanceof ServerPlayer serverPlayer) { PhaseOneGameplay.earn(serverPlayer,500); if(previous<3) JayceeAuthority.afterCore(serverPlayer); JayceeRoleSync.send(serverPlayer); }
      // Divine essence must be consumed even in Creative Mode.
      stack.shrink(1);
      player.displayClientMessage(Component.literal(previous==0
          ?"First divine core absorbed (1 / 3)."
          :(previous==2 ? "Third core absorbed. Jaycee advancement and role checked. Press M." : "Divine essence absorbed. Keep collecting cores." )),true);
    }
    return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
 }
}

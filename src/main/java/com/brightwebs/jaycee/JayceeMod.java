package com.brightwebs.jaycee;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
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
 public JayceeMod(IEventBus bus){ ITEMS.register(bus); bus.addListener(JayceeNetwork::register); NeoForge.EVENT_BUS.addListener(this::commands); NeoForge.EVENT_BUS.addListener(this::login); NeoForge.EVENT_BUS.addListener(this::clonePlayer); NeoForge.EVENT_BUS.addListener(this::respawn); NeoForge.EVENT_BUS.addListener(this::dimension); }
 private void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)JayceeNetwork.sync(p);}
 private void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)JayceeNetwork.sync(p);}
 private void respawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)JayceeNetwork.sync(p);}
 private void clonePlayer(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e){
  var old=e.getOriginal().getPersistentData();var data=e.getEntity().getPersistentData();
  data.putInt("JayceeConsumed",old.getInt("JayceeConsumed"));data.putInt("JayceePage",old.getInt("JayceePage"));data.putIntArray("JayceeSlots",JayceeNetwork.slots(e.getOriginal()));
 }
 private void commands(RegisterCommandsEvent event){
  event.getDispatcher().register(Commands.literal("jayceeessence")
   .requires(source->source.hasPermission(2)).executes(context->{
    ServerPlayer player=context.getSource().getPlayerOrException();
    player.getInventory().add(new ItemStack(ESSENCE.get(),3));
    context.getSource().sendSuccess(()->Component.literal("Received 3 Jayce's Essences"),false);
    return 1;
   }));
  event.getDispatcher().register(Commands.literal("jayceestatus").executes(context->{
   ServerPlayer player=context.getSource().getPlayerOrException();
   int count=player.getPersistentData().getInt("JayceeConsumed");
   context.getSource().sendSuccess(()->Component.literal(
    "Jaycee Essence "+count+" / 3; UI "+(unlocked(player)?"unlocked":"locked")+
    "; Essence update: HOLD RIGHT-CLICK TO EAT (2026-10-15)"),false);
   return 1;
  }));
 }
 public static boolean unlocked(Player p){return p.getPersistentData().getInt("JayceeConsumed")>=3;}
 public static final class Essence extends Item {
  public Essence(Properties properties){super(properties);}
  // A genuine hold-right-click consumable. No food/hunger requirement.
  @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.EAT;}
  @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 24;}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
   ItemStack stack=player.getItemInHand(hand);
   if(player.getPersistentData().getInt("JayceeConsumed")>=3){
    if(!level.isClientSide)player.displayClientMessage(
     Component.literal("Jaycee is already awakened. Press M."),true);
    return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
   }
   player.startUsingItem(hand);
   return InteractionResultHolder.consume(stack);
  }
  @Override public InteractionResult useOn(UseOnContext context){
   Player player=context.getPlayer();
   if(player==null)return InteractionResult.PASS;
   use(context.getLevel(),player,context.getHand());
   return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
  }
  @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
   if(level.isClientSide||!(entity instanceof ServerPlayer player))return stack;
   int count=player.getPersistentData().getInt("JayceeConsumed");
   if(count>=3)return stack;
   // Server grants the exclusive first-consumer advancement on actual absorption.
   FirstJayceeAwakening.onEssenceAbsorbed(player);
   int next=count+1;
   player.getPersistentData().putInt("JayceeConsumed",next);
   if(!player.getAbilities().instabuild)stack.shrink(1);
   JayceeNetwork.sync(player);
   player.displayClientMessage(Component.literal(next==3?
    "Jaycee awakened! Press M to open the interface.":
    "Jayce's Essence absorbed: "+next+" / 3"),true);
   return stack;
  }
 }
}
